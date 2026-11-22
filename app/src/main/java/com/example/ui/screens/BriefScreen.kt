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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InstalledApp
import com.example.ui.StarkUiState
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

private data class BriefPage(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun BriefScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit = {},
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStxAi: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedApps = remember(state.installedApps, state.preferences.pinnedPackages) {
        state.installedApps.filter { it.isPinnedToDock }
    }
    val cards = remember(state.telemetry.ramUsedPercent, pinnedApps.size, state.clock.timeText) {
        listOf(
            BriefPage("overview", "01", "EXECUTIVE BRIEF", "LIVE OS SUMMARY", state.clock.timeText),
            BriefPage("hardware", "02", "SYSTEM TELEMETRY", "MEMORY & STORAGE", "${state.telemetry.ramUsedPercent}% RAM"),
            BriefPage("dock", "03", "PINNED ARC DOCK", "STARRED SHORTCUTS", "${pinnedApps.size} PINNED", pinnedApps.isNotEmpty())
        )
    }

    val safeIndex = focusedIndex.coerceIn(0, cards.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { cards.size })
    LaunchedEffect(safeIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != safeIndex) {
            onSelectIndex(pagerState.settledPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xD8050A12))
                .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HudCrimsonSubtle)
                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("half_dial_center_back"),
                contentAlignment = Alignment.Center
            ) {
                Text("< BACK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("BRIEF", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
                Text("${cards[safeIndex].code} // ${cards[safeIndex].subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = HudCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex - 1 + cards.size) % cards.size) }
                        .testTag("carousel_prev_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous brief card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex + 1) % cards.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next brief card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cards.forEachIndexed { idx, c ->
                val sel = idx == safeIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) HudCyanSubtle else Color(0x8806101B))
                        .border(1.dp, if (sel) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${c.code} ${c.id.uppercase()}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (sel) HudIce else HudSilver, maxLines = 1)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { cards[it].id },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val card = cards[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEB040911))
                    .border(1.2.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
                    .then(if (page == pagerState.currentPage) Modifier.testTag("carousel_card_${card.id}") else Modifier),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(card.title, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HudIce)
                        Text("${card.code} // ${card.subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudCyan)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (card.isCrimson) HudCrimsonSubtle else HudCyanSubtle)
                            .border(0.8.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(card.badge, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = HudIce)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HudCyan.copy(alpha = 0.22f)))

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "overview" -> {
                            Text("${state.clock.greeting}, ${state.preferences.operatorCallsign}", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HudCyan)
                            Text("${state.clock.dateText} · ${state.telemetry.deviceModel}", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudSilver)
                            BriefSpecRow("POWER CELL", "${state.telemetry.batteryPercent}% (${state.telemetry.chargingSource})", HudEmerald)
                            BriefSpecRow("NETWORK LINK", "${state.telemetry.networkTransport} · ${state.telemetry.wifiSsid}", HudCyan)
                            BriefSpecRow("SENSORS & APPS", "${state.telemetry.stepsCount} STEPS · ${state.installedApps.size} APPS", HudIce)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                        .clickable(onClick = onOpenApps)
                                        .testTag("brief_open_apps_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Apps, "Open apps", tint = HudCyan, modifier = Modifier.size(14.dp))
                                        Text("APPS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCrimsonSubtle)
                                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                        .clickable(onClick = onOpenSettings)
                                        .testTag("brief_open_settings_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Settings, "Open settings", tint = HudCrimson, modifier = Modifier.size(14.dp))
                                        Text("CONFIG", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x66061826))
                                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                        .clickable(onClick = onOpenStxAi)
                                        .testTag("brief_open_stx_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("STX AI", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudCyan)
                                }
                            }
                        }

                        "hardware" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SYSTEM MEMORY", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                Text("${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB (${state.telemetry.ramUsedPercent}%)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudCyan)
                            }
                            LinearProgressIndicator(
                                progress = { (state.telemetry.ramUsedPercent / 100f).coerceIn(0f, 1f) },
                                color = HudCyan,
                                trackColor = Color(0xFF0A1624),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("INTERNAL STORAGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                Text("${"%.1f".format(state.telemetry.storageFreeGb)} GB FREE (${state.telemetry.storageUsedPercent}%)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudCrimson)
                            }
                            LinearProgressIndicator(
                                progress = { (state.telemetry.storageUsedPercent / 100f).coerceIn(0f, 1f) },
                                color = HudCrimson,
                                trackColor = Color(0xFF14080D),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            BriefSpecRow("THERMAL / VOLT", "${state.telemetry.batteryTempC}°C · ${state.telemetry.batteryVoltageMv} mV · ${state.telemetry.batteryHealth}", HudEmerald)
                            BriefSpecRow("COMPUTE / OS", "${state.telemetry.cpuCores} CORES · ANDROID ${state.telemetry.androidVersion}", HudIce)
                            BriefSpecRow("SESSION UPTIME", state.telemetry.uptimeFormatted, HudSilver)
                        }

                        "dock" -> {
                            if (pinnedApps.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x5506101B))
                                        .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Star, "Pinned dock", tint = HudCyan, modifier = Modifier.size(24.dp))
                                    Text("NO APPS PINNED TO ARC DOCK YET", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    Text("Star apps in the Apps focus view to dock them here for instant launch.", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCyanSubtle)
                                            .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                            .clickable(onClick = onOpenApps)
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("OPEN APPS MATRIX", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    }
                                }
                            } else {
                                pinnedApps.forEach { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x6606101B))
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .clickable { onLaunchApp(app) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0A1624))
                                                .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (app.icon != null) Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(22.dp))
                                            else Text(app.label.take(2).uppercase(), fontFamily = OrbitronFontFamily, fontSize = 10.sp, color = HudCyan)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.label.uppercase(), fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HudIce, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${app.categoryTag} · ${app.launchCount} RUNS", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudCyan)
                                        }
                                        Icon(Icons.Default.OpenInFull, "Launch ${app.label}", tint = HudCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefSpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}
