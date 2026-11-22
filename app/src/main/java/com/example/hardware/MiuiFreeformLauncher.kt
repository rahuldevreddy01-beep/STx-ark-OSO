package com.example.hardware

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

enum class ShizukuBridgeStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    NEEDS_PERMISSION,
    CONNECTING,
    ACTIVE
}

data class FloatingWindowBridgeTelemetry(
    val shizukuStatus: ShizukuBridgeStatus,
    val rootAvailable: Boolean,
    val enableFreeformSupport: Boolean,
    val forceResizableActivities: Boolean,
    val writeSecureSettingsGranted: Boolean
) {
    val isGlobalFreeformReady: Boolean
        get() = enableFreeformSupport && forceResizableActivities

    val isPrivilegedBridgeReady: Boolean
        get() = shizukuStatus == ShizukuBridgeStatus.ACTIVE || rootAvailable
}

/**
 * Helper to launch Settings panels (Wi-Fi, Bluetooth) and apps directly into Xiaomi MIUI 12.5 / HyperOS
 * native Windowing Mode 5 (Floating / Freeform Window) without opening fullscreen first.
 *
 * Execution priority for app launches:
 *   1. Shizuku Shell Bridge (UID 2000): `settings put global ...`, `cmd package resolve-activity --brief`,
 *      and `am start --windowingMode 5 -n <PKG>/<ACT>`.
 *   2. Root Shell (`su`): same exact `am start --windowingMode 5 -n <PKG>/<ACT>` command.
 *   3. Native MIUI `MiuiMultiWindowUtils` + `ActivityOptions.setLaunchWindowingMode(5)` (+ `Settings.Global`
 *      auto-arming when `WRITE_SECURE_SETTINGS` is granted).
 */
object MiuiFreeformLauncher {

    private const val WINDOWING_MODE_FREEFORM = 5
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val SHIZUKU_REQ_CODE = 5055

    private val safeNameRegex = Regex("^[A-Za-z0-9._$]+$")
    private val safeComponentRegex = Regex("^[A-Za-z0-9._$]+/[A-Za-z0-9._$]+$")

    @Volatile
    private var hostActivityRef: WeakReference<Activity>? = null

    @Volatile
    private var shizukuInitialized = false

    @Volatile
    private var shellService: IShizukuShellService? = null

    @Volatile
    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    @Volatile
    private var cachedRootSuPath: String? = null

    @Volatile
    private var rootChecked = false

