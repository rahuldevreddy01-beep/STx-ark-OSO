package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLaunchMode
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.SettingsSection
import com.example.ui.StarkUiState
import com.example.ui.components.ArcReactorRightHalfDial
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun HalfDialWorkspaceView(
    state: StarkUiState,
    dialItems: List<DialWedgeItem>,
    onSelectSubIndex: (Int, Int) -> Unit,
    onSelectLeftDialLaunchMode: (AppLaunchMode) -> Unit,
    onLaunchWithLeftDialMode: (InstalledApp) -> Unit,
    onFocusApp: (InstalledApp) -> Unit,
    onCenterBack: () -> Unit,
    onOpenModule: (HubModule) -> Unit,
    onLaunchWindowed: (InstalledApp) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit = {},
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
    onOpenWifiPanel: () -> Unit,
    onOpenBluetoothPanel: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCameraLens: () -> Unit,
    onCameraZoomChange: (Float) -> Unit,
    onAddVaultNote: (String, String) -> Unit,
    onDeleteVaultNote: (String) -> Unit,
    onStartVaultTimer: (Int) -> Unit,
    onStopVaultTimer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onWebSearch: (String) -> Unit,
    onAskStxAi: (String) -> Unit,
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
    modifier: Modifier = Modifier
) {
    val activeDialIndex = state.subItemIndex.coerceIn(0, (dialItems.size - 1).coerceAtLeast(0))
    val focusedItem = dialItems.getOrNull(activeDialIndex)

    fun activateNonAppsIndex(idx: Int) {
        val tapped = dialItems.getOrNull(idx) ?: return
        when (state.activeModule) {
            HubModule.WIFI -> {
                if (tapped.id == "panel") onOpenWifiPanel()
                else if (tapped.id == "bt") onOpenBluetoothPanel()
            }
            HubModule.CAMERA -> when (tapped.id) {
                "torch" -> onToggleTorch()
                "lens" -> onFlipCameraLens()
                "zoom" -> {
                    val nextZ = when {
                        state.cameraZoom < 1.8f -> 2.0f
                        state.cameraZoom < 3.2f -> 3.5f
                        else -> 1.0f
                    }
                    onCameraZoomChange(nextZ)
                }
            }
            HubModule.SETTINGS -> {
                if (tapped.id == "LAUNCHER") onOpenDefaultLauncher()
            }
            else -> Unit
        }
    }

    @Composable
    fun NonAppsPanelContent() {
        AnimatedContent(
            targetState = state.activeModule,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180)))
                    .togetherWith(fadeOut(tween(130)) + scaleOut(targetScale = 0.98f, animationSpec = tween(130)))
            },
            label = "module_panel_transition"
        ) { targetMod ->
            when (targetMod) {
                HubModule.SETTINGS -> SettingsScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectSection = { idx -> onSelectSubIndex(idx, SettingsSection.entries.size) },
                    onOpenDefaultLauncher = onOpenDefaultLauncher,
                    onReloadApps = onReloadApps,
                    onSetCallsign = onSetCallsign,
                    onToggleSfx = onToggleSfx,
                    onToggleMute = onToggleMute,
                    onToggleFullscreen = onToggleFullscreen,
                    onToggleKeepAwake = onToggleKeepAwake,
                    onToggleAutoMorph = onToggleAutoMorph,
                    onToggleEdgeColor = onToggleEdgeColor,
                    onToggleSystemFloatingAi = onToggleSystemFloatingAi,
                    onPermissionsUpdated = onPermissionsUpdated,
                    onSetLockCoverEnabled = onSetLockCoverEnabled,
                    onSetAutoLockOnScreenOff = onSetAutoLockOnScreenOff,
                    onSetBiometricUnlockEnabled = onSetBiometricUnlockEnabled,
                    onSetUseOsPhysicalLock = onSetUseOsPhysicalLock,
                    onSavePasscode = onSavePasscode,
                    onClearPasscode = onClearPasscode,
                    onSetLockWidgets = onSetLockWidgets,
                    onSetLockHeadline = onSetLockHeadline,
                    onEngageLockNow = onEngageLockNow,
                    onToggleOrbUseHtml = onToggleOrbUseHtml,
                    onOrbRotationSpeedChange = onOrbRotationSpeedChange,
                    onOrbBloomStrengthChange = onOrbBloomStrengthChange,
                    onOrbScaleChange = onOrbScaleChange,
                    onToggleOrbAutoRotate = onToggleOrbAutoRotate,
                    onToggleOrbShowTextNodes = onToggleOrbShowTextNodes,
                    onToggleOrbShowDebris = onToggleOrbShowDebris,
                    onToggleOrbSpeechGlowReactive = onToggleOrbSpeechGlowReactive,
                    onResetOrbDefaults = onResetOrbDefaults,
                    onOpenStxAiCore = { onOpenModule(HubModule.STX_AI) }
                )
                HubModule.HEALTH -> HealthScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onPermissionGranted = onPermissionsUpdated
                )
                HubModule.WIFI -> WifiScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onOpenWifiPanel = onOpenWifiPanel,
                    onOpenBluetoothPanel = onOpenBluetoothPanel,
                    onPermissionsUpdated = onPermissionsUpdated
                )
                HubModule.CAMERA -> CameraScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onToggleTorch = onToggleTorch,
                    onFlipLens = onFlipCameraLens,
                    onZoomChange = onCameraZoomChange,
                    onPermissionGranted = onPermissionsUpdated
                )
                HubModule.VAULT -> VaultScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onAddNote = onAddVaultNote,
                    onDeleteNote = onDeleteVaultNote,
                    onStartTimer = onStartVaultTimer,
                    onStopTimer = onStopVaultTimer
                )
                HubModule.SEARCH -> SearchScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onQueryChange = onSearchQueryChange,
                    onLaunchFullscreen = onLaunchWithLeftDialMode,
                    onWebSearch = onWebSearch,
                    onAskStxAi = onAskStxAi
                )
                HubModule.BRIEF, HubModule.STX_AI -> BriefScreen(
                    state = state,
                    focusedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onOpenApps = { onOpenModule(HubModule.APPS) },
                    onOpenSettings = { onOpenModule(HubModule.SETTINGS) },
                    onOpenStxAi = { onOpenModule(HubModule.STX_AI) },
                    onLaunchApp = onLaunchWithLeftDialMode
                )
                HubModule.APPS -> Unit
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = if (state.activeModule == HubModule.APPS) 0.dp else 8.dp,
                vertical = 4.dp
            )
    ) {
        val isWideLandscape = maxWidth >= 520.dp

        if (state.activeModule == HubModule.APPS) {
            AppsScreen(
                state = state,
                dialItems = dialItems,
                onSelectSubIndex = onSelectSubIndex,
                onSelectLeftDialLaunchMode = onSelectLeftDialLaunchMode,
                onLaunchWithLeftDialMode = onLaunchWithLeftDialMode,
                onFocusApp = onFocusApp,
                onCenterBack = onCenterBack,
                onOpenModule = onOpenModule,
                onLaunchWindowed = onLaunchWindowed,
                onLaunchFullscreen = onLaunchFullscreen,
                onTogglePin = onTogglePin,
                onOpenAppInfo = onOpenAppInfo,
                modifier = Modifier.fillMaxSize()
            )
            return@BoxWithConstraints
        }

        if (isWideLandscape) {
            val dialPanelWidth = (maxWidth * 0.27f).coerceIn(176.dp, 224.dp)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NonAppsPanelContent()
                    }
                    CarouselIndexStrip(
                        moduleTitle = state.activeModule.title,
                        focusedItem = focusedItem,
                        activeDialIndex = activeDialIndex,
                        totalItems = dialItems.size,
                        onPrev = { onSelectSubIndex(activeDialIndex - 1, dialItems.size) },
                        onNext = { onSelectSubIndex(activeDialIndex + 1, dialItems.size) }
                    )
                }

                ArcReactorRightHalfDial(
                    items = dialItems,
                    selectedIndex = activeDialIndex,
                    onSelectIndex = { idx -> onSelectSubIndex(idx, dialItems.size) },
                    onActivateIndex = ::activateNonAppsIndex,
                    onCenterBackClick = onCenterBack,
                    showOuterTicks = true,
                    activateOnlyWhenFocused = true,
                    modifier = Modifier
                        .width(dialPanelWidth)
                        .fillMaxHeight()
                        .testTag("right_half_dial")
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NonAppsPanelContent()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xBB060D16))
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimsonBorder, CircleShape)
                            .clickable(onClick = onCenterBack)
                            .testTag("half_dial_center_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "<",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HudCrimson
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dialItems.forEachIndexed { idx, item ->
                            val selected = idx == activeDialIndex
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selected) HudCyanSubtle else Color(0x5508121E))
                                    .border(
                                        1.dp,
                                        if (selected) HudCyan else HudDivider,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        onSelectSubIndex(idx, dialItems.size)
                                        if (selected) activateNonAppsIndex(idx)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${item.badge} ${item.label}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 9.5.sp,
                                    color = if (selected) HudIce else HudSilver,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                                .clickable { onSelectSubIndex(activeDialIndex - 1, dialItems.size) }
                                .testTag("carousel_prev_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", tint = HudCyan, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                                .clickable { onSelectSubIndex(activeDialIndex + 1, dialItems.size) }
                                .testTag("carousel_next_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = HudCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselIndexStrip(
    moduleTitle: String,
    focusedItem: DialWedgeItem?,
    activeDialIndex: Int,
    totalItems: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x99060D16))
            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildString {
                append(moduleTitle)
                if (focusedItem != null) {
                    append(" · ")
                    append(focusedItem.label)
                    if (focusedItem.subLabel.isNotBlank()) {
                        append(" · ")
                        append(focusedItem.subLabel)
                    }
                }
            },
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            color = HudCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onPrev)
                    .testTag("carousel_prev_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous item", tint = HudCyan, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "%02d/%02d".format(activeDialIndex + 1, totalItems.coerceAtLeast(1)),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = HudIce,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onNext)
                    .testTag("carousel_next_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next item", tint = HudCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}
