package com.example.hardware

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class LauncherPermissionStatus(
    val id: String,
    val title: String,
    val description: String,
    val permissions: Array<String>,
    val granted: Boolean
)

/**
 * Central registry & runtime safety helper for all STX OS Launcher hardware permissions.
 */
object LauncherPermissionManager {

    fun allRuntimePermissionsForSdk(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    fun missingRuntimePermissions(context: Context): Array<String> {
        return allRuntimePermissionsForSdk()
            .filter { perm -> !isPermissionGranted(context, perm) }
            .toTypedArray()
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return runCatching {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun hasAnyLocationPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun hasActivityRecognitionPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            isPermissionGranted(context, Manifest.permission.ACTIVITY_RECOGNITION)
    }

    fun hasBluetoothPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }

    fun hasAudioPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    fun canDrawOverlays(context: Context): Boolean {
        return runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
    }

    fun openAppPermissionSettings(context: Context): Boolean {
        return runCatching {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun isDefaultHomeLauncher(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return@runCatching roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                }
            }
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolved?.activityInfo?.packageName == context.packageName
        }.getOrDefault(false)
    }

    fun getPermissionStatuses(context: Context): List<LauncherPermissionStatus> {
        return buildList {
            add(
                LauncherPermissionStatus(
                    id = "camera",
                    title = "CAMERA & LED TORCH",
                    description = "Live optical feed, zoom optics & hardware flash",
                    permissions = arrayOf(Manifest.permission.CAMERA),
                    granted = hasCameraPermission(context)
                )
            )
            add(
                LauncherPermissionStatus(
                    id = "mic",
                    title = "MICROPHONE & VOICE AI",
                    description = "Voice wake, speech commands & STX orb word glow",
                    permissions = arrayOf(Manifest.permission.RECORD_AUDIO),
                    granted = hasAudioPermission(context)
                )
            )
            add(
                LauncherPermissionStatus(
                    id = "location",
                    title = "LOCATION & WIFI SSID",
                    description = "Exact Wi-Fi SSID, RSSI link telemetry & RF scan",
                    permissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    granted = hasAnyLocationPermission(context)
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(
                    LauncherPermissionStatus(
                        id = "activity",
                        title = "PHYSICAL ACTIVITY",
                        description = "Hardware step counter & kinetic health metrics",
                        permissions = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        granted = hasActivityRecognitionPermission(context)
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    LauncherPermissionStatus(
                        id = "bluetooth",
                        title = "BLUETOOTH & SCAN",
                        description = "Paired audio/devices & nearby RF status",
                        permissions = arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        granted = hasBluetoothPermission(context)
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    LauncherPermissionStatus(
                        id = "notifications",
                        title = "NOTIFICATIONS",
                        description = "Persistent floating overlay & system alerts",
                        permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        granted = hasNotificationPermission(context)
                    )
                )
            }
        }
    }
}
