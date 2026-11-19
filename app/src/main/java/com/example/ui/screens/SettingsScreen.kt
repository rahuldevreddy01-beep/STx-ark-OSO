package com.example.ui.screens

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.SettingsSection
import com.example.security.StarkDeviceAdminReceiver
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
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily

@Composable
fun SettingsScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectSection: (Int) -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onReloadApps: () -> Unit,
    onSetCallsign: (String) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onToggleKeepAwake: (Boolean) -> Unit,
    onToggleAutoMorph: (Boolean) -> Unit,
    onToggleEdgeColor: (Boolean) -> Unit,
    onToggleSystemFloatingAi: (Boolean) -> Unit,
    onPermissionsUpdated: () -> Unit,
    onSetLockCoverEnabled: (Boolean) -> Unit,
    onSetAutoLockOnScreenOff: (Boolean) -> Unit,
    onSetBiometricUnlockEnabled: (Boolean) -> Unit,
    onSetUseOsPhysicalLock: (Boolean) -> Unit,
    onSavePasscode: (String, Boolean) -> Unit,
    onClearPasscode: () -> Unit,
    onSetLockWidgets: (Boolean?, Boolean?, Boolean?) -> Unit,
    onSetLockHeadline: (String) -> Unit,
    onEngageLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = SettingsSection.entries
    val safeIndex = focusedIndex.coerceIn(0, sections.lastIndex)
    val context = LocalContext.current
    var callsignInput by remember(state.preferences.operatorCallsign) {
        mutableStateOf(state.preferences.operatorCallsign)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionsUpdated()
    }

    val canDrawOverlaysCached = remember(state.preferences.systemFloatingAiEnabled, state.mode) {
        Settings.canDrawOverlays(context)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Clickable 01..06 Section Tabs synced bidirectionally with carousel & right half-dial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sections.forEachIndexed { idx, sec ->
                val selected = idx == safeIndex
                val isLockTab = sec == SettingsSection.LOCKSCREEN
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                selected && isLockTab -> HudCrimsonSubtle
                                selected -> HudCyanSubtle
                                else -> Color(0x8806101B)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                selected && isLockTab -> HudCrimson
                                selected -> HudCyan
                                isLockTab -> HudCrimsonBorder
                                else -> HudDivider
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectSection(idx) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("settings_tab_${sec.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sec.code} ${sec.title}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 9.5.sp,
                        color = if (selected) HudIce else if (isLockTab) HudCrimson else HudSilver
                    )
                }
            }
        }

        HolographicCardCarousel(
            items = sections,
            focusedIndex = safeIndex,
            onSelectIndex = onSelectSection,
            itemKey = { _, sec -> sec.name },
            isCrimsonAccent = { _, sec, _ -> sec == SettingsSection.LAUNCHER || sec == SettingsSection.LOCKSCREEN },
            maxCardWidth = 540.dp,
            maxCardHeight = 460.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { idx, section, isCenter, isCompactStage, _, relOffset ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isCenter) Modifier.testTag("carousel_card_${section.name.lowercase()}") else Modifier),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                HolographicCardHeader(
                    index = idx,
                    totalCount = sections.size,
                    moduleLabel = "SETTINGS",
                    title = section.title,
                    subtitle = "${section.code} CONFIG // ${section.subtitle}",
                    badge = "OS",
                    isCenter = isCenter,
                    isCompactStage = isCompactStage,
                    crimsonBadge = section == SettingsSection.LAUNCHER || section == SettingsSection.LOCKSCREEN
                )

                if (isCenter) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (section) {
                            SettingsSection.LAUNCHER -> {
                                Text(
                                    text = "Set STX OS as your Android Home Launcher so pressing Home returns directly to this holographic deck.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = HudSilver
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsActionButton(
                                        label = "DEFAULT LAUNCHER",
                                        icon = Icons.Default.Home,
                                        crimson = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenDefaultLauncher
                                    )
                                    SettingsActionButton(
                                        label = "RE-INDEX (${state.installedApps.size})",
                                        icon = Icons.Default.Refresh,
                                        crimson = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = onReloadApps
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsActionButton(
                                        label = "06 LOCKSCREEN",
                                        icon = Icons.Default.Security,
                                        crimson = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectSection(sections.indexOf(SettingsSection.LOCKSCREEN)) }
                                    )
                                    SettingsActionButton(
                                        label = "LOCK COVER",
                                        icon = Icons.Default.Lock,
                                        crimson = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = onEngageLockNow
                                    )
                                }
                                CarouselSpecRow("INDEXED APPS", "${state.installedApps.size} PACKAGES", HudCyan, isCompactStage)
                                CarouselSpecRow("PINNED ARC DOCK", "${state.preferences.pinnedPackages.size} PINNED", HudSilver, isCompactStage)
                            }

                            SettingsSection.IDENTITY -> {
                                Text("OPERATOR CALLSIGN", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF040A12))
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = callsignInput,
                                            onValueChange = { callsignInput = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { onSetCallsign(callsignInput) }),
                                            textStyle = TextStyle(fontFamily = OrbitronFontFamily, fontSize = 14.sp, color = HudIce),
                                            cursorBrush = SolidColor(HudCyan),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 46.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCyanSubtle)
                                            .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                            .clickable { onSetCallsign(callsignInput) }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("SAVE", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HudIce)
                                    }
                                }
                                SettingsSwitchRow(
                                    title = "HUD TACTILE SFX",
                                    subtitle = "Synthesized audio cues on dial detent and taps",
                                    checked = state.preferences.sfxEnabled,
                                    onCheckedChange = onToggleSfx
                                )
                                SettingsSwitchRow(
                                    title = "SYSTEM AUDIO MUTE",
                                    subtitle = "Mutes media stream and HUD cues",
                                    checked = state.telemetry.audioMuted,
                                    onCheckedChange = { onToggleMute() }
                                )
                            }

                            SettingsSection.DISPLAY -> {
                                SettingsSwitchRow(
                                    title = "FULLSCREEN IMMERSIVE",
                                    subtitle = "Hide Android status & navigation bars edge-to-edge",
                                    checked = state.preferences.fullscreenImmersive,
                                    onCheckedChange = onToggleFullscreen
                                )
                                SettingsSwitchRow(
                                    title = "KEEP COMMAND DECK AWAKE",
                                    subtitle = "Prevent screen sleep while unlocked inside STX OS",
                                    checked = state.preferences.keepScreenOn,
                                    onCheckedChange = onToggleKeepAwake
                                )
                                SettingsSwitchRow(
                                    title = "AUTO-MORPH ON ROTATE",
                                    subtitle = "Automatically switch Portrait <-> Landscape Hub",
                                    checked = state.preferences.autoMorphOrientation,
                                    onCheckedChange = onToggleAutoMorph
                                )
                                SettingsSwitchRow(
                                    title = "CRIMSON EDGE TORCH GLOW",
                                    subtitle = "Use Crimson tactical perimeter instead of Ice Cyan",
                                    checked = state.preferences.edgeGlowCrimson,
                                    onCheckedChange = onToggleEdgeColor
                                )
                                SettingsSwitchRow(
                                    title = "SYSTEM-WIDE FLOATING STX AI",
                                    subtitle = if (canDrawOverlaysCached) {
                                        "Keep STX AI floating over all apps across the system"
                                    } else {
                                        "Tap to grant Display Over Other Apps permission"
                                    },
                                    checked = state.preferences.systemFloatingAiEnabled && canDrawOverlaysCached,
                                    onCheckedChange = onToggleSystemFloatingAi
                                )
                            }

                            SettingsSection.PERMISSIONS -> {
                                val perms = buildList {
                                    add(Manifest.permission.CAMERA to "CAMERA & LED TORCH")
                                    add(Manifest.permission.RECORD_AUDIO to "VOICE COMMAND MIC")
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION to "WIFI SSID & RF LINK")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        add(Manifest.permission.ACTIVITY_RECOGNITION to "HARDWARE STEP COUNTER")
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        add(Manifest.permission.BLUETOOTH_CONNECT to "BLUETOOTH TELEMETRY")
                                    }
                                }
                                perms.forEach { (perm, title) ->
                                    val granted = ContextCompat.checkSelfPermission(context, perm) ==
                                        PackageManager.PERMISSION_GRANTED
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x5506101B))
                                            .border(1.dp, if (granted) HudEmerald.copy(alpha = 0.5f) else HudCrimsonBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(title, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.5.sp, color = HudIce)
                                        Text(
                                            text = if (granted) "GRANTED" else "GRANT >",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = if (granted) HudEmerald else HudCrimson,
                                            modifier = if (!granted) {
                                                Modifier.clickable { permissionLauncher.launch(arrayOf(perm)) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                    }
                                }
                            }

                            SettingsSection.DIAGNOSTICS -> {
                                CarouselSpecRow("DEVICE MODEL", state.telemetry.deviceModel, HudIce, isCompactStage)
                                CarouselSpecRow("ANDROID OS", state.telemetry.androidVersion, HudCyan, isCompactStage)
                                CarouselSpecRow("CPU CORES", "${state.telemetry.cpuCores} CORES", HudSilver, isCompactStage)
                                CarouselSpecRow("SYSTEM RAM", "${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB (${state.telemetry.ramUsedPercent}%)", HudCyan, isCompactStage)
                                CarouselSpecRow("INTERNAL STORAGE", "${"%.1f".format(state.telemetry.storageFreeGb)} GB FREE / ${"%.1f".format(state.telemetry.storageTotalGb)} GB", HudSilver, isCompactStage)
                                CarouselSpecRow("BATTERY HEALTH", "${state.telemetry.batteryHealth} · ${state.telemetry.batteryVoltageMv} mV · ${state.telemetry.batteryTempC}°C", HudEmerald, isCompactStage)
                                CarouselSpecRow("UPTIME", state.telemetry.uptimeFormatted, HudIce, isCompactStage)
                            }

                            SettingsSection.LOCKSCREEN -> {
                                var newPasscode by remember { mutableStateOf("") }
                                var alphaMode by remember(state.preferences.passcodeIsAlphanumeric) {
                                    mutableStateOf(state.preferences.passcodeIsAlphanumeric)
                                }
                                var headlineDraft by remember(state.preferences.lockHeadline) {
                                    mutableStateOf(state.preferences.lockHeadline.replace("STARK OS", "STX OS"))
                                }
                                val km = remember(context) {
                                    context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                                }
                                val isOsKeyguardSecure = km?.isKeyguardSecure == true
                                val isAdminActive = StarkDeviceAdminReceiver.isLockAdminActive(context)

                                SettingsSwitchRow(
                                    title = "STX LOCK COVER",
                                    subtitle = "Engage STX holographic lockscreen cover on launch & wake",
                                    checked = state.preferences.lockCoverEnabled,
                                    onCheckedChange = onSetLockCoverEnabled
                                )
                                SettingsSwitchRow(
                                    title = "AUTO-LOCK ON SCREEN OFF",
                                    subtitle = "Arms STX Lock Cover automatically when screen turns off",
                                    checked = state.preferences.autoLockOnScreenOff,
                                    onCheckedChange = onSetAutoLockOnScreenOff
                                )
                                SettingsSwitchRow(
                                    title = "FINGERPRINT / FACE UNLOCK",
                                    subtitle = "Use Android BiometricPrompt alongside passcode",
                                    checked = state.preferences.biometricUnlockEnabled,
                                    onCheckedChange = onSetBiometricUnlockEnabled
                                )
                                SettingsSwitchRow(
                                    title = "PHYSICAL OS SCREEN SLEEP",
                                    subtitle = "Also call Device Admin lockNow() when locking",
                                    checked = state.preferences.useOsPhysicalLock,
                                    onCheckedChange = onSetUseOsPhysicalLock
                                )

                                Text(
                                    text = if (state.preferences.hasPasscode) "PASSCODE ARMED (SHA-256 SALTED)" else "PASSCODE NOT SET (SWIPE MODE)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (state.preferences.hasPasscode) HudEmerald else HudCyan
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (!alphaMode) HudCyanSubtle else Color(0x44081018))
                                            .border(1.dp, if (!alphaMode) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                                            .clickable { alphaMode = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("4-6 DIGIT PIN", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (alphaMode) HudCyanSubtle else Color(0x44081018))
                                            .border(1.dp, if (alphaMode) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                                            .clickable { alphaMode = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ALPHANUMERIC", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF040A12))
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newPasscode.isEmpty()) {
                                            Text(
                                                text = if (alphaMode) "NEW PASSCODE (4+ CHARS)" else "NEW PIN (4-6 DIGITS)",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = HudSlate
                                            )
                                        }
                                        BasicTextField(
                                            value = newPasscode,
                                            onValueChange = { raw ->
                                                newPasscode = if (alphaMode) raw.take(24) else raw.filter { it.isDigit() }.take(6)
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                onSavePasscode(newPasscode, alphaMode)
                                                newPasscode = ""
                                            }),
                                            visualTransformation = PasswordVisualTransformation(),
                                            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp, color = HudIce),
                                            cursorBrush = SolidColor(HudCyan),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 46.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCyanSubtle)
                                            .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                            .clickable {
                                                onSavePasscode(newPasscode, alphaMode)
                                                newPasscode = ""
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("SET", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    }
                                    if (state.preferences.hasPasscode) {
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 46.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(HudCrimsonSubtle)
                                                .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                                .clickable(onClick = onClearPasscode)
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("CLR", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudCrimson)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF040A12))
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = headlineDraft,
                                            onValueChange = { headlineDraft = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { onSetLockHeadline(headlineDraft) }),
                                            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = HudIce),
                                            cursorBrush = SolidColor(HudCyan),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 42.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCyanSubtle)
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .clickable { onSetLockHeadline(headlineDraft) }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("BANNER", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                                    }
                                }

                                SettingsSwitchRow(
                                    title = "CHARGING & BATTERY RING",
                                    subtitle = "Show power ring and wattage source on Lock Cover",
                                    checked = state.preferences.lockShowChargingRing,
                                    onCheckedChange = { onSetLockWidgets(null, it, null) }
                                )
                                SettingsSwitchRow(
                                    title = "STEP & COMPASS BADGE",
                                    subtitle = "Display live pedometer steps & heading on Lock Cover",
                                    checked = state.preferences.lockShowSteps,
                                    onCheckedChange = { onSetLockWidgets(it, null, null) }
                                )
                                SettingsSwitchRow(
                                    title = "STX AI LOCK TRIGGER",
                                    subtitle = "Allow quick STX AI queries without full unlock",
                                    checked = state.preferences.lockShowStxAiTrigger,
                                    onCheckedChange = { onSetLockWidgets(null, null, it) }
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    SettingsActionButton(
                                        label = if (isAdminActive) "ADMIN ARMED" else "OS SCREEN LOCK",
                                        icon = Icons.Default.Lock,
                                        crimson = !isAdminActive,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    StarkDeviceAdminReceiver.createEnableAdminIntent(context).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                    SettingsActionButton(
                                        label = "SYS SECURITY",
                                        icon = Icons.Default.Security,
                                        crimson = isOsKeyguardSecure,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                                SettingsActionButton(
                                    label = "LOCK SCREEN NOW",
                                    icon = Icons.Default.Lock,
                                    crimson = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onEngageLockNow
                                )
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
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x5506101B))
            .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = HudIce,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = HudSlate
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HudIce,
                checkedTrackColor = HudCyan.copy(alpha = 0.65f),
                uncheckedThumbColor = HudSlate,
                uncheckedTrackColor = Color(0xFF0A1420)
            )
        )
    }
}

@Composable
private fun SettingsActionButton(
    label: String,
    icon: ImageVector,
    crimson: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (crimson) HudCrimsonSubtle else HudCyanSubtle)
            .border(1.dp, if (crimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        Icon(icon, label, tint = if (crimson) HudCrimson else HudCyan, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = HudIce,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
