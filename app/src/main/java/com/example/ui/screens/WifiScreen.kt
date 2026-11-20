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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.ui.StarkUiState
import com.example.ui.components.CarouselSpecRow
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.HolographicCardHeader
import com.example.ui.components.HolographicSideCardFooter
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

private data class WifiDeckCard(
    val id: String,
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
            WifiDeckCard(
                id = "link",
                title = state.telemetry.wifiSsid,
                subtitle = "01 RF LINK // ACTIVE TRANSPORT",
                badge = state.telemetry.networkTransport
            ),
            WifiDeckCard(
                id = "rf",
                title = "SIGNAL TELEMETRY",
                subtitle = "02 SPECTRUM // RSSI & BAND",
                badge = "${state.telemetry.wifiSignalPercent}%"
            ),
            WifiDeckCard(
                id = "ip",
                title = "IPv4 & DNS ROUTE",
                subtitle = "03 NETWORK // ADDRESSING",
                badge = state.telemetry.ipv4Address
            ),
            WifiDeckCard(
                id = "bt",
                title = "BLUETOOTH RADIO",
                subtitle = "04 SHORT-RANGE // PAIRED",
                badge = state.telemetry.bluetoothStateLabel
            ),
            WifiDeckCard(
                id = "panel",
                title = "OS RF SWITCHBOARD",
                subtitle = "05 SYSTEM // WIFI & BT PANELS",
                badge = "OS WIFI",
                isCrimson = true
            )
        )
    }

    HolographicCardCarousel(
        items = cards,
        focusedIndex = focusedIndex,
        onSelectIndex = onSelectIndex,
        itemKey = { _, card -> card.id },
        isCrimsonAccent = { _, card, _ -> card.isCrimson },
        modifier = modifier
    ) { idx, card, isCenter, isCompactStage, _, relOffset ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isCenter) Modifier.testTag("carousel_card_${card.id}") else Modifier),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HolographicCardHeader(
                index = idx,
                totalCount = cards.size,
                moduleLabel = "WIFI",
                title = card.title,
                subtitle = card.subtitle,
                badge = card.badge,
                isCenter = isCenter,
                isCompactStage = isCompactStage,
                crimsonBadge = card.isCrimson
            )

            if (isCenter) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "link" -> {
                            CarouselSpecRow(
                                label = "TRANSPORT",
                                value = state.telemetry.networkTransport,
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "ACTIVE SSID",
                                value = state.telemetry.wifiSsid,
                                valueColor = HudIce,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "ADAPTER STATE",
                                value = when {
                                    state.telemetry.wifiConnected -> "CONNECTED"
                                    state.telemetry.wifiEnabled -> "ENABLED / SCANNING"
                                    else -> "DISABLED"
                                },
                                valueColor = if (state.telemetry.wifiConnected) HudEmerald else HudCrimson,
                                compact = isCompactStage
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
                                            locationLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                        .testTag("wifi_grant_location_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "GRANT LOCATION FOR SSID",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = HudIce
                                    )
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("SIGNAL STRENGTH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(
                                        text = "${state.telemetry.wifiSignalPercent}%",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactStage) 20.sp else 26.sp,
                                        color = HudEmerald
                                    )
                                }
                                Text(
                                    text = "${state.telemetry.wifiRssiDbm} dBm",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = HudCyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (state.telemetry.wifiSignalPercent / 100f).coerceIn(0f, 1f) },
                                color = HudEmerald,
                                trackColor = Color(0xFF0A1624),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            CarouselSpecRow(
                                label = "LINK SPEED",
                                value = "${state.telemetry.wifiLinkSpeedMbps} Mbps",
                                valueColor = HudIce,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "BAND / FREQ",
                                value = "${state.telemetry.wifiBandLabel} · ${state.telemetry.wifiFrequencyMhz} MHz",
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                        }

                        "ip" -> {
                            CarouselSpecRow(
                                label = "IPv4 ADDRESS",
                                value = state.telemetry.ipv4Address,
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "DNS SERVER",
                                value = state.telemetry.dnsServer,
                                valueColor = HudIce,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "CARRIER / LINK",
                                value = state.telemetry.networkTransport,
                                valueColor = HudSilver,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "BANDWIDTH",
                                value = "${state.telemetry.wifiLinkSpeedMbps} Mbps",
                                valueColor = HudEmerald,
                                compact = isCompactStage
                            )
                        }

                        "bt" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BLUETOOTH ADAPTER", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(
                                        text = state.telemetry.bluetoothStateLabel,
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactStage) 16.sp else 20.sp,
                                        color = if (state.telemetry.bluetoothEnabled) HudCyan else HudCrimson
                                    )
                                }
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth status",
                                    tint = if (state.telemetry.bluetoothEnabled) HudCyan else HudSlate,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            CarouselSpecRow(
                                label = "PAIRED DEVICES",
                                value = "${state.telemetry.bluetoothPairedCount} BONDED",
                                valueColor = HudIce,
                                compact = isCompactStage
                            )
                            if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                        .clickable {
                                            btLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.BLUETOOTH_CONNECT,
                                                    Manifest.permission.BLUETOOTH_SCAN
                                                )
                                            )
                                        }
                                        .testTag("wifi_grant_bt_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "GRANT BLUETOOTH ACCESS",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = HudIce
                                    )
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
                            Text(
                                text = "Launch native Android RF switchboards directly from STX OS.",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = HudSilver
                            )
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
            } else {
                Spacer(modifier = Modifier.weight(1f))
                HolographicSideCardFooter(relOffset = relOffset)
            }
        }
    }
}
