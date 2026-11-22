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
import android.os.SystemClock
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hardware.LauncherPermissionManager
import com.example.hardware.MiuiFreeformLauncher
import com.example.hardware.VoiceCommandManager
import com.example.model.HubModule
import com.example.model.SettingsSection
import com.example.model.StarkMode
import com.example.security.configureAppCheck
import com.example.service.StxFloatingAiService
import com.example.ui.StarkUiState
import com.example.ui.StarkViewModel
import com.example.ui.screens.AllAppsScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.BootScreen
import com.example.ui.screens.BriefScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.LandscapeHomeScreen
import com.example.ui.screens.LockscreenScreen
import com.example.ui.screens.PortraitHomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StxAiScreen
import com.example.ui.screens.StxFloatingBubbleOverlay
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.WifiScreen
import com.example.ui.theme.DisplayFontFamily
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
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianVoid
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
        MiuiFreeformLauncher.setHostActivity(this)
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
        MiuiFreeformLauncher.setHostActivity(this)
        viewModel.setForegroundActive(true)
        StxFloatingAiService.setAppInForeground(true)
        resumeTick.value += 1
    }

    override fun onPause() {
        viewModel.setForegroundActive(false)
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
        MiuiFreeformLauncher.setHostActivity(null)
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> true
            Configuration.ORIENTATION_PORTRAIT -> false
            else -> newConfig.screenWidthDp > newConfig.screenHeightDp && newConfig.screenWidthDp >= 520
        }
        viewModel.onConfigurationChanged(isLandscape = isLandscape)
    }

    private fun setLockDimmed(dimmed: Boolean) {
        runCatching {
            val attrs = window.attributes
            attrs.screenBrightness = if (dimmed) 0.015f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = attrs
        }
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

    var startupPermDismissed by remember { mutableStateOf(false) }
    var autoPermTriggered by remember { mutableStateOf(false) }
    var permCheckTick by remember { mutableIntStateOf(0) }
    var permLaunchStartedAtMs by remember { mutableLongStateOf(0L) }
    var permLaunchUserInitiated by remember { mutableStateOf(false) }
    var permDialogNote by remember { mutableStateOf<String?>(null) }

    val permissionStatuses = remember(resumeCounter, permCheckTick) {
        LauncherPermissionManager.getPermissionStatuses(context)
    }
    val missingRuntimePerms = remember(resumeCounter, permCheckTick) {
        LauncherPermissionManager.missingRuntimePermissions(context)
    }

    val startupPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val elapsedMs = SystemClock.elapsedRealtime() - permLaunchStartedAtMs
        val wasUserTap = permLaunchUserInitiated
        permLaunchUserInitiated = false
        viewModel.markPermissionsOnboarded()
        viewModel.onPermissionsUpdated()
        permCheckTick++
        val missingAfter = LauncherPermissionManager.missingRuntimePermissions(context)
        val anyGranted = results.values.any { it }
        if (missingAfter.isEmpty()) {
            permDialogNote = null
            startupPermDismissed = true
            viewModel.showStatusBanner("ALL LAUNCHER PERMISSIONS ARMED")
        } else if (anyGranted) {
            permDialogNote = "${missingAfter.size} permissions still needed — tap GRANT ALL or APP SETTINGS."
            viewModel.showStatusBanner("${missingAfter.size} PERMS PENDING")
        } else if (wasUserTap && elapsedMs < 500L) {
            permDialogNote = "Android blocked the popup (Don't ask again / MIUI). Opening App Permissions…"
            viewModel.showStatusBanner("OPENING ANDROID APP PERMISSIONS")
            LauncherPermissionManager.openAppPermissionSettings(context)
        } else {
            permDialogNote = "Tap GRANT ALL PERMISSIONS or open APP SETTINGS to enable sensors."
            viewModel.showStatusBanner("GRANT PERMISSIONS TO ENABLE ALL SENSORS")
        }
    }

    fun launchRuntimePermissions(perms: Array<String>, userInitiated: Boolean) {
        if (perms.isEmpty()) {
            viewModel.markPermissionsOnboarded()
            return
        }
        permLaunchUserInitiated = userInitiated
        permLaunchStartedAtMs = SystemClock.elapsedRealtime()
        runCatching {
            startupPermLauncher.launch(perms)
        }.onFailure {
            permDialogNote = "Unable to show OS popup — opening App Permissions."
            LauncherPermissionManager.openAppPermissionSettings(context)
        }
    }

    LaunchedEffect(
        state.preferencesLoaded,
        state.preferences.firstBootCompleted,
        state.preferences.permissionsOnboarded,
        state.telemetry.touchLocked
    ) {
        if (state.preferencesLoaded &&
            state.preferences.firstBootCompleted &&
            !state.preferences.permissionsOnboarded &&
            !state.telemetry.touchLocked &&
            !autoPermTriggered
        ) {
            autoPermTriggered = true
            val missing = LauncherPermissionManager.missingRuntimePermissions(context)
            if (missing.isNotEmpty()) {
                delay(750L)
                launchRuntimePermissions(missing, userInitiated = false)
            } else {
                viewModel.markPermissionsOnboarded()
            }
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
                overlayPermLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }.onFailure {
                pendingOverlayEnable = false
                viewModel.showStatusBanner("OPEN SETTINGS > DISPLAY OVER OTHER APPS")
            }
        }
    }

    fun dismissNonSecureOsKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
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
            viewModel.sendAiPrompt(prompt = clean, switchToAiView = false, openOverlay = !stayInFullAi)
        }
    }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (spoken.isNotBlank()) dispatchVoiceTranscript(spoken)
    }

    val voiceManager = remember(context) {
        VoiceCommandManager(
            context = context,
            onFinalTranscript = { spoken -> dispatchVoiceTranscript(spoken) },
            onStatusMessage = { msg -> viewModel.showStatusBanner(msg) },
            onFallbackIntentNeeded = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
            if (!state.telemetry.touchLocked && state.mode != StarkMode.STX_AI) viewModel.openStxOverlay()
            voiceManager.startListening()
        } else {
            viewModel.showStatusBanner("MIC PERMISSION DENIED")
        }
    }

    fun launchVoiceCommand() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
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

    val shouldHandleBack = state.allAppsDrawerOpen ||
        state.stxOverlayOpen ||
        state.mode == StarkMode.HALF_DIAL ||
        state.mode == StarkMode.STX_AI
    BackHandler(enabled = shouldHandleBack) {
        viewModel.stepBackFromHalfDialCenter()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF07111E), ObsidianVoid)
                )
            )
    ) {
        val windowIsLandscape = maxWidth > maxHeight && maxWidth >= 520.dp
        LaunchedEffect(windowIsLandscape) {
            viewModel.onConfigurationChanged(windowIsLandscape)
        }

        val showBootSplash = state.preferencesLoaded && !state.preferences.firstBootCompleted
        val showStartupPerms = state.preferencesLoaded &&
            state.preferences.firstBootCompleted &&
            !state.telemetry.touchLocked &&
            !startupPermDismissed &&
            (missingRuntimePerms.isNotEmpty() || !canDrawOverlays) &&
            !(state.mode == StarkMode.HALF_DIAL && state.activeModule == HubModule.SETTINGS)
        val pauseHomeAnimations = state.allAppsDrawerOpen || state.telemetry.touchLocked || showStartupPerms

        if (state.preferencesLoaded && !showBootSplash) {
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
                val effectiveHomeMode = when {
                    !state.preferences.autoMorphOrientation -> state.mode
                    state.mode == StarkMode.HUB && !windowIsLandscape -> StarkMode.PORTRAIT
                    state.mode == StarkMode.PORTRAIT && windowIsLandscape -> StarkMode.HUB
                    else -> state.mode
                }

                if (effectiveHomeMode == StarkMode.HALF_DIAL || effectiveHomeMode == StarkMode.STX_AI) {
                    TopHudBar(
                        dayProgressFraction = state.clock.dayProgressFraction,
                        timeText = state.clock.timeText,
                        batteryPercent = state.telemetry.batteryPercent,
                        isCharging = state.telemetry.isCharging,
                        showBackToHub = true,
                        onBackToHub = { viewModel.stepBackFromHalfDialCenter() }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = effectiveHomeMode,
                        transitionSpec = { fadeIn(tween(150)).togetherWith(fadeOut(tween(110))) },
                        label = "stx_screen_router"
                    ) { targetMode ->
                        when (targetMode) {
                            StarkMode.PORTRAIT -> PortraitHomeScreen(
                                state = state,
                                hubModules = viewModel.hubModules,
                                voiceListening = voiceState.isListening,
                                voicePartial = voiceState.partialTranscript,
                                onSelectHubIndex = viewModel::selectHubIndex,
                                onOpenModule = viewModel::openModule,
                                onLaunchAppFullscreen = viewModel::launchAppWithLeftDialMode,
                                onAskStxAi = { q ->
                                    viewModel.sendAiPrompt(q, switchToAiView = false, openOverlay = true)
                                },
                                onVoiceCommand = ::launchVoiceCommand,
                                onToggleWifi = viewModel::openWifiPanel,
                                onToggleBluetooth = viewModel::openBluetoothPanel,
                                onToggleFlash = viewModel::toggleFlashlight,
                                onToggleLock = viewModel::toggleTouchLock,
                                onSync = viewModel::syncHardwareAndApps,
                                onToggleMute = viewModel::toggleMute,
                                onFoldRotate = viewModel::cycleFoldMode,
                                onSelectMode = viewModel::setMode,
                                onLockDeck = { viewModel.engageLockCover() },
                                onToggleFullscreen = {
                                    viewModel.setFullscreenImmersive(!state.preferences.fullscreenImmersive)
                                },
                                onOpenOrbStudio = { viewModel.openSettingsSection(SettingsSection.ORB_STUDIO) },
                                onOpenPermissions = { viewModel.openSettingsSection(SettingsSection.PERMISSIONS) },
                                onOpenAllAppsDrawer = viewModel::openAllAppsDrawer,
                                onSelectProtocol = viewModel::selectProtocol,
                                onStartTimer = viewModel::startVaultTimer,
                                onStopTimer = viewModel::stopVaultTimer,
                                pauseBackgroundAnimations = pauseHomeAnimations
                            )

                            StarkMode.HUB -> LandscapeHomeScreen(
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
                                onVoiceCommand = ::launchVoiceCommand,
                                onAskStxAi = { q ->
                                    viewModel.sendAiPrompt(q, switchToAiView = false, openOverlay = true)
                                },
                                onFoldRotate = viewModel::cycleFoldMode,
                                onSelectMode = viewModel::setMode,
                                onLockDeck = { viewModel.engageLockCover() },
                                onToggleFullscreen = {
                                    viewModel.setFullscreenImmersive(!state.preferences.fullscreenImmersive)
                                },
                                onOpenOrbStudio = { viewModel.openSettingsSection(SettingsSection.ORB_STUDIO) },
                                onLaunchAppFullscreen = viewModel::launchAppWithLeftDialMode,
                                onOpenPermissions = { viewModel.openSettingsSection(SettingsSection.PERMISSIONS) },
                                onOpenAllAppsDrawer = viewModel::openAllAppsDrawer,
                                onSelectProtocol = viewModel::selectProtocol,
                                onStartTimer = viewModel::startVaultTimer,
                                onStopTimer = viewModel::stopVaultTimer,
                                pauseBackgroundAnimations = pauseHomeAnimations
                            )

                            StarkMode.HALF_DIAL -> when (state.activeModule) {
                                HubModule.APPS -> AppsScreen(
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
                                    onOpenAppInfo = viewModel::openAppInfo
                                )

                                HubModule.BRIEF -> BriefScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 3) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onOpenApps = { viewModel.openModule(HubModule.APPS) },
                                    onOpenSettings = { viewModel.openModule(HubModule.SETTINGS) },
                                    onOpenStxAi = { viewModel.openModule(HubModule.STX_AI) },
                                    onLaunchApp = viewModel::launchAppWithLeftDialMode
                                )

                                HubModule.HEALTH -> HealthScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 4) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onPermissionGranted = viewModel::onPermissionsUpdated
                                )

                                HubModule.WIFI -> WifiScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 5) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onOpenWifiPanel = viewModel::openWifiPanel,
                                    onOpenBluetoothPanel = viewModel::openBluetoothPanel,
                                    onPermissionsUpdated = viewModel::onPermissionsUpdated
                                )

                                HubModule.CAMERA -> CameraScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 4) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onToggleTorch = viewModel::toggleFlashlight,
                                    onFlipLens = viewModel::toggleCameraLens,
                                    onZoomChange = viewModel::setCameraZoom,
                                    onPermissionGranted = viewModel::onPermissionsUpdated
                                )

                                HubModule.VAULT -> VaultScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 3) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onAddNote = viewModel::addVaultNote,
                                    onDeleteNote = viewModel::deleteVaultNote,
                                    onStartTimer = viewModel::startVaultTimer,
                                    onStopTimer = viewModel::stopVaultTimer
                                )

                                HubModule.SEARCH -> SearchScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectIndex = { idx -> viewModel.selectSubIndex(idx, 3) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onLaunchFullscreen = viewModel::launchAppWithLeftDialMode,
                                    onWebSearch = viewModel::openWebSearch,
                                    onAskStxAi = { q ->
                                        viewModel.sendAiPrompt(q, switchToAiView = false, openOverlay = true)
                                    }
                                )

                                HubModule.SETTINGS -> SettingsScreen(
                                    state = state,
                                    focusedIndex = state.subItemIndex,
                                    onSelectSection = { idx -> viewModel.selectSubIndex(idx, SettingsSection.entries.size) },
                                    onBack = { viewModel.stepBackFromHalfDialCenter() },
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
                                    onSetLockCoverEnabled = viewModel::setLockCoverEnabled,
                                    onSetAutoLockOnScreenOff = viewModel::setAutoLockOnScreenOff,
                                    onSetBiometricUnlockEnabled = viewModel::setBiometricUnlockEnabled,
                                    onSetUseOsPhysicalLock = viewModel::setUseOsPhysicalLock,
                                    onSavePasscode = viewModel::savePasscode,
                                    onClearPasscode = viewModel::clearPasscode,
                                    onSetLockWidgets = viewModel::setLockWidgetVisibility,
                                    onSetLockHeadline = viewModel::setLockHeadline,
                                    onEngageLockNow = { viewModel.engageLockCover() },
                                    onToggleOrbUseHtml = viewModel::setOrbUseHtmlWebGl,
                                    onOrbRotationSpeedChange = viewModel::setOrbRotationSpeed,
                                    onOrbBloomStrengthChange = viewModel::setOrbBloomStrength,
                                    onOrbScaleChange = viewModel::setOrbScale,
                                    onToggleOrbAutoRotate = viewModel::setOrbAutoRotate,
                                    onToggleOrbShowTextNodes = viewModel::setOrbShowTextNodes,
                                    onToggleOrbShowDebris = viewModel::setOrbShowDebris,
                                    onToggleOrbSpeechGlowReactive = viewModel::setOrbSpeechGlowReactive,
                                    onResetOrbDefaults = viewModel::resetOrbDefaults,
                                    onOpenStxAiCore = { viewModel.openModule(HubModule.STX_AI) },
                                    onReplayFirstBoot = viewModel::replayFirstBoot
                                )

                                HubModule.STX_AI -> StxAiScreen(
                                    state = state,
                                    voiceListening = voiceState.isListening,
                                    voicePartial = voiceState.partialTranscript,
                                    voiceRms = voiceState.rmsNormalized,
                                    onVoiceCommand = ::launchVoiceCommand,
                                    onSendPrompt = { q -> viewModel.sendAiPrompt(q, switchToAiView = false) },
                                    onWarmUpSpeech = viewModel::warmUpSpeech,
                                    onToggleTts = viewModel::toggleTtsEnabled,
                                    onAvatarTap = viewModel::onHologramAvatarTap,
                                    onToggleOrbMode = viewModel::setOrbUseHtmlWebGl,
                                    onBack = { viewModel.stepBackFromHalfDialCenter() }
                                )
                            }

                            StarkMode.STX_AI -> StxAiScreen(
                                state = state,
                                voiceListening = voiceState.isListening,
                                voicePartial = voiceState.partialTranscript,
                                voiceRms = voiceState.rmsNormalized,
                                onVoiceCommand = ::launchVoiceCommand,
                                onSendPrompt = { q -> viewModel.sendAiPrompt(q, switchToAiView = false) },
                                onWarmUpSpeech = viewModel::warmUpSpeech,
                                onToggleTts = viewModel::toggleTtsEnabled,
                                onAvatarTap = viewModel::onHologramAvatarTap,
                                onToggleOrbMode = viewModel::setOrbUseHtmlWebGl,
                                onBack = { viewModel.stepBackFromHalfDialCenter() }
                            )
                        }
                    }
                }
            }

            if (!state.telemetry.touchLocked && state.mode != StarkMode.STX_AI && !state.allAppsDrawerOpen) {
                StxFloatingBubbleOverlay(
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

            AnimatedVisibility(
                visible = state.allAppsDrawerOpen && !state.telemetry.touchLocked,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(animationSpec = tween(140)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    targetOffsetY = { fullHeight -> fullHeight }
                ) + fadeOut(animationSpec = tween(120))
            ) {
                AllAppsScreen(
                    installedApps = state.installedApps,
                    topRecentApps = state.topRecentApps,
                    appsLoading = state.appsLoading,
                    onLaunchApp = { app ->
                        viewModel.closeAllAppsDrawer()
                        viewModel.launchAppWithLeftDialMode(app)
                    },
                    onLaunchWindowed = { app ->
                        viewModel.closeAllAppsDrawer()
                        viewModel.launchAppWindowed(app)
                    },
                    onLaunchFullscreen = { app ->
                        viewModel.closeAllAppsDrawer()
                        viewModel.launchAppFullscreen(app)
                    },
                    onTogglePin = viewModel::togglePinApp,
                    onOpenAppInfo = viewModel::openAppInfo,
                    onClose = viewModel::closeAllAppsDrawer
                )
            }

            AnimatedVisibility(
                visible = state.telemetry.touchLocked,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180))
            ) {
                LockscreenScreen(
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

            if (showStartupPerms) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC010409))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(16.dp)
                        .testTag("startup_permissions_dialog"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xF5040911))
                            .border(1.2.dp, HudCrimson, RoundedCornerShape(8.dp))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, "Launcher Permissions", tint = HudCyan, modifier = Modifier.size(20.dp))
                            Column {
                                Text("STX OS // LAUNCHER PERMISSIONS", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
                                Text("ARM HARDWARE ACCESS SO NO FEATURE CRASHES", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudAmber)
                            }
                        }

                        permDialogNote?.let { note ->
                            Text(note, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudAmber)
                        }

                        permissionStatuses.forEach { perm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x99060E1A))
                                    .border(0.8.dp, if (perm.granted) HudEmerald.copy(alpha = 0.45f) else HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .then(
                                        if (!perm.granted) Modifier.clickable { launchRuntimePermissions(perm.permissions, userInitiated = true) }
                                        else Modifier
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(perm.title, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                    Text(perm.description, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = HudSilver)
                                }
                                Text(if (perm.granted) "ARMED" else "GRANT >", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (perm.granted) HudEmerald else HudCrimson)
                            }
                        }

                        if (missingRuntimePerms.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable { launchRuntimePermissions(missingRuntimePerms, userInitiated = true) }
                                    .testTag("startup_grant_all_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("GRANT ALL PERMISSIONS (${missingRuntimePerms.size})", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!canDrawOverlays) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCrimsonSubtle)
                                        .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                                        .clickable { setSystemFloatingAi(true) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("OVERLAY", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudIce)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x9906121E))
                                    .border(1.dp, HudAmber.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .clickable { LauncherPermissionManager.openAppPermissionSettings(context) }
                                    .padding(horizontal = 6.dp)
                                    .testTag("startup_app_settings_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("APP SETTINGS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudAmber)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x9906121E))
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                    .clickable {
                                        startupPermDismissed = true
                                        viewModel.openSettingsSection(SettingsSection.PERMISSIONS)
                                    }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("PERMS PANEL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudCyan)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x88080D14))
                                    .border(1.dp, HudSlate.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                    .clickable {
                                        startupPermDismissed = true
                                        viewModel.markPermissionsOnboarded()
                                    }
                                    .padding(horizontal = 6.dp)
                                    .testTag("startup_skip_perms_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("LATER", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudSilver)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showBootSplash,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(220))
        ) {
            BootScreen(
                preferences = state.preferences,
                onBootComplete = { viewModel.markFirstBootCompleted() }
            )
        }

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

        if (state.telemetry.flashlightOn) {
            val edgeColor = if (state.preferences.edgeGlowCrimson) HudCrimson else HudCyan
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 4f || size.height <= 4f) return@Canvas
                drawRect(
                    color = edgeColor.copy(alpha = 0.85f),
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun TopHudBar(
    dayProgressFraction: Float,
    timeText: String,
    batteryPercent: Int,
    isCharging: Boolean,
    showBackToHub: Boolean,
    onBackToHub: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showBackToHub) {
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HudCrimsonSubtle)
                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBackToHub)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("top_bar_back_to_hub"),
                contentAlignment = Alignment.Center
            ) {
                Text("< HUB", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
            }
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            if (size.width <= 4f || size.height <= 4f) return@Canvas
            val barY = size.height * 0.55f
            val frac = dayProgressFraction.coerceIn(0f, 1f)
            val fillX = size.width * frac
            drawLine(HudDivider, Offset(0f, barY), Offset(size.width, barY), 1.5.dp.toPx())
            drawLine(HudCrimson, Offset(0f, barY), Offset(fillX, barY), 2.dp.toPx())
            drawCircle(HudIce, radius = 3.dp.toPx(), center = Offset(fillX, barY))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(timeText, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = HudIce)
            Text(
                text = "$batteryPercent%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (isCharging) HudEmerald else HudCyan
            )
            if (isCharging) {
                Icon(Icons.Default.Bolt, "Charging", tint = HudEmerald, modifier = Modifier.size(13.dp))
            }
        }
    }
}
