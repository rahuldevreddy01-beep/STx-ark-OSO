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
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
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
                    .background(HudCrimsonSubtle)
                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(3.dp))
                    .clickable(onClick = onBackToHub)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("top_back_hub_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "< HUB",
                    style = MaterialTheme.typography.labelMedium,
                    color = HudIce,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 00 - 24 Dual-Tone Red + Electric Blue Timeline Ruler with crimson active time needle
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val padX = 8.dp.toPx()
                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                val baseY = size.height - 3.dp.toPx()
                val needleX = padX + dayProgressFraction.coerceIn(0f, 1f) * usableW

                // Baseline (electric blue) + crimson active progress segment
                drawLine(
                    color = HudElectricBlue.copy(alpha = 0.35f),
                    start = Offset(padX, baseY),
                    end = Offset(size.width - padX, baseY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = HudCrimson.copy(alpha = 0.85f),
                    start = Offset(padX, baseY),
                    end = Offset(needleX, baseY),
                    strokeWidth = 1.8.dp.toPx()
                )

                // 24 major ticks + sub-ticks in dual Blue/Red scheme
                for (h in 0..24) {
                    val x = padX + (h / 24f) * usableW
                    val isMajor = h % 2 == 0
                    val isKeyHour = h % 6 == 0
                    val tickH = when {
                        isKeyHour -> 10.dp.toPx()
                        isMajor -> 7.dp.toPx()
                        else -> 4.dp.toPx()
                    }
                    drawLine(
                        color = when {
                            isKeyHour -> HudCrimson.copy(alpha = 0.85f)
                            isMajor -> HudCyan.copy(alpha = 0.72f)
                            else -> HudElectricBlue.copy(alpha = 0.35f)
                        },
                        start = Offset(x, baseY),
                        end = Offset(x, baseY - tickH),
                        strokeWidth = if (isKeyHour) 2f else if (isMajor) 1.5f else 1f
                    )
                }

                // Glowing Crimson + Ice active time marker needle
                drawRect(
                    color = HudCrimson.copy(alpha = 0.30f),
                    topLeft = Offset(needleX - 6.dp.toPx(), 2.dp.toPx()),
                    size = Size(12.dp.toPx(), baseY - 2.dp.toPx())
                )
                drawRect(
                    color = HudCrimson,
                    topLeft = Offset(needleX - 2.dp.toPx(), 4.dp.toPx()),
                    size = Size(4.dp.toPx(), baseY - 4.dp.toPx())
                )
                drawRect(
                    color = HudIce,
                    topLeft = Offset(needleX - 1.dp.toPx(), 7.dp.toPx()),
                    size = Size(2.dp.toPx(), baseY - 7.dp.toPx())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00", "04", "08", "12", "16", "20", "24").forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = if (idx % 2 == 0) HudSilver else HudSlate
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
            Text("·", color = HudCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
