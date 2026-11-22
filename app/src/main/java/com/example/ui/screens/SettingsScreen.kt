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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.runtime.LaunchedEffect
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
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.hardware.MiuiFreeformLauncher
import com.example.hardware.ShizukuBridgeStatus
import com.example.model.SettingsSection
import com.example.security.StarkDeviceAdminReceiver
import com.example.ui.StarkUiState
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectSection: (Int) -> Unit,
    onBack: () -> Unit = {},
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
    onReplayFirstBoot: () -> Unit = {},
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
            if (event == Lifecycle.Event.ON_RESUME) permRefreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        MiuiFreeformLauncher.setOnBridgeStateChangedListener { permRefreshTick++ }
        onDispose {
            MiuiFreeformLauncher.setOnBridgeStateChangedListener(null)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { sections.size })
    LaunchedEffect(safeIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != safeIndex) {
            onSelectSection(pagerState.settledPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header bar with < BACK and pager steppers
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

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text("SETTINGS", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce, maxLines = 1)
                Text(
                    text = "${sections[safeIndex].code} // ${sections[safeIndex].subtitle}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = HudCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectSection((safeIndex - 1 + sections.size) % sections.size) }
                        .testTag("carousel_prev_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous settings tab", tint = HudCyan, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectSection((safeIndex + 1) % sections.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next settings tab", tint = HudCyan, modifier = Modifier.size(16.dp))
                }
            }
        }

        // All 01..07 clickable tabs in FlowRow
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

        HorizontalPager(
            state = pagerState,
            key = { idx -> sections[idx].name },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val section = sections[page]
            val isCrimson = section == SettingsSection.LAUNCHER || section == SettingsSection.LOCKSCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEB040911))
                    .border(1.2.dp, if (isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .then(if (page == pagerState.currentPage) Modifier.testTag("carousel_card_${section.name.lowercase()}") else Modifier)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(section.title, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HudIce)
                        Text("${section.code} CONFIG // ${section.subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = if (isCrimson) HudCrimson else HudCyan)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isCrimson) HudCrimsonSubtle else HudCyanSubtle)
                            .border(0.8.dp, if (isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("OS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = HudIce)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HudCyan.copy(alpha = 0.22f)))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (section) {
                        SettingsSection.LAUNCHER -> {
                            Text(
                                text = "Set STX OS as your Android Home Launcher so pressing Home returns directly to this deck.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = HudSilver
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SettingsActionButton("DEFAULT LAUNCHER", Icons.Default.Home, true, Modifier.weight(1f), onOpenDefaultLauncher)
                                SettingsActionButton("RE-INDEX (${state.installedApps.size})", Icons.Default.Refresh, false, Modifier.weight(1f), onReloadApps)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SettingsActionButton(
                                    label = "07 ORB STUDIO",
                                    icon = Icons.Default.AutoAwesome,
                                    crimson = false,
                                    modifier = Modifier.weight(1f).testTag("settings_shortcut_orb_studio"),
                                    onClick = { onSelectSection(sections.indexOf(SettingsSection.ORB_STUDIO)) }
                                )
                                SettingsActionButton(
                                    label = "04 PERMISSIONS",
                                    icon = Icons.Default.VerifiedUser,
                                    crimson = false,
                                    modifier = Modifier.weight(1f).testTag("settings_shortcut_permissions"),
                                    onClick = { onSelectSection(sections.indexOf(SettingsSection.PERMISSIONS)) }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            SettingsActionButton(
                                label = "REPLAY BOOT SPLASH (0→100%)",
                                icon = Icons.Default.PowerSettingsNew,
                                crimson = false,
                                modifier = Modifier.fillMaxWidth().testTag("settings_replay_boot_btn"),
                                onClick = onReplayFirstBoot
                            )
                            SettingsSwitchRow(
                                title = "3D WIREFRAME ORB CORE",
                                subtitle = "Render interactive 60fps Red+Blue orb inside Dial & STX AI",
                                checked = state.preferences.orbUseHtmlWebGl,
                                onCheckedChange = onToggleOrbUseHtml
                            )
                            SettingsSpecRow("INDEXED APPS", "${state.installedApps.size} PACKAGES", HudCyan)
                            SettingsSpecRow("PINNED ARC DOCK", "${state.preferences.pinnedPackages.size} PINNED", HudSilver)

                            val bridgeTelemetry = remember(permRefreshTick) {
                                MiuiFreeformLauncher.getBridgeTelemetry(context)
                            }
                            var bridgeMsg by remember { mutableStateOf<String?>(null) }
                            val bridgeLabel = when (bridgeTelemetry.shizukuStatus) {
                                ShizukuBridgeStatus.ACTIVE -> "SHIZUKU UID 2000 ACTIVE"
                                ShizukuBridgeStatus.CONNECTING -> "SHIZUKU CONNECTING..."
                                ShizukuBridgeStatus.NEEDS_PERMISSION -> "SHIZUKU PERMISSION NEEDED"
                                ShizukuBridgeStatus.NOT_RUNNING -> "SHIZUKU STOPPED"
                                ShizukuBridgeStatus.NOT_INSTALLED -> if (bridgeTelemetry.rootAvailable) "ROOT SU ACTIVE" else "NATIVE OPTIONS"
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x6606101B))
                                    .border(1.dp, if (bridgeTelemetry.isPrivilegedBridgeReady) HudEmerald.copy(alpha = 0.6f) else HudCyanBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "MIUI 12.5 FLOATING WINDOW ENGINE (--windowingMode 5)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    color = HudCyan
                                )
                                SettingsSpecRow(
                                    label = "FREEFORM GLOBALS",
                                    value = if (bridgeTelemetry.isGlobalFreeformReady) "ENABLED (1/1)" else "DISABLED",
                                    valueColor = if (bridgeTelemetry.isGlobalFreeformReady) HudEmerald else HudAmber
                                )
                                SettingsSpecRow(
                                    label = "SHELL BRIDGE",
                                    value = bridgeLabel,
                                    valueColor = if (bridgeTelemetry.isPrivilegedBridgeReady) HudEmerald else HudCyan
                                )
                                bridgeMsg?.let { msg ->
                                    Text(
                                        text = msg,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.5.sp,
                                        color = HudAmber
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsActionButton(
                                        label = "ARM SHIZUKU",
                                        icon = Icons.Default.PictureInPictureAlt,
                                        crimson = !bridgeTelemetry.isPrivilegedBridgeReady,
                                        modifier = Modifier.weight(1f).testTag("settings_arm_freeform_btn"),
                                        onClick = {
                                            bridgeMsg = MiuiFreeformLauncher.armFreeformBridge(context)
                                            permRefreshTick++
                                        }
                                    )
                                    SettingsActionButton(
                                        label = "COPY ADB",
                                        icon = Icons.Default.ContentCopy,
                                        crimson = false,
                                        modifier = Modifier.weight(0.75f).testTag("settings_copy_adb_freeform_btn"),
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "MIUI Freeform Setup",
                                                    MiuiFreeformLauncher.buildAdbSetupCommands(context)
                                                )
                                            )
                                            bridgeMsg = "Copied ADB setup commands to clipboard."
                                        }
                                    )
                                }
                                SettingsActionButton(
                                    label = "DEV OPTIONS (ENABLE FREEFORM / RESIZABLE)",
                                    icon = Icons.Default.Security,
                                    crimson = !bridgeTelemetry.isGlobalFreeformReady,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_dev_freeform_btn"),
                                    onClick = { MiuiFreeformLauncher.openDeveloperOptions(context) }
                                )
                            }
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
                            SettingsSwitchRow("HUD TACTILE SFX", "Synthesized audio cues on dial detent and taps", state.preferences.sfxEnabled, onToggleSfx)
                            SettingsSwitchRow("SYSTEM AUDIO MUTE", "Mutes media stream and HUD cues", state.telemetry.audioMuted) { onToggleMute() }
                        }

                        SettingsSection.DISPLAY -> {
                            SettingsSwitchRow("FULLSCREEN IMMERSIVE", "Hide Android status & navigation bars edge-to-edge", state.preferences.fullscreenImmersive, onToggleFullscreen)
                            SettingsSwitchRow("KEEP COMMAND DECK AWAKE", "Prevent screen sleep while unlocked inside STX OS", state.preferences.keepScreenOn, onToggleKeepAwake)
                            SettingsSwitchRow("AUTO-MORPH ON ROTATE", "Automatically switch Portrait <-> Landscape Hub", state.preferences.autoMorphOrientation, onToggleAutoMorph)
                            SettingsSwitchRow("CRIMSON EDGE TORCH GLOW", "Use Crimson tactical perimeter instead of Ice Cyan", state.preferences.edgeGlowCrimson, onToggleEdgeColor)
                            SettingsSwitchRow(
                                title = "SYSTEM-WIDE FLOATING STX AI",
                                subtitle = if (canDrawOverlaysCached) "Keep STX AI floating over all apps" else "Tap to grant Display Over Other Apps permission",
                                checked = state.preferences.systemFloatingAiEnabled && canDrawOverlaysCached,
                                onCheckedChange = onToggleSystemFloatingAi
                            )
                        }

                        SettingsSection.PERMISSIONS -> {
                            val statuses = remember(permRefreshTick) { LauncherPermissionManager.getPermissionStatuses(context) }
                            val missingAll = remember(permRefreshTick) { LauncherPermissionManager.missingRuntimePermissions(context) }
                            val grantedCount = statuses.count { it.granted }
                            val isDefaultHome = remember(permRefreshTick) { LauncherPermissionManager.isDefaultHomeLauncher(context) }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("HARDWARE ACCESS ($grantedCount/${statuses.size})", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (missingAll.isEmpty()) HudEmerald else HudAmber)
                                Text(if (missingAll.isEmpty()) "ALL ARMED" else "${missingAll.size} PENDING", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = if (missingAll.isEmpty()) HudEmerald else HudCrimson)
                            }

                            permSettingsHint?.let { hint ->
                                Text(hint, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudAmber)
                            }

                            if (missingAll.isNotEmpty()) {
                                SettingsActionButton(
                                    label = "GRANT ALL LAUNCHER PERMISSIONS",
                                    icon = Icons.Default.VerifiedUser,
                                    crimson = true,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_grant_all_perms_btn"),
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
                                    Text("ALL HARDWARE PERMISSIONS GRANTED", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudEmerald)
                                }
                            }

                            statuses.forEach { st ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x5506101B))
                                        .border(1.dp, if (st.granted) HudEmerald.copy(alpha = 0.5f) else HudCrimsonBorder, RoundedCornerShape(4.dp))
                                        .then(if (!st.granted) Modifier.clickable { requestPermissionsOrSettings(st.permissions) } else Modifier)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(st.title, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                        Text(st.description, style = MaterialTheme.typography.bodyMedium, color = HudSlate)
                                    }
                                    Text(if (st.granted) "GRANTED" else "GRANT >", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (st.granted) HudEmerald else HudCrimson)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    modifier = Modifier.weight(0.9f).testTag("settings_open_app_perms_btn"),
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
                            SettingsSpecRow("DEVICE MODEL", state.telemetry.deviceModel, HudIce)
                            SettingsSpecRow("ANDROID OS", state.telemetry.androidVersion, HudCyan)
                            SettingsSpecRow("CPU CORES", "${state.telemetry.cpuCores} CORES", HudSilver)
                            SettingsSpecRow("SYSTEM RAM", "${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB (${state.telemetry.ramUsedPercent}%)", HudCyan)
                            SettingsSpecRow("INTERNAL STORAGE", "${"%.1f".format(state.telemetry.storageFreeGb)} GB FREE / ${"%.1f".format(state.telemetry.storageTotalGb)} GB", HudSilver)
                            SettingsSpecRow("BATTERY HEALTH", "${state.telemetry.batteryHealth} · ${state.telemetry.batteryVoltageMv} mV · ${state.telemetry.batteryTempC}°C", HudEmerald)
                            SettingsSpecRow("UPTIME", state.telemetry.uptimeFormatted, HudIce)
                        }

                        SettingsSection.LOCKSCREEN -> {
                            var newPasscode by remember { mutableStateOf("") }
                            var alphaMode by remember(state.preferences.passcodeIsAlphanumeric) {
                                mutableStateOf(state.preferences.passcodeIsAlphanumeric)
                            }
                            var headlineDraft by remember(state.preferences.lockHeadline) {
                                mutableStateOf(state.preferences.lockHeadline.replace("STARK OS", "STX OS"))
                            }
                            val km = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
                            val isOsKeyguardSecure = km?.isKeyguardSecure == true
                            val isAdminActive = StarkDeviceAdminReceiver.isLockAdminActive(context)

                            SettingsSwitchRow("STX LOCK COVER", "Engage STX holographic lockscreen cover on launch & wake", state.preferences.lockCoverEnabled, onSetLockCoverEnabled)
                            SettingsSwitchRow("AUTO-LOCK ON SCREEN OFF", "Arms STX Lock Cover automatically when screen turns off", state.preferences.autoLockOnScreenOff, onSetAutoLockOnScreenOff)
                            SettingsSwitchRow("FINGERPRINT / FACE UNLOCK", "Use Android BiometricPrompt alongside passcode", state.preferences.biometricUnlockEnabled, onSetBiometricUnlockEnabled)
                            SettingsSwitchRow("PHYSICAL OS SCREEN SLEEP", "Also call Device Admin lockNow() when locking", state.preferences.useOsPhysicalLock, onSetUseOsPhysicalLock)

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

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                        Text(if (alphaMode) "NEW PASSCODE (4+ CHARS)" else "NEW PIN (4-6 DIGITS)", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                                    }
                                    BasicTextField(
                                        value = newPasscode,
                                        onValueChange = { raw -> newPasscode = if (alphaMode) raw.take(24) else raw.filter { it.isDigit() }.take(6) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { onSavePasscode(newPasscode, alphaMode); newPasscode = "" }),
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
                                        .clickable { onSavePasscode(newPasscode, alphaMode); newPasscode = "" }
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

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

                            SettingsSwitchRow("CHARGING & BATTERY RING", "Show power ring on Lock Cover", state.preferences.lockShowChargingRing) { onSetLockWidgets(null, it, null) }
                            SettingsSwitchRow("STEP & COMPASS BADGE", "Display live pedometer steps & heading", state.preferences.lockShowSteps) { onSetLockWidgets(it, null, null) }
                            SettingsSwitchRow("STX AI LOCK TRIGGER", "Allow quick STX AI queries without full unlock", state.preferences.lockShowStxAiTrigger) { onSetLockWidgets(null, null, it) }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                SettingsActionButton(
                                    label = if (isAdminActive) "ADMIN ARMED" else "OS SCREEN LOCK",
                                    icon = Icons.Default.Lock,
                                    crimson = !isAdminActive,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        runCatching {
                                            context.startActivity(StarkDeviceAdminReceiver.createEnableAdminIntent(context).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
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
                                            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        }
                                    }
                                )
                            }
                            SettingsActionButton("LOCK SCREEN NOW", Icons.Default.Lock, true, Modifier.fillMaxWidth(), onEngageLockNow)
                        }

                        SettingsSection.ORB_STUDIO -> {
                            SettingsSwitchRow("3D WIREFRAME SPHERE", "Use native Red+Blue 60fps 3D orb in STX AI (off = Holo Girl)", state.preferences.orbUseHtmlWebGl, onToggleOrbUseHtml)
                            SettingsSwitchRow("AUTO-ROTATE SPHERES", "Continuous multi-layer planetary wireframe spin", state.preferences.orbAutoRotate, onToggleOrbAutoRotate)
                            SettingsSwitchRow("SPEECH WORD NEURON GLOW", "Ignite crimson pulse when speaking", state.preferences.orbSpeechGlowReactive, onToggleOrbSpeechGlowReactive)
                            SettingsSwitchRow("FLOATING CODE WORD NODES", "Orbiting neural markers", state.preferences.orbShowTextNodes, onToggleOrbShowTextNodes)
                            SettingsSwitchRow("ORBITING DEBRIS FIELD", "Particle field inside orb stage", state.preferences.orbShowDebris, onToggleOrbShowDebris)

                            OrbSliderRow("ROTATION SPEED", "${"%.2f".format(state.preferences.orbRotationSpeed)}X", state.preferences.orbRotationSpeed, 0.2f..2.5f, HudAmber, onOrbRotationSpeedChange)
                            OrbSliderRow("BLOOM GLOW INTENSITY", "${"%.2f".format(state.preferences.orbBloomStrength)}", state.preferences.orbBloomStrength, 0.3f..2.0f, HudCyan, onOrbBloomStrengthChange)
                            OrbSliderRow("ORB GEOMETRY SCALE", "${"%.2f".format(state.preferences.orbScale)}X", state.preferences.orbScale, 0.65f..1.35f, HudCrimson, onOrbScaleChange)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SettingsActionButton(
                                    label = "RESET DEFAULTS",
                                    icon = Icons.Default.Refresh,
                                    crimson = false,
                                    modifier = Modifier.weight(1f).testTag("settings_reset_orb_btn"),
                                    onClick = onResetOrbDefaults
                                )
                                SettingsActionButton(
                                    label = "OPEN STX AI CORE",
                                    icon = Icons.Default.AutoAwesome,
                                    crimson = true,
                                    modifier = Modifier.weight(1f).testTag("settings_open_stx_orb_btn"),
                                    onClick = onOpenStxAiCore
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
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
            Text(title, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = HudSlate)
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
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
            Text(valueLabel, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = accentColor)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = Color(0xFF0A1420))
        )
    }
}
