package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.model.DialWedgeItem
import com.example.model.StarkMode
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan

/**
 * Left-edge protruding Half-Dial replacing the old BottomHudBar.
 * Positioned with half its circle off the left screen edge and half visible on-screen,
 * putting all global navigation & utility actions (HUB, PORTRAIT, HALF-DIAL, STX AI,
 * SETTINGS, LOCK, FULL/EXIT, ROTATE) cleanly on a single rotary edge dial.
 */
@Composable
fun LeftEdgeCommandDial(
    currentMode: StarkMode,
    isFullscreen: Boolean,
    isSettingsActive: Boolean,
    onFoldRotate: () -> Unit,
    onSelectMode: (StarkMode) -> Unit,
    onOpenSettings: () -> Unit,
    onLockDeck: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(isFullscreen) {
        listOf(
            DialWedgeItem(id = "mode_hub", badge = "01", label = "HUB"),
            DialWedgeItem(id = "mode_portrait", badge = "02", label = "PORTRAIT"),
            DialWedgeItem(id = "mode_half_dial", badge = "03", label = "HALF-DIAL"),
            DialWedgeItem(id = "mode_stx_ai", badge = "04", label = "STX AI", isAccentCrimson = true),
            DialWedgeItem(id = "act_settings", badge = "05", label = "SETTINGS"),
            DialWedgeItem(id = "act_lock", badge = "06", label = "LOCK", isAccentCrimson = true),
            DialWedgeItem(
                id = "act_fullscreen",
                badge = "07",
                label = if (isFullscreen) "EXIT FULL" else "FULLSCRN",
                isAccentCrimson = isFullscreen
            ),
            DialWedgeItem(id = "act_rotate", badge = "08", label = "ROTATE")
        )
    }

    val syncedIndex = remember(currentMode, isSettingsActive) {
        when {
            isSettingsActive -> 4
            currentMode == StarkMode.HUB -> 0
            currentMode == StarkMode.PORTRAIT -> 1
            currentMode == StarkMode.HALF_DIAL -> 2
            currentMode == StarkMode.STX_AI -> 3
            else -> 0
        }
    }

    var dialIndex by remember { mutableIntStateOf(syncedIndex) }
    LaunchedEffect(syncedIndex) {
        dialIndex = syncedIndex
    }

    fun activateAction(index: Int) {
        val item = items.getOrNull(index) ?: return
        when (item.id) {
            "mode_hub" -> onSelectMode(StarkMode.HUB)
            "mode_portrait" -> onSelectMode(StarkMode.PORTRAIT)
            "mode_half_dial" -> onSelectMode(StarkMode.HALF_DIAL)
            "mode_stx_ai" -> onSelectMode(StarkMode.STX_AI)
            "act_settings" -> onOpenSettings()
            "act_lock" -> {
                dialIndex = syncedIndex
                onLockDeck()
            }
            "act_fullscreen" -> onToggleFullscreen()
            "act_rotate" -> onFoldRotate()
        }
    }

    val activeItem = items.getOrElse(dialIndex) { items.first() }
    val centerShortLabel = when (activeItem.id) {
        "mode_hub" -> "HUB"
        "mode_portrait" -> "PORT"
        "mode_half_dial" -> "APPS"
        "mode_stx_ai" -> "AI"
        "act_settings" -> "CFG"
        "act_lock" -> "LOCK"
        "act_fullscreen" -> if (isFullscreen) "EXIT" else "FULL"
        "act_rotate" -> "ROT"
        else -> "GO"
    }

    ArcReactorSideHalfDial(
        side = DialSide.LEFT,
        items = items,
        selectedIndex = dialIndex.coerceIn(0, items.lastIndex),
        onSelectIndex = { dialIndex = it },
        onActivateIndex = ::activateAction,
        onCenterActionClick = { activateAction(dialIndex) },
        centerLabel = centerShortLabel,
        centerSubLabel = "TAP",
        centerAccentCrimson = activeItem.isAccentCrimson,
        centerButtonTestTag = "left_mode_center_trigger",
        showOuterTicks = false,
        showCoreSpinner = true,
        accentPrimary = HudCyan,
        accentSecondary = HudCrimson,
        halfOffScreen = true,
        modifier = modifier.testTag("left_mode_half_dial")
    )
}
