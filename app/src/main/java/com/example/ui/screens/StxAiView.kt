package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.StarkUiState
import com.example.ui.components.HudBracketCard
import com.example.ui.components.SciFiDualPulseBeam
import com.example.ui.components.StxAvatarExpression
import com.example.ui.components.StxEntrancePanel
import com.example.ui.components.StxHologramGirlCore
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun StxAiView(
    state: StarkUiState,
    voiceListening: Boolean,
    voicePartial: String,
    voiceRms: Float = 0f,
    onVoiceCommand: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onWarmUpSpeech: () -> Unit = {},
    onToggleTts: () -> Unit = {},
    onAvatarTap: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val voiceOutputActive = state.ttsEnabled && !state.telemetry.audioMuted

    LaunchedEffect(Unit) {
        onWarmUpSpeech()
    }

    LaunchedEffect(state.aiMessages.size) {
        if (state.aiMessages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(state.aiMessages.lastIndex) }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        val isWide = maxWidth >= 540.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StxEntrancePanel(
                    visibleKey = "stx_holo_wide",
                    slideFromX = -24f,
                    slideFromY = 0f,
                    modifier = Modifier
                        .weight(0.39f)
                        .fillMaxHeight()
                ) {
                    StxHologramEmitterStage(
                        isSpeaking = state.aiSpeaking,
                        isListening = voiceListening,
                        isThinking = state.aiThinking,
                        ttsEnabled = voiceOutputActive,
                        speechPulseTick = state.aiSpeechPulseTick,
                        voiceRms = voiceRms,
                        onAvatarTap = onAvatarTap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                StxEntrancePanel(
                    visibleKey = "stx_deck_wide",
                    delayMs = 45,
                    slideFromX = 24f,
                    slideFromY = 0f,
                    modifier = Modifier
                        .weight(0.61f)
                        .fillMaxHeight()
                ) {
                    StxConversationDeck(
                        state = state,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        speakReplies = voiceOutputActive,
                        onToggleSpeak = onToggleTts,
                        voiceListening = voiceListening,
                        voicePartial = voicePartial,
                        onVoiceClick = onVoiceCommand,
                        onSend = {
                            val q = inputText
                            inputText = ""
                            onSendPrompt(q)
                        },
                        onQuickPrompt = onSendPrompt,
                        onBack = onBack,
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StxEntrancePanel(
                    visibleKey = "stx_holo_port",
                    slideFromY = -18f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(194.dp)
                ) {
                    StxHologramEmitterStage(
                        isSpeaking = state.aiSpeaking,
                        isListening = voiceListening,
                        isThinking = state.aiThinking,
                        ttsEnabled = voiceOutputActive,
                        speechPulseTick = state.aiSpeechPulseTick,
                        voiceRms = voiceRms,
                        onAvatarTap = onAvatarTap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                StxEntrancePanel(
                    visibleKey = "stx_deck_port",
                    delayMs = 45,
                    slideFromY = 22f,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    StxConversationDeck(
                        state = state,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        speakReplies = voiceOutputActive,
                        onToggleSpeak = onToggleTts,
                        voiceListening = voiceListening,
                        voicePartial = voicePartial,
                        onVoiceClick = onVoiceCommand,
                        onSend = {
                            val q = inputText
                            inputText = ""
                            onSendPrompt(q)
                        },
                        onQuickPrompt = onSendPrompt,
                        onBack = onBack,
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun StxHologramEmitterStage(
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    ttsEnabled: Boolean,
    speechPulseTick: Int,
    voiceRms: Float,
    onAvatarTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expression = when {
        isSpeaking -> StxAvatarExpression.SPEAKING
        isListening -> StxAvatarExpression.LISTENING
        isThinking -> StxAvatarExpression.THINKING
        else -> StxAvatarExpression.STANDBY
    }

    val statusColor = when (expression) {
        StxAvatarExpression.LISTENING, StxAvatarExpression.ALERT -> HudCrimson
        StxAvatarExpression.THINKING -> HudAmber
        StxAvatarExpression.SPEAKING, StxAvatarExpression.STANDBY -> HudCyan
    }

    HudBracketCard(
        modifier = modifier,
        borderColor = when {
            isListening -> HudCrimsonBorder
            isSpeaking -> HudCyan
            else -> HudCyanBorder
        },
        containerColor = Color(0x38031222),
        contentPadding = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = if (isSpeaking || isListening) 0.14f else 0.08f),
                            Color(0x22021426),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STX AI // CORE",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = HudIce,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(0.8.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("stx_expression_badge")
                ) {
                    Text(
                        text = expression.badge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = statusColor,
                        maxLines = 1
                    )
                }
            }

            StxHologramGirlCore(
                isSpeaking = isSpeaking,
                isListening = isListening,
                isThinking = isThinking,
                speechPulseTick = speechPulseTick,
                voiceRms = voiceRms,
                onAvatarTap = onAvatarTap,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(top = 14.dp, bottom = 14.dp)
                    .testTag("stx_hologram_core")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoloVoiceWaveMeter(
                    active = isSpeaking || isListening,
                    isCrimson = isListening,
                    amplitude = if (isSpeaking) 0.85f else voiceRms.coerceIn(0.25f, 1f),
                    modifier = Modifier
                        .size(width = 48.dp, height = 14.dp)
                )
                Text(
                    text = when {
                        isSpeaking -> "TAP TO STOP"
                        !ttsEnabled -> "VOICE MUTED"
                        else -> "TAP TO SPEAK"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = if (isSpeaking) HudCyan else HudSlate,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HoloVoiceWaveMeter(
    active: Boolean,
    isCrimson: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "holo_voice_wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(720, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )
    val barColor = if (isCrimson) HudCrimson else HudCyan

    Canvas(modifier = modifier) {
        if (size.width <= 2f || size.height <= 2f) return@Canvas
        val barCount = 7
        val spacing = size.width / (barCount * 1.8f)
        val barW = spacing * 0.85f
        val startX = 0f
        for (i in 0 until barCount) {
            val factor = if (active) {
                (0.28f + 0.72f * abs(sin(phase + i * 0.85f)) * amplitude.coerceIn(0.35f, 1f))
            } else {
                0.18f + (i % 2) * 0.08f
            }
            val barH = (size.height * factor).coerceAtLeast(2f)
            val x = startX + i * spacing * 1.8f
            val y = (size.height - barH) * 0.5f
            drawRoundRect(
                color = barColor.copy(alpha = if (active) 0.90f else 0.32f),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW * 0.5f, barW * 0.5f)
            )
        }
    }
}

@Composable
private fun StxConversationDeck(
    state: StarkUiState,
    inputText: String,
    onInputChange: (String) -> Unit,
    speakReplies: Boolean,
    onToggleSpeak: () -> Unit,
    voiceListening: Boolean,
    voicePartial: String,
    onVoiceClick: () -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onBack: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    HudBracketCard(
        modifier = modifier,
        borderColor = HudCyan,
        contentPadding = 10.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STX AI TERMINAL",
                    style = MaterialTheme.typography.titleMedium,
                    color = HudIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (speakReplies) HudCyanSubtle else Color(0x44091420))
                            .border(1.dp, if (speakReplies) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                            .clickable(onClick = onToggleSpeak)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("stx_tts_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (speakReplies) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (speakReplies) "Mute STX AI voice" else "Enable STX AI voice",
                                tint = if (speakReplies) HudCyan else HudSlate,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (speakReplies) "VOICE ON" else "VOICE OFF",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = if (speakReplies) HudIce else HudSlate
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("< BACK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                    }
                }
            }

            SciFiDualPulseBeam(
                isAlertCrimson = voiceListening || state.aiThinking || state.aiSpeaking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(HudCyanSubtle)
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (state.aiSpeaking) "STX AI // VOCAL PROJECTION ACTIVE" else stringResource(R.string.stx_ai_status_online),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = HudCyan
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.aiMessages, key = { it.id }) { msg ->
                    val bg = if (msg.isUser) HudCyanSubtle else Color(0x8806101B)
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
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (msg.isUser) state.preferences.operatorCallsign else "STX AI",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = if (msg.isUser) HudCyan else HudCrimson
                            )
                            Text(
                                text = msg.executedAction ?: msg.timestamp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                color = if (msg.executedAction != null) HudCyan else HudSlate
                            )
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isError) HudCrimson else HudIce
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("SYSTEM BRIEF", "FLASH ON", "STEP COUNT", "BATTERY", "OPEN APPS", "SETTINGS").forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(3.dp))
                            .clickable { onQuickPrompt(chip) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(chip, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudIce)
                    }
                }
            }

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
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = if (voiceListening) voicePartial.ifBlank { "LISTENING… SPEAK COMMAND" } else stringResource(R.string.ask_stx_ai_hint),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            color = if (voiceListening) HudCrimson else HudSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) onSend()
                        }),
                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 13.sp, color = HudIce),
                        cursorBrush = SolidColor(HudCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stx_ai_input")
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (voiceListening) HudCrimson.copy(alpha = 0.35f) else HudCrimsonSubtle)
                        .border(1.dp, if (voiceListening) HudIce else HudCrimson, CircleShape)
                        .clickable(onClick = onVoiceClick)
                        .testTag("stx_ai_mic"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        stringResource(R.string.cd_voice_command),
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
                        .clickable { if (inputText.isNotBlank()) onSend() }
                        .testTag("stx_ai_send"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send to STX AI", tint = HudCyan, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}
