package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.StarkUiState
import com.example.ui.components.HudBracketCard
import com.example.ui.components.SciFiDualPulseBeam
import com.example.ui.components.StxEntrancePanel
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StxAiView(
    state: StarkUiState,
    voiceListening: Boolean,
    voicePartial: String,
    onVoiceCommand: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var speakReplies by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(speakReplies, context) {
        if (!speakReplies) {
            tts = null
            onDispose {}
        } else {
            var engine: TextToSpeech? = null
            runCatching {
                engine = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        runCatching { engine?.language = Locale.US }
                    }
                }
                tts = engine
            }
            onDispose {
                runCatching { engine?.stop() }
                runCatching { engine?.shutdown() }
                tts = null
            }
        }
    }

    LaunchedEffect(state.aiMessages.size) {
        if (state.aiMessages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(state.aiMessages.lastIndex) }
            val last = state.aiMessages.last()
            if (!last.isUser && speakReplies && !state.telemetry.audioMuted) {
                runCatching {
                    tts?.speak(last.text.take(240), TextToSpeech.QUEUE_FLUSH, null, "stx_reply")
                }
            }
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
                        .weight(0.38f)
                        .fillMaxHeight()
                ) {
                    StxHologramEmitterStage(
                        isThinking = state.aiThinking || voiceListening,
                        isListening = voiceListening,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                StxEntrancePanel(
                    visibleKey = "stx_deck_wide",
                    delayMs = 45,
                    slideFromX = 24f,
                    slideFromY = 0f,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {
                    StxConversationDeck(
                        state = state,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        speakReplies = speakReplies,
                        onToggleSpeak = { speakReplies = !speakReplies },
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
                        .height(158.dp)
                ) {
                    StxHologramEmitterStage(
                        isThinking = state.aiThinking || voiceListening,
                        isListening = voiceListening,
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
                        speakReplies = speakReplies,
                        onToggleSpeak = { speakReplies = !speakReplies },
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
    isThinking: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "stx_holo")
    val floatOffset by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holo_float"
    )
    val scanY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "holo_scan"
    )

    HudBracketCard(
        modifier = modifier,
        borderColor = if (isListening) HudCrimson else HudCyanBorder,
        contentPadding = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                Text(
                    text = when {
                        isListening -> "MIC ACTIVE"
                        isThinking -> "PROCESSING"
                        else -> "STANDBY"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = if (isThinking || isListening) HudCrimson else HudCyan,
                    maxLines = 1
                )
            }

            Image(
                painter = painterResource(id = R.drawable.img_stx_hologram),
                contentDescription = stringResource(R.string.cd_stx_hologram),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.84f)
                    .padding(top = 12.dp, bottom = 14.dp)
                    .offset { IntOffset(0, floatOffset.roundToInt()) }
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val pedY = size.height * 0.88f

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            (if (isListening) HudCrimson else HudCyan).copy(alpha = 0.14f)
                        ),
                        startY = size.height * 0.18f,
                        endY = pedY
                    ),
                    topLeft = Offset(cx - size.width * 0.34f, size.height * 0.18f),
                    size = Size(size.width * 0.68f, pedY - size.height * 0.18f)
                )

                val beamY = size.height * (0.16f + scanY * 0.70f)
                drawLine(
                    color = (if (isListening) HudCrimson else HudCyan).copy(alpha = 0.28f),
                    start = Offset(cx - size.width * 0.32f, beamY),
                    end = Offset(cx + size.width * 0.32f, beamY),
                    strokeWidth = 1.5.dp.toPx()
                )

                drawOval(
                    color = (if (isListening) HudCrimson else HudCyan).copy(alpha = 0.75f),
                    topLeft = Offset(cx - size.width * 0.32f, pedY - 10.dp.toPx()),
                    size = Size(size.width * 0.64f, 20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (speakReplies) HudCyanSubtle else Color(0x44091420))
                            .border(1.dp, if (speakReplies) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                            .clickable(onClick = onToggleSpeak)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, "Speak replies", tint = if (speakReplies) HudCyan else HudSlate, modifier = Modifier.size(13.dp))
                            Text("TTS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = if (speakReplies) HudIce else HudSlate)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("< BACK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                    }
                }
            }

            SciFiDualPulseBeam(
                isAlertCrimson = voiceListening || state.aiThinking,
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
                    text = stringResource(R.string.stx_ai_status_online),
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
