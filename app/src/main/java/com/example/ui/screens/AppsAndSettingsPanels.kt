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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.InstalledApp
import com.example.model.SettingsSection
import com.example.security.StarkDeviceAdminReceiver
import com.example.ui.StarkUiState
import com.example.ui.components.ArcReactorLeftRimAccent
import com.example.ui.components.HudBracketCard
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

private val GridCardShape = RoundedCornerShape(6.dp)
private val GridIconShape = RoundedCornerShape(8.dp)

/**
 * Compact 5-column All Apps HUD Grid (tap any app to open directly).
 */
@Composable
fun AllAppsFiveColGrid(
    apps: List<InstalledApp>,
    focusedIndex: Int,
    appsLoading: Boolean,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
    showLeftRimAccent: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLeftRimAccent) {
            ArcReactorLeftRimAccent(modifier = Modifier.fillMaxHeight())
        }

        HudBracketCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            borderColor = HudCyanBorder,
            contentPadding = 8.dp
        ) {
            if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (appsLoading) "SCANNING PACKAGES…" else "NO APPS INDEXED",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudCyan
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL APPS (${apps.size})",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = HudCyan
                        )
                        Text(
                            text = "TAP TO LAUNCH",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.5.sp,
                            color = HudSlate
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("all_apps_5col_grid"),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = apps,
                            key = { idx, app -> "${app.packageName}/${app.activityName}#$idx" }
                        ) { idx, app ->
                            val isFocused = idx == focusedIndex
                            Column(
                                modifier = Modifier
                                    .clip(GridCardShape)
                                    .background(if (isFocused) HudCyanSubtle else Color(0x5506101B))
                                    .border(
                                        width = if (isFocused) 1.2.dp else 0.7.dp,
                                        color = if (isFocused) HudCyan else HudDivider,
                                        shape = GridCardShape
                                    )
                                    .clickable { onLaunchApp(app) }
                                    .padding(horizontal = 3.dp, vertical = 5.dp)
                                    .testTag("grid_app_${app.packageName}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(GridIconShape)
                                        .background(Color(0xFF0A1624))
                                        .border(1.dp, if (isFocused) HudCyan else HudCyanBorder, GridIconShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text(
                                            text = app.label.take(2).uppercase(),
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HudCyan
                                        )
                                    }
                                }
                                Text(
                                    text = app.label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 7.5.sp,
                                    lineHeight = 9.sp,
                                    color = if (isFocused) HudIce else HudSilver,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slightly-wider Portrait-proportioned Carousel Card scrolled via the Right Half-Dial or swipe.
 * Tapping card body opens the app directly; bottom bar holds ONLY 2 buttons: WINDOWED and FULLSCREEN.
 */
@Composable
fun AppFocusCarouselCard(
    apps: List<InstalledApp>,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onLaunchWindowed: (InstalledApp) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onTogglePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) {
        HudBracketCard(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO APPS FOUND", fontFamily = JetBrainsMonoFontFamily, color = HudSlate)
            }
        }
        return
    }

    val safeIdx = focusedIndex.coerceIn(0, apps.lastIndex)
    val activeApp = apps[safeIdx]
    var dragAccum by remember(safeIdx) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        HudBracketCard(
            modifier = Modifier
                .widthIn(min = 196.dp, max = 276.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.96f)
                .pointerInput(safeIdx, apps.size) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccum += dragAmount
                            if (dragAccum > 75f) {
                                dragAccum = 0f
                                onSelectIndex((safeIdx - 1 + apps.size) % apps.size)
                            } else if (dragAccum < -75f) {
                                dragAccum = 0f
                                onSelectIndex((safeIdx + 1) % apps.size)
                            }
                        },
                        onDragEnd = { dragAccum = 0f },
                        onDragCancel = { dragAccum = 0f }
                    )
                }
                .clickable { onLaunchFullscreen(activeApp) }
                .testTag("active_app_carousel_card"),
            borderColor = HudCyan,
            contentPadding = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Meta + Pin badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d/%02d · ${activeApp.categoryTag}".format(safeIdx + 1, apps.size),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = HudCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (activeApp.isPinnedToDock) HudCyanSubtle else Color(0x44081018))
                            .border(
                                1.dp,
                                if (activeApp.isPinnedToDock) HudCyan else HudDivider,
                                CircleShape
                            )
                            .clickable { onTogglePin(activeApp.packageName) }
                            .testTag("carousel_pin_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeApp.isPinnedToDock) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Pin app to dock",
                            tint = if (activeApp.isPinnedToDock) HudCyan else HudSlate,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Center Hero Icon & Info (tap anywhere opens app directly)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A1624))
                            .border(1.5.dp, HudCyan, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeApp.icon != null) {
                            Image(
                                bitmap = activeApp.icon,
                                contentDescription = activeApp.label,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Text(
                                text = activeApp.label.take(2).uppercase(),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeApp.label.uppercase(),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = HudIce,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeApp.packageName,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
                        color = HudSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v${activeApp.versionName} · SDK ${activeApp.targetSdk}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
                        color = HudCyan,
                        maxLines = 1
                    )
                }

                // Strictly ONLY 2 Action Buttons on Carousel Card: WINDOWED & FULLSCREEN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable { onLaunchWindowed(activeApp) }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .testTag("carousel_windowed_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureInPictureAlt,
                                contentDescription = "Launch floating",
                                tint = HudCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "FLOATING",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = HudIce,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                            .clickable { onLaunchFullscreen(activeApp) }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .testTag("carousel_fullscreen_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = "Launch fullscreen",
                                tint = HudCrimson,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "FULLSCREEN",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = HudIce,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModulePanel(
    state: StarkUiState,
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
    val activeSection = sections[state.subItemIndex.coerceIn(0, sections.lastIndex)]
    val context = LocalContext.current
    var callsignInput by remember(state.preferences.operatorCallsign) {
        mutableStateOf(state.preferences.operatorCallsign)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionsUpdated()
    }

    HudBracketCard(
        modifier = modifier.fillMaxSize(),
        borderColor = HudCyan,
        contentPadding = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${activeSection.code} SETTINGS // ${activeSection.subtitle}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudCyan
                    )
                    Text(
                        text = activeSection.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = HudIce
                    )
                }
                Text(
                    text = "OS CONFIG",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = HudCrimson,
                    fontWeight = FontWeight.Bold
                )
            }

            // Clickable 01..06 Section Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEachIndexed { idx, sec ->
                    val selected = sec == activeSection
                    val isLockTab = sec == SettingsSection.LOCKSCREEN
                    Box(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    selected && isLockTab -> HudCrimsonSubtle
                                    selected -> HudCyanSubtle
                                    else -> Color(0x5506101B)
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
                            fontSize = 10.sp,
                            color = if (selected) HudIce else if (isLockTab) HudCrimson else HudSilver
                        )
                    }
                }
            }

            HorizontalDivider(color = HudDivider)

            when (activeSection) {
                SettingsSection.LAUNCHER -> {
                    Text(
                        text = "Set STX OS as your Android Home Launcher so pressing Home returns directly to this holographic deck.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HudSilver
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsActionButton(
                            label = "SET DEFAULT LAUNCHER",
                            icon = Icons.Default.Home,
                            crimson = true,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenDefaultLauncher
                        )
                        SettingsActionButton(
                            label = "RE-INDEX APPS (${state.installedApps.size})",
                            icon = Icons.Default.Refresh,
                            crimson = false,
                            modifier = Modifier.weight(1f),
                            onClick = onReloadApps
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsActionButton(
                            label = "06 LOCKSCREEN SETUP",
                            icon = Icons.Default.Security,
                            crimson = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val lockIdx = sections.indexOf(SettingsSection.LOCKSCREEN)
                                if (lockIdx >= 0) onSelectSection(lockIdx)
                            }
                        )
                        SettingsActionButton(
                            label = "TEST LOCK COVER",
                            icon = Icons.Default.Lock,
                            crimson = true,
                            modifier = Modifier.weight(1f),
                            onClick = onEngageLockNow
                        )
                    }
                    SpecPairRow("INDEXED LAUNCHER APPS", "${state.installedApps.size} PACKAGES", HudCyan)
                    SpecPairRow("PINNED ARC DOCK APPS", "${state.preferences.pinnedPackages.size} PINNED", HudSilver)
                }

                SettingsSection.IDENTITY -> {
                    Text(
                        text = "OPERATOR CALLSIGN",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudCyan
                    )
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
                                textStyle = TextStyle(
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 14.sp,
                                    color = HudIce
                                ),
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
                        subtitle = if (Settings.canDrawOverlays(context)) {
                            "Keep STX AI floating over all apps across the system"
                        } else {
                            "Tap to grant Display Over Other Apps permission"
                        },
                        checked = state.preferences.systemFloatingAiEnabled && Settings.canDrawOverlays(context),
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
                            Text(
                                text = title,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudIce
                            )
                            Text(
                                text = if (granted) "GRANTED" else "GRANT >",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
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
                    SpecPairRow("DEVICE MODEL", state.telemetry.deviceModel, HudIce)
                    SpecPairRow("ANDROID OS", state.telemetry.androidVersion, HudCyan)
                    SpecPairRow("CPU CORES", "${state.telemetry.cpuCores} CORES", HudSilver)
                    SpecPairRow("SYSTEM RAM", "${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB (${state.telemetry.ramUsedPercent}%)", HudCyan)
                    SpecPairRow("INTERNAL STORAGE", "${"%.1f".format(state.telemetry.storageFreeGb)} GB FREE / ${"%.1f".format(state.telemetry.storageTotalGb)} GB", HudSilver)
                    SpecPairRow("BATTERY HEALTH", "${state.telemetry.batteryHealth} · ${state.telemetry.batteryVoltageMv} mV · ${state.telemetry.batteryTempC}°C", HudEmerald)
                    SpecPairRow("UPTIME", state.telemetry.uptimeFormatted, HudIce)
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

                    SettingsSwitchRow(
                        title = "STX LOCK COVER",
                        subtitle = "Engage STX holographic lockscreen cover on launch & wake (auto-sleeps in 10s)",
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
                        subtitle = "Also call Device Admin lockNow() when locking (turns screen off)",
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
                                    newPasscode = if (alphaMode) raw.take(24)
                                    else raw.filter { it.isDigit() }.take(6)
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

                    val isAdminActive = StarkDeviceAdminReceiver.isLockAdminActive(context)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x6606101B))
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MIUI OS LOCK & DIRECT WAKE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = HudCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isOsKeyguardSecure) "OS PIN DETECTED" else if (isAdminActive) "ADMIN ARMED" else "DIRECT READY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isOsKeyguardSecure) HudCrimson else HudEmerald
                            )
                        }
                        Text(
                            text = if (isOsKeyguardSecure) {
                                "MIUI has a secure system PIN/Pattern enabled. Android prohibits 3rd-party launchers from bypassing or typing into MIUI's secure lockscreen.\n• For instant 1-step STX OS PIN/Biometric unlock & wake, set MIUI Screen Lock to None or Swipe in System Security."
                            } else {
                                "System Keyguard has no blocking PIN. STX Lock Cover acts directly as your primary lockscreen on launch & wake, and auto-sleeps after 10s idle when Device Admin is armed."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudSilver
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
                                label = "SYSTEM SECURITY",
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
        }
    }
}

@Composable
fun SpecPairRow(
    key: String,
    value: String,
    valueColor: Color = HudSilver
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            color = HudSlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
