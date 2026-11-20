package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.model.SettingsSection
import com.example.security.StarkDeviceAdminReceiver
import com.example.ui.StarkUiState
import com.example.ui.components.CarouselSpecRow
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.HolographicCardHeader
import com.example.ui.components.HolographicSideCardFooter
import com.example.ui.theme.HudAmber
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
    onToggleOrbUseHtml: (Boolean) -> Unit = {},
    onOrbRotationSpeedChange: (Float) -> Unit = {},
    onOrbBloomStrengthChange: (Float) -> Unit = {},
    onOrbScaleChange: (Float) -> Unit = {},
    onToggleOrbAutoRotate: (Boolean) -> Unit = {},
    onToggleOrbShowTextNodes: (Boolean) -> Unit = {},
    onToggleOrbShowDebris: (Boolean) -> Unit = {},
    onToggleOrbSpeechGlowReactive: (Boolean) -> Unit = {},
    onResetOrbDefaults: () -> Unit = {},
    onOpenStxAiCore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sections = SettingsSection.entries
    val safeIndex = focusedIndex.coerceIn(0, sections.lastIndex)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var callsignInput by remember(state.preferences.operatorCallsign) {
        mutableStateOf(state.preferences.operatorCallsign)
    }
    var permRefreshTick by remember { mutableIntStateOf(0) }
    var permPromptStartedMs by remember { mutableLongStateOf(0L) }
    var permSettingsHint by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val elapsedMs = SystemClock.elapsedRealtime() - permPromptStartedMs
        permRefreshTick++
        onPermissionsUpdated()
        val anyGranted = results.values.any { it }
        val stillMissing = LauncherPermissionManager.missingRuntimePermissions(context)
        if (stillMissing.isEmpty()) {
            permSettingsHint = null
        } else if (!anyGranted && elapsedMs < 500L) {
            permSettingsHint = "Android blocked popup (Don't ask again / MIUI) — opening App Settings."
            LauncherPermissionManager.openAppPermissionSettings(context)
        } else if (!anyGranted) {
            permSettingsHint = "Permission denied — tap GRANT again or use APP PERMS."
        } else {
            permSettingsHint = null
        }
    }

    fun requestPermissionsOrSettings(perms: Array<String>) {
        if (perms.isEmpty()) return
        permPromptStartedMs = SystemClock.elapsedRealtime()
        runCatching {
            permissionLauncher.launch(perms)
        }.onFailure {
            LauncherPermissionManager.openAppPermissionSettings(context)
        }
    }

    val canDrawOverlaysCached = remember(state.preferences.systemFloatingAiEnabled, state.mode, permRefreshTick) {
        LauncherPermissionManager.canDrawOverlays(context)
    }

    @OptIn(ExperimentalLayoutApi::class)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Clickable 01..07 Section Tabs — FlowRow keeps 04 PERMISSIONS & 07 ORB STUDIO visible without scrolling
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            sections.forEachIndexed { idx, sec ->
                val selected = idx == safeIndex
                val isLockTab = sec == SettingsSection.LOCKSCREEN
                val isOrbTab = sec == SettingsSection.ORB_STUDIO
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                selected && isLockTab -> HudCrimsonSubtle
                                selected && isOrbTab -> HudAmber.copy(alpha = 0.22f)
                                selected -> HudCyanSubtle
                                else -> Color(0x8806101B)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                selected && isLockTab -> HudCrimson
                                selected && isOrbTab -> HudAmber
                                selected -> HudCyan
                                isLockTab -> HudCrimsonBorder
                                isOrbTab -> HudAmber.copy(alpha = 0.45f)
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
                        color = when {
                            selected -> HudIce
                            isLockTab -> HudCrimson
                            isOrbTab -> HudAmber
                            else -> HudSilver
                        }
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
                                        label = "07 ORB STUDIO",
                                        icon = Icons.Default.AutoAwesome,
                                        crimson = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_shortcut_orb_studio"),
                                        onClick = { onSelectSection(sections.indexOf(SettingsSection.ORB_STUDIO)) }
                                    )
                                    SettingsActionButton(
                                        label = "04 PERMISSIONS",
                                        icon = Icons.Default.VerifiedUser,
                                        crimson = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_shortcut_permissions"),
                                        onClick = { onSelectSection(sections.indexOf(SettingsSection.PERMISSIONS)) }
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
                                SettingsSwitchRow(
                                    title = "3D HTML WEBGL ORB",
                                    subtitle = "Render the exact interactive HTML Three.js orb inside Dial & STX AI",
                                    checked = state.preferences.orbUseHtmlWebGl,
                                    onCheckedChange = onToggleOrbUseHtml
                                )
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
                                val statuses = remember(permRefreshTick) {
                                    LauncherPermissionManager.getPermissionStatuses(context)
                                }
                                val missingAll = remember(permRefreshTick) {
                                    LauncherPermissionManager.missingRuntimePermissions(context)
                                }
                                val grantedCount = statuses.count { it.granted }
                                val isDefaultHome = remember(permRefreshTick) {
                                    LauncherPermissionManager.isDefaultHomeLauncher(context)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "HARDWARE ACCESS ($grantedCount/${statuses.size})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (missingAll.isEmpty()) HudEmerald else HudAmber
                                    )
                                    Text(
                                        text = if (missingAll.isEmpty()) "ALL ARMED" else "${missingAll.size} PENDING",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        color = if (missingAll.isEmpty()) HudEmerald else HudCrimson
                                    )
                                }

                                permSettingsHint?.let { hint ->
                                    Text(
                                        text = hint,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = HudAmber
                                    )
                                }

                                if (missingAll.isNotEmpty()) {
                                    SettingsActionButton(
                                        label = "GRANT ALL LAUNCHER PERMISSIONS",
                                        icon = Icons.Default.VerifiedUser,
                                        crimson = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_grant_all_perms_btn"),
                                        onClick = { requestPermissionsOrSettings(missingAll) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudEmerald.copy(alpha = 0.14f))
                                            .border(1.dp, HudEmerald.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .testTag("settings_grant_all_perms_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ALL HARDWARE PERMISSIONS GRANTED",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = HudEmerald
                                        )
                                    }
                                }

                                statuses.forEach { st ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x5506101B))
                                            .border(
                                                1.dp,
                                                if (st.granted) HudEmerald.copy(alpha = 0.5f) else HudCrimsonBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .then(
                                                if (!st.granted) {
                                                    Modifier.clickable { requestPermissionsOrSettings(st.permissions) }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = st.title,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = HudIce
                                            )
                                            Text(
                                                text = st.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = HudSlate
                                            )
                                        }
                                        Text(
                                            text = if (st.granted) "GRANTED" else "GRANT >",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (st.granted) HudEmerald else HudCrimson
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SettingsActionButton(
                                        label = if (canDrawOverlaysCached) "OVERLAY ARMED" else "GRANT OVERLAY",
                                        icon = Icons.Default.Security,
                                        crimson = !canDrawOverlaysCached,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onToggleSystemFloatingAi(!canDrawOverlaysCached) }
                                    )
                                    SettingsActionButton(
                                        label = "APP PERMS",
                                        icon = Icons.Default.VerifiedUser,
                                        crimson = false,
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("settings_open_app_perms_btn"),
                                        onClick = { LauncherPermissionManager.openAppPermissionSettings(context) }
                                    )
                                    SettingsActionButton(
                                        label = if (isDefaultHome) "HOME ACTIVE" else "SET HOME ROLE",
                                        icon = Icons.Default.Home,
                                        crimson = !isDefaultHome,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenDefaultLauncher
                                    )
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

                            SettingsSection.ORB_STUDIO -> {
                                SettingsSwitchRow(
                                    title = "3D HTML THREE.JS ORB",
                                    subtitle = "Use exact Three.js shader orb in STX AI (off = Holo Girl)",
                                    checked = state.preferences.orbUseHtmlWebGl,
                                    onCheckedChange = onToggleOrbUseHtml
                                )
                                SettingsSwitchRow(
                                    title = "AUTO-ROTATE SPHERES",
                                    subtitle = "Continuous multi-layer planetary wireframe spin",
                                    checked = state.preferences.orbAutoRotate,
                                    onCheckedChange = onToggleOrbAutoRotate
                                )
                                SettingsSwitchRow(
                                    title = "SPEECH WORD NEURON GLOW",
                                    subtitle = "Ignite crimson code words in 3D orb when speaking",
                                    checked = state.preferences.orbSpeechGlowReactive,
                                    onCheckedChange = onToggleOrbSpeechGlowReactive
                                )
                                SettingsSwitchRow(
                                    title = "FLOATING CODE WORD NODES",
                                    subtitle = "Render 1,700 orbiting neural lexicon sprites",
                                    checked = state.preferences.orbShowTextNodes,
                                    onCheckedChange = onToggleOrbShowTextNodes
                                )
                                SettingsSwitchRow(
                                    title = "ORBITING DEBRIS FIELD",
                                    subtitle = "Render 250 polyhedra and comet orbit trails",
                                    checked = state.preferences.orbShowDebris,
                                    onCheckedChange = onToggleOrbShowDebris
                                )

                                OrbSliderRow(
                                    title = "ROTATION SPEED",
                                    valueLabel = "${"%.2f".format(state.preferences.orbRotationSpeed)}X",
                                    value = state.preferences.orbRotationSpeed,
                                    valueRange = 0.2f..2.5f,
                                    accentColor = HudAmber,
                                    onValueChange = onOrbRotationSpeedChange
                                )
                                OrbSliderRow(
                                    title = "BLOOM GLOW INTENSITY",
                                    valueLabel = "${"%.2f".format(state.preferences.orbBloomStrength)}",
                                    value = state.preferences.orbBloomStrength,
                                    valueRange = 0.3f..2.0f,
                                    accentColor = HudCyan,
                                    onValueChange = onOrbBloomStrengthChange
                                )
                                OrbSliderRow(
                                    title = "ORB GEOMETRY SCALE",
                                    valueLabel = "${"%.2f".format(state.preferences.orbScale)}X",
                                    value = state.preferences.orbScale,
                                    valueRange = 0.65f..1.35f,
                                    accentColor = HudCrimson,
                                    onValueChange = onOrbScaleChange
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsActionButton(
                                        label = "RESET DEFAULTS",
                                        icon = Icons.Default.Refresh,
                                        crimson = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_reset_orb_btn"),
                                        onClick = onResetOrbDefaults
                                    )
                                    SettingsActionButton(
                                        label = "OPEN STX AI CORE",
                                        icon = Icons.Default.AutoAwesome,
                                        crimson = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_open_stx_orb_btn"),
                                        onClick = onOpenStxAiCore
                                    )
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

@Composable
private fun OrbSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x5506101B))
            .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = HudIce
            )
            Text(
                text = valueLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = accentColor
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF0A1420)
            )
        )
    }
}
