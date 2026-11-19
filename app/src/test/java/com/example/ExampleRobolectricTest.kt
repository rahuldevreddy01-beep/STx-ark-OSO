package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.LauncherAppsRepository
import com.example.data.LocalStxCommandRouter
import com.example.data.StxAction
import com.example.model.AppLaunchMode
import com.example.model.DeviceTelemetry
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.StarkMode
import com.example.security.PasscodeCrypto
import com.example.ui.StarkUiState
import com.example.ui.StarkViewModel
import com.example.ui.components.ArcReactorFullDial
import com.example.ui.components.ArcReactorSideHalfDial
import com.example.ui.components.DialSide
import com.example.ui.components.FloatingStxAiOverlay
import com.example.ui.components.HologramBitmapProcessor
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.StxHologramGirlCore
import com.example.ui.screens.HalfDialWorkspaceView
import com.example.ui.screens.LandscapeHubView
import com.example.ui.screens.PortraitCommandView
import com.example.ui.screens.StxAiView
import com.example.ui.theme.StarkOsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches STX OS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("STX OS", appName)
    }

    private fun sampleMultiActivityApps(): List<InstalledApp> = listOf(
        InstalledApp(
            label = "Google",
            packageName = "com.google.android.googlequicksearchbox",
            activityName = "com.google.android.googlequicksearchbox.SearchActivity",
            versionName = "15.0",
            targetSdk = 35,
            isSystemApp = true,
            permissionsCount = 12,
            icon = null,
            categoryTag = "SYSTEM",
            launchCount = 5,
            lastUsedMs = 5000L
        ),
        InstalledApp(
            label = "Voice Search",
            packageName = "com.google.android.googlequicksearchbox",
            activityName = "com.google.android.googlequicksearchbox.VoiceSearchActivity",
            versionName = "15.0",
            targetSdk = 35,
            isSystemApp = true,
            permissionsCount = 12,
            icon = null,
            categoryTag = "SYSTEM"
        ),
        InstalledApp(
            label = "Settings",
            packageName = "com.android.settings",
            activityName = "com.android.settings.Settings",
            versionName = "15",
            targetSdk = 35,
            isSystemApp = true,
            permissionsCount = 20,
            icon = null,
            categoryTag = "SYSTEM",
            launchCount = 2,
            lastUsedMs = 3000L
        )
    )

    @Test
    fun `apps portrait cockpit shows left dial center carousel right recent dial and grid`() {
        val apps = sampleMultiActivityApps()
        val topRecent = apps.take(2)
        val dialItems = topRecent.mapIndexed { idx, app ->
            DialWedgeItem(
                id = "${app.packageName}/${app.activityName}",
                badge = "%02d".format(idx + 1),
                label = app.label.uppercase().take(10),
                subLabel = "RECENT"
            )
        }
        val state = StarkUiState(
            mode = StarkMode.HALF_DIAL,
            activeModule = HubModule.APPS,
            installedApps = apps,
            topRecentApps = topRecent,
            leftDialLaunchMode = AppLaunchMode.FLOATING,
            appsLoading = false
        )

        var selectedMode: AppLaunchMode? = null
        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    HalfDialWorkspaceView(
                        state = state,
                        dialItems = dialItems,
                        onSelectSubIndex = { _, _ -> },
                        onSelectLeftDialLaunchMode = { selectedMode = it },
                        onLaunchWithLeftDialMode = {},
                        onFocusApp = {},
                        onCenterBack = {},
                        onOpenModule = {},
                        onLaunchWindowed = {},
                        onLaunchFullscreen = {},
                        onTogglePin = {},
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
                        onOpenWifiPanel = {},
                        onOpenBluetoothPanel = {},
                        onToggleTorch = {},
                        onFlipCameraLens = {},
                        onCameraZoomChange = {},
                        onAddVaultNote = { _, _ -> },
                        onDeleteVaultNote = {},
                        onStartVaultTimer = {},
                        onStopVaultTimer = {},
                        onSearchQueryChange = {},
                        onWebSearch = {},
                        onAskStxAi = {},
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
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("left_half_dial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("active_app_carousel_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_half_dial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("all_apps_5col_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("left_dial_mode_fullscreen").performClick()
        assertEquals(AppLaunchMode.FULLSCREEN, selectedMode)
        composeTestRule.onNodeWithTag("left_dial_mode_toggle").performClick()
        assertEquals(AppLaunchMode.FULLSCREEN, selectedMode)
    }

    @Test
    @Config(qualifiers = "w840dp-h420dp-land")
    fun `apps landscape cockpit keeps carousel centered between both dials and toggles all apps grid`() {
        val apps = sampleMultiActivityApps()
        val dialItems = apps.mapIndexed { idx, app ->
            DialWedgeItem(
                id = "${app.packageName}/${app.activityName}",
                badge = "%02d".format(idx + 1),
                label = app.label.uppercase().take(10),
                subLabel = "RECENT"
            )
        }
        val state = StarkUiState(
            mode = StarkMode.HALF_DIAL,
            isDeviceLandscape = true,
            activeModule = HubModule.APPS,
            installedApps = apps,
            topRecentApps = apps,
            appsLoading = false
        )

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 800.dp, height = 400.dp)) {
                    HalfDialWorkspaceView(
                        state = state,
                        dialItems = dialItems,
                        onSelectSubIndex = { _, _ -> },
                        onSelectLeftDialLaunchMode = {},
                        onLaunchWithLeftDialMode = {},
                        onFocusApp = {},
                        onCenterBack = {},
                        onOpenModule = {},
                        onLaunchWindowed = {},
                        onLaunchFullscreen = {},
                        onTogglePin = {},
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
                        onOpenWifiPanel = {},
                        onOpenBluetoothPanel = {},
                        onToggleTorch = {},
                        onFlipCameraLens = {},
                        onCameraZoomChange = {},
                        onAddVaultNote = { _, _ -> },
                        onDeleteVaultNote = {},
                        onStartVaultTimer = {},
                        onStopVaultTimer = {},
                        onSearchQueryChange = {},
                        onWebSearch = {},
                        onAskStxAi = {},
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
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("left_half_dial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("active_app_carousel_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_half_dial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("apps_grid_toggle_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_apps_5col_grid").assertIsDisplayed()
    }

    @Test
    fun `viewmodel right dial apps list stays lightweight within 1 to 6 items`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = StarkViewModel(app)
        val items = vm.currentHalfDialItems()
        assertTrue(items.size in 1..6)
    }

    @Test
    fun `stx ai command router routes hardware and module commands`() {
        val telemetry = DeviceTelemetry(
            batteryPercent = 88,
            stepsCount = 1420,
            azimuthDeg = 90,
            cardinalDirection = "E"
        )

        val flashReply = LocalStxCommandRouter.tryMatchLocalCommand("turn on flash", telemetry, emptyList())
        assertEquals(StxAction.ToggleFlashlight, flashReply?.action)

        val settingsReply = LocalStxCommandRouter.tryMatchLocalCommand("open settings", telemetry, emptyList())
        assertEquals(StxAction.OpenModule(HubModule.SETTINGS), settingsReply?.action)

        val lockReply = LocalStxCommandRouter.tryMatchLocalCommand("lock deck", telemetry, emptyList())
        assertEquals(StxAction.LockDeck, lockReply?.action)

        val generalReply = LocalStxCommandRouter.tryMatchLocalCommand("tell me a joke", telemetry, emptyList())
        assertNull(generalReply)
    }

    @Test
    fun `passcode crypto verifies salted SHA-256 PIN and alphanumeric passcodes`() {
        val salt1 = PasscodeCrypto.generateSaltHex()
        val salt2 = PasscodeCrypto.generateSaltHex()
        assertNotEquals(salt1, salt2)

        val pinHash = PasscodeCrypto.hashPasscode("1939", salt1)
        assertTrue(PasscodeCrypto.verifyPasscode("1939", salt1, pinHash))
        assertFalse(PasscodeCrypto.verifyPasscode("1940", salt1, pinHash))
        assertFalse(PasscodeCrypto.verifyPasscode("1939", salt2, pinHash))
        assertFalse(PasscodeCrypto.verifyPasscode("1939", "", pinHash))
        assertFalse(PasscodeCrypto.verifyPasscode("1939", salt1, ""))

        val alphaHash = PasscodeCrypto.hashPasscode("Stx#Mark42", salt2)
        assertTrue(PasscodeCrypto.verifyPasscode("Stx#Mark42", salt2, alphaHash))
        assertFalse(PasscodeCrypto.verifyPasscode("stx#mark42", salt2, alphaHash))
    }

    @Test
    fun `center orb click opens currently updated top selectedIndex`() {
        val items = (0..5).map { idx ->
            DialWedgeItem(
                id = "mod_$idx",
                badge = "%02d".format(idx + 1),
                label = "MOD_$idx"
            )
        }
        var currentSelected by mutableIntStateOf(0)
        var activatedIndex: Int? = null

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(320.dp)) {
                    ArcReactorFullDial(
                        items = items,
                        selectedIndex = currentSelected,
                        onSelectIndex = { currentSelected = it },
                        onActivateIndex = { activatedIndex = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { currentSelected = 3 }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dial_center_orb_trigger").assertIsDisplayed().performClick()
        assertEquals(3, activatedIndex)
    }

    @Test
    @Config(qualifiers = "w840dp-h420dp-land")
    fun `landscape hub displays left edge mode half dial without bottom bar`() {
        val state = StarkUiState(
            mode = StarkMode.HUB,
            isDeviceLandscape = true
        )
        var lockCalled = false
        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 840.dp, height = 420.dp)) {
                    LandscapeHubView(
                        state = state,
                        hubModules = HubModule.entries,
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
                        onFoldRotate = {},
                        onSelectMode = {},
                        onLockDeck = { lockCalled = true },
                        onToggleFullscreen = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("left_mode_half_dial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("left_mode_center_trigger").assertIsDisplayed().performClick()
        assertEquals(false, lockCalled)
        composeTestRule.onNodeWithTag("hub_rotary_dial").assertIsDisplayed()
    }

    @Test
    fun `left half dial swipe up advances selection forward and right half dial swipe down advances forward`() {
        val items = (0..5).map { idx ->
            DialWedgeItem(id = "item_$idx", badge = "%02d".format(idx + 1), label = "ITEM_$idx")
        }
        var leftSelected by mutableIntStateOf(0)
        var rightSelected by mutableIntStateOf(0)
        var rightActivated: Int? = null

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 300.dp, height = 240.dp)) {
                    ArcReactorSideHalfDial(
                        side = DialSide.LEFT,
                        items = items,
                        selectedIndex = leftSelected,
                        onSelectIndex = { leftSelected = it },
                        onActivateIndex = {},
                        onCenterActionClick = {},
                        halfOffScreen = true,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(width = 140.dp, height = 240.dp)
                            .testTag("test_left_half_dial")
                    )
                    ArcReactorSideHalfDial(
                        side = DialSide.RIGHT,
                        items = items,
                        selectedIndex = rightSelected,
                        onSelectIndex = { rightSelected = it },
                        onActivateIndex = { rightActivated = it },
                        onCenterActionClick = {},
                        halfOffScreen = true,
                        activateOnlyWhenFocused = true,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 140.dp, height = 240.dp)
                            .testTag("test_right_half_dial")
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_left_half_dial").performTouchInput {
            swipeUp(startY = centerY + 60f, endY = centerY - 60f)
        }
        composeTestRule.waitForIdle()
        assertTrue("Expected left dial to advance forward from 0, got $leftSelected", leftSelected in 1..4)

        composeTestRule.onNodeWithTag("test_right_half_dial").performTouchInput {
            swipeDown(startY = centerY - 60f, endY = centerY + 60f)
        }
        composeTestRule.waitForIdle()
        assertTrue("Expected right dial to advance forward from 0, got $rightSelected", rightSelected in 1..4)

        // Tap off-center top wedge on right dial: focuses it without immediate launch
        val beforeTapIdx = rightSelected
        composeTestRule.onNodeWithTag("test_right_half_dial").performTouchInput {
            click(Offset(width * 0.45f, height * 0.25f))
        }
        composeTestRule.waitForIdle()
        assertNotEquals(beforeTapIdx, rightSelected)
        assertNull(rightActivated)

        // Tap centered focused wedge on right dial: now activates it
        composeTestRule.onNodeWithTag("test_right_half_dial").performTouchInput {
            click(Offset(width * 0.35f, height * 0.50f))
        }
        composeTestRule.waitForIdle()
        assertEquals(rightSelected, rightActivated)
    }

    @Test
    @Config(qualifiers = "w840dp-h420dp-land")
    fun `right half dial and carousel controls switch visible 3d cards on health brief and settings`() {
        var subIndex by mutableIntStateOf(0)
        var currentModule by androidx.compose.runtime.mutableStateOf(HubModule.HEALTH)

        composeTestRule.setContent {
            val items = when (currentModule) {
                HubModule.HEALTH -> listOf(
                    DialWedgeItem("steps", "01", "STEPS", "1240"),
                    DialWedgeItem("gyro", "02", "GYROSCOPE", "3-AXIS"),
                    DialWedgeItem("compass", "03", "COMPASS", "NE 45°"),
                    DialWedgeItem("power", "04", "POWER", "88%")
                )
                HubModule.BRIEF -> listOf(
                    DialWedgeItem("overview", "01", "SUMMARY", "LIVE OS"),
                    DialWedgeItem("hardware", "02", "TELEMETRY", "48% RAM"),
                    DialWedgeItem("dock", "03", "PINNED", "0 APPS")
                )
                else -> com.example.model.SettingsSection.entries.map {
                    DialWedgeItem(it.name, it.code, it.title, it.subtitle)
                }
            }
            val state = StarkUiState(
                mode = StarkMode.HALF_DIAL,
                isDeviceLandscape = true,
                activeModule = currentModule,
                subItemIndex = subIndex,
                telemetry = DeviceTelemetry(
                    batteryPercent = 88,
                    stepsCount = 1240,
                    stepsSensorAvailable = true,
                    azimuthDeg = 45,
                    cardinalDirection = "NE",
                    ramUsedPercent = 48,
                    ramUsedMb = 3800,
                    ramTotalMb = 8000,
                    deviceModel = "STX-TEST"
                )
            )
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 840.dp, height = 420.dp)) {
                    HalfDialWorkspaceView(
                        state = state,
                        dialItems = items,
                        onSelectSubIndex = { idx, count ->
                            if (count > 0) subIndex = ((idx % count) + count) % count
                        },
                        onSelectLeftDialLaunchMode = {},
                        onLaunchWithLeftDialMode = {},
                        onFocusApp = {},
                        onCenterBack = {},
                        onOpenModule = {},
                        onLaunchWindowed = {},
                        onLaunchFullscreen = {},
                        onTogglePin = {},
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
                        onOpenWifiPanel = {},
                        onOpenBluetoothPanel = {},
                        onToggleTorch = {},
                        onFlipCameraLens = {},
                        onCameraZoomChange = {},
                        onAddVaultNote = { _, _ -> },
                        onDeleteVaultNote = {},
                        onStartVaultTimer = {},
                        onStopVaultTimer = {},
                        onSearchQueryChange = {},
                        onWebSearch = {},
                        onAskStxAi = {},
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
        }

        // HEALTH starts at 01 steps card; swiping right dial updates subIndex and switches center card
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_card_steps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_half_dial").performTouchInput {
            swipeDown(startY = centerY - 70f, endY = centerY + 70f)
        }
        composeTestRule.waitForIdle()
        assertTrue("Expected subIndex > 0 after right half dial swipe, was $subIndex", subIndex in 1..3)
        val expectedHealthTag = listOf("steps", "gyro", "compass", "power")[subIndex]
        composeTestRule.onNodeWithTag("carousel_card_$expectedHealthTag").assertIsDisplayed()

        // BRIEF switches cards via next button and subItemIndex
        composeTestRule.runOnIdle {
            currentModule = HubModule.BRIEF
            subIndex = 0
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_card_overview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_next_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, subIndex)
        composeTestRule.onNodeWithTag("carousel_card_hardware").assertIsDisplayed()

        // SETTINGS keeps tabs synced with 3D carousel cards
        composeTestRule.runOnIdle {
            currentModule = HubModule.SETTINGS
            subIndex = 0
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_card_launcher").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_tab_diagnostics").performClick()
        composeTestRule.waitForIdle()
        assertEquals(4, subIndex)
        composeTestRule.onNodeWithTag("carousel_card_diagnostics").assertIsDisplayed()
    }

    @Test
    fun `zero size containers do not crash gradients or dials during charging`() {
        val dialItems = listOf(
            DialWedgeItem("a", "01", "ONE", "SUB"),
            DialWedgeItem("b", "02", "TWO", "SUB")
        )
        val chargingState = StarkUiState(
            mode = StarkMode.HUB,
            telemetry = DeviceTelemetry(
                batteryPercent = 50,
                isCharging = true,
                chargingSource = "AC FAST",
                flashlightOn = true
            )
        )

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(0.dp)) {
                    LandscapeHubView(
                        state = chargingState,
                        hubModules = HubModule.entries,
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
                        onFoldRotate = {},
                        onSelectMode = {},
                        onLockDeck = {},
                        onToggleFullscreen = {}
                    )
                    PortraitCommandView(
                        state = chargingState,
                        hubModules = HubModule.entries,
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
                    ArcReactorFullDial(
                        items = dialItems,
                        selectedIndex = 0,
                        onSelectIndex = {},
                        onActivateIndex = {}
                    )
                    ArcReactorSideHalfDial(
                        side = DialSide.RIGHT,
                        items = dialItems,
                        selectedIndex = 0,
                        onSelectIndex = {},
                        onActivateIndex = {},
                        onCenterActionClick = {},
                        showCoreSpinner = true
                    )
                    HolographicCardCarousel(
                        items = dialItems,
                        focusedIndex = 0,
                        onSelectIndex = {}
                    ) { _, _, _, _, _, _ -> }
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun `carousel handles shrinking item count and all hub modules render cleanly`() {
        var listItems by androidx.compose.runtime.mutableStateOf(
            (0..5).map { DialWedgeItem("k_$it", "%02d".format(it + 1), "CARD_$it") }
        )
        var focused by mutableIntStateOf(5)
        var activeMod by androidx.compose.runtime.mutableStateOf(HubModule.BRIEF)

        composeTestRule.setContent {
            val state = StarkUiState(
                mode = StarkMode.HALF_DIAL,
                isDeviceLandscape = true,
                activeModule = activeMod,
                subItemIndex = focused
            )
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 800.dp, height = 420.dp)) {
                    HolographicCardCarousel(
                        items = listItems,
                        focusedIndex = focused,
                        onSelectIndex = { focused = it }
                    ) { _, item, isCenter, _, _, _ ->
                        androidx.compose.material3.Text(
                            text = item.label,
                            modifier = if (isCenter) Modifier.testTag("shrink_center_${item.id}") else Modifier
                        )
                    }
                    HalfDialWorkspaceView(
                        state = state,
                        dialItems = listItems,
                        onSelectSubIndex = { _, _ -> },
                        onSelectLeftDialLaunchMode = {},
                        onLaunchWithLeftDialMode = {},
                        onFocusApp = {},
                        onCenterBack = {},
                        onOpenModule = {},
                        onLaunchWindowed = {},
                        onLaunchFullscreen = {},
                        onTogglePin = {},
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
                        onOpenWifiPanel = {},
                        onOpenBluetoothPanel = {},
                        onToggleTorch = {},
                        onFlipCameraLens = {},
                        onCameraZoomChange = {},
                        onAddVaultNote = { _, _ -> },
                        onDeleteVaultNote = {},
                        onStartVaultTimer = {},
                        onStopVaultTimer = {},
                        onSearchQueryChange = {},
                        onWebSearch = {},
                        onAskStxAi = {},
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
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            listItems = listOf(DialWedgeItem("k_0", "01", "ONLY"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shrink_center_k_0").assertIsDisplayed()

        listItems = listOf(
            DialWedgeItem("optical", "01", "OPTICAL"),
            DialWedgeItem("torch", "02", "TORCH"),
            DialWedgeItem("lens", "03", "LENS"),
            DialWedgeItem("zoom", "04", "ZOOM")
        )
        HubModule.entries.forEach { mod ->
            composeTestRule.runOnIdle {
                focused = 0
                activeMod = mod
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `launcher apps repository safely rejects empty or unresolvable launches`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LauncherAppsRepository(context)
        val blankApp = InstalledApp(
            label = "Broken",
            packageName = "",
            activityName = "",
            versionName = "1.0",
            targetSdk = 35,
            isSystemApp = false,
            permissionsCount = 0,
            icon = null,
            categoryTag = "APP"
        )
        val noActivityApp = blankApp.copy(packageName = "com.nonexistent.stx.pkg")

        assertFalse(repo.launchFullscreen(blankApp))
        assertFalse(repo.launchWindowed(blankApp))
        assertFalse(repo.launchFullscreen(noActivityApp))
        assertFalse(repo.launchWindowed(noActivityApp))
    }

    @Test
    @Config(qualifiers = "w840dp-h420dp-land")
    fun `redesigned landscape homescreen target lock banner opens focused module`() {
        val state = StarkUiState(
            mode = StarkMode.HUB,
            hubSelectedIndex = 2,
            isDeviceLandscape = true
        )
        var openedLandscape: HubModule? = null

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 840.dp, height = 420.dp)) {
                    LandscapeHubView(
                        state = state,
                        hubModules = HubModule.entries,
                        voiceListening = false,
                        voicePartial = "",
                        onSelectHubIndex = {},
                        onOpenModule = { openedLandscape = it },
                        onToggleWifi = {},
                        onToggleBluetooth = {},
                        onToggleFlash = {},
                        onToggleLock = {},
                        onSync = {},
                        onToggleMute = {},
                        onVoiceCommand = {},
                        onFoldRotate = {},
                        onSelectMode = {},
                        onLockDeck = {},
                        onToggleFullscreen = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hub_target_lock_banner").assertIsDisplayed().performClick()
        assertEquals(HubModule.entries[2], openedLandscape)
    }

    @Test
    fun `redesigned portrait homescreen target lock banner opens focused module`() {
        val state = StarkUiState(
            mode = StarkMode.HUB,
            hubSelectedIndex = 2,
            isDeviceLandscape = false
        )
        var openedPortrait: HubModule? = null

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 820.dp)) {
                    PortraitCommandView(
                        state = state,
                        hubModules = HubModule.entries,
                        voiceListening = true,
                        voicePartial = "scan perimeter",
                        onSelectHubIndex = {},
                        onOpenModule = { openedPortrait = it },
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
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portrait_target_lock_banner").assertIsDisplayed().performClick()
        assertEquals(HubModule.entries[2], openedPortrait)
    }

    @Test
    fun `hologram cutout removes outer black background pixels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cutout = HologramBitmapProcessor.getOrCreateCutoutBitmap(context, 128)
        assertTrue(cutout != null && cutout.width > 16 && cutout.height > 16)
        val topLeftCornerAlpha = (cutout!!.getPixel(0, 0) ushr 24) and 0xFF
        val topRightCornerAlpha = (cutout.getPixel(cutout.width - 1, 0) ushr 24) and 0xFF
        assertEquals(0, topLeftCornerAlpha)
        assertEquals(0, topRightCornerAlpha)
    }

    @Test
    fun `stx ai core girl shows speaking expression toggles tts and handles avatar tap`() {
        var speaking by androidx.compose.runtime.mutableStateOf(false)
        var listening by androidx.compose.runtime.mutableStateOf(false)
        var thinking by androidx.compose.runtime.mutableStateOf(false)
        var ttsOn by androidx.compose.runtime.mutableStateOf(true)
        var avatarTapped = false
        var pulseTick by mutableIntStateOf(0)

        composeTestRule.setContent {
            val state = StarkUiState(
                mode = StarkMode.STX_AI,
                ttsEnabled = ttsOn,
                aiSpeaking = speaking,
                aiThinking = thinking,
                aiSpeechPulseTick = pulseTick
            )
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 410.dp, height = 820.dp)) {
                    StxAiView(
                        state = state,
                        voiceListening = listening,
                        voicePartial = "",
                        voiceRms = 0.65f,
                        onVoiceCommand = {},
                        onSendPrompt = {},
                        onToggleTts = { ttsOn = !ttsOn },
                        onAvatarTap = {
                            avatarTapped = true
                            speaking = !speaking
                            pulseTick++
                        },
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stx_expression_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stx_hologram_core").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertTrue(avatarTapped)
        assertTrue(speaking)

        composeTestRule.onNodeWithTag("stx_tts_toggle").assertIsDisplayed().performClick()
        assertFalse(ttsOn)

        composeTestRule.runOnIdle {
            speaking = false
            listening = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stx_expression_badge").assertIsDisplayed()

        composeTestRule.runOnIdle {
            listening = false
            thinking = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stx_expression_badge").assertIsDisplayed()
    }

    @Test
    fun `viewmodel defaults ttsEnabled true and floating overlay renders holographic orb cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = StarkViewModel(app)
        assertTrue(vm.uiState.value.ttsEnabled)
        vm.toggleTtsEnabled()
        assertFalse(vm.uiState.value.ttsEnabled)
        vm.toggleTtsEnabled()
        assertTrue(vm.uiState.value.ttsEnabled)
        vm.onHologramAvatarTap()

        composeTestRule.setContent {
            StarkOsTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 760.dp)) {
                    FloatingStxAiOverlay(
                        state = StarkUiState(stxOverlayOpen = true, aiSpeaking = true, aiSpeechPulseTick = 2),
                        voiceListening = false,
                        voicePartial = "",
                        systemFloatingEnabled = false,
                        canDrawOverlays = false,
                        onToggleSystemFloating = {},
                        onToggleOverlay = {},
                        onDismissOverlay = {},
                        onExpandFullStx = {},
                        onVoiceCommand = {},
                        onSendCommand = {}
                    )
                }
                Box(modifier = Modifier.size(0.dp)) {
                    StxHologramGirlCore(
                        isSpeaking = true,
                        isListening = false,
                        isThinking = false,
                        speechPulseTick = 1
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("floating_stx_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("floating_stx_bot_orb").assertIsDisplayed()
    }
}

