package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
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

private data class WifiPage(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun WifiScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit = {},
    onOpenWifiPanel: () -> Unit,
    onOpenBluetoothPanel: () -> Unit,
    onPermissionsUpdated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasLocationPerm by remember {
        mutableStateOf(LauncherPermissionManager.hasAnyLocationPermission(context))
    }
    var hasBtPerm by remember {
        mutableStateOf(LauncherPermissionManager.hasBluetoothPermission(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val locNow = LauncherPermissionManager.hasAnyLocationPermission(context)
                val btNow = LauncherPermissionManager.hasBluetoothPermission(context)
                val changed = (locNow && !hasLocationPerm) || (btNow && !hasBtPerm)
                hasLocationPerm = locNow
                hasBtPerm = btNow
                if (changed) onPermissionsUpdated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPerm = LauncherPermissionManager.hasAnyLocationPermission(context)
        onPermissionsUpdated()
    }
    val btLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasBtPerm = LauncherPermissionManager.hasBluetoothPermission(context)
        onPermissionsUpdated()
    }

    val cards = remember(
        state.telemetry.networkTransport,
        state.telemetry.wifiSignalPercent,
        state.telemetry.ipv4Address,
        state.telemetry.bluetoothStateLabel
    ) {
        listOf(
            WifiPage("link", "01", state.telemetry.wifiSsid, "ACTIVE TRANSPORT", state.telemetry.networkTransport),
            WifiPage("rf", "02", "SIGNAL TELEMETRY", "RSSI & FREQUENCY BAND", "${state.telemetry.wifiSignalPercent}%"),
            WifiPage("ip", "03", "IPv4 & DNS ROUTE", "NETWORK ADDRESSING", state.telemetry.ipv4Address),
            WifiPage("bt", "04", "BLUETOOTH RADIO", "SHORT-RANGE PAIRED", state.telemetry.bluetoothStateLabel),
            WifiPage("panel", "05", "OS RF SWITCHBOARD", "SYSTEM WIFI & BT PANELS", "OS WIFI", true)
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
                Text("WIFI & RF LINK", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
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
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous RF card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex + 1) % cards.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next RF card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${c.code} ${c.id.uppercase()}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = if (sel) HudIce else HudSilver, maxLines = 1)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.title, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HudIce, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        "link" -> {
                            WifiSpecRow("TRANSPORT", state.telemetry.networkTransport, HudCyan)
                            WifiSpecRow("ACTIVE SSID", state.telemetry.wifiSsid, HudIce)
                            WifiSpecRow(
                                "ADAPTER STATE",
                                when {
                                    state.telemetry.wifiConnected -> "CONNECTED"
                                    state.telemetry.wifiEnabled -> "ENABLED / SCANNING"
                                    else -> "DISABLED"
                                },
                                if (state.telemetry.wifiConnected) HudEmerald else HudCrimson
                            )
                            if (!hasLocationPerm) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCrimsonSubtle)
                                        .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                                        .clickable {
                                            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                        .testTag("wifi_grant_location_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("GRANT LOCATION FOR SSID", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onOpenWifiPanel)
                                    .testTag("wifi_open_os_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Wifi, "Open Wi-Fi panel", tint = HudCyan, modifier = Modifier.size(16.dp))
                                    Text("OPEN OS WIFI PANEL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                }
                            }
                        }

                        "rf" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("SIGNAL STRENGTH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text("${state.telemetry.wifiSignalPercent}%", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = HudEmerald)
                                }
                                Text("${state.telemetry.wifiRssiDbm} dBm", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudCyan)
                            }
                            LinearProgressIndicator(
                                progress = { (state.telemetry.wifiSignalPercent / 100f).coerceIn(0f, 1f) },
                                color = HudEmerald,
                                trackColor = Color(0xFF0A1624),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            WifiSpecRow("LINK SPEED", "${state.telemetry.wifiLinkSpeedMbps} Mbps", HudIce)
                            WifiSpecRow("BAND / FREQ", "${state.telemetry.wifiBandLabel} · ${state.telemetry.wifiFrequencyMhz} MHz", HudCyan)
                        }

                        "ip" -> {
                            WifiSpecRow("IPv4 ADDRESS", state.telemetry.ipv4Address, HudCyan)
                            WifiSpecRow("DNS SERVER", state.telemetry.dnsServer, HudIce)
                            WifiSpecRow("CARRIER / LINK", state.telemetry.networkTransport, HudSilver)
                            WifiSpecRow("BANDWIDTH", "${state.telemetry.wifiLinkSpeedMbps} Mbps", HudEmerald)
                        }

                        "bt" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("BLUETOOTH ADAPTER", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(state.telemetry.bluetoothStateLabel, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (state.telemetry.bluetoothEnabled) HudCyan else HudCrimson)
                                }
                                Icon(Icons.Default.Bluetooth, "Bluetooth status", tint = if (state.telemetry.bluetoothEnabled) HudCyan else HudSlate, modifier = Modifier.size(26.dp))
                            }
                            WifiSpecRow("PAIRED DEVICES", "${state.telemetry.bluetoothPairedCount} BONDED", HudIce)
                            if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                        .clickable { btLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)) }
                                        .testTag("wifi_grant_bt_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("GRANT BLUETOOTH ACCESS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onOpenBluetoothPanel)
                                    .testTag("wifi_open_bt_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OPEN OS BLUETOOTH", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                            }
                        }

                        "panel" -> {
                            Text("Launch native Android RF switchboards directly from STX OS.", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSilver)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onOpenWifiPanel),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SWITCH WIFI NETWORK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onOpenBluetoothPanel),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("BLUETOOTH SETTINGS OS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiSpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}
