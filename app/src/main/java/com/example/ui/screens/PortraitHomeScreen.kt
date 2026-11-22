package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * Portrait Home v3 screen matching the Stark OS HTML v3 design 1:1.
 */
@Composable
fun PortraitHomeScreen(
    state: StarkUiState,
    hubModules: List<HubModule>,
    voiceListening: Boolean,
    voicePartial: String,
    onSelectHubIndex: (Int) -> Unit,
    onOpenModule: (HubModule) -> Unit,
    onLaunchAppFullscreen: (InstalledApp) -> Unit,
    onAskStxAi: (String) -> Unit,
    onVoiceCommand: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleLock: () -> Unit,
    onSync: () -> Unit,
    onToggleMute: () -> Unit,
    onFoldRotate: () -> Unit,
    onSelectMode: (StarkMode) -> Unit,
    onLockDeck: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenOrbStudio: () -> Unit = {},
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
    var commandText by remember { mutableStateOf("") }
    val focusedModule = hubModules.getOrElse(state.hubSelectedIndex) { HubModule.APPS }

    Box(modifier = modifier.fillMaxSize()) {
        HomeV3Background(
            isLandscape = false,
            dialCenterYFraction = 0.58f,
            pauseAnimations = pauseOrb
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeV3StatusBar(
                batteryPercent = state.telemetry.batteryPercent,
                isCharging = state.telemetry.isCharging,
                wifiConnected = state.telemetry.wifiConnected,
                pauseOrb = pauseOrb
            )

            HomeV3Hero(
                state = state,
                missingPermCount = missingPermCount,
                voiceListening = voiceListening,
                onOpenStxAi = { onOpenModule(HubModule.STX_AI) },
                isCompactLandscape = false
            )

            HomeV3VitalsRow(state = state)

            Column(modifier = Modifier.fillMaxWidth()) {
                HomeV3TogglesGrid(
                    state = state,
                    columns = 6,
                    onToggleWifi = onToggleWifi,
                    onToggleBluetooth = onToggleBluetooth,
                    onToggleFlash = onToggleFlash,
                    onToggleMute = onToggleMute,
                    onSync = onSync,
                    onToggleLock = onToggleLock
                )
                Spacer(modifier = Modifier.height(6.dp))
                HomeV3ActionsStrip(
                    isFullscreen = state.preferences.fullscreenImmersive,
                    onSelectMode = onSelectMode,
                    onFoldRotate = onFoldRotate,
                    onToggleFullscreen = onToggleFullscreen,
                    onLockDeck = onLockDeck
                )
            }

            // Dial Zone: Left protocol + live focus preview + chips, Right clipped ball-bearing dial
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(376.dp)
            ) {
                val dialWidth = (maxWidth * 0.46f).coerceIn(158.dp, 210.dp)
                val leftWidth = (maxWidth - dialWidth + 6.dp).coerceAtLeast(182.dp)

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(leftWidth)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HomeV3ProtocolSelector(
                        protocolIndex = state.protocolIndex,
                        onSelectProtocol = onSelectProtocol,
                        horizontalRow = false
                    )

                    HomeV3FocusPanel(
                        state = state,
                        focusedModule = focusedModule,
                        missingPermCount = missingPermCount,
                        onEngage = onOpenModule,
                        onStartTimer = onStartTimer,
                        onStopTimer = onStopTimer,
                        onLaunchApp = onLaunchAppFullscreen,
                        isLandscape = false,
                        engageTestTag = "portrait_target_lock_banner"
                    )

                    HomeV3ChipsRow(
                        missingPermCount = missingPermCount,
                        onOpenPermissions = onOpenPermissions,
                        onOpenOrbStudio = onOpenOrbStudio,
                        horizontal = false
                    )
                }

                // Extend flush to the right screen edge (cancelling the 10.dp horizontal padding)
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
                        .align(Alignment.CenterEnd)
                        .width(dialWidth + 10.dp)
                        .fillMaxHeight()
                        .testTag("portrait_rotary_dial")
                )
            }

            HomeV3CommandBar(
                commandText = commandText,
                onCommandTextChange = { commandText = it },
                voiceListening = voiceListening,
                voicePartial = voicePartial,
                onVoiceCommand = onVoiceCommand,
                onSendCommand = {
                    if (commandText.isNotBlank()) {
                        onAskStxAi(commandText)
                        commandText = ""
                    } else {
                        onOpenModule(HubModule.STX_AI)
                    }
                }
            )

            HomeV3Dock(
                installedApps = state.installedApps,
                isLandscape = false,
                onOpenAllAppsDrawer = onOpenAllAppsDrawer,
                onOpenModule = onOpenModule,
                onLaunchApp = onLaunchAppFullscreen
            )
        }
    }
}
