package com.example.model

import androidx.compose.ui.graphics.ImageBitmap

enum class StarkMode {
    PORTRAIT,
    HUB,
    HALF_DIAL,
    STX_AI
}

enum class AppLaunchMode {
    FLOATING,
    FULLSCREEN
}

enum class HubModule(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String
) {
    BRIEF("brief", "01", "BRIEF", "TACTICAL SUMMARY"),
    APPS("apps", "02", "APPS", "INSTALLED MATRIX"),
    HEALTH("health", "03", "HEALTH", "SENSORS & STEPS"),
    VAULT("vault", "04", "VAULT", "NOTES & TIMER"),
    CAMERA("camera", "05", "CAMERA", "OPTICAL HUD"),
    SEARCH("search", "06", "SEARCH", "APP & CMD QUERY"),
    WIFI("wifi", "07", "WIFI", "RF & NETWORK"),
    SETTINGS("settings", "08", "SETTINGS", "OS & LAUNCHER"),
    STX_AI("stx_ai", "09", "STX AI", "HOLOGRAPHIC CORE")
}

enum class SettingsSection(
    val code: String,
    val title: String,
    val subtitle: String
) {
    LAUNCHER("01", "HOME LAUNCHER", "DEFAULT OS ROLE"),
    IDENTITY("02", "OPERATOR ID", "CALLSIGN & AUDIO"),
    DISPLAY("03", "DISPLAY & HUD", "IMMERSIVE & GLOW"),
    PERMISSIONS("04", "PERMISSIONS", "HARDWARE ACCESS"),
    DIAGNOSTICS("05", "DIAGNOSTICS", "DEVICE & STORAGE"),
    LOCKSCREEN("06", "LOCKSCREEN", "PASSCODE & WAKE")
}

data class DialWedgeItem(
    val id: String,
    val badge: String,
    val label: String,
    val subLabel: String = "",
    val isAccentCrimson: Boolean = false
)

data class InstalledApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val versionName: String,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val permissionsCount: Int,
    val icon: ImageBitmap?,
    val categoryTag: String,
    val isPinnedToDock: Boolean = false,
    val alertsEnabled: Boolean = true,
    val bgSyncEnabled: Boolean = true,
    val launchCount: Int = 0,
    val lastUsedMs: Long = 0L
)

data class DeviceTelemetry(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargingSource: String = "BATTERY",
    val batteryTempC: Float = 0f,
    val batteryVoltageMv: Int = 0,
    val batteryHealth: String = "GOOD",
    val ramUsedPercent: Int = 0,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val storageFreeGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storageUsedPercent: Int = 0,
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiSsid: String = "NO LINK",
    val wifiRssiDbm: Int = -100,
    val wifiSignalPercent: Int = 0,
    val wifiLinkSpeedMbps: Int = 0,
    val wifiFrequencyMhz: Int = 0,
    val wifiBandLabel: String = "--",
    val ipv4Address: String = "--",
    val dnsServer: String = "--",
    val networkTransport: String = "OFFLINE",
    val bluetoothEnabled: Boolean = false,
    val bluetoothPairedCount: Int = 0,
    val bluetoothStateLabel: String = "OFF",
    val flashlightOn: Boolean = false,
    val flashlightAvailable: Boolean = false,
    val audioMuted: Boolean = false,
    val touchLocked: Boolean = false,
    val keepScreenOn: Boolean = false,
    val stepsCount: Int = 0,
    val stepsSensorAvailable: Boolean = false,
    val azimuthDeg: Int = 0,
    val cardinalDirection: String = "N",
    val pitchDeg: Int = 0,
    val rollDeg: Int = 0,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gyroHistory: List<Float> = emptyList(),
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val ambientLux: Float = 0f,
    val lightSensorAvailable: Boolean = false,
    val uptimeFormatted: String = "00:00:00",
    val uptimeSeconds: Long = 0L,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val cpuCores: Int = 1
)

data class VaultNote(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String
)

data class LauncherPreferences(
    val operatorCallsign: String = "SANJU",
    val pinnedPackages: Set<String> = emptySet(),
    val mutedAlertPackages: Set<String> = emptySet(),
    val disabledSyncPackages: Set<String> = emptySet(),
    val fullscreenImmersive: Boolean = true,
    val sfxEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val autoMorphOrientation: Boolean = true,
    val edgeGlowCrimson: Boolean = false,
    val vaultNotes: List<VaultNote> = emptyList(),
    val lockCoverEnabled: Boolean = true,
    val autoLockOnScreenOff: Boolean = true,
    val biometricUnlockEnabled: Boolean = true,
    val useOsPhysicalLock: Boolean = true,
    val passcodeHash: String = "",
    val passcodeSalt: String = "",
    val passcodeIsAlphanumeric: Boolean = false,
    val lockShowSteps: Boolean = true,
    val lockShowChargingRing: Boolean = true,
    val lockShowStxAiTrigger: Boolean = true,
    val lockHeadline: String = "STX OS // SECURE DECK",
    val systemFloatingAiEnabled: Boolean = false,
    val appLaunchCounts: Map<String, Int> = emptyMap(),
    val appLastUsedMs: Map<String, Long> = emptyMap()
) {
    val hasPasscode: Boolean get() = passcodeHash.isNotBlank() && passcodeSalt.isNotBlank()
}

data class AiMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: String,
    val isError: Boolean = false,
    val executedAction: String? = null
)
