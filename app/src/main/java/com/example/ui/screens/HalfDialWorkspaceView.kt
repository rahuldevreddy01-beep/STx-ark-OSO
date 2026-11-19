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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.ArcReactorSideHalfDial
import com.example.ui.components.DialSide
import com.example.ui.components.SciFiDualPulseBeam
import com.example.ui.components.StxEntrancePanel
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
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
                HubModule.SETTINGS -> SettingsModulePanel(
                    state = state,
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
                    onEngageLockNow = onEngageLockNow
                )
                HubModule.HEALTH -> HealthSensorsPanel(
                    state = state,
                    onPermissionGranted = onPermissionsUpdated
                )
                HubModule.WIFI -> WifiNetworkPanel(
                    state = state,
                    onOpenWifiPanel = onOpenWifiPanel,
                    onOpenBluetoothPanel = onOpenBluetoothPanel
                )
                HubModule.CAMERA -> CameraOpticalPanel(
                    state = state,
                    onToggleTorch = onToggleTorch,
                    onFlipLens = onFlipCameraLens,
                    onZoomChange = onCameraZoomChange
                )
                HubModule.VAULT -> VaultModulePanel(
                    state = state,
                    onAddNote = onAddVaultNote,
                    onDeleteNote = onDeleteVaultNote,
                    onStartTimer = onStartVaultTimer,
                    onStopTimer = onStopVaultTimer
                )
                HubModule.SEARCH -> SearchModulePanel(
                    state = state,
                    onQueryChange = onSearchQueryChange,
                    onLaunchFullscreen = onLaunchFullscreen,
                    onWebSearch = onWebSearch,
                    onAskStxAi = onAskStxAi
                )
                HubModule.BRIEF, HubModule.STX_AI -> BriefModulePanel(
                    state = state,
                    onOpenApps = { onOpenModule(HubModule.APPS) },
                    onOpenSettings = { onOpenModule(HubModule.SETTINGS) }
                )
                HubModule.APPS -> Unit
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        val isWideLandscape = maxWidth >= 520.dp

        if (state.activeModule == HubModule.APPS) {
            val allApps = state.installedApps
            val activeAppIndex = state.subItemIndex.coerceIn(0, (allApps.size - 1).coerceAtLeast(0))
            val focusedApp = allApps.getOrNull(activeAppIndex)

            var showCenterGridInLandscape by remember { mutableStateOf(false) }

            // Left Half-Dial is a Crimson-primary Launch & Action Control Reactor (distinct from Right Dial)
            val isFloatingMode = state.leftDialLaunchMode == AppLaunchMode.FLOATING
            var leftActionIndex by remember(isFloatingMode) {
                mutableIntStateOf(if (isFloatingMode) 0 else 1)
            }
            val isFocusedPinned = focusedApp?.isPinnedToDock == true
            val leftControlItems = remember(isFloatingMode, isFocusedPinned, allApps.size, showCenterGridInLandscape) {
                listOf(
                    DialWedgeItem(
                        id = "act_float",
                        badge = if (isFloatingMode) "ARMED" else "WIN",
                        label = "FLOATING",
                        subLabel = "WINDOWED",
                        isAccentCrimson = !isFloatingMode
                    ),
                    DialWedgeItem(
                        id = "act_full",
                        badge = if (!isFloatingMode) "ARMED" else "MAX",
                        label = "FULLSCREEN",
                        subLabel = "LAUNCH",
                        isAccentCrimson = true
                    ),
                    DialWedgeItem(
                        id = "act_pin",
                        badge = if (isFocusedPinned) "PINNED" else "DOCK",
                        label = if (isFocusedPinned) "UNPIN" else "PIN DOCK",
                        subLabel = "ARC DOCK",
                        isAccentCrimson = isFocusedPinned
                    ),
                    DialWedgeItem(
                        id = "act_grid",
                        badge = "${allApps.size}",
                        label = if (showCenterGridInLandscape) "CAROUSEL" else "ALL APPS",
                        subLabel = "MATRIX",
                        isAccentCrimson = false
                    ),
                    DialWedgeItem(
                        id = "act_info",
                        badge = "SYS",
                        label = "APP INFO",
                        subLabel = "DETAILS",
                        isAccentCrimson = true
                    )
                )
            }

            fun activateLeftControl(idx: Int) {
                leftActionIndex = idx
                when (leftControlItems.getOrNull(idx)?.id) {
                    "act_float" -> {
                        onSelectLeftDialLaunchMode(AppLaunchMode.FLOATING)
                        focusedApp?.let(onLaunchWindowed)
                    }
                    "act_full" -> {
                        onSelectLeftDialLaunchMode(AppLaunchMode.FULLSCREEN)
                        focusedApp?.let(onLaunchFullscreen)
                    }
                    "act_pin" -> {
                        focusedApp?.let { onTogglePin(it.packageName) }
                    }
                    "act_grid" -> {
                        if (isWideLandscape) {
                            showCenterGridInLandscape = !showCenterGridInLandscape
                        } else {
                            onOpenModule(HubModule.SEARCH)
                        }
                    }
                    "act_info" -> {
                        focusedApp?.let { onOpenAppInfo(it.packageName) }
                    }
                }
            }

            // Right Half-Dial shows lightweight Top Used / Recent Apps
            var rightFallbackIndex by remember { mutableIntStateOf(0) }
            val matchedRightIdx = remember(focusedApp, state.topRecentApps) {
                if (focusedApp == null) -1
                else state.topRecentApps.indexOfFirst { it.packageName == focusedApp.packageName }
            }
            val rightSelectedIdx = if (matchedRightIdx >= 0) {
                matchedRightIdx
            } else {
                rightFallbackIndex.coerceIn(0, (dialItems.size - 1).coerceAtLeast(0))
            }

            @Composable
            fun LeftLaunchDialColumn(dialModifier: Modifier) {
                Column(
                    modifier = dialModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Compact Floating / Fullscreen switcher pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x9912050B))
                            .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 26.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isFloatingMode) HudCyanSubtle else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isFloatingMode) HudCyan else Color.Transparent,
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable { onSelectLeftDialLaunchMode(AppLaunchMode.FLOATING) }
                                .testTag("left_dial_mode_floating"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FLOAT",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = if (isFloatingMode) HudIce else HudSlate,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 26.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (!isFloatingMode) HudCrimsonSubtle else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (!isFloatingMode) HudCrimson else Color.Transparent,
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable { onSelectLeftDialLaunchMode(AppLaunchMode.FULLSCREEN) }
                                .testTag("left_dial_mode_fullscreen"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FULL",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = if (!isFloatingMode) HudIce else HudSlate,
                                maxLines = 1
                            )
                        }
                    }

                    ArcReactorSideHalfDial(
                        side = DialSide.LEFT,
                        items = leftControlItems,
                        selectedIndex = leftActionIndex.coerceIn(0, leftControlItems.lastIndex),
                        onSelectIndex = { idx ->
                            leftActionIndex = idx
                            when (leftControlItems.getOrNull(idx)?.id) {
                                "act_float" -> onSelectLeftDialLaunchMode(AppLaunchMode.FLOATING)
                                "act_full" -> onSelectLeftDialLaunchMode(AppLaunchMode.FULLSCREEN)
                            }
                        },
                        onActivateIndex = ::activateLeftControl,
                        onCenterActionClick = {
                            onSelectLeftDialLaunchMode(
                                if (isFloatingMode) AppLaunchMode.FULLSCREEN else AppLaunchMode.FLOATING
                            )
                        },
                        centerLabel = if (isFloatingMode) "FLOAT" else "FULL",
                        centerSubLabel = "ACTION",
                        centerAccentCrimson = true,
                        centerButtonTestTag = "left_dial_mode_toggle",
                        showOuterTicks = false,
                        showCoreSpinner = false,
                        accentPrimary = HudCrimson,
                        accentSecondary = HudCyan,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("left_half_dial")
                    )
                }
            }

            @Composable
            fun RightRecentDialColumn(dialModifier: Modifier) {
                Column(
                    modifier = dialModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x99060D16))
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TOP USED · ${state.topRecentApps.size}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SciFiDualPulseBeam(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                    }

                    ArcReactorSideHalfDial(
                        side = DialSide.RIGHT,
                        items = dialItems,
                        selectedIndex = rightSelectedIdx,
                        onSelectIndex = { idx ->
                            rightFallbackIndex = idx
                            state.topRecentApps.getOrNull(idx)?.let(onFocusApp)
                        },
                        onActivateIndex = { idx ->
                            state.topRecentApps.getOrNull(idx)?.let(onLaunchWithLeftDialMode)
                        },
                        onCenterActionClick = onCenterBack,
                        centerLabel = "< BACK",
                        centerAccentCrimson = false,
                        centerButtonTestTag = "half_dial_center_back",
                        showOuterTicks = false,
                        showCoreSpinner = false,
                        accentPrimary = HudCyan,
                        accentSecondary = HudCrimson,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("right_half_dial")
                    )
                }
            }

            if (isWideLandscape) {
                // Symmetrical Dual-Dial Apps Cockpit:
                // [Left Launch/Action Dial] — [CENTER Carousel / All Apps] — [Right Top Used Dial]
                val dialWidth = (maxWidth * 0.25f).coerceIn(154.dp, 206.dp)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StxEntrancePanel(
                        visibleKey = "apps_left_land",
                        slideFromX = -26f,
                        slideFromY = 0f,
                        modifier = Modifier
                            .width(dialWidth)
                            .fillMaxHeight()
                    ) {
                        LeftLaunchDialColumn(dialModifier = Modifier.fillMaxSize())
                    }

                    StxEntrancePanel(
                        visibleKey = "apps_center_land",
                        delayMs = 35,
                        slideFromY = 18f,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AnimatedContent(
                                    targetState = showCenterGridInLandscape,
                                    transitionSpec = {
                                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.94f, animationSpec = tween(180)))
                                            .togetherWith(fadeOut(tween(120)))
                                    },
                                    label = "apps_center_grid_toggle"
                                ) { showGrid ->
                                    if (showGrid) {
                                        AllAppsFiveColGrid(
                                            apps = allApps,
                                            focusedIndex = activeAppIndex,
                                            appsLoading = state.appsLoading,
                                            onLaunchApp = onLaunchWithLeftDialMode,
                                            showLeftRimAccent = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        AppFocusCarouselCard(
                                            apps = allApps,
                                            focusedIndex = activeAppIndex,
                                            onSelectIndex = { idx -> onSelectSubIndex(idx, allApps.size) },
                                            onLaunchWindowed = onLaunchWindowed,
                                            onLaunchFullscreen = onLaunchFullscreen,
                                            onTogglePin = onTogglePin
                                        )
                                    }
                                }
                            }

                            AppsCenterControlStrip(
                                focusedApp = focusedApp,
                                activeIndex = activeAppIndex,
                                totalApps = allApps.size,
                                showingGrid = showCenterGridInLandscape,
                                onPrev = { onSelectSubIndex(activeAppIndex - 1, allApps.size) },
                                onNext = { onSelectSubIndex(activeAppIndex + 1, allApps.size) },
                                onToggleGrid = { showCenterGridInLandscape = !showCenterGridInLandscape }
                            )
                        }
                    }

                    StxEntrancePanel(
                        visibleKey = "apps_right_land",
                        delayMs = 60,
                        slideFromX = 26f,
                        slideFromY = 0f,
                        modifier = Modifier
                            .width(dialWidth)
                            .fillMaxHeight()
                    ) {
                        RightRecentDialColumn(dialModifier = Modifier.fillMaxSize())
                    }
                }
            } else {
                // Portrait Dual-Tier Symmetrical Apps Cockpit:
                // Top Row: [Left Launch/Action Dial] — [Center App Carousel Card] — [Right Top Used Dial]
                // Bottom Row: Lightweight 5-col All Apps Grid
                val topRowHeight = (maxHeight * 0.44f).coerceIn(212.dp, 266.dp)
                val dialWidth = (maxWidth * 0.275f).coerceIn(96.dp, 128.dp)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topRowHeight),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StxEntrancePanel(
                            visibleKey = "apps_left_port",
                            slideFromX = -22f,
                            slideFromY = 0f,
                            modifier = Modifier
                                .width(dialWidth)
                                .fillMaxHeight()
                        ) {
                            LeftLaunchDialColumn(dialModifier = Modifier.fillMaxSize())
                        }

                        StxEntrancePanel(
                            visibleKey = "apps_center_port",
                            delayMs = 35,
                            slideFromY = -14f,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AppFocusCarouselCard(
                                apps = allApps,
                                focusedIndex = activeAppIndex,
                                onSelectIndex = { idx -> onSelectSubIndex(idx, allApps.size) },
                                onLaunchWindowed = onLaunchWindowed,
                                onLaunchFullscreen = onLaunchFullscreen,
                                onTogglePin = onTogglePin
                            )
                        }

                        StxEntrancePanel(
                            visibleKey = "apps_right_port",
                            delayMs = 55,
                            slideFromX = 22f,
                            slideFromY = 0f,
                            modifier = Modifier
                                .width(dialWidth)
                                .fillMaxHeight()
                        ) {
                            RightRecentDialColumn(dialModifier = Modifier.fillMaxSize())
                        }
                    }

                    StxEntrancePanel(
                        visibleKey = "apps_grid_port",
                        delayMs = 75,
                        slideFromY = 24f,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AllAppsFiveColGrid(
                            apps = allApps,
                            focusedIndex = activeAppIndex,
                            appsLoading = state.appsLoading,
                            onLaunchApp = onLaunchWithLeftDialMode,
                            showLeftRimAccent = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            return@BoxWithConstraints
        }

        // Standard Half-Dial Layout for Settings / Health / Wifi / Camera / Vault / Search / Brief
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
private fun AppsCenterControlStrip(
    focusedApp: InstalledApp?,
    activeIndex: Int,
    totalApps: Int,
    showingGrid: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleGrid: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x99060D16))
            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (showingGrid) HudCrimsonSubtle else HudCyanSubtle)
                .border(1.dp, if (showingGrid) HudCrimson else HudCyanBorder, RoundedCornerShape(3.dp))
                .clickable(onClick = onToggleGrid)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("apps_grid_toggle_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Toggle all apps grid",
                    tint = if (showingGrid) HudCrimson else HudCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (showingGrid) "CAROUSEL" else "ALL (${totalApps})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    color = HudIce,
                    maxLines = 1
                )
            }
        }

        Text(
            text = focusedApp?.label?.uppercase() ?: "APPS",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = HudCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onPrev)
                    .testTag("carousel_prev_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous app", tint = HudCyan, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "%02d/%02d".format(activeIndex + 1, totalApps.coerceAtLeast(1)),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = HudIce,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onNext)
                    .testTag("carousel_next_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next app", tint = HudCyan, modifier = Modifier.size(16.dp))
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
