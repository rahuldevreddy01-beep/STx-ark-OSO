package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StarkDarkColorScheme = darkColorScheme(
    primary = HudCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = HudCyanMuted,
    onPrimaryContainer = HudIce,
    secondary = HudCrimson,
    onSecondary = HudIce,
    secondaryContainer = HudCrimsonSubtle,
    onSecondaryContainer = HudCrimsonSoft,
    tertiary = HudEmerald,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = HudIce,
    surface = ObsidianPanel,
    onSurface = HudIce,
    surfaceVariant = ObsidianGlass,
    onSurfaceVariant = HudSilver,
    outline = HudCyanBorder,
    outlineVariant = HudDivider,
    error = HudCrimson,
    onError = HudIce
)

private val StarkShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun StarkOsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StarkDarkColorScheme,
        typography = StarkTypography,
        shapes = StarkShapes,
        content = content
    )
}
