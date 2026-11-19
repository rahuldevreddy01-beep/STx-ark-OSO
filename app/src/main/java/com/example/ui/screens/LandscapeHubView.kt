package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DialWedgeItem
import com.example.model.HubModule
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
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
fun LandscapeHubView(
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
    onFoldRotate: () -> Unit,
    onSelectMode: (StarkMode) -> Unit,
    onLockDeck: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialItems = remember(hubModules) {
        hubModules.map { m ->
            DialWedgeItem(
                id = m.id,
                badge = m.code,
                label = m.title,
                subLabel = m.subtitle,
                isAccentCrimson = m == HubModule.STX_AI
            )
        }
    }
    val focusedModule = hubModules.getOrElse(state.hubSelectedIndex) { HubModule.APPS }
    val targetAccent = if (focusedModule == HubModule.STX_AI) HudCrimson else HudCyan

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 10.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // LEFT EDGE — Protruding Mode Half-Dial
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
                .weight(0.21f)
                .fillMaxHeight()
        )

        // CENTERPIECE — Iron Man 2 MK-VI Holographic Command Stage
        StxEntrancePanel(
            visibleKey = "hub_center_land",
            delayMs = 35,
            slideFromY = 16f,
            modifier = Modifier
                .weight(0.51f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Micro Stark Telemetry Header Line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HudCyan)
                        )
                        Text(
                            text = "STX-MK.VI // ${state.clock.greeting}, ${state.preferences.operatorCallsign}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.5.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${state.installedApps.size} APPS · ${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp,
                        color = HudAmber,
                        maxLines = 1
                    )
                }

                // Clickable Stark Target-Lock Banner for focused dial wedge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xA6050D1A))
                        .border(1.dp, targetAccent.copy(alpha = 0.52f), RoundedCornerShape(4.dp))
                        .clickable { onOpenModule(focusedModule) }
                        .testTag("hub_target_lock_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
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
                                fontSize = 9.5.sp,
                                color = HudCrimson,
                                maxLines = 1
                            )
                            Text(
                                text = focusedModule.title,
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(targetAccent.copy(alpha = 0.16f))
                                .border(0.8.dp, targetAccent.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ENGAGE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                color = HudIce,
                                maxLines = 1
                            )
                        }
                    }
                    SciFiDualPulseBeam(
                        isAlertCrimson = focusedModule == HubModule.STX_AI,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                // Polar Blueprint Radar Stage + Smooth Bearing Arc Reactor Dial
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("hub_rotary_dial")
                    )
                }

                // Upgraded Stark Voice Transceiver & Wave Equalizer Capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xBA060E1B), Color(0xCC0A1626), Color(0xBA060E1B))
                            )
                        )
                        .border(
                            1.dp,
                            if (voiceListening) HudCrimsonBorder else HudCyanBorder,
                            RoundedCornerShape(5.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (voiceListening) HudCrimson.copy(alpha = 0.35f) else HudCrimsonSubtle)
                            .border(1.dp, if (voiceListening) HudIce else HudCrimson, CircleShape)
                            .clickable(onClick = onVoiceCommand)
                            .testTag("hub_voice_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice command STX AI",
                            tint = if (voiceListening) HudIce else HudCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    VoiceWaveBars(
                        active = voiceListening,
                        barColor = if (voiceListening) HudCrimson else HudCyan,
                        modifier = Modifier
                            .width(28.dp)
                            .height(18.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenModule(HubModule.STX_AI) }
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = if (voiceListening) "AUDIO UPLINK ACTIVE" else "STX AI // VOICE CORE",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = if (voiceListening) HudCrimson else HudIce,
                            maxLines = 1
                        )
                        Text(
                            text = if (voiceListening && voicePartial.isNotBlank()) voicePartial else "TAP MIC OR OPEN COMMS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable { onOpenModule(HubModule.SETTINGS) }
                            .testTag("hub_settings_shortcut"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open STX OS Settings",
                            tint = HudCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // RIGHT SIDEBAR — Stark Smoked-Glass Telemetry Blade & 6 Tactical Relay Toggles
        StxEntrancePanel(
            visibleKey = "hub_right_land",
            delayMs = 60,
            slideFromX = 26f,
            slideFromY = 0f,
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight()
        ) {
            HudBracketCard(
                borderColor = HudCyanBorder,
                containerColor = ObsidianGlass,
                contentPadding = 10.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Chronometer + Thermal + RF Status Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x22FFB020))
                                    .border(0.8.dp, HudAmber.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${"%.1f".format(state.telemetry.batteryTempC)}°C",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    color = HudAmber,
                                    maxLines = 1
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = state.clock.timeText,
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HudIce,
                                    maxLines = 1
                                )
                                Text(
                                    text = ":${state.clock.secondsText}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = HudCyan,
                                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UPTIME",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = HudCrimson,
                                maxLines = 1
                            )
                            Text(
                                text = state.telemetry.uptimeFormatted,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = HudIce,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.telemetry.networkTransport,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                color = HudSlate,
                                maxLines = 1
                            )
                            Text(
                                text = state.telemetry.wifiSsid,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                color = if (state.telemetry.wifiConnected) HudCyan else HudSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Segmented Stark Reactor Telemetry Bars
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        TelemetryBarGauge(
                            label = "PWR · ${state.telemetry.chargingSource}",
                            valueText = if (state.telemetry.isCharging) "${state.telemetry.batteryPercent}% ⚡" else "${state.telemetry.batteryPercent}%",
                            fraction = state.telemetry.batteryPercent / 100f,
                            fillColor = if (state.telemetry.isCharging) HudEmerald else HudCyan,
                            isChargingPulse = state.telemetry.isCharging
                        )
                        TelemetryBarGauge(
                            label = "RF · ${state.telemetry.networkTransport}",
                            valueText = "${state.telemetry.wifiSignalPercent}%",
                            fraction = state.telemetry.wifiSignalPercent / 100f,
                            fillColor = HudCrimson
                        )
                        TelemetryBarGauge(
                            label = "MEM · ${state.telemetry.ramUsedMb}/${state.telemetry.ramTotalMb}MB",
                            valueText = "${state.telemetry.ramUsedPercent}%",
                            fraction = state.telemetry.ramUsedPercent / 100f,
                            fillColor = HudSilver
                        )
                    }

                    // Tactical 6-Channel Relay Matrix
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SciFiDualPulseBeam(
                            isAlertCrimson = state.telemetry.flashlightOn || state.telemetry.touchLocked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RELAY MATRIX", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = HudSlate, maxLines = 1)
                            Text("6 CH", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = HudCyan, maxLines = 1)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickHardwareToggle(
                                label = "WIFI",
                                icon = Icons.Default.Wifi,
                                active = state.telemetry.wifiConnected || state.telemetry.wifiEnabled,
                                crimsonWhenActive = false,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_wifi",
                                onClick = onToggleWifi
                            )
                            QuickHardwareToggle(
                                label = "BT",
                                icon = Icons.Default.Bluetooth,
                                active = state.telemetry.bluetoothEnabled,
                                crimsonWhenActive = false,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_bt",
                                onClick = onToggleBluetooth
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickHardwareToggle(
                                label = "FLASH",
                                icon = Icons.Default.FlashlightOn,
                                active = state.telemetry.flashlightOn,
                                crimsonWhenActive = true,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_flash",
                                onClick = onToggleFlash
                            )
                            QuickHardwareToggle(
                                label = "LOCK",
                                icon = Icons.Default.Lock,
                                active = state.telemetry.touchLocked,
                                crimsonWhenActive = true,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_lock",
                                onClick = onToggleLock
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickHardwareToggle(
                                label = "SYNC",
                                icon = Icons.Default.Sync,
                                active = true,
                                crimsonWhenActive = false,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_sync",
                                onClick = onSync
                            )
                            QuickHardwareToggle(
                                label = "MUTE",
                                icon = Icons.AutoMirrored.Filled.VolumeOff,
                                active = state.telemetry.audioMuted,
                                crimsonWhenActive = true,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_mute",
                                onClick = onToggleMute
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialPolarBlueprintStage(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "polar_blueprint")
    val outerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "blueprint_orbit"
    )
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val maxR = (min(size.width, size.height) * 0.49f).coerceAtLeast(1f)

        // Precision orthogonal HUD crosshair rays outside dial
        val innerGap = maxR * 0.92f
        val outerExtent = maxR * 1.12f
        drawLine(
            color = HudCyan.copy(alpha = 0.22f),
            start = Offset(cx - outerExtent, cy),
            end = Offset(cx - innerGap, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = HudCyan.copy(alpha = 0.22f),
            start = Offset(cx + innerGap, cy),
            end = Offset(cx + outerExtent, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = HudCrimson.copy(alpha = 0.24f),
            start = Offset(cx, cy - outerExtent),
            end = Offset(cx, cy - innerGap),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = HudCrimson.copy(alpha = 0.24f),
            start = Offset(cx, cy + innerGap),
            end = Offset(cx, cy + outerExtent),
            strokeWidth = 1.dp.toPx()
        )

        // Subtle rotating calibration arc framing the entire dial
        val arcR = maxR * 1.03f
        val topLeft = Offset(cx - arcR, cy - arcR)
        val arcSize = Size(arcR * 2f, arcR * 2f)
        drawArc(
            color = HudCyan.copy(alpha = 0.16f),
            startAngle = outerOrbit,
            sweepAngle = 54f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = HudAmber.copy(alpha = 0.18f),
            startAngle = outerOrbit + 180f,
            sweepAngle = 42f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun VoiceWaveBars(
    active: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "voice_wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 620 else 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_wave_phase"
    )
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val barCount = 5
        val spacing = size.width / barCount
        val barW = (spacing * 0.48f).coerceAtLeast(1.5f)
        for (i in 0 until barCount) {
            val amp = ((sin(phase + i * 1.15f) + 1f) * 0.5f)
            val normH = if (active) (0.28f + 0.72f * amp) else (0.18f + 0.24f * amp)
            val h = (size.height * normH).coerceAtLeast(2f)
            val x = spacing * i + (spacing - barW) * 0.5f
            val y = (size.height - h) * 0.5f
            drawLine(
                color = barColor.copy(alpha = if (active) 0.92f else 0.52f),
                start = Offset(x + barW * 0.5f, y),
                end = Offset(x + barW * 0.5f, y + h),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TelemetryBarGauge(
    label: String,
    valueText: String,
    fraction: Float,
    fillColor: Color,
    isChargingPulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0.03f, 1f),
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "telemetry_bar_fraction"
    )
    val pulsePos = if (isChargingPulse) {
        val infinite = rememberInfiniteTransition(label = "bar_charge_pulse")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "bar_charge_x"
        )
        p
    } else {
        null
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.5.sp,
                color = HudSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            )
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = fillColor,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0E1A28))
                .border(0.6.dp, fillColor.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(fillColor.copy(alpha = 0.65f), fillColor)
                        )
                    )
            )
            // Segmented Stark reactor divider notches + hot tip spark
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val segments = 8
                val segW = size.width / segments
                for (s in 1 until segments) {
                    val x = segW * s
                    drawLine(
                        color = Color(0x9902060D),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val tipX = (size.width * animatedFraction).coerceIn(1f, size.width)
                drawLine(
                    color = HudIce.copy(alpha = 0.88f),
                    start = Offset(tipX, 0f),
                    end = Offset(tipX, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
                if (pulsePos != null) {
                    val fillW = (size.width * animatedFraction.coerceIn(0.05f, 1f)).coerceAtLeast(1f)
                    val glowX = pulsePos * fillW
                    val startX = (glowX - 24f).coerceIn(0f, (fillW - 1f).coerceAtLeast(0f))
                    val endX = (glowX + 24f).coerceIn(startX + 1f, fillW.coerceAtLeast(startX + 1f))
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, HudIce.copy(alpha = 0.85f), Color.Transparent),
                            startX = startX,
                            endX = endX
                        ),
                        size = Size(fillW, size.height)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickHardwareToggle(
    label: String,
    icon: ImageVector,
    active: Boolean,
    crimsonWhenActive: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f),
        label = "toggle_press_scale"
    )
    val activeColor = if (crimsonWhenActive) HudCrimson else HudCyan
    val border by animateColorAsState(
        targetValue = if (active) activeColor else Color(0x366A8399),
        animationSpec = tween(durationMillis = 200),
        label = "toggle_border"
    )
    val bg by animateColorAsState(
        targetValue = when {
            active && crimsonWhenActive -> HudCrimsonSubtle
            active -> HudCyanSubtle
            else -> Color(0x66060C14)
        },
        animationSpec = tween(durationMillis = 200),
        label = "toggle_bg"
    )
    val fg = if (active) HudIce else HudSlate

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        // Top-right micro LED relay indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color(0x336F88A0))
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeColor else HudSlate,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontSize = 9.5.sp,
                color = fg,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