    @Volatile
    private var onBridgeStateChanged: (() -> Unit)? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        maybeBindUserServiceLocked()
        onBridgeStateChanged?.invoke()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        onBridgeStateChanged?.invoke()
    }

    private val permResultListener = Shizuku.OnRequestPermissionResultListener { reqCode, grantResult ->
        if (reqCode == SHIZUKU_REQ_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
            maybeBindUserServiceLocked()
            runCatching { shellService?.enableGlobalFreeformSettings() }
            onBridgeStateChanged?.invoke()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bound = runCatching { IShizukuShellService.Stub.asInterface(service) }.getOrNull()
            shellService = bound
            if (bound != null) {
                runCatching { bound.enableGlobalFreeformSettings() }
            }
            onBridgeStateChanged?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            onBridgeStateChanged?.invoke()
        }
    }

    fun setHostActivity(activity: Activity?) {
        hostActivityRef = if (activity != null) WeakReference(activity) else null
        if (activity != null) {
            initShizukuBridge(activity)
        }
    }

    fun setOnBridgeStateChangedListener(listener: (() -> Unit)?) {
        onBridgeStateChanged = listener
    }

    fun initShizukuBridge(context: Context) {
        val appCtx = context.applicationContext ?: context
        if (userServiceArgs == null) {
            userServiceArgs = runCatching {
                Shizuku.UserServiceArgs(
                    ComponentName(appCtx.packageName, ShizukuShellService::class.java.name)
                )
                    .daemon(false)
                    .processNameSuffix("freeform_shell")
                    .debuggable(false)
                    .version(1)
            }.getOrNull()
        }
        if (!shizukuInitialized) {
            shizukuInitialized = true
            runCatching {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permResultListener)
            }
        }
        maybeBindUserServiceLocked()
    }

    private fun maybeBindUserServiceLocked() {
        val args = userServiceArgs ?: return
        runCatching {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED &&
                shellService == null
            ) {
                Shizuku.bindUserService(args, serviceConnection)
            }
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    SHIZUKU_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun hasRootSu(): Boolean {
        if (rootChecked) return cachedRootSuPath != null
        val candidates = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su"
        )
        cachedRootSuPath = candidates.firstOrNull { path ->
            runCatching { File(path).let { it.exists() && it.canExecute() } }.getOrDefault(false)
        }
        rootChecked = true
        return cachedRootSuPath != null
    }

    fun getBridgeTelemetry(context: Context): FloatingWindowBridgeTelemetry {
        val resolver = context.contentResolver
        val freeform = runCatching {
            Settings.Global.getInt(resolver, "enable_freeform_support", 0) == 1
        }.getOrDefault(false)
        val resizable = runCatching {
            Settings.Global.getInt(resolver, "force_resizable_activities", 0) == 1
        }.getOrDefault(false)
        val writeSecure = context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            PackageManager.PERMISSION_GRANTED

        val shizukuRunning = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val shizukuGranted = if (shizukuRunning) {
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        } else {
            false
        }
        val status = when {
            shellService != null && shizukuRunning -> ShizukuBridgeStatus.ACTIVE
            shizukuRunning && shizukuGranted -> {
                maybeBindUserServiceLocked()
                ShizukuBridgeStatus.CONNECTING
            }
            shizukuRunning -> ShizukuBridgeStatus.NEEDS_PERMISSION
            isShizukuInstalled(context) -> ShizukuBridgeStatus.NOT_RUNNING
            else -> ShizukuBridgeStatus.NOT_INSTALLED
        }

        return FloatingWindowBridgeTelemetry(
            shizukuStatus = status,
            rootAvailable = hasRootSu(),
            enableFreeformSupport = freeform,
            forceResizableActivities = resizable,
            writeSecureSettingsGranted = writeSecure
        )
    }

    /**
     * Attempts to enable global freeform settings directly via Shizuku, root `su`, or `WRITE_SECURE_SETTINGS`,
     * and requests Shizuku permission or opens Shizuku if needed. Returns a status message.
     */
    fun armFreeformBridge(context: Context): String {
        initShizukuBridge(context)
        val globalArmed = ensureGlobalFreeformSettings(context)
        val shizukuRunning = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

        if (shizukuRunning) {
            val granted = runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            if (!granted) {
                runCatching { Shizuku.requestPermission(SHIZUKU_REQ_CODE) }
                return "REQUESTED SHIZUKU AUTHORIZATION"
            }
            maybeBindUserServiceLocked()
            val svc = shellService
            if (svc != null) {
                val rc = runCatching { svc.enableGlobalFreeformSettings() }.getOrDefault(-1)
                if (rc == 0) {
                    return "SHIZUKU UID 2000 & FREEFORM ARMED"
                }
            }
            return "SHIZUKU CONNECTING · TAP AGAIN"
        }

        if (hasRootSu() && runSuEnableFreeform()) {
            return "ROOT SU & FREEFORM ARMED"
        }

        if (globalArmed) {
            return "FREEFORM GLOBALS ARMED (START SHIZUKU FOR FULL MIUI SUPPORT)"
        }

        if (isShizukuInstalled(context)) {
            runCatching {
                context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            return "OPENED SHIZUKU · START WIRELESS DEBUGGING SERVICE"
        }

        return "GRANT WRITE_SECURE_SETTINGS OR INSTALL SHIZUKU"
    }

    fun buildAdbSetupCommands(context: Context): String {
        val pkg = context.packageName
        return buildString {
            appendLine("adb shell settings put global enable_freeform_support 1")
            appendLine("adb shell settings put global force_resizable_activities 1")
            append("adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")
        }
    }

    fun openDeveloperOptions(context: Context) {
        val launchCtx = activeLaunchContext(context)
        val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching {
            launchCtx.startActivity(devIntent)
            true
        }.getOrDefault(false)
        if (!opened) {
            runCatching {
                launchCtx.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun ensureGlobalFreeformSettings(context: Context): Boolean {
        val resolver = context.contentResolver
        val curFreeform = runCatching { Settings.Global.getInt(resolver, "enable_freeform_support", 0) }.getOrDefault(0)
        val curResizable = runCatching { Settings.Global.getInt(resolver, "force_resizable_activities", 0) }.getOrDefault(0)
        if (curFreeform == 1 && curResizable == 1) return true

        val svc = shellService
        if (svc != null && runCatching { svc.enableGlobalFreeformSettings() == 0 }.getOrDefault(false)) {
            return true
        }

        val writtenNative = runCatching {
            val ok1 = Settings.Global.putInt(resolver, "enable_freeform_support", 1)
            val ok2 = Settings.Global.putInt(resolver, "force_resizable_activities", 1)
            ok1 && ok2
        }.getOrDefault(false)
        if (writtenNative) return true

        if (hasRootSu() && runSuEnableFreeform()) {
            return true
        }
        return false
    }

    fun isXiaomiOrHyperOs(): Boolean {
        val mfg = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        if (mfg.equals("Xiaomi", ignoreCase = true) ||
            brand.equals("Xiaomi", ignoreCase = true) ||
            brand.equals("Redmi", ignoreCase = true) ||
            brand.equals("POCO", ignoreCase = true)
        ) {
            return true
        }
        return runCatching {
            Class.forName("android.util.MiuiMultiWindowUtils")
            true
        }.getOrDefault(false)
    }

    fun hasActivePrivilegedShell(): Boolean {
        val shizukuReady = runCatching {
            Shizuku.pingBinder() && (shellService != null || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
        }.getOrDefault(false)
        return shizukuReady || hasRootSu()
    }

    /**
     * Resolves `<PACKAGE_NAME>/<ACTIVITY_NAME>` cleanly using PackageManager (in-process equivalent
     * of `cmd package resolve-activity --brief <PACKAGE_NAME>`).
     */
    fun resolveLaunchComponent(
        context: Context,
        packageName: String,
        knownActivityName: String = ""
    ): ComponentName? {
        if (packageName.isBlank()) return null
        if (knownActivityName.isNotBlank()) {
            return ComponentName(packageName, knownActivityName)
        }
        return runCatching {
            val pm = context.packageManager
            val query = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(query, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(query, 0)
            }
            val actInfo = resolved?.activityInfo
            if (actInfo != null && actInfo.name.isNotBlank()) {
                ComponentName(actInfo.packageName ?: packageName, actInfo.name)
            } else {
                pm.getLaunchIntentForPackage(packageName)?.component
            }
        }.getOrNull()
    }

    /**
     * Launches an app directly in MIUI 12.5 Windowing Mode 5 using Shizuku or Root `am start --windowingMode 5 -n`,
     * and returns `true` only if the privileged shell command succeeded without opening fullscreen.
     * Safe to run on `Dispatchers.IO`.
     */
    fun tryPrivilegedWindowMode5Launch(
        context: Context,
        packageName: String,
        activityName: String = ""
    ): Boolean {
        if (!safeNameRegex.matches(packageName)) return false
        val comp = resolveLaunchComponent(context, packageName, activityName)
        val actName = comp?.className.orEmpty().ifBlank { activityName }

        // Ensure UserService is bound if Shizuku is running and permitted
        if (shellService == null) {
            val shizukuPermitted = runCatching {
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            if (shizukuPermitted) {
                initShizukuBridge(context)
                for (attempt in 0 until 8) {
                    if (shellService != null) break
                    runCatching { Thread.sleep(45) }
                }
            }
        }

        // 1. Shizuku UID 2000 UserService
        val svc = shellService
        if (svc != null && runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            val rc = runCatching { svc.launchWindowMode5(packageName, actName) }.getOrDefault(-1)
            if (rc == 0) return true
        }

        // 2. Root su fallback running identical commands
        if (hasRootSu()) {
            if (runSuWindowMode5Launch(packageName, actName)) return true
        }

        return false
    }

    /**
     * Always computes a clean portrait-ratio (~9:16) centered floating window rectangle so
     * phones/tablets in landscape do not force launched apps into awkward wide boxes.
     */
    fun computeFloatingWindowBounds(context: Context): Rect {
        val dm = context.resources.displayMetrics
        val sw = dm.widthPixels.coerceAtLeast(360)
        val sh = dm.heightPixels.coerceAtLeast(360)
        val isLandscape = sw > sh

        val winH: Int
        val winW: Int
        if (isLandscape) {
            winH = (sh * 0.86f).toInt().coerceAtLeast(420).coerceAtMost(sh - 32)
            winW = (winH * 0.54f).toInt().coerceIn(320, (sw * 0.46f).toInt().coerceAtLeast(320))
        } else {
            winW = (sw * 0.82f).toInt().coerceAtLeast(320).coerceAtMost(sw - 32)
            winH = (sh * 0.68f).toInt().coerceIn(420, (sh * 0.84f).toInt().coerceAtLeast(420))
        }

        val left = ((sw - winW) / 2).coerceAtLeast(0)
        val top = ((sh - winH) / 2).coerceAtLeast(0)
        return Rect(left, top, left + winW, top + winH)
    }

    fun buildFloatingOptions(
        context: Context,
        targetPackage: String? = null
    ): ActivityOptions {
        val launchCtx = activeLaunchContext(context)
        val bounds = computeFloatingWindowBounds(launchCtx)

        val miuiOptions: ActivityOptions? = if (!targetPackage.isNullOrBlank()) {
            tryBuildMiuiOptions(launchCtx, targetPackage)
        } else {
            null
        }

        val options = miuiOptions ?: ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { options.setLaunchBounds(bounds) }
        }
        runCatching {
            val method = ActivityOptions::class.java.getMethod(
                "setLaunchWindowingMode",
                Int::class.javaPrimitiveType
            )
            method.invoke(options, WINDOWING_MODE_FREEFORM)
        }
        return options
    }

    fun buildFloatingOptionsBundle(
        context: Context,
        targetPackage: String? = null
    ): Bundle {
        val launchCtx = activeLaunchContext(context)
        val bounds = computeFloatingWindowBounds(launchCtx)
        val bundle = buildFloatingOptions(launchCtx, targetPackage).toBundle() ?: Bundle()
        // Explicitly populate AOSP ActivityOptions keys in case MIUI/Android 11 hidden API blocks reflection
        bundle.putInt("android.activity.windowingMode", WINDOWING_MODE_FREEFORM)
        if (!bundle.containsKey("android.activity.launchBounds")) {
            bundle.putParcelable("android.activity.launchBounds", bounds)
        }
        return bundle
    }

    fun startFloatingActivity(
        context: Context,
        intent: Intent,
        targetPackage: String? = null,
        allowMultipleTask: Boolean = true
    ): Boolean {
        val launchCtx = activeLaunchContext(context)
        val resolvedPkg = targetPackage
            ?: intent.component?.packageName
            ?: intent.`package`
            ?: runCatching {
                launchCtx.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
            }.getOrNull()

        if (!resolvedPkg.isNullOrBlank() && intent.component == null) {
            resolveLaunchComponent(launchCtx, resolvedPkg)?.let { comp ->
                intent.component = comp
            }
        }

        // Ensure global freeform support flags if we hold WRITE_SECURE_SETTINGS
        runCatching { ensureGlobalFreeformSettings(launchCtx) }

        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            if (allowMultipleTask) {
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            putExtra("miui.intent.extra.OPEN_IN_FREEFORM", true)
            putExtra("android.intent.extra.FREEFORM_WINDOW", true)
        }

        val optionsBundle = buildFloatingOptionsBundle(launchCtx, resolvedPkg)
        val launchedFreeform = runCatching {
            launchCtx.startActivity(intent, optionsBundle)
            true
        }.getOrDefault(false)

        if (launchedFreeform) return true

        // Retry without FLAG_ACTIVITY_MULTIPLE_TASK for singleTask-strict apps
        return runCatching {
            val fallbackIntent = Intent(intent).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            launchCtx.startActivity(fallbackIntent, optionsBundle)
            true
        }.getOrElse {
            runCatching {
                val plain = Intent(intent).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                launchCtx.startActivity(plain)
                true
            }.getOrDefault(false)
        }
    }

    private fun activeLaunchContext(fallback: Context): Context {
        val act = hostActivityRef?.get()
        return if (act != null && !act.isFinishing && !act.isDestroyed) act else fallback
    }

    private fun tryBuildMiuiOptions(context: Context, packageName: String): ActivityOptions? {
        return runCatching {
            val cls = Class.forName("android.util.MiuiMultiWindowUtils")
            runCatching {
                val m = cls.getMethod(
                    "getActivityOptions",
                    Context::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType
                )
                m.invoke(null, context, packageName, true, false) as? ActivityOptions
            }.getOrNull() ?: runCatching {
                val m = cls.getMethod(
                    "getActivityOptions",
                    Context::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                m.invoke(null, context, packageName, true) as? ActivityOptions
            }.getOrNull() ?: runCatching {
                val m = cls.getMethod(
                    "getActivityOptions",
                    Context::class.java,
                    String::class.java
                )
                m.invoke(null, context, packageName) as? ActivityOptions
            }.getOrNull()
        }.getOrNull()
    }

    private fun runSuEnableFreeform(): Boolean {
        val su = cachedRootSuPath ?: return false
        val script = "settings put global enable_freeform_support 1 && settings put global force_resizable_activities 1"
        return execSu(su, script).first == 0
    }

    private fun runSuWindowMode5Launch(packageName: String, activityName: String): Boolean {
        val su = cachedRootSuPath ?: return false
        val explicit = if (activityName.isNotBlank() && safeNameRegex.matches(activityName)) {
            "$packageName/$activityName"
        } else {
            ""
        }
        val component = explicit.ifEmpty {
            val (_, resolvedOut) = execSu(su, "cmd package resolve-activity --brief $packageName")
            resolvedOut.lineSequence().map { it.trim() }.lastOrNull {
                it.contains('/') && safeComponentRegex.matches(it)
            }.orEmpty()
        }
        if (!safeComponentRegex.matches(component)) return false
        val script =
            "settings put global enable_freeform_support 1; " +
                "settings put global force_resizable_activities 1; " +
                "am start --windowingMode 5 -n $component"
        val (rc, out) = execSu(su, script)
        return rc == 0 && !out.lowercase().contains("error:")
    }

    private fun execSu(suPath: String, command: String): Pair<Int, String> {
        return try {
            val proc = ProcessBuilder(listOf(suPath, "-c", command))
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
            if (!proc.waitFor(1500, TimeUnit.MILLISECONDS)) {
                proc.destroyForcibly()
                return -1 to "timeout"
            }
            proc.exitValue() to out.trim()
        } catch (_: Throwable) {
            -1 to "error"
        }
    }
}
