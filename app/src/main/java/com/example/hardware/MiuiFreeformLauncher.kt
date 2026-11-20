package com.example.hardware

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import java.lang.ref.WeakReference

/**
 * Helper to launch Settings panels (Wi-Fi, Bluetooth) and apps into Xiaomi MIUI/HyperOS
 * floating windows or AOSP freeform windows with portrait-proportioned bounds so launched
 * windows never get stuck in confused landscape/fullscreen orientation.
 */
object MiuiFreeformLauncher {

    private const val WINDOWING_MODE_FREEFORM = 5

    @Volatile
    private var hostActivityRef: WeakReference<Activity>? = null

    fun setHostActivity(activity: Activity?) {
        hostActivityRef = if (activity != null) WeakReference(activity) else null
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

    fun startFloatingActivity(
        context: Context,
        intent: Intent,
        targetPackage: String? = null,
        allowMultipleTask: Boolean = false
    ): Boolean {
        val launchCtx = activeLaunchContext(context)
        val resolvedPkg = targetPackage
            ?: intent.component?.packageName
            ?: intent.`package`
            ?: runCatching {
                launchCtx.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
            }.getOrNull()

        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            if (allowMultipleTask) {
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            putExtra("miui.intent.extra.OPEN_IN_FREEFORM", true)
            putExtra("android.intent.extra.FREEFORM_WINDOW", true)
        }

        val options = buildFloatingOptions(launchCtx, resolvedPkg)
        val launchedFreeform = runCatching {
            launchCtx.startActivity(intent, options.toBundle())
            true
        }.getOrDefault(false)

        if (launchedFreeform) return true

        return runCatching {
            val fallbackIntent = Intent(intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchCtx.startActivity(fallbackIntent)
            true
        }.getOrDefault(false)
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
}
