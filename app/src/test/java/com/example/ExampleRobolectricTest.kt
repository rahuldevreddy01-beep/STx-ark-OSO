package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
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
import com.example.ui.screens.HalfDialWorkspaceView
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
}
