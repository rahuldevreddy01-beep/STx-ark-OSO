package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

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
        var leftActionIndex by remember { mutableIntStateOf(if (isFloatingMode) 0 else 1) }
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
                "act_pin" -> focusedApp?.let { onTogglePin(it.packageName) }
                "act_grid" -> {
                    if (isWideLandscape) {
                        showCenterGridInLandscape = !showCenterGridInLandscape
                    } else {
                        onOpenModule(HubModule.SEARCH)
                    }
                }
                "act_info" -> focusedApp?.let { onOpenAppInfo(it.packageName) }
            }
        }

        var rightFallbackIndex by remember { mutableIntStateOf(0) }
        val matchedRightIdx = remember(focusedApp, state.topRecentApps) {
            if (focusedApp == null) -1
            else state.topRecentApps.indexOfFirst { it.packageName == focusedApp.packageName }
        }
        LaunchedEffect(matchedRightIdx) {
            if (matchedRightIdx >= 0) rightFallbackIndex = matchedRightIdx
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
            val leftCenterSubLabel = if (focusedLeftItem.id == "act_float" || focusedLeftItem.id == "act_full") "MODE" else "TAP"

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
                            .heightIn(min = 28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isFloatingMode) HudCyanSubtle else Color.Transparent)
                            .border(1.dp, if (isFloatingMode) HudCyan else Color.Transparent, RoundedCornerShape(3.dp))
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
                            .heightIn(min = 28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (!isFloatingMode) HudCrimsonSubtle else Color.Transparent)
                            .border(1.dp, if (!isFloatingMode) HudCrimson else Color.Transparent, RoundedCornerShape(3.dp))
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

                AppsEdgeHalfDial(
                    isLeftEdge = true,
                    items = leftControlItems,
                    selectedIndex = safeLeftIdx,
                    onSelectIndex = { idx -> leftActionIndex = idx },
                    onActivateIndex = ::activateLeftControl,
                    onCenterActionClick = {
                        if (safeLeftIdx == 0 || safeLeftIdx == 1) {
                            onSelectLeftDialLaunchMode(if (isFloatingMode) AppLaunchMode.FULLSCREEN else AppLaunchMode.FLOATING)
                        } else {
                            activateLeftControl(safeLeftIdx)
                        }
                    },
                    centerLabel = leftCenterLabel,
                    centerSubLabel = leftCenterSubLabel,
                    centerAccentCrimson = true,
                    centerButtonTestTag = "left_dial_mode_toggle",
                    accentPrimary = HudCrimson,
                    accentSecondary = HudCyan,
                    activateOnlyWhenFocused = false,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x99060D16))
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
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
                }

                AppsEdgeHalfDial(
                    isLeftEdge = false,
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
                    centerSubLabel = null,
                    centerAccentCrimson = false,
                    centerButtonTestTag = "half_dial_center_back",
                    accentPrimary = HudCyan,
                    accentSecondary = HudCrimson,
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LeftLaunchDialColumn(
                    dialModifier = Modifier
                        .width(dialWidth)
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (showCenterGridInLandscape) {
                            AppsFiveColGrid(
                                apps = allApps,
                                focusedIndex = activeAppIndex,
                                appsLoading = state.appsLoading,
                                onLaunchApp = onLaunchWithLeftDialMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AppFocusPagerDeck(
                                apps = allApps,
                                focusedIndex = activeAppIndex,
                                isFloatingDefault = isFloatingMode,
                                onSelectIndex = { idx -> onSelectSubIndex(idx, allApps.size) },
                                onCardTap = onLaunchWithLeftDialMode,
                                onLaunchWindowed = onLaunchWindowed,
                                onLaunchFullscreen = onLaunchFullscreen,
                                onTogglePin = onTogglePin,
                                modifier = Modifier.fillMaxSize()
                            )
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

                RightRecentDialColumn(
                    dialModifier = Modifier
                        .width(dialWidth)
                        .fillMaxHeight()
                )
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
                    LeftLaunchDialColumn(
                        dialModifier = Modifier
                            .width(dialWidth)
                            .fillMaxHeight()
                    )
                    AppFocusPagerDeck(
                        apps = allApps,
                        focusedIndex = activeAppIndex,
                        isFloatingDefault = isFloatingMode,
                        onSelectIndex = { idx -> onSelectSubIndex(idx, allApps.size) },
                        onCardTap = onLaunchWithLeftDialMode,
                        onLaunchWindowed = onLaunchWindowed,
                        onLaunchFullscreen = onLaunchFullscreen,
                        onTogglePin = onTogglePin,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RightRecentDialColumn(
                        dialModifier = Modifier
                            .width(dialWidth)
                            .fillMaxHeight()
                    )
                }

                AppsFiveColGrid(
                    apps = allApps,
                    focusedIndex = activeAppIndex,
                    appsLoading = state.appsLoading,
                    onLaunchApp = onLaunchWithLeftDialMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AppFocusPagerDeck(
    apps: List<InstalledApp>,
    focusedIndex: Int,
    isFloatingDefault: Boolean = true,
    onSelectIndex: (Int) -> Unit,
    onCardTap: (InstalledApp) -> Unit,
    onLaunchWindowed: (InstalledApp) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onTogglePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xD8050A12))
                .border(1.dp, HudCyanBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("NO APPS FOUND", fontFamily = JetBrainsMonoFontFamily, color = HudSlate)
        }
        return
    }

    val safeIndex = focusedIndex.coerceIn(0, apps.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { apps.size })

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

    HorizontalPager(
        state = pagerState,
        key = { idx -> "${apps[idx].packageName}/${apps[idx].activityName}#$idx" },
        beyondViewportPageCount = 1,
        modifier = modifier
    ) { page ->
        val app = apps[page]
        val isCenter = page == pagerState.currentPage
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompactStage = maxWidth < 320.dp
            val isShortStage = maxHeight < 250.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEB040911))
                    .border(1.2.dp, HudCyan, RoundedCornerShape(8.dp))
                    .clickable { onCardTap(app) }
                    .padding(if (isCompactStage) 10.dp else 14.dp)
                    .then(if (isCenter) Modifier.testTag("active_app_carousel_card") else Modifier),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(if (isCompactStage) 4.dp else 6.dp)) {
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
                                text = "%02d".format(page + 1),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactStage) 10.sp else 12.sp,
                                color = HudIce
                            )
                            Text(
                                text = "APPS · %02d/%02d".format(page + 1, apps.size),
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
                            Box(
                                modifier = Modifier
                                    .size(if (isCompactStage) 26.dp else 30.dp)
                                    .clip(CircleShape)
                                    .background(if (app.isPinnedToDock) HudCrimsonSubtle else Color(0x44081018))
                                    .border(1.dp, if (app.isPinnedToDock) HudCrimson else HudDivider, CircleShape)
                                    .clickable { onTogglePin(app.packageName) }
                                    .then(if (isCenter) Modifier.testTag("carousel_pin_toggle") else Modifier),
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
                                .border(1.dp, HudCyan, RoundedCornerShape(8.dp)),
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    AppSpecRow(
                        label = "STATE",
                        value = if (app.isSystemApp) "SYSTEM · SDK ${app.targetSdk}" else "USER · SDK ${app.targetSdk}",
                        valueColor = HudCyan,
                        compact = isCompactStage
                    )
                    AppSpecRow(
                        label = "PERMISSIONS",
                        value = "${app.permissionsCount} DECLARED",
                        valueColor = HudIce,
                        compact = isCompactStage
                    )
                    if (!isShortStage) {
                        AppSpecRow(
                            label = "ENTRY",
                            value = app.activityName.substringAfterLast('.').ifBlank { "LAUNCHER" },
                            valueColor = HudSilver,
                            compact = isCompactStage
                        )
                        AppSpecRow(
                            label = "DOCK / RUNS",
                            value = "${if (app.isPinnedToDock) "PINNED" else "UNPINNED"} · ${app.launchCount} LAUNCHES",
                            valueColor = if (app.isPinnedToDock) HudCrimson else HudIce,
                            compact = isCompactStage
                        )
                    }
                }

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
                            text = if (isFloatingDefault) {
                                if (isCompactStage) "FLOAT ON TAP" else "TAP CARD · FLOAT"
                            } else {
                                if (isCompactStage) "FULL ON TAP" else "TAP CARD · FULL"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactStage) 7.sp else 8.5.sp,
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
                                .then(if (isCenter) Modifier.testTag("carousel_windowed_btn") else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.PictureInPictureAlt, "Launch floating", tint = HudCyan, modifier = Modifier.size(11.dp))
                                Text("FLOAT", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = HudIce, maxLines = 1)
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
                                .then(if (isCenter) Modifier.testTag("carousel_fullscreen_btn") else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.OpenInFull, "Launch fullscreen", tint = HudCrimson, modifier = Modifier.size(11.dp))
                                Text("FULL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = HudIce, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSpecRow(
    label: String,
    value: String,
    valueColor: Color,
    compact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.5.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = if (compact) 8.sp else 9.sp,
            color = HudSlate,
            maxLines = 1
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun AppsFiveColGrid(
    apps: List<InstalledApp>,
    focusedIndex: Int,
    appsLoading: Boolean,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xD8040911))
            .border(1.dp, HudCyanBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
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
                        text = "FLOAT LAUNCH",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
                        color = HudCyan
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
                                    Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(26.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Apps, "Toggle all apps grid", tint = if (showingGrid) HudCrimson else HudCyan, modifier = Modifier.size(13.dp))
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

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
private fun AppsEdgeHalfDial(
    isLeftEdge: Boolean,
    items: List<DialWedgeItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateIndex: (Int) -> Unit,
    onCenterActionClick: () -> Unit,
    centerLabel: String,
    centerSubLabel: String?,
    centerAccentCrimson: Boolean,
    centerButtonTestTag: String,
    accentPrimary: Color,
    accentSecondary: Color,
    activateOnlyWhenFocused: Boolean,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val count = items.size
    val currentItems by rememberUpdatedState(items)
    val currentSelected by rememberUpdatedState(selectedIndex.coerceIn(0, count - 1))
    val currentOnSelect by rememberUpdatedState(onSelectIndex)
    val currentOnActivate by rememberUpdatedState(onActivateIndex)
    val currentOnCenter by rememberUpdatedState(onCenterActionClick)

    var dragAccumDeg by remember { mutableFloatStateOf(0f) }
    val slotStepDeg = (180f / count.coerceAtLeast(3).coerceAtMost(6)).coerceIn(28f, 44f)

    Box(
        modifier = modifier
            .pointerInput(isLeftEdge, count) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        val deltaDeg = if (isLeftEdge) -dragAmount * 0.45f else dragAmount * 0.45f
                        dragAccumDeg += deltaDeg
                        while (dragAccumDeg >= slotStepDeg) {
                            dragAccumDeg -= slotStepDeg
                            val next = (currentSelected + 1) % count
                            currentOnSelect(next)
                        }
                        while (dragAccumDeg <= -slotStepDeg) {
                            dragAccumDeg += slotStepDeg
                            val prev = (currentSelected - 1 + count) % count
                            currentOnSelect(prev)
                        }
                    },
                    onDragEnd = { dragAccumDeg = 0f },
                    onDragCancel = { dragAccumDeg = 0f }
                )
            }
            .pointerInput(isLeftEdge, count, activateOnlyWhenFocused) {
                detectTapGestures { pos ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val cx = if (isLeftEdge) w * 0.14f else w * 0.86f
                    val cy = h * 0.5f
                    val maxR = minOf(w * 0.82f, h * 0.46f)
                    val innerR = maxR * 0.34f
                    val dx = pos.x - cx
                    val dy = pos.y - cy
                    val dist = hypot(dx, dy)
                    if (dist <= innerR * 1.1f) {
                        currentOnCenter()
                    } else if (dist <= maxR * 1.15f) {
                        val relAngle = atan2(dy, dx) * (180f / PI.toFloat())
                        val centerAngle = if (isLeftEdge) 0f else 180f
                        var diff = relAngle - centerAngle
                        if (diff > 180f) diff -= 360f
                        if (diff < -180f) diff += 360f
                        if (isLeftEdge) {
                            val steps = (-diff / slotStepDeg).roundToInt()
                            val hit = ((currentSelected + steps) % count + count) % count
                            if (activateOnlyWhenFocused && hit != currentSelected) {
                                currentOnSelect(hit)
                            } else {
                                currentOnSelect(hit)
                                currentOnActivate(hit)
                            }
                        } else {
                            // Right edge: bottom is smaller angle (<180 -> +diff on screen y)
                            val signedDiff = if (dy >= 0f) -(180f - kotlin.math.abs(relAngle)) else (180f - kotlin.math.abs(relAngle))
                            val steps = (-signedDiff / slotStepDeg).roundToInt()
                            val hit = ((currentSelected + steps) % count + count) % count
                            if (activateOnlyWhenFocused && hit != currentSelected) {
                                currentOnSelect(hit)
                            } else {
                                currentOnSelect(hit)
                                currentOnActivate(hit)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 4f || size.height <= 4f) return@Canvas
            val cx = if (isLeftEdge) size.width * 0.14f else size.width * 0.86f
            val cy = size.height * 0.5f
            val outerR = minOf(size.width * 0.82f, size.height * 0.46f)
            val innerR = outerR * 0.36f
            val centerAngle = if (isLeftEdge) 0f else 180f

            // Outer rail arc
            val arcRectTopLeft = Offset(cx - outerR, cy - outerR)
            val arcSize = Size(outerR * 2f, outerR * 2f)
            val startArc = if (isLeftEdge) -90f else 90f
            drawArc(
                color = accentPrimary.copy(alpha = 0.65f),
                startAngle = startArc,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw visible slots around centerAngle
            val halfSpan = 2
            for (offset in -halfSpan..halfSpan) {
                val itemIdx = ((currentSelected + offset) % count + count) % count
                val item = currentItems[itemIdx]
                val isCenter = offset == 0
                val midAngleDeg = if (isLeftEdge) centerAngle - offset * slotStepDeg else centerAngle + offset * slotStepDeg
                val wedgeHalf = slotStepDeg * 0.44f
                val a1 = Math.toRadians((midAngleDeg - wedgeHalf).toDouble())
                val a2 = Math.toRadians((midAngleDeg + wedgeHalf).toDouble())

                val rOut = if (isCenter) outerR * 0.94f else outerR * 0.86f
                val rIn = innerR * 1.12f

                val p = Path().apply {
                    moveTo(cx + rIn * cos(a1).toFloat(), cy + rIn * sin(a1).toFloat())
                    lineTo(cx + rOut * cos(a1).toFloat(), cy + rOut * sin(a1).toFloat())
                    lineTo(cx + rOut * cos(a2).toFloat(), cy + rOut * sin(a2).toFloat())
                    lineTo(cx + rIn * cos(a2).toFloat(), cy + rIn * sin(a2).toFloat())
                    close()
                }
                val wedgeColor = when {
                    isCenter && item.isAccentCrimson -> HudCrimsonSubtle.copy(alpha = 0.85f)
                    isCenter -> accentPrimary.copy(alpha = 0.24f)
                    else -> Color(0x6606101B)
                }
                val strokeColor = when {
                    isCenter -> HudIce
                    item.isAccentCrimson -> accentSecondary.copy(alpha = 0.45f)
                    else -> accentPrimary.copy(alpha = 0.35f)
                }
                drawPath(p, color = wedgeColor)
                drawPath(p, color = strokeColor, style = Stroke(width = if (isCenter) 1.6.dp.toPx() else 0.8.dp.toPx()))
            }

            // Inner hub ring
            drawCircle(
                color = if (centerAccentCrimson) HudCrimsonSubtle else Color(0xCC06101B),
                radius = innerR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (centerAccentCrimson) HudCrimson else HudCyan,
                radius = innerR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // Crisp wedge labels around the active center wedge
        val safeIdx = currentSelected
        val topItem = items[((safeIdx + 1) % count + count) % count]
        val midItem = items[safeIdx]
        val botItem = items[((safeIdx - 1 + count) % count + count) % count]

        Column(
            modifier = Modifier
                .align(if (isLeftEdge) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight(0.84f)
                .fillMaxWidth(0.62f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (isLeftEdge) Alignment.End else Alignment.Start
        ) {
            listOf(topItem to false, midItem to true, botItem to false).forEach { (w, active) ->
                Column(
                    horizontalAlignment = if (isLeftEdge) Alignment.End else Alignment.Start,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = w.label,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (active) 9.sp else 7.5.sp,
                        color = if (active) HudIce else HudSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (w.subLabel.isNotBlank() || w.badge.isNotBlank()) {
                        Text(
                            text = w.badge.ifBlank { w.subLabel },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 6.5.sp,
                            color = if (active) HudCrimson else HudCyan.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Center clickable core button
        Box(
            modifier = Modifier
                .align(if (isLeftEdge) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 2.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onCenterActionClick)
                .testTag(centerButtonTestTag),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = HudIce,
                    maxLines = 1
                )
                centerSubLabel?.let {
                    Text(
                        text = it,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 6.5.sp,
                        color = if (centerAccentCrimson) HudCrimson else HudCyan,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
