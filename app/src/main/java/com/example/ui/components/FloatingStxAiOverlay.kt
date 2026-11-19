package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.StarkUiState
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

/**
 * Floating STX AI Corner Bot Orb + Compact Translucent Tactical Command Overlay.
 */
@Composable
fun BoxScope.FloatingStxAiOverlay(
    state: StarkUiState,
    voiceListening: Boolean,
    voicePartial: String,
    systemFloatingEnabled: Boolean,
    canDrawOverlays: Boolean,
    onToggleSystemFloating: () -> Unit,
    onToggleOverlay: () -> Unit,
    onDismissOverlay: () -> Unit,
    onExpandFullStx: () -> Unit,
    onVoiceCommand: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val recentMessages = remember(state.aiMessages) { state.aiMessages.takeLast(8) }

    LaunchedEffect(recentMessages.size, state.stxOverlayOpen) {
        if (state.stxOverlayOpen && recentMessages.isNotEmpty()) {
            listState.animateScrollToItem(recentMessages.lastIndex)
        }
    }

    // Floating STX AI Overlay Window
    AnimatedVisibility(
        visible = state.stxOverlayOpen,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 68.dp, start = 14.dp)
    ) {
        HudBracketCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 380.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .testTag("floating_stx_overlay"),
            borderColor = if (voiceListening || state.aiThinking) HudCrimson else HudCyan,
            contentPadding = 12.dp
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stx_hologram),
                            contentDescription = "STX AI Bot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.dp, HudCyan, CircleShape)
                        )
                        Column {
                            Text(
                                text = "STX AI // COMMAND OVERLAY",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = HudIce,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    voiceListening -> "MIC LISTENING…"
                                    state.aiThinking -> "PROCESSING COMMAND…"
                                    else -> "GEMINI 3.8 FLASH · IN-APP TASKS"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                color = if (voiceListening || state.aiThinking) HudCrimson else HudCyan,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val globalActive = systemFloatingEnabled && canDrawOverlays
                        Box(
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (globalActive) HudEmerald.copy(alpha = 0.22f) else HudCyanSubtle)
                                .border(
                                    1.dp,
                                    if (globalActive) HudEmerald else HudCyanBorder,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable(onClick = onToggleSystemFloating)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("overlay_system_float_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Float STX AI across all apps",
                                    tint = if (globalActive) HudEmerald else HudCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (globalActive) "ALL APPS ON" else "FLOAT ALL",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = if (globalActive) HudEmerald else HudIce,
                                    maxLines = 1
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                .clickable(onClick = onExpandFullStx)
                                .testTag("overlay_expand_stx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Open full STX AI screen",
                                tint = HudCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(HudCrimsonSubtle)
                                .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                .clickable(onClick = onDismissOverlay)
                                .testTag("overlay_close_stx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close STX AI overlay",
                                tint = HudCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Compact message feed
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(recentMessages, key = { it.id }) { msg ->
                        val bg = if (msg.isUser) HudCyanSubtle else Color(0xAA06101B)
                        val border = when {
                            msg.isError -> HudCrimsonBorder
                            msg.isUser -> HudCyanBorder
                            else -> HudDivider
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (msg.isUser) state.preferences.operatorCallsign else "STX AI",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = if (msg.isUser) HudCyan else HudCrimson
                                )
                                msg.executedAction?.let { act ->
                                    Text(
                                        text = act,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = HudEmerald
                                    )
                                }
                            }
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.isError) HudCrimson else HudIce,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Quick task chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("FLASH", "APPS", "WIFI", "STEPS", "BRIEF", "SETTINGS", "LOCK").forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                                .clickable { onSendCommand(chip) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(chip, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudIce)
                        }
                    }
                }

                // Input row with Mic + Keyboard IME Send
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF040A12))
                        .border(1.dp, if (voiceListening) HudCrimson else HudCyan, RoundedCornerShape(22.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fun submitNow() {
                        val clean = promptText.trim()
                        if (clean.isNotEmpty()) {
                            promptText = ""
                            onSendCommand(clean)
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (promptText.isEmpty()) {
                            Text(
                                text = if (voiceListening) voicePartial.ifBlank { "LISTENING… SPEAK TASK" }
                                else stringResource(R.string.ask_stx_ai_hint),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                color = if (voiceListening) HudCrimson else HudSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitNow() }),
                            textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 13.5.sp, color = HudIce),
                            cursorBrush = SolidColor(HudCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("floating_stx_input")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (voiceListening) HudCrimson.copy(alpha = 0.35f) else HudCrimsonSubtle)
                            .border(1.dp, if (voiceListening) HudIce else HudCrimson, CircleShape)
                            .clickable(onClick = onVoiceCommand)
                            .testTag("floating_stx_mic"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.cd_voice_command),
                            tint = if (voiceListening) HudIce else HudCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyan, CircleShape)
                            .clickable { submitNow() }
                            .testTag("floating_stx_send"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send command to STX AI",
                            tint = HudCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    // Floating Corner STX AI Bot Orb (always reachable above bottom bar)
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 58.dp)
            .size(50.dp)
            .clip(CircleShape)
            .background(Color(0xEE040C16))
            .border(
                width = if (state.stxOverlayOpen || voiceListening) 2.dp else 1.5.dp,
                color = if (voiceListening || state.stxOverlayOpen) HudCrimson else HudCyan,
                shape = CircleShape
            )
            .clickable(onClick = onToggleOverlay)
            .testTag("floating_stx_bot_orb"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_stx_hologram),
            contentDescription = "Toggle STX AI Bot Overlay",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(if (voiceListening || state.aiThinking) HudCrimson else HudEmerald)
                .border(1.dp, Color.Black, CircleShape)
        )
    }
}
