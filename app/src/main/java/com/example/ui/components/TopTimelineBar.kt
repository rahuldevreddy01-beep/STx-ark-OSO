package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun TopTimelineBar(
    dayProgressFraction: Float,
    timeText: String,
    batteryPercent: Int,
    isCharging: Boolean,
    showBackToHub: Boolean,
    onBackToHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBackToHub) {
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onBackToHub)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("top_back_hub_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "< HUB",
                    style = MaterialTheme.typography.labelMedium,
                    color = HudCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 00 - 24 Timeline Ruler with active time marker needle
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val padX = 8.dp.toPx()
                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                val baseY = size.height - 3.dp.toPx()

                // Baseline
                drawLine(
                    color = Color(0x4400F0FF),
                    start = Offset(padX, baseY),
                    end = Offset(size.width - padX, baseY),
                    strokeWidth = 1.dp.toPx()
                )

                // 24 major ticks + sub-ticks
                for (h in 0..24) {
                    val x = padX + (h / 24f) * usableW
                    val isMajor = h % 2 == 0
                    val tickH = if (isMajor) 9.dp.toPx() else 4.dp.toPx()
                    drawLine(
                        color = if (isMajor) Color(0xAA00F0FF) else Color(0x4400F0FF),
                        start = Offset(x, baseY),
                        end = Offset(x, baseY - tickH),
                        strokeWidth = if (isMajor) 1.5f else 1f
                    )
                }

                // Active time marker needle
                val needleX = padX + dayProgressFraction.coerceIn(0f, 1f) * usableW
                drawRect(
                    color = HudCyan.copy(alpha = 0.28f),
                    topLeft = Offset(needleX - 6.dp.toPx(), 2.dp.toPx()),
                    size = Size(12.dp.toPx(), baseY - 2.dp.toPx())
                )
                drawRect(
                    color = HudIce,
                    topLeft = Offset(needleX - 2.5.dp.toPx(), 6.dp.toPx()),
                    size = Size(5.dp.toPx(), baseY - 6.dp.toPx())
                )
            }

            // Hour numbers along top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00", "04", "08", "12", "16", "20", "24").forEach { label ->
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudSlate
                    )
                }
            }
        }

        // Right live clock & real battery percentage
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = timeText,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = HudIce
            )
            Text("·", color = HudSlate, fontSize = 12.sp)
            Text(
                text = if (isCharging) "$batteryPercent%⚡" else "$batteryPercent%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isCharging) HudEmerald else HudCyan
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
