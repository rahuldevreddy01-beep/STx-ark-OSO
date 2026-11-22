package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.hardware.LauncherPermissionManager
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.StarkMode
import com.example.ui.StarkUiState

/**
 * Landscape Home v3 screen matching the Stark OS HTML v3 design 1:1.
 */
@Composable
fun LandscapeHomeScreen(
    state: StarkUiState,
    hubModules: List<HubModule>,
    voiceListening: Boolean,
    voicePartial: String,
    onSelectHubIndex: (Int) -> Unit,
    onOpenModule: (HubModule) -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleLock: () -> Unit,
    onSync: () -> Unit,
    onToggleMute: () -> Unit,
    onVoiceCommand: () -> Unit,
    onAskStxAi: (String) -> Unit = {},
    onFoldRotate: () -> Unit,
    onSelectMode: (StarkMode) -> Unit,
    onLockDeck: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenOrbStudio: () -> Unit = {},
    onLaunchAppFullscreen: (InstalledApp) -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenAllAppsDrawer: () -> Unit = {},
    onSelectProtocol: (Int) -> Unit = {},
    onStartTimer: (Int) -> Unit = {},
    onStopTimer: () -> Unit = {},
    pauseBackgroundAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pauseOrb = pauseBackgroundAnimations || state.allAppsDrawerOpen || state.telemetry.touchLocked
    val missingPermCount = remember(state.telemetry, state.mode) {
        LauncherPermissionManager.missingRuntimePermissions(context).size
    }
    val focusedModule = hubModules.getOrElse(state.hubSelectedIndex) { HubModule.APPS }
    var commandDraft by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dialWidth = (maxWidth * 0.24f).coerceIn(176.dp, 224.dp)

        HomeV3Background(
            isLandscape = true,
            dialCenterYFraction = 0.51f,
            pauseAnimations = pauseOrb
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left 2-column grid (Status bar across top, Col A + Col B below)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HomeV3StatusBar(
                    batteryPercent = state.telemetry.batteryPercent,
                    isCharging = state.telemetry.isCharging,
                    wifiConnected = state.telemetry.wifiConnected,
                    pauseOrb = pauseOrb
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Col A: Hero + Vitals + 3x2 Hex Toggles + Actions + Perms/Orb chips
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HomeV3Hero(
                            state = state,
                            missingPermCount = missingPermCount,
                            voiceListening = voiceListening,
                            onOpenStxAi = { onOpenModule(HubModule.STX_AI) },
                            isCompactLandscape = true
                        )

                        HomeV3VitalsRow(state = state)

                        HomeV3TogglesGrid(
                            state = state,
                            columns = 3,
                            onToggleWifi = onToggleWifi,
                            onToggleBluetooth = onToggleBluetooth,
                            onToggleFlash = onToggleFlash,
                            onToggleMute = onToggleMute,
                            onSync = onSync,
                            onToggleLock = onToggleLock
                        )

                        HomeV3ActionsStrip(
                            isFullscreen = state.preferences.fullscreenImmersive,
                            onSelectMode = onSelectMode,
                            onFoldRotate = onFoldRotate,
                            onToggleFullscreen = onToggleFullscreen,
                            onLockDeck = onLockDeck,
                            modifier = Modifier.testTag("left_mode_half_dial")
                        )

                        HomeV3ChipsRow(
                            missingPermCount = missingPermCount,
                            onOpenPermissions = onOpenPermissions,
                            onOpenOrbStudio = onOpenOrbStudio,
                            horizontal = true
                        )
                    }

                    // Col B: Inline Protocol + Focus Preview Panel + Command Bar + Dock
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HomeV3ProtocolSelector(
                            protocolIndex = state.protocolIndex,
                            onSelectProtocol = onSelectProtocol,
                            horizontalRow = true
                        )

                        HomeV3FocusPanel(
                            state = state,
                            focusedModule = focusedModule,
                            missingPermCount = missingPermCount,
                            onEngage = onOpenModule,
                            onStartTimer = onStartTimer,
                            onStopTimer = onStopTimer,
                            onLaunchApp = onLaunchAppFullscreen,
                            isLandscape = true,
                            engageTestTag = "hub_target_lock_banner",
                            modifier = Modifier.weight(1f)
                        )

                        HomeV3CommandBar(
                            commandText = commandDraft,
                            onCommandTextChange = { commandDraft = it },
                            voiceListening = voiceListening,
                            voicePartial = voicePartial,
                            onVoiceCommand = onVoiceCommand,
                            onSendCommand = {
                                if (commandDraft.isNotBlank()) {
                                    onAskStxAi(commandDraft)
                                    commandDraft = ""
                                } else {
                                    onSelectMode(StarkMode.STX_AI)
                                }
                            }
                        )

                        HomeV3Dock(
                            installedApps = state.installedApps,
                            isLandscape = true,
                            onOpenAllAppsDrawer = onOpenAllAppsDrawer,
                            onOpenModule = onOpenModule,
                            onLaunchApp = onLaunchAppFullscreen
                        )
                    }
                }
            }

            // Right-edge clipped ball-bearing rotary dial
            HomeV3RightEdgeDial(
                modules = hubModules,
                selectedIndex = state.hubSelectedIndex,
                onSelectIndex = onSelectHubIndex,
                onActivateModule = onOpenModule,
                onOrbClick = { onOpenModule(HubModule.STX_AI) },
                speedMult = state.preferences.orbRotationSpeed,
                bloomMult = state.preferences.orbBloomStrength,
                pauseAnimations = pauseOrb,
                modifier = Modifier
                    .width(dialWidth)
                    .fillMaxHeight()
                    .testTag("hub_rotary_dial")
            )
        }
    }
}
