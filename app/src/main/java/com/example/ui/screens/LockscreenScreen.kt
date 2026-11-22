package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.StarkUiState
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianVoid
import kotlinx.coroutines.delay

/**
 * Self-contained Lockscreen Cover Screen:
 * Clock/date, battery & steps widgets, numeric PIN & alphanumeric passcode keypad,
 * Android BiometricPrompt unlock, quick STX AI lockscreen prompt, and 10s awake auto-sleep.
 */
@Composable
fun LockscreenScreen(
    state: StarkUiState,
    onAttemptPasscode: (String) -> Boolean,
    onBiometricAuthenticated: () -> Unit,
    onSwipeUnlock: () -> Unit,
    onClearError: () -> Unit,
    onAskStxAi: (String) -> Unit,
    onDimChange: (Boolean) -> Unit,
    onIdleSleepTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = state.preferences
    val hasPasscode = prefs.hasPasscode

    var pinBuffer by remember { mutableStateOf("") }
    var alphaBuffer by remember { mutableStateOf("") }
    var stxDrawerOpen by remember { mutableStateOf(false) }
    var stxQuery by remember { mutableStateOf("") }
    var bioStatusMsg by remember { mutableStateOf<String?>(null) }

    var interactionNonce by remember { mutableIntStateOf(0) }
    var isDimmed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onDimChange(false) }
    }

    LaunchedEffect(interactionNonce) {
        if (isDimmed) {
            isDimmed = false
            onDimChange(false)
        }
        for (sec in 1..10) {
            delay(1000L)
            if (sec >= 7 && !isDimmed) {
                isDimmed = true
                onDimChange(true)
            }
            if (sec == 10) {
                onIdleSleepTimeout()
            }
        }
    }

    val biometricAvailable = remember(context) {
        val mgr = BiometricManager.from(context)
        val res = mgr.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        res == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun launchBiometricPrompt() {
        interactionNonce++
        val activity = context as? FragmentActivity
        if (activity == null || !biometricAvailable) {
            bioStatusMsg = "NO BIOMETRICS AVAILABLE"
            return
        }
        bioStatusMsg = null
        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onBiometricAuthenticated()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        bioStatusMsg = errString.toString().uppercase()
                    }
                    override fun onAuthenticationFailed() {
                        bioStatusMsg = "BIOMETRIC NOT RECOGNIZED"
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("STX OS AUTHENTICATION")
                .setSubtitle("Verify fingerprint or face to unlock deck")
                .setNegativeButtonText(if (hasPasscode) "USE PASSCODE" else "CANCEL")
                .setConfirmationRequired(false)
                .build()
            prompt.authenticate(info)
        }.onFailure {
            bioStatusMsg = "BIOMETRIC PROMPT ERROR"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .alpha(if (isDimmed) 0.42f else 1f)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            interactionNonce++
                        }
                    }
                }
            }
            .testTag("lockscreen_cover_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Lock Headline + Battery/Steps Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = prefs.lockHeadline.ifBlank { "STX OS // SECURED DECK" },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = HudCyan
                )
                Text(
                    text = state.clock.timeText,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = HudIce,
                    modifier = Modifier.testTag("lockscreen_clock_text")
                )
                Text(
                    text = state.clock.dateText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = HudSilver
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prefs.lockShowChargingRing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x9906101B))
                                .border(0.8.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Bolt, "Battery", tint = HudEmerald, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${state.telemetry.batteryPercent}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = HudIce
                            )
                        }
                    }
                    if (prefs.lockShowSteps) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x9906101B))
                                .border(0.8.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DirectionsWalk, "Steps", tint = HudCyan, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${state.telemetry.stepsCount} STEPS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = HudIce
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Passcode Keypad OR Biometric / Swipe Unlock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
            ) {
                val errText = state.lockScreenError ?: bioStatusMsg
                if (!errText.isNullOrBlank()) {
                    Text(
                        text = errText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = HudCrimson,
                        textAlign = TextAlign.Center
                    )
                }

                if (hasPasscode) {
                    if (prefs.passcodeIsAlphanumeric) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF060E18))
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (alphaBuffer.isEmpty()) {
                                    Text("ENTER PASSCODE…", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = HudSlate)
                                }
                                BasicTextField(
                                    value = alphaBuffer,
                                    onValueChange = {
                                        interactionNonce++
                                        onClearError()
                                        alphaBuffer = it.take(24)
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (!onAttemptPasscode(alphaBuffer)) alphaBuffer = ""
                                    }),
                                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp, color = HudIce),
                                    cursorBrush = SolidColor(HudCyan),
                                    modifier = Modifier.fillMaxWidth().testTag("lockscreen_alpha_input")
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (!onAttemptPasscode(alphaBuffer)) alphaBuffer = ""
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("UNLOCK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                            }
                        }
                    } else {
                        // PIN Dots + Numeric Keypad
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val slots = 6
                            for (i in 0 until slots) {
                                val filled = i < pinBuffer.length
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) HudCyan else Color(0xFF0A1420))
                                        .border(1.dp, if (filled) HudIce else HudCyanBorder, CircleShape)
                                )
                            }
                        }

                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("CLR", "0", "OK")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                ) {
                                    row.forEach { keyLabel ->
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (keyLabel) {
                                                        "OK" -> HudCyanSubtle
                                                        "CLR" -> HudCrimsonSubtle
                                                        else -> Color(0xCC07101C)
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    when (keyLabel) {
                                                        "OK" -> HudCyan
                                                        "CLR" -> HudCrimsonBorder
                                                        else -> HudDivider
                                                    },
                                                    CircleShape
                                                )
                                                .clickable {
                                                    interactionNonce++
                                                    onClearError()
                                                    when (keyLabel) {
                                                        "CLR" -> if (pinBuffer.isNotEmpty()) pinBuffer = pinBuffer.dropLast(1)
                                                        "OK" -> if (!onAttemptPasscode(pinBuffer)) pinBuffer = ""
                                                        else -> if (pinBuffer.length < 6) pinBuffer += keyLabel
                                                    }
                                                }
                                                .testTag("lock_key_$keyLabel"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (keyLabel == "CLR") {
                                                Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace", tint = HudCrimson, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text(
                                                    text = keyLabel,
                                                    fontFamily = DisplayFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (keyLabel == "OK") 12.sp else 18.sp,
                                                    color = HudIce
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Swipe / Tap Arc Reactor Ring to Unlock
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0x9906121F))
                            .border(1.5.dp, HudCyan, CircleShape)
                            .clickable(onClick = onSwipeUnlock)
                            .testTag("lockscreen_unlock_ring"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = HudCrimson.copy(alpha = 0.55f),
                                radius = size.minDimension * 0.42f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LockOpen, "Unlock deck", tint = HudCyan, modifier = Modifier.size(24.dp))
                            Text(
                                text = "UNLOCK",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = HudIce
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions: Biometric + Quick STX AI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (prefs.biometricUnlockEnabled && biometricAvailable) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                                .clickable { launchBiometricPrompt() }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Fingerprint, "Biometric unlock", tint = HudCyan, modifier = Modifier.size(18.dp))
                            Text("BIOMETRIC", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                        }
                    }
                    if (prefs.lockShowStxAiTrigger) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HudCrimsonSubtle)
                                .border(1.dp, HudCrimsonBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    interactionNonce++
                                    stxDrawerOpen = !stxDrawerOpen
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.AutoAwesome, "Quick STX AI", tint = HudCrimson, modifier = Modifier.size(16.dp))
                            Text("STX AI", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                        }
                    }
                }

                AnimatedVisibility(visible = stxDrawerOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDD06101B))
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.lockAiReply?.let { reply ->
                            Text(
                                text = reply,
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = HudIce,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF03070D))
                                    .border(0.8.dp, HudDivider, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (stxQuery.isEmpty()) {
                                    Text("Ask STX AI…", fontFamily = BodyFontFamily, fontSize = 12.sp, color = HudSlate)
                                }
                                BasicTextField(
                                    value = stxQuery,
                                    onValueChange = {
                                        interactionNonce++
                                        stxQuery = it
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (stxQuery.isNotBlank()) {
                                            onAskStxAi(stxQuery)
                                            stxQuery = ""
                                        }
                                    }),
                                    textStyle = TextStyle(fontFamily = BodyFontFamily, fontSize = 13.sp, color = HudIce),
                                    cursorBrush = SolidColor(HudCyan),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (stxQuery.isNotBlank()) {
                                            interactionNonce++
                                            onAskStxAi(stxQuery)
                                            stxQuery = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = HudCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
