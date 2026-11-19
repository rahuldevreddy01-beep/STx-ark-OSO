package com.example.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.security.PasscodeCrypto
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
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import kotlinx.coroutines.delay

@Composable
fun LockscreenCoverOverlay(
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

    // 10-second awake timeout tracker while Lock Cover is shown without user interaction
    var interactionNonce by remember { mutableIntStateOf(0) }
    var idleSeconds by remember { mutableIntStateOf(0) }
    var isDimmed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onDimChange(false) }
    }

    LaunchedEffect(interactionNonce) {
        idleSeconds = 0
        if (isDimmed) {
            isDimmed = false
            onDimChange(false)
        }
        for (sec in 1..10) {
            delay(1000L)
            idleSeconds = sec
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
        if (activity == null) {
            bioStatusMsg = "BIOMETRIC HOST UNAVAILABLE"
            return
        }
        if (!biometricAvailable) {
            bioStatusMsg = "NO BIOMETRICS ENROLLED ON DEVICE"
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
            bioStatusMsg = "BIOMETRIC PROMPT UNAVAILABLE"
        }
    }

    // Auto-check PIN once length >= 4
    LaunchedEffect(pinBuffer) {
        if (hasPasscode && !prefs.passcodeIsAlphanumeric && pinBuffer.length >= 4) {
            val matched = PasscodeCrypto.verifyPasscode(
                candidate = pinBuffer,
                saltHex = prefs.passcodeSalt,
                expectedHashHex = prefs.passcodeHash
            )
            if (matched) {
                pinBuffer = ""
                onBiometricAuthenticated()
            } else if (pinBuffer.length >= 6) {
                onAttemptPasscode(pinBuffer)
                pinBuffer = ""
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.98f))
            .pointerInput(Unit) {
                var lastWakeMs = 0L
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val now = android.os.SystemClock.uptimeMillis()
                        if (event.type == PointerEventType.Press || now - lastWakeMs >= 400L) {
                            lastWakeMs = now
                            interactionNonce++
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .alpha(if (isDimmed) 0.55f else 1f)
            .then(
                if (!prefs.fullscreenImmersive) Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                else Modifier.padding(top = 12.dp, bottom = 10.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("stx_lockscreen_cover")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth >= 580.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Lock Cover Banner + Sleep Timer Badge + STX AI Trigger
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Lock shield",
                            tint = HudCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = prefs.lockHeadline.replace("STARK OS", "STX OS"),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = HudCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (idleSeconds >= 5 && idleSeconds < 10) "SLEEP ${10 - idleSeconds}S"
                            else state.telemetry.networkTransport,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            color = if (idleSeconds >= 5) HudCrimson else HudSlate
                        )
                        if (prefs.lockShowStxAiTrigger) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (stxDrawerOpen) HudCrimsonSubtle else HudCyanSubtle)
                                    .border(
                                        1.dp,
                                        if (stxDrawerOpen) HudCrimson else HudCyanBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        interactionNonce++
                                        stxDrawerOpen = !stxDrawerOpen
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("lockscreen_stx_ai_trigger"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Quick STX AI on lockscreen",
                                    tint = if (stxDrawerOpen) HudCrimson else HudCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "STX AI",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = HudIce
                                )
                            }
                        }
                    }
                }

                // Inline STX AI Quick Query Drawer on Lockscreen
                AnimatedVisibility(visible = stxDrawerOpen && prefs.lockShowStxAiTrigger) {
                    fun submitLockAi() {
                        interactionNonce++
                        val q = stxQuery.trim()
                        stxQuery = ""
                        onAskStxAi(q.ifBlank { "system brief" })
                    }

                    HudBracketCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        borderColor = HudCyan,
                        contentPadding = 10.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF040A12))
                                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (stxQuery.isEmpty()) {
                                        Text(
                                            text = "ASK STX AI (BATTERY, STEPS, FLASH)…",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = HudSlate
                                        )
                                    }
                                    BasicTextField(
                                        value = stxQuery,
                                        onValueChange = {
                                            interactionNonce++
                                            stxQuery = it
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = { submitLockAi() }),
                                        textStyle = TextStyle(
                                            fontFamily = RajdhaniFontFamily,
                                            fontSize = 14.sp,
                                            color = HudIce
                                        ),
                                        cursorBrush = SolidColor(HudCyan),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                        .clickable { submitLockAi() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Ask STX AI",
                                        tint = HudCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            state.lockAiReply?.let { reply ->
                                Text(
                                    text = reply,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = HudIce
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LockArcHeroSection(
                            state = state,
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight()
                        )
                        LockUnlockSection(
                            hasPasscode = hasPasscode,
                            isAlphanumeric = prefs.passcodeIsAlphanumeric,
                            biometricEnabled = prefs.biometricUnlockEnabled,
                            biometricAvailable = biometricAvailable,
                            pinBuffer = pinBuffer,
                            alphaBuffer = alphaBuffer,
                            errorText = state.lockScreenError ?: bioStatusMsg,
                            onPinAppend = { d ->
                                interactionNonce++
                                onClearError()
                                if (pinBuffer.length < 6) pinBuffer += d
                            },
                            onPinBackspace = {
                                interactionNonce++
                                onClearError()
                                if (pinBuffer.isNotEmpty()) pinBuffer = pinBuffer.dropLast(1)
                            },
                            onAlphaChange = {
                                interactionNonce++
                                onClearError()
                                alphaBuffer = it
                            },
                            onSubmitPinOrAlpha = {
                                interactionNonce++
                                val candidate = if (prefs.passcodeIsAlphanumeric) alphaBuffer else pinBuffer
                                val ok = onAttemptPasscode(candidate)
                                if (!ok) {
                                    pinBuffer = ""
                                    alphaBuffer = ""
                                }
                            },
                            onBiometricClick = ::launchBiometricPrompt,
                            onSwipeUnlock = onSwipeUnlock,
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        LockArcHeroSection(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                        LockUnlockSection(
                            hasPasscode = hasPasscode,
                            isAlphanumeric = prefs.passcodeIsAlphanumeric,
                            biometricEnabled = prefs.biometricUnlockEnabled,
                            biometricAvailable = biometricAvailable,
                            pinBuffer = pinBuffer,
                            alphaBuffer = alphaBuffer,
                            errorText = state.lockScreenError ?: bioStatusMsg,
                            onPinAppend = { d ->
                                interactionNonce++
                                onClearError()
                                if (pinBuffer.length < 6) pinBuffer += d
                            },
                            onPinBackspace = {
                                interactionNonce++
                                onClearError()
                                if (pinBuffer.isNotEmpty()) pinBuffer = pinBuffer.dropLast(1)
                            },
                            onAlphaChange = {
                                interactionNonce++
                                onClearError()
                                alphaBuffer = it
                            },
                            onSubmitPinOrAlpha = {
                                interactionNonce++
                                val candidate = if (prefs.passcodeIsAlphanumeric) alphaBuffer else pinBuffer
                                val ok = onAttemptPasscode(candidate)
                                if (!ok) {
                                    pinBuffer = ""
                                    alphaBuffer = ""
                                }
                            },
                            onBiometricClick = ::launchBiometricPrompt,
                            onSwipeUnlock = onSwipeUnlock,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockArcHeroSection(
    state: StarkUiState,
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val tel = state.telemetry
    val chargeOrbit = if (tel.isCharging && prefs.lockShowChargingRing) {
        val infinite = rememberInfiniteTransition(label = "lock_charge_orbit")
        val sweep by infinite.animateFloat(
            initialValue = -90f,
            targetValue = 270f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "charge_orbit_angle"
        )
        sweep
    } else {
        null
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.46f
                val c = Offset(size.width / 2f, size.height / 2f)

                // Crimson + Cyan split perimeter ticks
                drawCircle(
                    color = HudCrimson.copy(alpha = 0.42f),
                    radius = r * 0.99f,
                    center = c,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = HudCyan.copy(alpha = 0.28f),
                    radius = r * 0.94f,
                    center = c,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Lightweight single-pass Charging / Battery Ring
                if (prefs.lockShowChargingRing) {
                    val ringR = r * 0.84f
                    val ringColor = if (tel.isCharging) HudEmerald else HudCyan
                    drawCircle(
                        color = Color(0xFF0C1A28),
                        radius = ringR,
                        center = c,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    val batterySweep = 360f * (tel.batteryPercent / 100f).coerceIn(0.05f, 1f)
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = batterySweep,
                        useCenter = false,
                        topLeft = Offset(c.x - ringR, c.y - ringR),
                        size = Size(ringR * 2f, ringR * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    if (chargeOrbit != null) {
                        drawArc(
                            color = HudCrimson.copy(alpha = 0.90f),
                            startAngle = chargeOrbit,
                            sweepAngle = 42f,
                            useCenter = false,
                            topLeft = Offset(c.x - ringR, c.y - ringR),
                            size = Size(ringR * 2f, ringR * 2f),
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Subtle static inner ring
                drawCircle(
                    color = HudCrimson.copy(alpha = 0.42f),
                    radius = r * 0.70f,
                    center = c,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.clock.timeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    color = HudIce,
                    maxLines = 1
                )
                Text(
                    text = "${state.clock.dateText} · :${state.clock.secondsText}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = if (tel.isCharging) HudEmerald else HudCyan,
                    maxLines = 1
                )
                if (tel.isCharging) {
                    Text(
                        text = "⚡ CHARGING ${tel.batteryPercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = HudEmerald,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefs.lockShowChargingRing) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x8806101B))
                        .border(
                            1.dp,
                            if (tel.isCharging) HudEmerald else HudCyanBorder,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Power status",
                        tint = if (tel.isCharging) HudEmerald else HudCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (tel.isCharging) "${tel.batteryPercent}% CHARGING" else "${tel.batteryPercent}% ${tel.chargingSource}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = if (tel.isCharging) HudEmerald else HudIce,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (prefs.lockShowSteps) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x8806101B))
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = HudCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${tel.stepsCount} STEPS · ${tel.cardinalDirection}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LockUnlockSection(
    hasPasscode: Boolean,
    isAlphanumeric: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    pinBuffer: String,
    alphaBuffer: String,
    errorText: String?,
    onPinAppend: (String) -> Unit,
    onPinBackspace: () -> Unit,
    onAlphaChange: (String) -> Unit,
    onSubmitPinOrAlpha: () -> Unit,
    onBiometricClick: () -> Unit,
    onSwipeUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (errorText != null) {
            Text(
                text = errorText,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = HudCrimson
            )
        }

        if (!hasPasscode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HudCyanSubtle)
                    .border(1.5.dp, HudCyan, RoundedCornerShape(6.dp))
                    .clickable(onClick = onSwipeUnlock)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("lockscreen_swipe_unlock"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LockOpen, "Unlock STX OS", tint = HudCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "UNLOCK STX OS DECK",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HudIce
                    )
                }
            }
        } else if (isAlphanumeric) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF040A12))
                    .border(1.dp, HudCyan, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (alphaBuffer.isEmpty()) {
                        Text(
                            text = "ENTER PASSCODE…",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = HudSlate
                        )
                    }
                    BasicTextField(
                        value = alphaBuffer,
                        onValueChange = onAlphaChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmitPinOrAlpha() }),
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 15.sp, color = HudIce),
                        cursorBrush = SolidColor(HudCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lockscreen_alpha_input")
                    )
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                        .clickable(onClick = onSubmitPinOrAlpha)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("UNLOCK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val filled = i < pinBuffer.length
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (filled) HudCyan else Color.Transparent)
                            .border(1.5.dp, if (filled) HudIce else HudCyanBorder, CircleShape)
                    )
                }
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("OK", "0", "DEL")
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { key ->
                            val isAction = key == "OK" || key == "DEL"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (key) {
                                            "OK" -> HudCyanSubtle
                                            "DEL" -> HudCrimsonSubtle
                                            else -> Color(0xAA07111D)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when (key) {
                                            "OK" -> HudCyan
                                            "DEL" -> HudCrimsonBorder
                                            else -> HudDivider
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        when (key) {
                                            "OK" -> onSubmitPinOrAlpha()
                                            "DEL" -> onPinBackspace()
                                            else -> onPinAppend(key)
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = HudCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isAction) 11.sp else 16.sp,
                                        color = if (key == "OK") HudCyan else HudIce,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (biometricEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (biometricAvailable) HudCyanSubtle else Color(0x44081018))
                    .border(
                        1.dp,
                        if (biometricAvailable) HudCyanBorder else HudDivider,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onBiometricClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("lockscreen_biometric_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock with Fingerprint or Face",
                    tint = if (biometricAvailable) HudCyan else HudSlate,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (biometricAvailable) "FINGERPRINT / FACE UNLOCK" else "BIOMETRIC (NOT ENROLLED)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (biometricAvailable) HudIce else HudSlate
                )
            }
        }
    }
}
