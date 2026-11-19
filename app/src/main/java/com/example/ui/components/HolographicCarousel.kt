package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shared Iron Man 2–style GPU-accelerated 3D holographic cover-flow carousel.
 * Synchronizes bidirectionally with the right-side half-dial (`focusedIndex` / `onSelectIndex`).
 */
@Composable
fun <T> HolographicCardCarousel(
    items: List<T>,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemKey: (Int, T) -> Any = { idx, _ -> idx },
    activeCardTag: String = "active_module_carousel_card",
    isCrimsonAccent: (Int, T, Boolean) -> Boolean = { _, _, _ -> false },
    maxCardWidth: Dp = 500.dp,
    maxCardHeight: Dp = 420.dp,
    compactCardFraction: Float = 0.86f,
    wideCardFraction: Float = 0.78f,
    onActiveCardClick: ((Int, T) -> Unit)? = null,
    cardContent: @Composable BoxScope.(
        index: Int,
        item: T,
        isCenter: Boolean,
        isCompactStage: Boolean,
        isShortStage: Boolean,
        relOffset: Int
    ) -> Unit
) {
    if (items.isEmpty()) return

    val count = items.size
    val safeIdx = focusedIndex.coerceIn(0, count - 1)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnSelect by rememberUpdatedState(onSelectIndex)

    val animPos = remember(count) { Animatable(safeIdx.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(safeIdx) }
    var slotVelocity by remember { mutableFloatStateOf(0f) }
    var activeMotionJob by remember(count) { mutableStateOf<Job?>(null) }

    LaunchedEffect(safeIdx, count, isDragging) {
        if (!isDragging && safeIdx != lastSyncedIndex) {
            lastSyncedIndex = safeIdx
            val cur = animPos.value
            val curMod = ((cur.roundToInt() % count) + count) % count
            var diff = safeIdx - curMod
            if (diff > count / 2) diff -= count
            if (diff < -count / 2) diff += count
            val target = cur.roundToInt() + diff.toFloat()
            animPos.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 380f
                )
            )
        }
    }

    val centerWhole by remember(count) {
        derivedStateOf { animPos.value.roundToInt() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val isCompactStage = maxWidth < 280.dp
        val isShortStage = maxHeight < 235.dp
        val cardWidth = if (isCompactStage) {
            (maxWidth * compactCardFraction).coerceIn(146.dp, 248.dp)
        } else {
            (maxWidth * wideCardFraction).coerceIn(252.dp, maxCardWidth)
        }
        val cardHeight = (maxHeight * 0.94f).coerceIn(172.dp, maxCardHeight)
        val spacingPx = with(density) {
            (if (isCompactStage) 48.dp else 94.dp).toPx()
        }

        val visibleOffsets = remember(count, isCompactStage) {
            when {
                count <= 1 -> listOf(0)
                count == 2 -> listOf(-1, 0)
                count in 3..4 || isCompactStage -> listOf(-1, 0, 1)
                else -> listOf(-2, -1, 0, 1, 2)
            }
        }
        val visibleSlots = remember(centerWhole, count, visibleOffsets) {
            visibleOffsets
                .map { rel -> rel to (((centerWhole + rel) % count) + count) % count }
                .distinctBy { it.second }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, spacingPx) {
                    if (count <= 1) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            activeMotionJob?.cancel()
                            isDragging = true
                            slotVelocity = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!dragAmount.isFinite()) return@detectHorizontalDragGestures
                            val denom = (spacingPx * 1.15f).coerceAtLeast(64f)
                            val deltaSlots = (-dragAmount / denom).coerceIn(-1.5f, 1.5f)
                            if (!deltaSlots.isFinite()) return@detectHorizontalDragGestures
                            slotVelocity = slotVelocity * 0.55f + deltaSlots * 0.45f
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                val nextPos = animPos.value + deltaSlots
                                if (nextPos.isFinite()) {
                                    animPos.snapTo(nextPos)
                                    val idx = ((nextPos.roundToInt() % count) + count) % count
                                    if (idx != lastSyncedIndex) {
                                        lastSyncedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnSelect(idx)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                val projected = animPos.value + (slotVelocity * 1.5f).coerceIn(-1.4f, 1.4f)
                                val targetSlot = if (projected.isFinite()) projected.roundToInt().toFloat() else safeIdx.toFloat()
                                val idx = ((targetSlot.roundToInt() % count) + count) % count
                                if (idx != lastSyncedIndex) {
                                    lastSyncedIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnSelect(idx)
                                }
                                animPos.animateTo(
                                    targetValue = targetSlot,
                                    animationSpec = spring(
                                        dampingRatio = 0.84f,
                                        stiffness = 380f
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                val cur = animPos.value
                                val snapTarget = if (cur.isFinite()) cur.roundToInt().toFloat() else safeIdx.toFloat()
                                animPos.animateTo(
                                    targetValue = snapTarget,
                                    animationSpec = spring(
                                        dampingRatio = 0.86f,
                                        stiffness = 380f
                                    )
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            visibleSlots.forEach { (rel, itemIdx) ->
                val item = items.getOrNull(itemIdx) ?: return@forEach
                val isCenter = rel == 0
                val crimson = isCrimsonAccent(itemIdx, item, isCenter)
                key("${itemIdx}_${itemKey(itemIdx, item)}") {
                    val cardBorderColor = when {
                        isCenter && crimson -> HudCrimson
                        isCenter -> HudCyan
                        crimson -> HudCrimson.copy(alpha = 0.45f)
                        else -> HudCyanBorder.copy(alpha = 0.45f)
                    }
                    // Opaque dark fills eliminate background card bleed-through in 3D overlaps
                    val cardFillColor = if (isCenter) Color(0xFF040810) else Color(0xFF03060B)

                    val clickModifier = when {
                        !isCenter -> Modifier.clickable { currentOnSelect(itemIdx) }
                        onActiveCardClick != null -> Modifier.clickable { onActiveCardClick(itemIdx, item) }
                        else -> Modifier
                    }

                    HudBracketCard(
                        modifier = Modifier
                            .zIndex(30f - abs(rel) * 10f)
                            .graphicsLayer {
                                val liveOffset = rel - (animPos.value - centerWhole)
                                val absOff = abs(liveOffset)
                                val clamped = absOff.coerceIn(0f, 2.2f)
                                cameraDistance = 14f * this.density
                                translationX = liveOffset * spacingPx
                                rotationY = (-liveOffset * 9.5f).coerceIn(-22f, 22f)
                                val sc = when {
                                    clamped < 0.05f -> 1f
                                    clamped <= 1.0f -> 1f - clamped * 0.12f
                                    else -> (0.88f - (clamped - 1f) * 0.10f).coerceIn(0.76f, 0.88f)
                                }
                                scaleX = sc
                                scaleY = sc
                                alpha = when {
                                    clamped < 0.05f -> 1f
                                    clamped <= 1.0f -> 1f - clamped * 0.64f
                                    else -> (0.36f - (clamped - 1f) * 0.22f).coerceIn(0.12f, 0.36f)
                                }
                            }
                            .size(width = cardWidth, height = cardHeight)
                            .then(clickModifier)
                            .then(
                                if (isCenter) Modifier.testTag(activeCardTag) else Modifier
                            ),
                        borderColor = cardBorderColor,
                        accentCornerColor = if (crimson) HudCrimson else HudCyan,
                        bottomLeftAccent = if (crimson) HudCyan else HudCrimson,
                        containerColor = cardFillColor,
                        contentPadding = if (isCompactStage) 10.dp else 14.dp
                    ) {
                        cardContent(itemIdx, item, isCenter, isCompactStage, isShortStage, rel)
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicCardHeader(
    index: Int,
    totalCount: Int,
    moduleLabel: String,
    title: String,
    subtitle: String,
    badge: String,
    isCenter: Boolean,
    isCompactStage: Boolean,
    crimsonBadge: Boolean = false
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
                    text = "%02d".format(index + 1),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompactStage) 10.sp else 12.sp,
                    color = if (isCenter) HudIce else HudSlate
                )
                Text(
                    text = "$moduleLabel · %02d/%02d".format(index + 1, totalCount),
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
                        .border(0.8.dp, if (crimsonBadge) HudCrimson else HudCyanBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = badge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isCompactStage) 7.sp else 8.sp,
                        color = if (crimsonBadge) HudCrimson else HudCyan,
                        maxLines = 1
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = subtitle,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (isCompactStage) 7.5.sp else 8.5.sp,
                color = HudCyan.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontFamily = com.example.ui.theme.OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompactStage) 12.sp else 16.sp,
                color = HudIce,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HudCyan.copy(alpha = 0.24f))
        )
    }
}

@Composable
fun HolographicSideCardFooter(relOffset: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HudCyan.copy(alpha = 0.18f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TAP TO FOCUS",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 7.5.sp,
                color = HudCyan.copy(alpha = 0.75f),
                maxLines = 1
            )
            Text(
                text = if (relOffset < 0) "< PREV" else "NEXT >",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 7.5.sp,
                color = HudSlate,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CarouselSpecRow(
    label: String,
    value: String,
    valueColor: Color,
    compact: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 3.dp else 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (compact) 7.5.sp else 9.sp,
                color = HudSlate,
                maxLines = 1
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 7.5.sp else 9.sp,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.7.dp)
                .background(HudCyan.copy(alpha = 0.12f))
        )
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
