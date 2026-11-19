package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.StarkMode
import com.example.ui.StarkUiState
import com.example.ui.components.ArcReactorFullDial
import com.example.ui.components.HudBracketCard
import com.example.ui.components.LeftEdgeCommandDial
import com.example.ui.components.SciFiDualPulseBeam
import com.example.ui.components.StxEntrancePanel
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun PortraitCommandView(
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
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    val dialItems = remember(hubModules) {
        hubModules.map { m ->
            DialWedgeItem(
                id = m.id,
                badge = m.code,
                label = m.title,
                isAccentCrimson = m == HubModule.STX_AI
            )
        }
    }
    val focusedModule = hubModules.getOrElse(state.hubSelectedIndex) { HubModule.APPS }
    val targetAccent = if (focusedModule == HubModule.STX_AI) HudCrimson else HudCyan
    val pinnedApps = remember(state.installedApps) {
        state.installedApps.filter { it.isPinnedToDock }.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top MK-VI Chronometer & Uplink Telemetry Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.clock.timeText,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = HudIce,
                        maxLines = 1
                    )
                    Text(
                        text = ":${state.clock.secondsText}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = HudCyan,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                        maxLines = 1
                    )
                }
                Text(
                    text = "MK-VI // ${state.clock.dateText} · ${state.telemetry.networkTransport}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = HudSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (state.telemetry.isCharging) Color(0x2200E68A) else HudCyanSubtle
                        )
                        .border(
                            0.8.dp,
                            if (state.telemetry.isCharging) HudEmerald else HudCyanBorder,
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (state.telemetry.isCharging) "${state.telemetry.batteryPercent}%⚡" else "${state.telemetry.batteryPercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (state.telemetry.isCharging) HudEmerald else HudCyan,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (voiceListening) HudCrimsonSubtle else HudCyanSubtle)
                        .border(1.dp, if (voiceListening) HudCrimson else HudCyan, RoundedCornerShape(4.dp))
                        .clickable { onOpenModule(HubModule.STX_AI) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("portrait_stx_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (voiceListening) HudCrimson else HudCyan)
                        )
                        Text(
                            text = if (voiceListening) "LISTENING…" else "STX AI",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = HudIce,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Upper Hero: Clickable Target-Lock Pill + Polar Blueprint Crosshair + Bearing Reactor Dial
        StxEntrancePanel(
            visibleKey = "portrait_hero",
            slideFromY = -18f,
            initialScale = 0.90f,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Clickable Focused Sector Target-Lock Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xA6050D1A))
                        .border(1.dp, targetAccent.copy(alpha = 0.48f), RoundedCornerShape(4.dp))
                        .clickable { onOpenModule(focusedModule) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("portrait_target_lock_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "[${focusedModule.code}]",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = HudCrimson,
                            maxLines = 1
                        )
                        Text(
                            text = focusedModule.title,
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = HudIce,
                            maxLines = 1
                        )
                        Text(
                            text = "// ${focusedModule.subtitle}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.sp,
                            color = HudSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "ENGAGE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = targetAccent,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DialPolarBlueprintStage(modifier = Modifier.fillMaxSize())
                    ArcReactorFullDial(
                        items = dialItems,
                        selectedIndex = state.hubSelectedIndex,
                        onSelectIndex = onSelectHubIndex,
                        onActivateIndex = { idx ->
                            onOpenModule(hubModules.getOrElse(idx) { HubModule.APPS })
                        },
                        compactMode = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("portrait_rotary_dial")
                    )
                }
            }
        }

        // Middle Row: Left-Edge Protruding Half-Dial + Stark Glass Telemetry Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeftEdgeCommandDial(
                currentMode = state.mode,
                isFullscreen = state.preferences.fullscreenImmersive,
                isSettingsActive = state.mode == StarkMode.HALF_DIAL && state.activeModule == HubModule.SETTINGS,
                onFoldRotate = onFoldRotate,
                onSelectMode = onSelectMode,
                onOpenSettings = { onOpenModule(HubModule.SETTINGS) },
                onLockDeck = onLockDeck,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier
                    .width(126.dp)
                    .fillMaxHeight()
            )

            HudBracketCard(
                contentPadding = 10.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 6.dp, end = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${state.clock.greeting}, ${state.preferences.operatorCallsign}",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = HudIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SciFiDualPulseBeam(
                            isAlertCrimson = voiceListening,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${"%.1f".format(state.telemetry.batteryTempC)}°C",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = HudAmber
                            )
                            Text(
                                text = "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                color = HudCyan
                            )
                            Text(
                                text = "${state.installedApps.size} APPS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                color = HudCrimson
                            )
                        }
                    }

                    // Segmented live telemetry reactor gauges
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TelemetryBarGauge(
                            label = "PWR · ${state.telemetry.chargingSource}",
                            valueText = if (state.telemetry.isCharging) "${state.telemetry.batteryPercent}% ⚡" else "${state.telemetry.batteryPercent}%",
                            fraction = state.telemetry.batteryPercent / 100f,
                            fillColor = if (state.telemetry.isCharging) HudEmerald else HudCyan,
                            isChargingPulse = state.telemetry.isCharging
                        )
                        TelemetryBarGauge(
                            label = "MEM · ${state.telemetry.ramUsedMb}/${state.telemetry.ramTotalMb}MB",
                            valueText = "${state.telemetry.ramUsedPercent}%",
                            fraction = state.telemetry.ramUsedPercent / 100f,
                            fillColor = HudSilver
                        )
                        TelemetryBarGauge(
                            label = "STOR · ${"%.1f".format(state.telemetry.storageFreeGb)}GB FREE",
                            valueText = "${state.telemetry.storageUsedPercent}%",
                            fraction = state.telemetry.storageUsedPercent / 100f,
                            fillColor = HudCrimson
                        )
                    }
                }
            }
        }

        // Interactive ASK STX AI Transceiver Capsule
        StxEntrancePanel(
            visibleKey = "portrait_deck",
            delayMs = 40,
            slideFromY = 20f,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            HudBracketCard(contentPadding = 10.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (voiceListening && voicePartial.isNotBlank()) {
                                "MIC // $voicePartial"
                            } else {
                                "${state.telemetry.wifiSsid} · STX VOICE & COMMAND"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = if (voiceListening) HudCrimson else HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        VoiceWaveBars(
                            active = voiceListening,
                            barColor = if (voiceListening) HudCrimson else HudCyan,
                            modifier = Modifier
                                .width(24.dp)
                                .height(12.dp)
                        )
                    }

                    // Interactive ASK STX AI Input Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF040A12), Color(0xFF071220))
                                )
                            )
                            .border(
                                1.dp,
                                if (voiceListening) HudCrimson else HudCyanBorder,
                                RoundedCornerShape(23.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (commandText.isEmpty()) {
                                Text(
                                    text = if (voiceListening) "LISTENING…" else stringResource(R.string.ask_stx_ai_hint),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (voiceListening) HudCrimson else HudSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = commandText,
                                onValueChange = { commandText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    val q = commandText
                                    commandText = ""
                                    onAskStxAi(q.ifBlank { "system brief" })
                                }),
                                textStyle = TextStyle(
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 13.sp,
                                    color = HudIce
                                ),
                                cursorBrush = SolidColor(HudCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portrait_ask_input")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (voiceListening) HudCrimson.copy(alpha = 0.35f) else HudCrimsonSubtle)
                                .border(1.dp, if (voiceListening) HudIce else HudCrimsonBorder, CircleShape)
                                .clickable(onClick = onVoiceCommand)
                                .testTag("portrait_voice_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.cd_voice_command),
                                tint = if (voiceListening) HudIce else HudCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyan, CircleShape)
                                .clickable {
                                    val q = commandText
                                    commandText = ""
                                    onAskStxAi(q.ifBlank { "system brief" })
                                }
                                .testTag("portrait_send_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Submit STX AI command",
                                tint = HudCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6-Channel Tactical Relay Deck
        StxEntrancePanel(
            visibleKey = "portrait_toggles",
            delayMs = 70,
            slideFromY = 20f,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickHardwareToggle(
                        label = "WIFI",
                        icon = Icons.Default.Wifi,
                        active = state.telemetry.wifiConnected || state.telemetry.wifiEnabled,
                        crimsonWhenActive = false,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleWifi
                    )
                    QuickHardwareToggle(
                        label = "BT",
                        icon = Icons.Default.Bluetooth,
                        active = state.telemetry.bluetoothEnabled,
                        crimsonWhenActive = false,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleBluetooth
                    )
                    QuickHardwareToggle(
                        label = "FLASH",
                        icon = Icons.Default.FlashlightOn,
                        active = state.telemetry.flashlightOn,
                        crimsonWhenActive = true,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleFlash
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickHardwareToggle(
                        label = "LOCK",
                        icon = Icons.Default.Lock,
                        active = state.telemetry.touchLocked,
                        crimsonWhenActive = true,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleLock
                    )
                    QuickHardwareToggle(
                        label = "SYNC",
                        icon = Icons.Default.Sync,
                        active = true,
                        crimsonWhenActive = false,
                        modifier = Modifier.weight(1f),
                        onClick = onSync
                    )
                    QuickHardwareToggle(
                        label = "MUTE",
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        active = state.telemetry.audioMuted,
                        crimsonWhenActive = true,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleMute
                    )
                }
            }
        }

        // Bottom Stark Arc Quick-Launch Pedestal
        StxEntrancePanel(
            visibleKey = "portrait_dock",
            delayMs = 95,
            slideFromY = 22f,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            HudBracketCard(
                borderColor = HudCyanBorder,
                contentPadding = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pinnedApps.isNotEmpty()) {
                            pinnedApps.forEach { app ->
                                PinnedDockAppButton(
                                    app = app,
                                    onClick = { onLaunchAppFullscreen(app) }
                                )
                            }
                            ArcDockModuleButton(
                                label = "APPS",
                                icon = Icons.Default.Apps,
                                onClick = { onOpenModule(HubModule.APPS) }
                            )
                        } else {
                            ArcDockModuleButton(
                                label = "APPS",
                                icon = Icons.Default.Apps,
                                onClick = { onOpenModule(HubModule.APPS) }
                            )
                            ArcDockModuleButton(
                                label = "SEARCH",
                                icon = Icons.Default.Search,
                                onClick = { onOpenModule(HubModule.SEARCH) }
                            )
                            ArcDockModuleButton(
                                label = "VAULT",
                                icon = Icons.Default.Security,
                                onClick = { onOpenModule(HubModule.VAULT) }
                            )
                            ArcDockModuleButton(
                                label = "CAMERA",
                                icon = Icons.Default.CameraAlt,
                                onClick = { onOpenModule(HubModule.CAMERA) }
                            )
                            ArcDockModuleButton(
                                label = "SETTINGS",
                                icon = Icons.Default.Settings,
                                onClick = { onOpenModule(HubModule.SETTINGS) }
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(1.dp)
                                .background(HudCyan.copy(alpha = 0.45f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HudCrimson)
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(1.dp)
                                .background(HudCyan.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun ArcDockModuleButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(HudCyanSubtle)
                .border(1.dp, HudCyanBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = HudCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 8.5.sp,
            color = HudSilver,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PinnedDockAppButton(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A1624))
                .border(1.dp, HudCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = app.label.take(2).uppercase(),
                    fontFamily = OrbitronFontFamily,
                    fontSize = 9.sp,
                    color = HudCyan
                )
            }
        }
        Text(
            text = app.label.uppercase().take(7),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 8.5.sp,
            color = HudIce,
            maxLines = 1,
            softWrap = false
        )
    }
}
