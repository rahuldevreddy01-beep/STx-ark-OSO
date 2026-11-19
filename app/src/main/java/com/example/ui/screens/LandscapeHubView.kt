package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.ui.StarkUiState
import com.example.ui.components.ArcReactorFullDial
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.cos
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

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // LEFT SIDEBAR (01) — Real Android Battery, Network, RAM, Compass & Steps
        Column(
            modifier = Modifier
                .weight(0.26f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "01 // TELEMETRY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = HudCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${state.clock.greeting}, ${state.preferences.operatorCallsign}",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = HudIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "STX AI · ${state.installedApps.size} APPS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = HudCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(color = HudDivider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CompassMiniRadar(
                            azimuthDeg = state.telemetry.azimuthDeg,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = HudIce,
                                maxLines = 1
                            )
                            Text(
                                text = "${state.telemetry.stepsCount} STEPS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                color = HudSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                            .clickable { onOpenModule(HubModule.BRIEF) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("hub_briefing_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BRIEF",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = HudIce,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Divider hairline
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(HudDivider)
        )

        // CENTERPIECE — Lubricated Bearing Rotary Arc-Reactor Dial
        Column(
            modifier = Modifier
                .weight(0.48f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "[${focusedModule.code}]",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = HudCrimson,
                    maxLines = 1
                )
                Text(
                    text = "${focusedModule.title} · TAP WEDGE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = HudIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ArcReactorFullDial(
                items = dialItems,
                selectedIndex = state.hubSelectedIndex,
                onSelectIndex = onSelectHubIndex,
                onActivateIndex = { idx ->
                    onOpenModule(hubModules.getOrElse(idx) { HubModule.APPS })
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("hub_rotary_dial")
            )

            // Functional Voice Command & Settings Trigger Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x99060E18))
                    .border(1.dp, if (voiceListening) HudCrimson else HudCyanBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onOpenModule(HubModule.STX_AI) }
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (voiceListening) "MIC LISTENING…" else "STX AI VOICE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        color = if (voiceListening) HudCrimson else HudIce,
                        maxLines = 1
                    )
                    Text(
                        text = if (voiceListening && voicePartial.isNotBlank()) voicePartial else "TAP MIC OR QUERY",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
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

        // Divider hairline
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(HudDivider)
        )

        // RIGHT SIDEBAR (02) — Real Uptime, Temp & 6 Real Hardware Quick Toggles
        Column(
            modifier = Modifier
                .weight(0.26f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${"%.1f".format(state.telemetry.batteryTempC)}°C",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudSlate,
                        maxLines = 1
                    )
                    Text(
                        text = "${state.clock.timeText}:${state.clock.secondsText}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HudIce,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "UPTIME",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudCrimson,
                        maxLines = 1
                    )
                    Text(
                        text = "02",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = state.telemetry.uptimeFormatted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = HudIce,
                    maxLines = 1
                )
                Text(
                    text = state.telemetry.wifiSsid,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = if (state.telemetry.wifiConnected) HudCyan else HudSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SYSTEM", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
                    Text("6 CH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudCyan, maxLines = 1)
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
                        icon = Icons.Default.VolumeOff,
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

@Composable
fun TelemetryBarGauge(
    label: String,
    valueText: String,
    fraction: Float,
    fillColor: Color,
    isChargingPulse: Boolean = false,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.weight(1f).padding(end = 6.dp)
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
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF101C28))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .background(fillColor)
            )
            if (pulsePos != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val fillW = size.width * fraction.coerceIn(0.05f, 1f)
                    val glowX = pulsePos * fillW
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, HudIce.copy(alpha = 0.85f), Color.Transparent),
                            startX = (glowX - 24f).coerceAtLeast(0f),
                            endX = (glowX + 24f).coerceAtMost(fillW)
                        ),
                        size = androidx.compose.ui.geometry.Size(fillW, size.height)
                    )
                }
            }
        }
    }
}

@Composable
fun CompassMiniRadar(
    azimuthDeg: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF07131E), radius = r, center = c)
        drawCircle(color = HudCyan.copy(alpha = 0.45f), radius = r, center = c, style = Stroke(1.5f))
        drawCircle(color = HudCyan.copy(alpha = 0.25f), radius = r * 0.55f, center = c, style = Stroke(1f))

        val rad = Math.toRadians((azimuthDeg - 90).toDouble())
        val tip = Offset(c.x + (r * 0.85f * cos(rad)).toFloat(), c.y + (r * 0.85f * sin(rad)).toFloat())
        drawLine(
            color = HudCrimson,
            start = c,
            end = tip,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = HudCyan, radius = 2.dp.toPx(), center = c)
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
    val activeColor = if (crimsonWhenActive) HudCrimson else HudCyan
    val border = if (active) activeColor else Color(0x336A8399)
    val bg = when {
        active && crimsonWhenActive -> HudCrimsonSubtle
        active -> HudCyanSubtle
        else -> Color(0x55060C14)
    }
    val fg = if (active) HudIce else HudSlate

    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
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
