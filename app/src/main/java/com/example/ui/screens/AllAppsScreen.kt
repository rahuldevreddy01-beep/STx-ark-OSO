package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.InstalledApp
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily

enum class AllAppsSortMode {
    AZ,
    MOST_USED,
    RECENT
}

private val DrawerBgColor = Color(0xFA03060B)
private val DrawerFieldBg = Color(0xFF060B12)
private val DrawerBorderColor = Color(0xFF1F2D3D)
private val DrawerActiveTabColor = Color(0xFFD61F36)
private val DrawerIconFallbackBg = Color(0xFF101926)
private val DrawerIconShape = RoundedCornerShape(12.dp)
private val DrawerBoxShape = RoundedCornerShape(4.dp)

/**
 * Self-contained, zero-clutter, butter-smooth All Apps Screen.
 * Supports instant search, A-Z / Most Used / Recent tabs, Grid <-> List toggle,
 * suggested top apps, long-press quick actions, and swipe-down dismiss.
 */
@Composable
fun AllAppsScreen(
    installedApps: List<InstalledApp>,
    topRecentApps: List<InstalledApp>,
    appsLoading: Boolean,
    onLaunchApp: (InstalledApp) -> Unit,
    onLaunchWindowed: (InstalledApp) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(AllAppsSortMode.AZ) }
    var isListView by rememberSaveable { mutableStateOf(false) }
    var quickActionApp by remember { mutableStateOf<InstalledApp?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val filteredApps = remember(installedApps, searchQuery, sortMode) {
        val q = searchQuery.trim()
        val matched = if (q.isEmpty()) {
            installedApps
        } else {
            installedApps.filter { app ->
                app.label.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true)
            }
        }
        when (sortMode) {
            AllAppsSortMode.AZ -> matched.sortedBy { it.label.lowercase() }
            AllAppsSortMode.MOST_USED -> matched.sortedWith(
                compareByDescending<InstalledApp> { it.launchCount }
                    .thenByDescending { it.lastUsedMs }
                    .thenBy { it.label.lowercase() }
            )
            AllAppsSortMode.RECENT -> matched.sortedWith(
                compareByDescending<InstalledApp> { it.lastUsedMs }
                    .thenByDescending { it.launchCount }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    val suggestedApps = remember(topRecentApps, searchQuery, sortMode) {
        if (searchQuery.isNotBlank() || sortMode != AllAppsSortMode.AZ) {
            emptyList()
        } else {
            topRecentApps.take(4)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DrawerBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("all_apps_drawer_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            var headerDragSum by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { headerDragSum = 0f },
                            onDragCancel = { headerDragSum = 0f }
                        ) { _, dragAmount ->
                            headerDragSum += dragAmount
                            if (headerDragSum > 64f) {
                                headerDragSum = 0f
                                keyboardController?.hide()
                                onClose()
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2D3C4E))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(DrawerBoxShape)
                            .background(DrawerFieldBg)
                            .border(1.dp, DrawerBorderColor, DrawerBoxShape)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.all_apps_search_hint),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                letterSpacing = 1.4.sp,
                                color = HudSlate
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                filteredApps.firstOrNull()?.let(onLaunchApp)
                            }),
                            textStyle = TextStyle(
                                fontFamily = BodyFontFamily,
                                fontSize = 15.sp,
                                color = HudIce
                            ),
                            cursorBrush = SolidColor(HudCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("all_apps_drawer_search")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(DrawerBoxShape)
                            .background(DrawerFieldBg)
                            .border(1.dp, DrawerBorderColor, DrawerBoxShape)
                            .clickable {
                                keyboardController?.hide()
                                if (searchQuery.isNotEmpty()) searchQuery = "" else onClose()
                            }
                            .testTag("all_apps_drawer_close_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close all apps",
                            tint = HudIce,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawerFilterTab(
                        label = "A-Z",
                        selected = sortMode == AllAppsSortMode.AZ,
                        onClick = { sortMode = AllAppsSortMode.AZ },
                        testTag = "all_apps_filter_az"
                    )
                    DrawerFilterTab(
                        label = "MOST USED",
                        selected = sortMode == AllAppsSortMode.MOST_USED,
                        onClick = { sortMode = AllAppsSortMode.MOST_USED },
                        testTag = "all_apps_filter_most_used"
                    )
                    DrawerFilterTab(
                        label = "RECENT",
                        selected = sortMode == AllAppsSortMode.RECENT,
                        onClick = { sortMode = AllAppsSortMode.RECENT },
                        testTag = "all_apps_filter_recent"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(DrawerBoxShape)
                            .background(DrawerFieldBg)
                            .border(1.dp, DrawerBorderColor, DrawerBoxShape)
                            .clickable { isListView = !isListView }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("all_apps_view_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isListView) "GRID" else "LIST",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = HudSilver
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = DrawerBorderColor)

            if (appsLoading && installedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "LOADING APPS…",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = HudSlate
                    )
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = HudSlate,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "NO MATCHING APPS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = HudSlate
                        )
                    }
                }
            } else if (isListView) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("all_apps_drawer_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (suggestedApps.isNotEmpty()) {
                        item(key = "suggested_header_list") {
                            SuggestedStripSection(
                                suggestedApps = suggestedApps,
                                onLaunchApp = { app ->
                                    keyboardController?.hide()
                                    onLaunchApp(app)
                                },
                                onLongPressApp = { quickActionApp = it }
                            )
                        }
                    }
                    items(
                        items = filteredApps,
                        key = { app -> "${app.packageName}/${app.activityName}" },
                        contentType = { "app_list_row" }
                    ) { app ->
                        CleanAppListRow(
                            app = app,
                            onClick = {
                                keyboardController?.hide()
                                onLaunchApp(app)
                            },
                            onLongClick = { quickActionApp = app }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("all_apps_drawer_grid"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (suggestedApps.isNotEmpty()) {
                        item(
                            key = "suggested_section_grid",
                            span = { GridItemSpan(4) },
                            contentType = "suggested_strip"
                        ) {
                            SuggestedStripSection(
                                suggestedApps = suggestedApps,
                                onLaunchApp = { app ->
                                    keyboardController?.hide()
                                    onLaunchApp(app)
                                },
                                onLongPressApp = { quickActionApp = it }
                            )
                        }
                    }

                    items(
                        items = filteredApps,
                        key = { app -> "${app.packageName}/${app.activityName}" },
                        contentType = { "app_grid_tile" }
                    ) { app ->
                        CleanAppGridTile(
                            app = app,
                            onClick = {
                                keyboardController?.hide()
                                onLaunchApp(app)
                            },
                            onLongClick = { quickActionApp = app }
                        )
                    }
                }
            }
        }

        quickActionApp?.let { target ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { quickActionApp = null }
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A101A))
                        .border(1.dp, DrawerBorderColor, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(14.dp)
                        .testTag("all_apps_quick_actions_sheet"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanAppIconBox(app = target, sizeDp = 40)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.label,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = HudIce,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.packageName,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = HudSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.7.dp, color = DrawerBorderColor)
                    QuickSheetActionRow(
                        icon = Icons.Default.OpenInFull,
                        label = "Open Fullscreen",
                        onClick = {
                            quickActionApp = null
                            onLaunchFullscreen(target)
                        }
                    )
                    QuickSheetActionRow(
                        icon = Icons.Default.PictureInPictureAlt,
                        label = "Open Floating Window",
                        onClick = {
                            quickActionApp = null
                            onLaunchWindowed(target)
                        }
                    )
                    QuickSheetActionRow(
                        icon = if (target.isPinnedToDock) Icons.Default.Star else Icons.Default.StarOutline,
                        label = if (target.isPinnedToDock) "Unpin from Home Dock" else "Pin to Home Dock",
                        onClick = {
                            onTogglePin(target.packageName)
                            quickActionApp = null
                        }
                    )
                    QuickSheetActionRow(
                        icon = Icons.Default.Info,
                        label = "App Info & Permissions",
                        onClick = {
                            quickActionApp = null
                            onOpenAppInfo(target.packageName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedStripSection(
    suggestedApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onLongPressApp: (InstalledApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.all_apps_suggested_header),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            color = HudSlate
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedApps.forEach { app ->
                Box(modifier = Modifier.weight(1f)) {
                    CleanAppGridTile(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onLongPressApp(app) }
                    )
                }
            }
            repeat((4 - suggestedApps.size).coerceAtLeast(0)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(thickness = 0.7.dp, color = DrawerBorderColor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanAppGridTile(
    app: InstalledApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag("drawer_app_${app.packageName}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CleanAppIconBox(app = app, sizeDp = 50)
        Text(
            text = app.label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = HudIce,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanAppListRow(
    app: InstalledApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF070C14))
            .border(0.7.dp, DrawerBorderColor, RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("drawer_app_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CleanAppIconBox(app = app, sizeDp = 38)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                color = HudIce,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.categoryTag,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = HudSlate,
                maxLines = 1
            )
        }
        if (app.isPinnedToDock) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Pinned",
                tint = HudCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CleanAppIconBox(app: InstalledApp, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(DrawerIconShape)
            .background(DrawerIconFallbackBg),
        contentAlignment = Alignment.Center
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = app.label.take(2).uppercase(),
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.34f).sp,
                color = HudIce
            )
        }
    }
}

@Composable
private fun DrawerFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(DrawerBoxShape)
            .background(if (selected) DrawerActiveTabColor else DrawerFieldBg)
            .border(
                width = 1.dp,
                color = if (selected) DrawerActiveTabColor else DrawerBorderColor,
                shape = DrawerBoxShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = if (selected) Color.White else HudSilver
        )
    }
}

@Composable
private fun QuickSheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0E1724))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, label, tint = HudCyan, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = HudIce
        )
    }
}
