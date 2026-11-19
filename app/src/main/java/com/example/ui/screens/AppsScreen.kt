package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLaunchMode
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.ui.StarkUiState
import com.example.ui.components.ArcReactorLeftRimAccent
import com.example.ui.components.ArcReactorSideHalfDial
import com.example.ui.components.CarouselSpecRow
import com.example.ui.components.DialSide
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.HolographicSideCardFooter
import com.example.ui.components.HudBracketCard
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
import com.example.ui.theme.OrbitronFontFamily

private val GridCardShape = RoundedCornerShape(6.dp)
private val GridIconShape = RoundedCornerShape(8.dp)

@Composable
fun AppsScreen(
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
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLandscape = maxWidth >= 520.dp
        val allApps = state.installedApps
        val activeAppIndex = state.subItemIndex.coerceIn(0, (allApps.size - 1).coerceAtLeast(0))
        val focusedApp = allApps.getOrNull(activeAppIndex)

        var showCenterGridInLandscape by remember { mutableStateOf(false) }

        val isFloatingMode = state.leftDialLaunchMode == AppLaunchMode.FLOATING
        var leftActionIndex by remember {
            mutableIntStateOf(if (isFloatingMode) 0 else 1)
        }
        LaunchedEffect(isFloatingMode) {
            if (leftActionIndex == 0 || leftActionIndex == 1) {
                leftActionIndex = if (isFloatingMode) 0 else 1
            }
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
                    label = "FULLSCRN",
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

        var rightFallbackIndex by remember { mutableIntStateOf(0) }
        val matchedRightIdx = remember(focusedApp, state.topRecentApps) {
            if (focusedApp == null) -1
            else state.topRecentApps.indexOfFirst { it.packageName == focusedApp.packageName }
        }
        LaunchedEffect(matchedRightIdx) {
            if (matchedRightIdx >= 0) {
                rightFallbackIndex = matchedRightIdx
            }
        }
        val rightSelectedIdx = if (matchedRightIdx >= 0) {
            matchedRightIdx
        } else {
            rightFallbackIndex.coerceIn(0, (dialItems.size - 1).coerceAtLeast(0))
        }

        @Composable
        fun LeftLaunchDialColumn(dialModifier: Modifier) {
            val safeLeftIdx = leftActionIndex.coerceIn(0, leftControlItems.lastIndex)
            val focusedLeftItem = leftControlItems[safeLeftIdx]
            val leftCenterLabel = when (focusedLeftItem.id) {
                "act_float", "act_full" -> if (isFloatingMode) "FLOAT" else "FULL"
                "act_pin" -> if (isFocusedPinned) "UNPIN" else "PIN"
                "act_grid" -> if (showCenterGridInLandscape) "CARO" else "GRID"
                "act_info" -> "INFO"
                else -> if (isFloatingMode) "FLOAT" else "FULL"
            }
            val leftCenterSubLabel = when (focusedLeftItem.id) {
                "act_float", "act_full" -> "MODE"
                else -> "TAP"
            }

            Column(
                modifier = dialModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                    selectedIndex = safeLeftIdx,
                    onSelectIndex = { idx -> leftActionIndex = idx },
                    onActivateIndex = ::activateLeftControl,
                    onCenterActionClick = {
                        if (safeLeftIdx == 0 || safeLeftIdx == 1) {
                            onSelectLeftDialLaunchMode(
                                if (isFloatingMode) AppLaunchMode.FULLSCREEN else AppLaunchMode.FLOATING
                            )
                        } else {
                            activateLeftControl(safeLeftIdx)
                        }
                    },
                    centerLabel = leftCenterLabel,
                    centerSubLabel = leftCenterSubLabel,
                    centerAccentCrimson = true,
                    centerButtonTestTag = "left_dial_mode_toggle",
                    showOuterTicks = false,
                    showCoreSpinner = true,
                    accentPrimary = HudCrimson,
                    accentSecondary = HudCyan,
                    halfOffScreen = true,
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
                    showCoreSpinner = true,
                    accentPrimary = HudCyan,
                    accentSecondary = HudCrimson,
                    halfOffScreen = true,
                    activateOnlyWhenFocused = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("right_half_dial")
                )
            }
        }

        if (isWideLandscape) {
            val dialWidth = (maxWidth * 0.185f).coerceIn(132.dp, 174.dp)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            val topRowHeight = (maxHeight * 0.45f).coerceIn(216.dp, 272.dp)
            val dialWidth = (maxWidth * 0.27f).coerceIn(98.dp, 120.dp)
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
    }
}

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
                            color = HudCrimson
                        )
                    }

                    SciFiDualPulseBeam(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )

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
                                    .background(if (isFocused) HudCrimsonSubtle else Color(0x5506101B))
                                    .border(
                                        width = if (isFocused) 1.3.dp else 0.7.dp,
                                        color = if (isFocused) HudCrimson else HudDivider,
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

    HolographicCardCarousel(
        items = apps,
        focusedIndex = focusedIndex,
        onSelectIndex = onSelectIndex,
        itemKey = { idx, app -> "${app.packageName}/${app.activityName}#$idx" },
        activeCardTag = "active_app_carousel_card",
        maxCardWidth = 460.dp,
        maxCardHeight = 336.dp,
        compactCardFraction = 0.84f,
        wideCardFraction = 0.72f,
        onActiveCardClick = { _, app -> onLaunchFullscreen(app) },
        modifier = modifier
    ) { appIdx, app, isCenter, isCompactStage, isShortStage, relOffset ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (isCompactStage) 3.dp else 5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "%02d".format(appIdx + 1),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactStage) 10.sp else 12.sp,
                            color = if (isCenter) HudIce else HudSlate
                        )
                        Text(
                            text = "APPS · %02d/%02d".format(appIdx + 1, apps.size),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = if (isCompactStage) 7.5.sp else 9.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isCenter) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(0.8.dp, HudCrimson, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(HudCrimson)
                                )
                                Text(
                                    text = "LIVE",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 6.5.sp,
                                    color = HudCrimson,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .border(0.8.dp, HudCyanBorder, RoundedCornerShape(2.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "v${app.versionName}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = if (isCompactStage) 7.sp else 8.sp,
                                color = HudCyan,
                                maxLines = 1
                            )
                        }
                        if (isCenter) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompactStage) 24.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(if (app.isPinnedToDock) HudCrimsonSubtle else Color(0x44081018))
                                    .border(
                                        1.dp,
                                        if (app.isPinnedToDock) HudCrimson else HudDivider,
                                        CircleShape
                                    )
                                    .clickable { onTogglePin(app.packageName) }
                                    .testTag("carousel_pin_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (app.isPinnedToDock) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Pin app to dock",
                                    tint = if (app.isPinnedToDock) HudCrimson else HudSlate,
                                    modifier = Modifier.size(if (isCompactStage) 12.dp else 14.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactStage) 8.dp else 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompactStage) 30.dp else 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A1624))
                            .border(
                                1.dp,
                                if (isCenter) HudCyan else HudCyanBorder,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(if (isCompactStage) 22.dp else 30.dp)
                            )
                        } else {
                            Text(
                                text = app.label.take(2).uppercase(),
                                fontFamily = OrbitronFontFamily,
                                fontSize = if (isCompactStage) 10.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudCyan
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${app.categoryTag} · ${app.packageName}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = if (isCompactStage) 7.5.sp else 8.5.sp,
                            color = HudCyan.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.label.uppercase(),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactStage) 12.sp else 17.sp,
                            color = HudIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HudCyan.copy(alpha = 0.22f))
                )
            }

            if (isCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    CarouselSpecRow(
                        label = "STATE",
                        value = if (app.isSystemApp) "SYSTEM · SDK ${app.targetSdk}" else "USER · SDK ${app.targetSdk}",
                        valueColor = HudCyan,
                        compact = isCompactStage
                    )
                    CarouselSpecRow(
                        label = "PERMISSIONS",
                        value = "${app.permissionsCount} DECLARED",
                        valueColor = HudIce,
                        compact = isCompactStage
                    )
                    if (!isShortStage) {
                        CarouselSpecRow(
                            label = "ENTRY",
                            value = app.activityName.substringAfterLast('.').ifBlank { "LAUNCHER" },
                            valueColor = HudSilver,
                            compact = isCompactStage
                        )
                        CarouselSpecRow(
                            label = "DOCK / RUNS",
                            value = "${if (app.isPinnedToDock) "PINNED" else "UNPINNED"} · ${app.launchCount} LAUNCHES",
                            valueColor = if (app.isPinnedToDock) HudCrimson else HudIce,
                            compact = isCompactStage
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isCenter) {
                Column(verticalArrangement = Arrangement.spacedBy(if (isCompactStage) 4.dp else 6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HudCyan.copy(alpha = 0.18f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompactStage) 4.dp else 6.dp)
                    ) {
                        Text(
                            text = if (isCompactStage) "TAP TO OPEN" else "TAP CARD TO OPEN",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactStage) 7.5.sp else 8.5.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .heightIn(min = if (isCompactStage) 28.dp else 32.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                                .clickable { onLaunchWindowed(app) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("carousel_windowed_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Launch floating",
                                    tint = HudCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "FLOAT",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = HudIce,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .heightIn(min = if (isCompactStage) 28.dp else 32.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(HudCrimsonSubtle)
                                .border(1.dp, HudCrimsonBorder, RoundedCornerShape(3.dp))
                                .clickable { onLaunchFullscreen(app) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("carousel_fullscreen_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.OpenInFull,
                                    contentDescription = "Launch fullscreen",
                                    tint = HudCrimson,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "FULL",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = HudIce,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                HolographicSideCardFooter(relOffset = relOffset)
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
                    text = if (showingGrid) "CAROUSEL" else "ALL ($totalApps)",
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
