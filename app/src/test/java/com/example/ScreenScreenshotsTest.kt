package com.example

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.model.AiMessage
import com.example.model.AppLaunchMode
import com.example.model.DeviceTelemetry
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.LauncherPreferences
import com.example.model.StarkMode
import com.example.model.VaultNote
import com.example.ui.StarkClockSnapshot
import com.example.ui.StarkUiState
import com.example.ui.screens.AllAppsScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.BootScreen
import com.example.ui.screens.BriefScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.LandscapeHomeScreen
import com.example.ui.screens.LockscreenScreen
import com.example.ui.screens.PortraitHomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StxAiScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.WifiScreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.StarkOsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenScreenshotsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val hubModules = listOf(
        HubModule.BRIEF,
        HubModule.APPS,
        HubModule.HEALTH,
        HubModule.VAULT,
        HubModule.CAMERA,
        HubModule.SEARCH,
        HubModule.WIFI,
        HubModule.SETTINGS,
        HubModule.STX_AI
    )

    private fun outputDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val root = if (cwd.name == "app") cwd.parentFile else cwd
        return File(root, "SCREENS-ss").apply { mkdirs() }
    }

    private fun sampleState(
        mode: StarkMode = StarkMode.PORTRAIT,
        landscape: Boolean = false
    ): StarkUiState {
        val apps = listOf(
            InstalledApp("Chrome", "com.android.chrome", "Main", "131.0", 35, false, 6, null, "NET", isPinnedToDock = true, launchCount = 12, lastUsedMs = 1000L),
            InstalledApp("Messages", "com.google.android.apps.messaging", "Main", "2024", 35, false, 8, null, "COMMS", isPinnedToDock = true, launchCount = 8, lastUsedMs = 900L),
            InstalledApp("Camera", "com.android.camera", "Main", "9.2", 35, true, 5, null, "OPTIC", isPinnedToDock = true, launchCount = 6, lastUsedMs = 800L),
            InstalledApp("Phone", "com.google.android.dialer", "Main", "14.0", 35, true, 7, null, "COMMS", isPinnedToDock = true, launchCount = 14, lastUsedMs = 950L),
            InstalledApp("ChatGPT", "com.openai.chatgpt", "Main", "1.2026", 35, false, 3, null, "AI", launchCount = 5, lastUsedMs = 700L),
            InstalledApp("YouTube", "com.google.android.youtube", "Main", "19.45", 35, false, 4, null, "MEDIA", launchCount = 4, lastUsedMs = 600L),
            InstalledApp("WhatsApp", "com.whatsapp", "Main", "2.24", 35, false, 9, null, "COMMS", launchCount = 7),
            InstalledApp("Settings", "com.android.settings", "Main", "15", 35, true, 12, null, "SYS", launchCount = 3)
        )
        val notes = listOf(
            VaultNote("n1", "Mark VII thruster calc", "Re-run thermal sim at 22:00", "22:00"),
            VaultNote("n2", "Lab supply list", "Solder wick · flux · ESP32-S3", "18:40"),
            VaultNote("n3", "Orb shader preset", "Bloom 1.15 · Speed 1.0x", "YESTERDAY")
        )
        val msgs = listOf(
            AiMessage("m1", false, "STX AI online. Voice & text commands control hardware, apps, and Gemini 3.8 Flash.", "05:10"),
            AiMessage("m2", true, "Run quick diagnostics and check power.", "05:11"),
            AiMessage("m3", false, "Battery at 61%, core temp 27.0°C, 8 apps indexed. All subsystems nominal.", "05:12", executedAction = "DIAGNOSTICS")
        )
        return StarkUiState(
            mode = mode,
            isDeviceLandscape = landscape,
            activeModule = HubModule.VAULT,
            hubSelectedIndex = 3,
            subItemIndex = 0,
            installedApps = apps,
            topRecentApps = apps.take(6),
            appsLoading = false,
            clock = StarkClockSnapshot(
                timeText = "05:12",
                secondsText = "34",
                dateText = "SUN 22 NOV",
                greeting = "GOOD MORNING",
                dayProgressFraction = 0.64f
            ),
            telemetry = DeviceTelemetry(
                batteryPercent = 61,
                isCharging = false,
                chargingSource = "BATTERY",
                batteryTempC = 27.0f,
                batteryVoltageMv = 3920,
                batteryHealth = "GOOD",
                ramUsedPercent = 67,
                ramUsedMb = 5420,
                ramTotalMb = 8192,
                storageFreeGb = 34.8f,
                storageTotalGb = 128.0f,
                storageUsedPercent = 73,
                wifiEnabled = true,
                wifiConnected = true,
                wifiSsid = "STARK-NET",
                wifiRssiDbm = -54,
                wifiSignalPercent = 82,
                wifiLinkSpeedMbps = 433,
                wifiFrequencyMhz = 5180,
                wifiBandLabel = "5 GHZ",
                ipv4Address = "192.168.1.42",
                dnsServer = "1.1.1.1",
                networkTransport = "WIFI 5G",
                bluetoothEnabled = false,
                bluetoothStateLabel = "OFF",
                flashlightAvailable = true,
                audioMuted = true,
                stepsCount = 3420,
                stepsSensorAvailable = true,
                azimuthDeg = 128,
                cardinalDirection = "SE",
                pitchDeg = 4,
                rollDeg = -2,
                gyroX = 0.12f,
                gyroY = -0.05f,
                gyroZ = 0.08f,
                gyroHistory = listOf(0.1f, 0.35f, 0.22f, 0.5f, 0.18f, 0.42f, 0.29f, 0.6f),
                ambientLux = 240f,
                lightSensorAvailable = true,
                uptimeFormatted = "04:18:12",
                uptimeSeconds = 15492L,
                deviceModel = "STX-PHONE",
                androidVersion = "15",
                cpuCores = 8
            ),
            preferences = LauncherPreferences(
                operatorCallsign = "SANJU",
                pinnedPackages = apps.take(4).map { it.packageName }.toSet(),
                vaultNotes = notes,
                firstBootCompleted = true,
                permissionsOnboarded = true,
                orbUseHtmlWebGl = true
            ),
            vaultTimerSeconds = 1500,
            vaultTimerRunning = false,
            aiMessages = msgs,
            preferencesLoaded = true,
            protocolIndex = 2
        )
    }

    private fun renderAndSave(
        fileName: String,
        useHudBg: Boolean = true,
        advanceFrames: Int = 20,
        content: @Composable () -> Unit
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            StarkOsTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (useHudBg) {
                                Modifier.background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF07111E), ObsidianVoid)
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    content()
                }
            }
        }
        repeat(advanceFrames) {
            composeRule.mainClock.advanceTimeByFrame()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        val activity = composeRule.activity
        val contentView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val targetView = contentView.getChildAt(0) ?: activity.window.decorView
        val dm = activity.resources.displayMetrics
        val w = if (targetView.width > 0) targetView.width else dm.widthPixels
        val h = if (targetView.height > 0) targetView.height else dm.heightPixels
        if (targetView.width != w || targetView.height != h) {
            targetView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY)
            )
            targetView.layout(0, 0, w, h)
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        targetView.draw(canvas)

        val outFile = File(outputDir(), fileName)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bitmap.recycle()
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun capturePortraitHomeScreen() {
        val st = sampleState(mode = StarkMode.PORTRAIT, landscape = false)
        renderAndSave("PortraitHomeScreen.png", useHudBg = false) {
            PortraitHomeScreen(
                state = st,
                hubModules = hubModules,
                voiceListening = false,
                voicePartial = "",
                onSelectHubIndex = {},
                onOpenModule = {},
                onLaunchAppFullscreen = {},
                onAskStxAi = {},
                onVoiceCommand = {},
                onToggleWifi = {},
                onToggleBluetooth = {},
                onToggleFlash = {},
                onToggleLock = {},
                onSync = {},
                onToggleMute = {},
                onFoldRotate = {},
                onSelectMode = {},
                onLockDeck = {},
                onToggleFullscreen = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w845dp-h390dp-land-xxhdpi")
    fun captureLandscapeHomeScreen() {
        val st = sampleState(mode = StarkMode.HUB, landscape = true)
        renderAndSave("LandscapeHomeScreen.png", useHudBg = false) {
            LandscapeHomeScreen(
                state = st,
                hubModules = hubModules,
                voiceListening = false,
                voicePartial = "",
                onSelectHubIndex = {},
                onOpenModule = {},
                onToggleWifi = {},
                onToggleBluetooth = {},
                onToggleFlash = {},
                onToggleLock = {},
                onSync = {},
                onToggleMute = {},
                onVoiceCommand = {},
                onAskStxAi = {},
                onFoldRotate = {},
                onSelectMode = {},
                onLockDeck = {},
                onToggleFullscreen = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureAllAppsScreen() {
        val st = sampleState()
        renderAndSave("AllAppsScreen.png", useHudBg = false) {
            AllAppsScreen(
                installedApps = st.installedApps,
                topRecentApps = st.topRecentApps,
                appsLoading = false,
                onLaunchApp = {},
                onLaunchWindowed = {},
                onLaunchFullscreen = {},
                onTogglePin = {},
                onOpenAppInfo = {},
                onClose = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureAppsScreen() {
        val st = sampleState()
        val dialItems = st.topRecentApps.mapIndexed { idx, app ->
            DialWedgeItem(
                id = app.packageName,
                badge = "%02d".format(idx + 1),
                label = app.label.uppercase(),
                subLabel = app.categoryTag,
                isAccentCrimson = app.isPinnedToDock
            )
        }
        renderAndSave("AppsScreen.png") {
            AppsScreen(
                state = st,
                dialItems = dialItems,
                onSelectSubIndex = { _, _ -> },
                onSelectLeftDialLaunchMode = {},
                onLaunchWithLeftDialMode = {},
                onFocusApp = {},
                onCenterBack = {},
                onOpenModule = {},
                onLaunchWindowed = {},
                onLaunchFullscreen = {},
                onTogglePin = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureBootScreen() {
        val st = sampleState()
        renderAndSave("BootScreen.png", useHudBg = false, advanceFrames = 90) {
            BootScreen(
                preferences = st.preferences,
                onBootComplete = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureBriefScreen() {
        val st = sampleState()
        renderAndSave("BriefScreen.png") {
            BriefScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onOpenApps = {},
                onOpenSettings = {},
                onOpenStxAi = {},
                onLaunchApp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureCameraScreen() {
        val st = sampleState()
        renderAndSave("CameraScreen.png") {
            CameraScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onToggleTorch = {},
                onFlipLens = {},
                onZoomChange = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureHealthScreen() {
        val st = sampleState()
        renderAndSave("HealthScreen.png") {
            HealthScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onPermissionGranted = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureLockscreenScreen() {
        val st = sampleState()
        renderAndSave("LockscreenScreen.png", useHudBg = false) {
            LockscreenScreen(
                state = st,
                onAttemptPasscode = { true },
                onBiometricAuthenticated = {},
                onSwipeUnlock = {},
                onClearError = {},
                onAskStxAi = {},
                onDimChange = {},
                onIdleSleepTimeout = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureSearchScreen() {
        val st = sampleState()
        renderAndSave("SearchScreen.png") {
            SearchScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onQueryChange = {},
                onLaunchFullscreen = {},
                onWebSearch = {},
                onAskStxAi = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureSettingsScreen() {
        val st = sampleState()
        renderAndSave("SettingsScreen.png") {
            SettingsScreen(
                state = st,
                focusedIndex = 0,
                onSelectSection = {},
                onOpenDefaultLauncher = {},
                onReloadApps = {},
                onSetCallsign = {},
                onToggleSfx = {},
                onToggleMute = {},
                onToggleFullscreen = {},
                onToggleKeepAwake = {},
                onToggleAutoMorph = {},
                onToggleEdgeColor = {},
                onToggleSystemFloatingAi = {},
                onPermissionsUpdated = {},
                onSetLockCoverEnabled = {},
                onSetAutoLockOnScreenOff = {},
                onSetBiometricUnlockEnabled = {},
                onSetUseOsPhysicalLock = {},
                onSavePasscode = { _, _ -> },
                onClearPasscode = {},
                onSetLockWidgets = { _, _, _ -> },
                onSetLockHeadline = {},
                onEngageLockNow = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureStxAiScreen() {
        val st = sampleState()
        renderAndSave("StxAiScreen.png") {
            StxAiScreen(
                state = st,
                voiceListening = false,
                voicePartial = "",
                onVoiceCommand = {},
                onSendPrompt = {},
                onBack = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureVaultScreen() {
        val st = sampleState()
        renderAndSave("VaultScreen.png") {
            VaultScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onAddNote = { _, _ -> },
                onDeleteNote = {},
                onStartTimer = {},
                onStopTimer = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w390dp-h845dp-xxhdpi")
    fun captureWifiScreen() {
        val st = sampleState()
        renderAndSave("WifiScreen.png") {
            WifiScreen(
                state = st,
                focusedIndex = 0,
                onSelectIndex = {},
                onOpenWifiPanel = {},
                onOpenBluetoothPanel = {}
            )
        }
    }
}
