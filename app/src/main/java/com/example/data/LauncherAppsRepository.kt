package com.example.data

import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val appInfo = info.activityInfo.applicationInfo
                val label = info.loadLabel(pm).toString().ifBlank { pkg }
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

                val activityName = info.activityInfo.name.orEmpty()
                val iconKey = "$pkg/$activityName"
                val iconBitmap = iconCache[iconKey] ?: runCatching {
                    drawableToImageBitmap(info.loadIcon(pm))
                }.getOrNull()?.also { iconCache[iconKey] = it }

                val categoryTag = resolveCategoryTag(appInfo)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                InstalledApp(
                    label = label,
                    packageName = pkg,
                    activityName = info.activityInfo.name,
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
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchFullscreen(app: InstalledApp): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        }.getOrElse {
            runCatching {
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
    }

    /**
     * Requests Android Freeform / MIUI Floating Window mode via ActivityOptions.
     * Uses WINDOWING_MODE_FREEFORM (5) + centered Rect launch bounds + MIUI freeform flags.
     */
    fun launchWindowed(app: InstalledApp): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                // MIUI / HyperOS floating window hints (safe extras)
                putExtra("miui.intent.extra.OPEN_IN_FREEFORM", true)
            }

            // Apply MIUI internal MiuiIntent freeform bit (0x00000800) only on Xiaomi/MIUI/HyperOS devices
            val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Redmi", ignoreCase = true) ||
                Build.BRAND.equals("POCO", ignoreCase = true)
            if (isXiaomi) {
                intent.addFlags(0x00000800)
            }

            val dm = context.resources.displayMetrics
            val w = (minOf(dm.widthPixels, dm.heightPixels) * 0.78f).toInt().coerceAtLeast(360)
            val h = (dm.heightPixels * 0.72f).toInt().coerceAtLeast(420)
            val left = (dm.widthPixels - w) / 2
            val top = (dm.heightPixels - h) / 2
            val bounds = Rect(left, top, left + w, top + h)

            val options = ActivityOptions.makeBasic().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setLaunchBounds(bounds)
                }
                // Request WINDOWING_MODE_FREEFORM (5) on AOSP & MIUI Android 10–15
                runCatching {
                    val method = ActivityOptions::class.java.getMethod(
                        "setLaunchWindowingMode",
                        Int::class.javaPrimitiveType
                    )
                    method.invoke(this, WINDOWING_MODE_FREEFORM)
                }
            }

            context.startActivity(intent, options.toBundle())
            true
        }.getOrElse {
            launchFullscreen(app)
        }
    }

    fun openAppSystemInfo(packageName: String) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
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

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        if (drawable is BitmapDrawable) {
            val bmp = drawable.bitmap
            val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                bmp?.config == Bitmap.Config.HARDWARE
            if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0 && !isHardware) {
                val scaled = Bitmap.createScaledBitmap(bmp, 96, 96, true)
                return scaled.asImageBitmap()
            }
        }
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    companion object {
        private const val WINDOWING_MODE_FREEFORM = 5
    }
}
