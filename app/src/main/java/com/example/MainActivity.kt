package com.example

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hardware.VoiceCommandManager
import com.example.model.HubModule
import com.example.model.StarkMode
import com.example.security.configureAppCheck
import com.example.service.StxFloatingAiService
import com.example.ui.StarkUiState
import com.example.ui.StarkViewModel
import com.example.ui.components.BottomHudBar
import com.example.ui.components.EdgeFlashOverlay
import com.example.ui.components.FloatingStxAiOverlay
import com.example.ui.components.LockscreenCoverOverlay
import com.example.ui.components.StarkObsidianBackground
import com.example.ui.components.TopTimelineBar
import com.example.ui.screens.HalfDialWorkspaceView
import com.example.ui.screens.LandscapeHubView
import com.example.ui.screens.PortraitCommandView
import com.example.ui.screens.StxAiView
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StarkOsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {

    private val viewModel: StarkViewModel by viewModels()
    private val pendingVoiceTrigger = MutableStateFlow(0)
    private val resumeTick = MutableStateFlow(0)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.onScreenOffEvent()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must initialize App Check before any Firebase AI or ViewModel usage
        configureAppCheck(this, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenOffReceiver, filter)
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val voiceTriggerCount by pendingVoiceTrigger.collectAsStateWithLifecycle()
            val resumeCounter by resumeTick.collectAsStateWithLifecycle()

            LaunchedEffect(state.preferences.fullscreenImmersive) {
                applyImmersiveFullscreen(state.preferences.fullscreenImmersive)
            }
            LaunchedEffect(state.preferences.keepScreenOn, state.telemetry.touchLocked) {
                // Only keep awake while unlocked so Lock Cover can sleep naturally after 10s
                if (state.preferences.keepScreenOn && !state.telemetry.touchLocked) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            StarkOsTheme {
                StarkOsRoot(
                    state = state,
                    viewModel = viewModel,
                    voiceTriggerCount = voiceTriggerCount,
                    resumeCounter = resumeCounter,
                    onSetWindowDimmed = ::setLockDimmed
                )
            }
        }

        handleServiceLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleServiceLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        StxFloatingAiService.setAppInForeground(true)
        resumeTick.value += 1
    }

    override fun onPause() {
        StxFloatingAiService.setAppInForeground(false)
        super.onPause()
    }

    private fun handleServiceLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val modName = intent.getStringExtra(StxFloatingAiService.EXTRA_OPEN_MODULE)
        if (!modName.isNullOrBlank()) {
            runCatching { HubModule.valueOf(modName) }.getOrNull()?.let { mod ->
                viewModel.openModule(mod)
            }
            intent.removeExtra(StxFloatingAiService.EXTRA_OPEN_MODULE)
        }
        if (intent.getBooleanExtra(StxFloatingAiService.EXTRA_TRIGGER_VOICE, false)) {
            intent.removeExtra(StxFloatingAiService.EXTRA_TRIGGER_VOICE)
            pendingVoiceTrigger.value += 1
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.onConfigurationChanged(
            isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        )
    }

    private fun setLockDimmed(dimmed: Boolean) {
        val attrs = window.attributes
        attrs.screenBrightness = if (dimmed) 0.015f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
    }

    private fun applyImmersiveFullscreen(fullscreen: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun StarkOsRoot(
    state: StarkUiState,
    viewModel: StarkViewModel,
    voiceTriggerCount: Int,
    resumeCounter: Int,
    onSetWindowDimmed: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val canDrawOverlays = remember(resumeCounter, state.preferences.systemFloatingAiEnabled) {
        Settings.canDrawOverlays(context)
    }
    var pendingOverlayEnable by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusBanner) {
        if (state.statusBanner != null) {
            delay(2600L)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(state.telemetry.touchLocked) {
        StxFloatingAiService.setScreenLocked(state.telemetry.touchLocked)
    }

    LaunchedEffect(state.preferences.systemFloatingAiEnabled, canDrawOverlays) {
        if (state.preferences.systemFloatingAiEnabled && canDrawOverlays) {
            StxFloatingAiService.start(context)
        } else {
            StxFloatingAiService.stop(context)
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (Settings.canDrawOverlays(context)) {
            viewModel.setSystemFloatingAiEnabled(true)
            StxFloatingAiService.start(context)
            viewModel.showStatusBanner("STX AI FLOATING ACROSS ALL APPS")
        }
    }

    val overlayPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            pendingOverlayEnable = false
            viewModel.setSystemFloatingAiEnabled(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                StxFloatingAiService.start(context)
                viewModel.showStatusBanner("STX AI FLOATING ACROSS ALL APPS")
            }
        } else {
            pendingOverlayEnable = false
            viewModel.setSystemFloatingAiEnabled(false)
            viewModel.showStatusBanner("OVERLAY PERMISSION REQUIRED FOR ALL-APPS AI")
        }
    }

    LaunchedEffect(resumeCounter) {
        if (pendingOverlayEnable && Settings.canDrawOverlays(context)) {
            pendingOverlayEnable = false
            viewModel.setSystemFloatingAiEnabled(true)
            StxFloatingAiService.start(context)
        }
    }

    fun setSystemFloatingAi(desired: Boolean) {
        if (!desired) {
            pendingOverlayEnable = false
            viewModel.setSystemFloatingAiEnabled(false)
            StxFloatingAiService.stop(context)
            viewModel.showStatusBanner("SYSTEM FLOATING AI DISABLED")
            return
        }
        if (Settings.canDrawOverlays(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setSystemFloatingAiEnabled(true)
                StxFloatingAiService.start(context)
                viewModel.showStatusBanner("STX AI FLOATING ACROSS ALL APPS")
            }
        } else {
            pendingOverlayEnable = true
            runCatching {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayPermLauncher.launch(intent)
            }.onFailure {
                pendingOverlayEnable = false
                viewModel.showStatusBanner("OPEN SETTINGS > DISPLAY OVER OTHER APPS")
            }
        }
    }

    fun dismissNonSecureOsKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            // Only ask OS to dismiss Keyguard if there is NO secure PIN/Pattern; otherwise calling
            // requestDismissKeyguard forces MIUI's PIN dialog directly on top of STX OS unlock.
            if (km?.isKeyguardLocked == true && !km.isKeyguardSecure) {
                runCatching { km.requestDismissKeyguard(activity, null) }
            }
        }
    }

    fun dispatchVoiceTranscript(spoken: String) {
        val clean = spoken.trim()
        if (clean.isEmpty()) return
        if (state.telemetry.touchLocked) {
            viewModel.askStxAiOnLockscreen(clean)
        } else {
            val stayInFullAi = state.mode == StarkMode.STX_AI
            viewModel.sendAiPrompt(
                prompt = clean,
                switchToAiView = false,
                openOverlay = !stayInFullAi
            )
        }
    }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isNotBlank()) {
            dispatchVoiceTranscript(spoken)
        }
    }

    val voiceManager = remember(context) {
        VoiceCommandManager(
            context = context,
            onFinalTranscript = { spoken -> dispatchVoiceTranscript(spoken) },
            onStatusMessage = { msg -> viewModel.showStatusBanner(msg) },
            onFallbackIntentNeeded = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "STX AI Voice Command")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    runCatching { speechFallbackLauncher.launch(intent) }
                        .onFailure {
                            viewModel.openStxOverlay()
                            viewModel.showStatusBanner("VOICE ENGINE UNAVAILABLE · USE STX TEXT")
                        }
                } else {
                    viewModel.openStxOverlay()
                    viewModel.showStatusBanner("NO SPEECH SERVICE · TYPE COMMAND BELOW")
                }
            }
        )
    }

    DisposableEffect(voiceManager) {
        onDispose { voiceManager.destroy() }
    }

    val voiceState by voiceManager.voiceState.collectAsStateWithLifecycle()

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!state.telemetry.touchLocked && state.mode != StarkMode.STX_AI) {
                viewModel.openStxOverlay()
            }
            voiceManager.startListening()
        } else {
            viewModel.showStatusBanner("MIC PERMISSION DENIED")
        }
    }

    fun launchVoiceCommand() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            if (!voiceState.isListening && !state.telemetry.touchLocked && state.mode != StarkMode.STX_AI) {
                viewModel.openStxOverlay()
            }
            voiceManager.toggleListening()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var lastHandledVoiceTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(voiceTriggerCount) {
        if (voiceTriggerCount > lastHandledVoiceTrigger) {
            lastHandledVoiceTrigger = voiceTriggerCount
            launchVoiceCommand()
        }
    }

    val shouldHandleBack = state.stxOverlayOpen ||
        state.mode == StarkMode.HALF_DIAL ||
        state.mode == StarkMode.STX_AI
    BackHandler(enabled = shouldHandleBack) {
        viewModel.stepBackFromHalfDialCenter()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Deep Obsidian Tactical Backdrop
        StarkObsidianBackground()

        // 2. Main HUD Shell
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!state.preferences.fullscreenImmersive) {
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    } else {
                        Modifier.padding(top = 6.dp, bottom = 2.dp)
                    }
                )
        ) {
            if (state.mode != StarkMode.PORTRAIT) {
                TopTimelineBar(
                    dayProgressFraction = state.clock.dayProgressFraction,
                    timeText = state.clock.timeText,
                    batteryPercent = state.telemetry.batteryPercent,
                    isCharging = state.telemetry.isCharging,
                    showBackToHub = state.mode == StarkMode.HALF_DIAL || state.mode == StarkMode.STX_AI,
                    onBackToHub = { viewModel.stepBackFromHalfDialCenter() }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.985f, animationSpec = tween(180)))
                            .togetherWith(fadeOut(tween(140)))
                    },
                    label = "stx_mode_transition"
                ) { targetMode ->
                    when (targetMode) {
                        StarkMode.PORTRAIT -> PortraitCommandView(
                            state = state,
                            hubModules = viewModel.hubModules,
                            voiceListening = voiceState.isListening,
                            voicePartial = voiceState.partialTranscript,
                            onSelectHubIndex = viewModel::selectHubIndex,
                            onOpenModule = viewModel::openModule,
                            onLaunchAppFullscreen = viewModel::launchAppFullscreen,
                            onAskStxAi = { q ->
                                viewModel.sendAiPrompt(q, switchToAiView = false, openOverlay = true)
                            },
                            onVoiceCommand = ::launchVoiceCommand,
                            onToggleWifi = viewModel::openWifiPanel,
                            onToggleBluetooth = viewModel::openBluetoothPanel,
                            onToggleFlash = viewModel::toggleFlashlight,
                            onToggleLock = viewModel::toggleTouchLock,
                            onSync = viewModel::syncHardwareAndApps,
                            onToggleMute = viewModel::toggleMute
                        )

                        StarkMode.HUB -> LandscapeHubView(
                            state = state,
                            hubModules = viewModel.hubModules,
                            voiceListening = voiceState.isListening,
                            voicePartial = voiceState.partialTranscript,
                            onSelectHubIndex = viewModel::selectHubIndex,
                            onOpenModule = viewModel::openModule,
                            onToggleWifi = viewModel::openWifiPanel,
                            onToggleBluetooth = viewModel::openBluetoothPanel,
                            onToggleFlash = viewModel::toggleFlashlight,
                            onToggleLock = viewModel::toggleTouchLock,
                            onSync = viewModel::syncHardwareAndApps,
                            onToggleMute = viewModel::toggleMute,
                            onVoiceCommand = ::launchVoiceCommand
                        )

                        StarkMode.HALF_DIAL -> HalfDialWorkspaceView(
                            state = state,
                            dialItems = viewModel.currentHalfDialItems(),
                            onSelectSubIndex = viewModel::selectSubIndex,
                            onSelectLeftDialLaunchMode = viewModel::setLeftDialLaunchMode,
                            onLaunchWithLeftDialMode = viewModel::launchAppWithLeftDialMode,
                            onFocusApp = viewModel::focusApp,
                            onCenterBack = { viewModel.stepBackFromHalfDialCenter() },
                            onOpenModule = viewModel::openModule,
                            onLaunchWindowed = viewModel::launchAppWindowed,
                            onLaunchFullscreen = viewModel::launchAppFullscreen,
                            onTogglePin = viewModel::togglePinApp,
                            onOpenDefaultLauncher = viewModel::openDefaultLauncherSettings,
                            onReloadApps = viewModel::reloadInstalledApps,
                            onSetCallsign = viewModel::setOperatorCallsign,
                            onToggleSfx = viewModel::setSfxEnabled,
                            onToggleMute = viewModel::toggleMute,
                            onToggleFullscreen = viewModel::setFullscreenImmersive,
                            onToggleKeepAwake = viewModel::setKeepScreenOn,
                            onToggleAutoMorph = viewModel::setAutoMorphOrientation,
                            onToggleEdgeColor = viewModel::setEdgeGlowCrimson,
                            onToggleSystemFloatingAi = { desired -> setSystemFloatingAi(desired) },
                            onPermissionsUpdated = viewModel::onPermissionsUpdated,
                            onOpenWifiPanel = viewModel::openWifiPanel,
                            onOpenBluetoothPanel = viewModel::openBluetoothPanel,
                            onToggleTorch = viewModel::toggleFlashlight,
                            onFlipCameraLens = viewModel::toggleCameraLens,
                            onCameraZoomChange = viewModel::setCameraZoom,
                            onAddVaultNote = viewModel::addVaultNote,
                            onDeleteVaultNote = viewModel::deleteVaultNote,
                            onStartVaultTimer = viewModel::startVaultTimer,
                            onStopVaultTimer = viewModel::stopVaultTimer,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onWebSearch = viewModel::openWebSearch,
                            onAskStxAi = { q ->
                                viewModel.sendAiPrompt(q, switchToAiView = false, openOverlay = true)
                            },
                            onSetLockCoverEnabled = viewModel::setLockCoverEnabled,
                            onSetAutoLockOnScreenOff = viewModel::setAutoLockOnScreenOff,
                            onSetBiometricUnlockEnabled = viewModel::setBiometricUnlockEnabled,
                            onSetUseOsPhysicalLock = viewModel::setUseOsPhysicalLock,
                            onSavePasscode = viewModel::savePasscode,
                            onClearPasscode = viewModel::clearPasscode,
                            onSetLockWidgets = viewModel::setLockWidgetVisibility,
                            onSetLockHeadline = viewModel::setLockHeadline,
                            onEngageLockNow = { viewModel.engageLockCover() }
                        )

                        StarkMode.STX_AI -> StxAiView(
                            state = state,
                            voiceListening = voiceState.isListening,
                            voicePartial = voiceState.partialTranscript,
                            onVoiceCommand = ::launchVoiceCommand,
                            onSendPrompt = { q -> viewModel.sendAiPrompt(q, switchToAiView = false) },
                            onBack = { viewModel.stepBackFromHalfDialCenter() }
                        )
                    }
                }
            }

            BottomHudBar(
                currentMode = state.mode,
                isFullscreen = state.preferences.fullscreenImmersive,
                isSettingsActive = state.mode == StarkMode.HALF_DIAL && state.activeModule == HubModule.SETTINGS,
                onFoldRotate = viewModel::cycleFoldMode,
                onSelectMode = viewModel::setMode,
                onOpenSettings = { viewModel.openModule(HubModule.SETTINGS) },
                onLockDeck = { viewModel.engageLockCover() },
                onToggleFullscreen = {
                    viewModel.setFullscreenImmersive(!state.preferences.fullscreenImmersive)
                }
            )
        }

        // 3. Floating STX AI Corner Bot + Translucent Command Overlay (when unlocked)
        if (!state.telemetry.touchLocked && state.mode != StarkMode.STX_AI) {
            FloatingStxAiOverlay(
                state = state,
                voiceListening = voiceState.isListening,
                voicePartial = voiceState.partialTranscript,
                systemFloatingEnabled = state.preferences.systemFloatingAiEnabled,
                canDrawOverlays = canDrawOverlays,
                onToggleSystemFloating = {
                    setSystemFloatingAi(!(state.preferences.systemFloatingAiEnabled && canDrawOverlays))
                },
                onToggleOverlay = viewModel::toggleStxOverlay,
                onDismissOverlay = viewModel::closeStxOverlay,
                onExpandFullStx = { viewModel.openModule(HubModule.STX_AI) },
                onVoiceCommand = ::launchVoiceCommand,
                onSendCommand = { cmd ->
                    viewModel.sendAiPrompt(cmd, switchToAiView = false, openOverlay = true)
                }
            )
        }

        // 4. Full-Screen Arc Reactor Lockscreen Cover (10s awake auto-sleep)
        if (state.telemetry.touchLocked) {
            LockscreenCoverOverlay(
                state = state,
                onAttemptPasscode = { code ->
                    val ok = viewModel.unlockWithPasscode(code)
                    if (ok) dismissNonSecureOsKeyguard()
                    ok
                },
                onBiometricAuthenticated = {
                    viewModel.unlockSuccess()
                    dismissNonSecureOsKeyguard()
                },
                onSwipeUnlock = {
                    viewModel.unlockSuccess()
                    dismissNonSecureOsKeyguard()
                },
                onClearError = viewModel::clearLockError,
                onAskStxAi = viewModel::askStxAiOnLockscreen,
                onDimChange = onSetWindowDimmed,
                onIdleSleepTimeout = viewModel::onLockscreenIdleSleep
            )
        }

        // 5. Transient Status Toast Banner
        AnimatedVisibility(
            visible = state.statusBanner != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 42.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xEE06121E))
                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.statusBanner.orEmpty(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    color = HudCyan
                )
            }
        }

        // 6. Full-Screen All-4-Edges Flashlight Perimeter Glow
        EdgeFlashOverlay(
            active = state.telemetry.flashlightOn,
            crimsonMode = state.preferences.edgeGlowCrimson
        )
    }
}
