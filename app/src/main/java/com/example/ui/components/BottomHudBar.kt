package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StarkMode
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianPanel

@Composable
fun BottomHudBar(
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
    val barModifier = modifier
        .fillMaxWidth()
        .background(ObsidianPanel.copy(alpha = 0.92f))
        .then(
            if (!isFullscreen) {
                Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            } else {
                Modifier
            }
        )

    Column(modifier = barModifier) {
        SciFiDualPulseBeam(
            isAlertCrimson = currentMode == StarkMode.HALF_DIAL || isFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
        HudActionChip(
            label = "FOLD / ROTATE",
            icon = Icons.Default.Refresh,
            active = false,
            borderColor = HudCyanBorder,
            activeBg = HudCyanSubtle,
            textColor = HudCyan,
            testTag = "bottom_fold_rotate",
            onClick = onFoldRotate
        )

        ModeNavTextButton(
            label = "PORTRAIT",
            active = currentMode == StarkMode.PORTRAIT,
            useCrimson = false,
            testTag = "bottom_mode_portrait",
            onClick = { onSelectMode(StarkMode.PORTRAIT) }
        )

        ModeNavTextButton(
            label = "HUB",
            active = currentMode == StarkMode.HUB,
            useCrimson = false,
            testTag = "bottom_mode_hub",
            onClick = { onSelectMode(StarkMode.HUB) }
        )

        ModeNavTextButton(
            label = "HALF-DIAL",
            active = currentMode == StarkMode.HALF_DIAL && !isSettingsActive,
            useCrimson = true,
            testTag = "bottom_mode_half_dial",
            onClick = { onSelectMode(StarkMode.HALF_DIAL) }
        )

        ModeNavTextButton(
            label = "STX AI",
            active = currentMode == StarkMode.STX_AI,
            useCrimson = false,
            testTag = "bottom_mode_stx_ai",
            onClick = { onSelectMode(StarkMode.STX_AI) }
        )

        HudActionChip(
            label = "SETTINGS",
            icon = Icons.Default.Settings,
            active = isSettingsActive,
            borderColor = if (isSettingsActive) HudCyan else Color(0x446A8399),
            activeBg = HudCyanSubtle,
            textColor = if (isSettingsActive) HudCyan else HudSlate,
            testTag = "bottom_settings_button",
            onClick = onOpenSettings
        )

        HudActionChip(
            label = "LOCK",
            icon = Icons.Default.Lock,
            active = false,
            borderColor = HudCrimsonBorder,
            activeBg = HudCrimsonSubtle,
            textColor = HudCrimson,
            testTag = "bottom_lock_button",
            onClick = onLockDeck
        )

        HudActionChip(
            label = if (isFullscreen) "EXIT FULL" else "FULLSCREEN",
            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            active = isFullscreen,
            borderColor = if (isFullscreen) HudCrimsonBorder else HudCyanBorder,
            activeBg = if (isFullscreen) HudCrimsonSubtle else HudCyanSubtle,
            textColor = if (isFullscreen) HudCrimson else HudCyan,
            testTag = "bottom_fullscreen_button",
            onClick = onToggleFullscreen
        )
        }
    }
}

@Composable
private fun ModeNavTextButton(
    label: String,
    active: Boolean,
    useCrimson: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val border = when {
        active && useCrimson -> HudCrimson
        active -> HudCyan
        else -> Color.Transparent
    }
    val bg = when {
        active && useCrimson -> HudCrimsonSubtle
        active -> HudCyanSubtle
        else -> Color.Transparent
    }
    val textColor = when {
        active && useCrimson -> HudIce
        active -> HudIce
        else -> HudSlate
    }

    Box(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = textColor,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun HudActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    borderColor: Color,
    activeBg: Color,
    textColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) activeBg else Color(0x44091420))
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = textColor,
            letterSpacing = 1.sp
        )
    }
}
