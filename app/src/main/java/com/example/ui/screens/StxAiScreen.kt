package com.example.ui.screens

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.hardware.HologramBitmapProcessor
import com.example.model.LauncherPreferences
import com.example.ui.StarkUiState
import com.example.ui.theme.HudAmber
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private enum class StxVisualCoreMode(val label: String) {
    ORB_3D("3D ORB"),
    HOLO_GIRL("HOLO GIRL")
}

@Composable
fun StxAiScreen(
    state: StarkUiState,
    voiceListening: Boolean,
    voicePartial: String,
    voiceRms: Float = 0f,
    onVoiceCommand: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onWarmUpSpeech: () -> Unit = {},
    onToggleTts: () -> Unit = {},
    onAvatarTap: () -> Unit = {},
    onToggleOrbMode: ((Boolean) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val voiceOutputActive = state.ttsEnabled && !state.telemetry.audioMuted

    LaunchedEffect(Unit) { onWarmUpSpeech() }
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
                StxEmitterStageCard(
                    preferences = state.preferences,
                    isSpeaking = state.aiSpeaking,
                    isListening = voiceListening,
                    isThinking = state.aiThinking,
                    ttsEnabled = voiceOutputActive,
                    onAvatarTap = onAvatarTap,
                    onToggleOrbMode = onToggleOrbMode,
                    modifier = Modifier
                        .weight(0.41f)
                        .fillMaxHeight()
                )
                StxConversationCard(
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
                    modifier = Modifier
                        .weight(0.59f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StxEmitterStageCard(
                    preferences = state.preferences,
                    isSpeaking = state.aiSpeaking,
                    isListening = voiceListening,
                    isThinking = state.aiThinking,
                    ttsEnabled = voiceOutputActive,
                    onAvatarTap = onAvatarTap,
                    onToggleOrbMode = onToggleOrbMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                )
                StxConversationCard(
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StxEmitterStageCard(
    preferences: LauncherPreferences,
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    ttsEnabled: Boolean,
    onAvatarTap: () -> Unit,
    onToggleOrbMode: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var visualMode by remember(preferences.orbUseHtmlWebGl) {
        mutableStateOf(
            if (preferences.orbUseHtmlWebGl) StxVisualCoreMode.ORB_3D
            else StxVisualCoreMode.HOLO_GIRL
        )
    }

    val badgeText = when {
        isListening -> "LISTENING"
        isSpeaking -> "SPEAKING"
        isThinking -> "THINKING"
        else -> "STANDBY"
    }
    val statusColor = when {
        isListening -> HudCrimson
        isThinking -> HudAmber
        else -> HudCyan
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (visualMode == StxVisualCoreMode.ORB_3D) Color.Black else Color(0xC8031222))
            .border(1.2.dp, if (isListening) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAvatarTap
                )
                .testTag("stx_hologram_core"),
            contentAlignment = Alignment.Center
        ) {
            when (visualMode) {
                StxVisualCoreMode.ORB_3D -> {
                    StxInteractiveOrbCanvas(
                        speedMultiplier = preferences.orbRotationSpeed,
                        scaleMultiplier = preferences.orbScale,
                        autoRotate = preferences.orbAutoRotate,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("stx_html_orb_view")
                    )
                }
                StxVisualCoreMode.HOLO_GIRL -> {
                    HoloGirlAvatarCore(
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

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
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(HudAmber.copy(alpha = 0.14f))
                        .border(0.8.dp, HudAmber.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                        .clickable {
                            val next = if (visualMode == StxVisualCoreMode.ORB_3D) {
                                StxVisualCoreMode.HOLO_GIRL
                            } else {
                                StxVisualCoreMode.ORB_3D
                            }
                            visualMode = next
                            onToggleOrbMode?.invoke(next == StxVisualCoreMode.ORB_3D)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("stx_orb_mode_chip")
                ) {
                    Text(
                        text = visualMode.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = HudAmber,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(0.8.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("stx_expression_badge")
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = statusColor,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (visualMode == StxVisualCoreMode.ORB_3D) "RED+BLUE SPHERICAL WIREFRAME" else "HOLOGRAPHIC AVATAR",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = HudCyan
            )
            Text(
                text = when {
                    visualMode == StxVisualCoreMode.ORB_3D -> "DRAG TO ORBIT"
                    isSpeaking -> "TAP TO STOP"
                    !ttsEnabled -> "VOICE MUTED"
                    else -> "TAP TO SPEAK"
                },
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = if (isSpeaking) HudCyan else HudSlate
            )
        }
    }
}

@Composable
private fun StxConversationCard(
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
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xEB040911))
            .border(1.2.dp, HudCyan, RoundedCornerShape(8.dp))
            .padding(10.dp),
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
                        .heightIn(min = 32.dp)
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
                        .heightIn(min = 32.dp)
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
                    keyboardActions = KeyboardActions(onSend = { if (inputText.isNotBlank()) onSend() }),
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
                Icon(Icons.Default.Mic, stringResource(R.string.cd_voice_command), tint = if (voiceListening) HudIce else HudCrimson, modifier = Modifier.size(16.dp))
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

@Composable
fun BoxScope.StxFloatingBubbleOverlay(
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
            runCatching { listState.animateScrollToItem(recentMessages.lastIndex) }
        }
    }

    AnimatedVisibility(
        visible = state.stxOverlayOpen,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.92f, animationSpec = tween(150)),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.94f, animationSpec = tween(120)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 68.dp, start = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 380.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF2040911))
                .border(1.2.dp, if (voiceListening || state.aiThinking) HudCrimson else HudCyan, RoundedCornerShape(8.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(12.dp)
                .testTag("floating_stx_overlay"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    HoloGirlAvatarCore(
                        isSpeaking = state.aiSpeaking,
                        isListening = voiceListening,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .border(1.dp, if (state.aiSpeaking) HudIce else HudCyan, CircleShape)
                    )
                    Column {
                        Text(
                            text = "STX AI // COMMAND",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = HudIce,
                            maxLines = 1
                        )
                        Text(
                            text = when {
                                voiceListening -> "MIC LISTENING…"
                                state.aiSpeaking -> "VOCALIZING REPLY…"
                                state.aiThinking -> "PROCESSING COMMAND…"
                                else -> "GEMINI 3.8 FLASH"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.5.sp,
                            color = if (voiceListening || state.aiThinking) HudCrimson else HudCyan,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val globalActive = systemFloatingEnabled && canDrawOverlays
                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (globalActive) HudEmerald.copy(alpha = 0.22f) else HudCyanSubtle)
                            .border(1.dp, if (globalActive) HudEmerald else HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onToggleSystemFloating)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("overlay_system_float_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Layers, "Float across all apps", tint = if (globalActive) HudEmerald else HudCyan, modifier = Modifier.size(13.dp))
                            Text(if (globalActive) "ALL APPS ON" else "FLOAT ALL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = if (globalActive) HudEmerald else HudIce)
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
                        Icon(Icons.Default.OpenInFull, "Expand STX AI", tint = HudCyan, modifier = Modifier.size(15.dp))
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
                        Icon(Icons.Default.Close, "Close STX AI overlay", tint = HudCrimson, modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(recentMessages, key = { it.id }) { msg ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (msg.isUser) HudCyanSubtle else Color(0xAA06101B))
                            .border(1.dp, if (msg.isError) HudCrimsonBorder else if (msg.isUser) HudCyanBorder else HudDivider, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (msg.isUser) state.preferences.operatorCallsign else "STX AI",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = if (msg.isUser) HudCyan else HudCrimson
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isError) HudCrimson else HudIce,
                            fontSize = 12.sp
                        )
                    }
                }
            }

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
                            text = if (voiceListening) voicePartial.ifBlank { "LISTENING…" } else stringResource(R.string.ask_stx_ai_hint),
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
                    Icon(Icons.Default.Mic, stringResource(R.string.cd_voice_command), tint = if (voiceListening) HudIce else HudCrimson, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.AutoMirrored.Filled.Send, "Send command to STX AI", tint = HudCyan, modifier = Modifier.size(15.dp))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 58.dp)
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        (if (voiceListening) HudCrimson else HudCyan).copy(alpha = if (state.aiSpeaking || voiceListening) 0.34f else 0.16f),
                        Color(0x99041828),
                        Color(0x55020A14)
                    )
                )
            )
            .border(
                width = if (state.stxOverlayOpen || voiceListening || state.aiSpeaking) 2.dp else 1.5.dp,
                color = when {
                    voiceListening || state.stxOverlayOpen -> HudCrimson
                    state.aiSpeaking -> HudIce
                    else -> HudCyan
                },
                shape = CircleShape
            )
            .clickable(onClick = onToggleOverlay)
            .testTag("floating_stx_bot_orb"),
        contentAlignment = Alignment.Center
    ) {
        HoloGirlAvatarCore(
            isSpeaking = state.aiSpeaking,
            isListening = voiceListening,
            modifier = Modifier.size(38.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    when {
                        voiceListening || state.aiThinking -> HudCrimson
                        state.aiSpeaking -> HudCyan
                        else -> HudEmerald
                    }
                )
                .border(1.dp, Color.Black, CircleShape)
        )
    }
}

@Composable
private fun HoloGirlAvatarCore(
    isSpeaking: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBmp by produceState<android.graphics.Bitmap?>(initialValue = null, context) {
        value = HologramBitmapProcessor.getOrCreateCutoutBitmap(context, 160)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val ringColor = if (isListening) HudCrimson else HudCyan
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.minDimension <= 4f) return@Canvas
            drawCircle(
                color = ringColor.copy(alpha = if (isSpeaking || isListening) 0.7f else 0.35f),
                radius = size.minDimension * 0.46f,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        avatarBmp?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "STX AI Hologram Core",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.84f)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun StxInteractiveOrbCanvas(
    speedMultiplier: Float,
    scaleMultiplier: Float,
    autoRotate: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val scene = remember { StxOrbWireframeRenderer() }
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(autoRotate, speedMultiplier, isSpeaking, isListening) {
        var last = 0L
        while (true) {
            withFrameNanos { n ->
                if (last == 0L) last = n
                val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                last = n
                val boost = if (isSpeaking || isListening) 1.6f else 1.0f
                scene.step(dt * (if (autoRotate) speedMultiplier else 0.1f) * boost, isListening)
                frameTick = n
            }
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scene.onDrag(pan.x, pan.y, zoom)
            }
        }
    ) {
        @Suppress("UNUSED_VARIABLE") val redraw = frameTick
        if (size.width <= 4f || size.height <= 4f) return@Canvas
        drawIntoCanvas {
            scene.draw(it.nativeCanvas, size.width, size.height, density, scaleMultiplier)
        }
    }
}

private class StxOrbWireframeRenderer {
    private var yaw = 0f
    private var pitch = 0.25f
    private var scaleUser = 1f
    private var pulse = 0f

    private val blueBright = 0x00EAFF
    private val redBright = 0xFF2B4D
    private val iceCore = 0xD8F8FF

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    fun step(dt: Float, alertMode: Boolean) {
        yaw += dt * (if (alertMode) 1.55f else 0.85f)
        pitch += dt * 0.28f
        pulse += dt * 3.0f
    }

    fun onDrag(panX: Float, panY: Float, zoom: Float) {
        yaw += panX * 0.008f
        pitch = (pitch + panY * 0.008f).coerceIn(-1.2f, 1.2f)
        if (zoom != 1f) scaleUser = (scaleUser * zoom).coerceIn(0.65f, 1.65f)
    }

    fun draw(canvas: android.graphics.Canvas, width: Float, height: Float, density: Float, scaleMult: Float) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val baseR = minOf(width, height) * 0.36f * scaleMult.coerceIn(0.6f, 1.4f) * scaleUser
        val tau = (PI * 2.0).toFloat()

        fun draw3DRing(radius: Float, rx: Float, ry: Float, rz: Float, rgb: Int, alpha: Int, strokeDp: Float) {
            linePaint.color = (alpha shl 24) or rgb
            linePaint.strokeWidth = max(1f, strokeDp * density)
            val cosX = cos(rx); val sinX = sin(rx)
            val cosY = cos(ry); val sinY = sin(ry)
            val cosZ = cos(rz); val sinZ = sin(rz)
            val segs = 36
            var prevX = 0f; var prevY = 0f
            for (i in 0..segs) {
                val a = (i.toFloat() / segs) * tau
                var x = cos(a) * radius
                var y = 0f
                var z = sin(a) * radius

                val y1 = y * cosX - z * sinX; val z1 = y * sinX + z * cosX; y = y1; z = z1
                val x2 = x * cosY + z * sinY; val z2 = -x * sinY + z * cosY; x = x2; z = z2
                val x3 = x * cosZ - y * sinZ; val y3 = x * sinZ + y * cosZ; x = x3; y = y3

                val persp = 4.2f / (4.2f - (z / max(1f, baseR)))
                val sx = cx + x * persp
                val sy = cy + y * persp
                if (i > 0) canvas.drawLine(prevX, prevY, sx, sy, linePaint)
                prevX = sx; prevY = sy
            }
        }

        // Outer blue wireframe shell
        draw3DRing(baseR, pitch, yaw, 0f, blueBright, 180, 1.4f)
        draw3DRing(baseR, pitch + 0.7f, yaw * 1.1f, 0.5f, blueBright, 150, 1.1f)
        draw3DRing(baseR * 0.94f, pitch - 0.8f, -yaw * 0.9f, -0.4f, blueBright, 130, 1.0f)

        // Crimson energy rings counter-rotating
        draw3DRing(baseR * 0.84f, -pitch * 0.8f, -yaw * 1.45f, 0.9f, redBright, 200, 1.6f)
        draw3DRing(baseR * 0.72f, pitch * 1.3f, yaw * 1.6f, -0.7f, redBright, 175, 1.3f)

        // Inner bright cyan reactor core
        draw3DRing(baseR * 0.48f, pitch * 1.6f, -yaw * 2.1f, 0.3f, iceCore, 210, 1.4f)

        val coreRadius = baseR * (0.14f + 0.03f * sin(pulse))
        dotPaint.color = (0x88 shl 24) or blueBright
        canvas.drawCircle(cx, cy, coreRadius * 1.6f, dotPaint)
        dotPaint.color = (0xCC shl 24) or redBright
        canvas.drawCircle(cx, cy, coreRadius, dotPaint)
    }
}
