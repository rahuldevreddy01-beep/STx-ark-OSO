package com.example.data

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.hardware.MiuiFreeformLauncher
import com.example.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.util.concurrent.ConcurrentHashMap

class LauncherAppsRepository(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun loadInstalledApps(
        pinnedPackages: Set<String>,
        mutedAlertPackages: Set<String>,
        disabledSyncPackages: Set<String>
    ): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        }.getOrDefault(emptyList())

        resolveInfos
            .asSequence()
            .filter { info ->
                val act = info.activityInfo ?: return@filter false
                !act.packageName.isNullOrBlank() && act.packageName != context.packageName
            }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name.orEmpty()}" }
            .mapNotNull { info ->
                runCatching {
                    val actInfo = info.activityInfo ?: return@runCatching null
                    val pkg = actInfo.packageName ?: return@runCatching null
                    val appInfo = actInfo.applicationInfo ?: return@runCatching null
                    val label = runCatching { info.loadLabel(pm)?.toString() }.getOrNull()
                        ?.ifBlank { pkg } ?: pkg
                    val pkgInfo = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(
                                pkg,
                                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                        }
                    }.getOrNull()

                    val activityName = actInfo.name.orEmpty()
                    val iconKey = "$pkg/$activityName"
                    val iconBitmap = iconCache[iconKey] ?: runCatching {
                        drawableToImageBitmap(info.loadIcon(pm))
                    }.getOrNull()?.also { iconCache[iconKey] = it }

                    val categoryTag = resolveCategoryTag(appInfo)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    InstalledApp(
                        label = label,
                        packageName = pkg,
                        activityName = activityName,
                        versionName = pkgInfo?.versionName ?: "1.0",
                        targetSdk = appInfo.targetSdkVersion,
                        isSystemApp = isSystem,
                        permissionsCount = pkgInfo?.requestedPermissions?.size ?: 0,
                        icon = iconBitmap,
                        categoryTag = categoryTag,
                        isPinnedToDock = pkg in pinnedPackages,
                        alertsEnabled = pkg !in mutedAlertPackages,
                        bgSyncEnabled = pkg !in disabledSyncPackages
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchFullscreen(app: InstalledApp): Boolean {
        if (app.packageName.isBlank()) return false
        if (app.activityName.isNotBlank()) {
            val launchedDirect = runCatching {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(app.packageName, app.activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launchedDirect) return true
        }
        return runCatching {
            val fallback = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (fallback != null) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Requests MIUI/HyperOS Floating Window (`--windowingMode 5`) using Shizuku / Root shell first
     * (when active) so MIUI 12.5 opens directly into a floating window without opening fullscreen first,
     * falling back seamlessly to MiuiMultiWindowUtils + WindowingMode 5 ActivityOptions.
     */
    suspend fun launchWindowedAsync(app: InstalledApp): Boolean {
        if (app.packageName.isBlank()) return false
        if (MiuiFreeformLauncher.hasActivePrivilegedShell()) {
            val shellOk = withContext(Dispatchers.IO) {
                MiuiFreeformLauncher.tryPrivilegedWindowMode5Launch(
                    context = context,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            }
            if (shellOk) return true
        }
        return withContext(Dispatchers.Main.immediate) {
            launchWindowedNative(app)
        }
    }

    fun launchWindowed(app: InstalledApp): Boolean {
        if (app.packageName.isBlank()) return false
        if (MiuiFreeformLauncher.hasActivePrivilegedShell()) {
            val shellOk = MiuiFreeformLauncher.tryPrivilegedWindowMode5Launch(
                context = context,
                packageName = app.packageName,
                activityName = app.activityName
            )
            if (shellOk) return true
        }
        return launchWindowedNative(app)
    }

    private fun launchWindowedNative(app: InstalledApp): Boolean {
        return runCatching {
            val comp = MiuiFreeformLauncher.resolveLaunchComponent(
                context = context,
                packageName = app.packageName,
                knownActivityName = app.activityName
            )
            val intent = if (comp != null) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = comp
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?: return false
            }
            MiuiFreeformLauncher.startFloatingActivity(
                context = context,
                intent = intent,
                targetPackage = app.packageName,
                allowMultipleTask = false
            )
        }.getOrElse {
            launchFullscreen(app)
        }
    }

    fun openAppSystemInfo(packageName: String) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            MiuiFreeformLauncher.startFloatingActivity(
                context = context,
                intent = intent,
                targetPackage = "com.android.settings",
                allowMultipleTask = false
            )
        }
    }

    fun openDefaultLauncherSettings() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    fun openWebSearch(query: String) {
        runCatching {
            val url = if (query.isBlank()) {
                "https://www.google.com"
            } else {
                "https://www.google.com/search?q=${Uri.encode(query)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun resolveCategoryTag(info: ApplicationInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return when (info.category) {
                ApplicationInfo.CATEGORY_GAME -> "GAME"
                ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
                ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
                ApplicationInfo.CATEGORY_IMAGE -> "MEDIA"
                ApplicationInfo.CATEGORY_SOCIAL -> "COMMS"
                ApplicationInfo.CATEGORY_NEWS -> "BRIEF"
                ApplicationInfo.CATEGORY_MAPS -> "NAV"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "WORK"
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> "SYS"
                else -> if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "SYSTEM" else "APP"
            }
        }
        return if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "SYSTEM" else "APP"
    }

    private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        return try {
            val size = 64
            if (drawable is BitmapDrawable) {
                val bmp = drawable.bitmap
                val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    bmp?.config == Bitmap.Config.HARDWARE
                if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0 && !isHardware) {
                    val scaled = Bitmap.createScaledBitmap(bmp, size, size, true)
                    return scaled.asImageBitmap()
                }
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

}
