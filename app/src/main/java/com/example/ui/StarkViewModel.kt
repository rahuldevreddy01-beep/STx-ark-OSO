package com.example.ui

import android.app.Application
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.FirebaseAiStxRepository
import com.example.data.LauncherAppsRepository
import com.example.data.LauncherPreferencesRepository
import com.example.data.StxAction
import com.example.hardware.DeviceTelemetryManager
import com.example.hardware.HudSoundEngine
import com.example.hardware.StxSpeechEngine
import com.example.model.AiMessage
import com.example.model.AppLaunchMode
import com.example.model.DeviceTelemetry
import com.example.model.DialWedgeItem
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.LauncherPreferences
import com.example.model.SettingsSection
import com.example.model.StarkMode
import com.example.security.PasscodeCrypto
import com.example.security.StarkDeviceAdminReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StarkClockSnapshot(
    val timeText: String = "00:00",
    val secondsText: String = "00",
    val dateText: String = "MON 01 JAN",
    val greeting: String = "GOOD AFTERNOON",
    val dayProgressFraction: Float = 0.5f
)

data class StarkUiState(
    val mode: StarkMode = StarkMode.HUB,
    val isDeviceLandscape: Boolean = true,
    val activeModule: HubModule = HubModule.APPS,
    val hubSelectedIndex: Int = 1,
    val subItemIndex: Int = 0,
    val leftDialLaunchMode: AppLaunchMode = AppLaunchMode.FLOATING,
    val stxOverlayOpen: Boolean = false,
    val allAppsDrawerOpen: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val topRecentApps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = true,
    val searchQuery: String = "",
    val clock: StarkClockSnapshot = StarkClockSnapshot(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val cameraFacingBack: Boolean = true,
    val cameraZoom: Float = 1f,
    val vaultTimerSeconds: Int = 0,
    val vaultTimerRunning: Boolean = false,
    val aiMessages: List<AiMessage> = listOf(
        AiMessage(
            id = "init_stx",
            isUser = false,
            text = "STX AI online. Voice & text commands control hardware, apps, modes, and Gemini 3.8 Flash.",
            timestamp = "READY"
        )
    ),
    val aiThinking: Boolean = false,
    val ttsEnabled: Boolean = true,
    val aiSpeaking: Boolean = false,
    val aiSpeechPulseTick: Int = 0,
    val statusBanner: String? = null,
    val lockScreenError: String? = null,
    val lockAiReply: String? = null,
    val preferencesLoaded: Boolean = false,
    val protocolIndex: Int = 2
)

class StarkViewModel(application: Application) : AndroidViewModel(application) {

    private val appsRepo = LauncherAppsRepository(application)
    private val prefsRepo = LauncherPreferencesRepository(application)
    private val telemetryManager = DeviceTelemetryManager(application)
    private val aiRepo: AiRepository by lazy { FirebaseAiStxRepository() }
    private val soundEngine = HudSoundEngine()
    private val speechEngine = StxSpeechEngine(application)

    private val _rawApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _uiState = MutableStateFlow(StarkUiState())
    val uiState: StateFlow<StarkUiState> = _uiState.asStateFlow()

    val hubModules: List<HubModule> = listOf(
        HubModule.BRIEF,
        HubModule.APPS,
        HubModule.HEALTH,
        HubModule.VAULT,
        HubModule.CAMERA,
        HubModule.SEARCH,
        HubModule.WIFI,
        HubModule.SETTINGS,
        HubModule.STX_AI
    )

    @Volatile
    private var isForegroundActive = true

    private val preferencesFlow: StateFlow<LauncherPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherPreferences())

    init {
        val initialLandscape = application.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        _uiState.update {
            it.copy(
                isDeviceLandscape = initialLandscape,
                mode = if (initialLandscape) StarkMode.HUB else StarkMode.PORTRAIT
            )
        }

        telemetryManager.startMonitoring()

        // Clock & live system snapshot ticker (1s) — Binder/Disk IPCs run on Dispatchers.IO
        viewModelScope.launch {
            while (isActive) {
                if (isForegroundActive) {
                    telemetryManager.refreshSystemSnapshotAsync()
                    _uiState.update { it.copy(clock = buildClockSnapshot()) }
                }
                delay(1000L)
            }
        }

        // Countdown timer for Vault
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _uiState.value
                if (current.vaultTimerRunning && current.vaultTimerSeconds > 0) {
                    val next = current.vaultTimerSeconds - 1
                    _uiState.update {
                        it.copy(
                            vaultTimerSeconds = next,
                            vaultTimerRunning = next > 0,
                            statusBanner = if (next == 0) "VAULT TIMER COMPLETE" else it.statusBanner
                        )
                    }
                    if (next == 0) {
                        soundEngine.playSelectConfirm(
                            current.preferences.sfxEnabled,
                            current.telemetry.audioMuted
                        )
                    }
                }
            }
        }

        // Collect high-frequency telemetry updates without re-mapping the entire installedApps list
        viewModelScope.launch {
            telemetryManager.telemetry.collect { telemetry ->
                _uiState.update { state ->
                    if (state.telemetry == telemetry) state else state.copy(telemetry = telemetry)
                }
            }
        }

        // Collect real-time TTS speaking & word-boundary pulse for hologram lip-sync
        viewModelScope.launch {
            speechEngine.speechState.collect { speech ->
                _uiState.update { state ->
                    if (state.aiSpeaking == speech.isSpeaking &&
                        state.aiSpeechPulseTick == speech.pulseTick
                    ) {
                        state
                    } else {
                        state.copy(
                            aiSpeaking = speech.isSpeaking,
                            aiSpeechPulseTick = speech.pulseTick
                        )
                    }
                }
            }
        }

        // Only enrich/re-sort installedApps & topRecentApps when preferences or package list change
        viewModelScope.launch {
            combine(preferencesFlow, _rawApps) { prefs, rawApps ->
                val enrichedApps = rawApps.map { app ->
                    app.copy(
                        isPinnedToDock = app.packageName in prefs.pinnedPackages,
                        alertsEnabled = app.packageName !in prefs.mutedAlertPackages,
                        bgSyncEnabled = app.packageName !in prefs.disabledSyncPackages,
                        launchCount = prefs.appLaunchCounts[app.packageName] ?: 0,
                        lastUsedMs = prefs.appLastUsedMs[app.packageName] ?: 0L
                    )
                }
                val topRecent = computeTopRecentApps(enrichedApps)
                Triple(prefs, enrichedApps, topRecent)
            }.collect { (prefs, enrichedApps, topRecent) ->
                _uiState.update { state ->
                    state.copy(
                        preferences = prefs,
                        installedApps = enrichedApps,
                        topRecentApps = topRecent,
                        preferencesLoaded = true
                    )
                }
            }
        }

        // Engage STX Lock Cover on launch if enabled
        viewModelScope.launch {
            runCatching {
                val initialPrefs = prefsRepo.preferencesFlow.first()
                if (initialPrefs.lockCoverEnabled) {
                    telemetryManager.setTouchLocked(true)
                }
            }
        }

        reloadInstalledApps()
    }

    fun reloadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(appsLoading = true) }
            val prefs = preferencesFlow.value
            val loaded = runCatching {
                appsRepo.loadInstalledApps(
                    pinnedPackages = prefs.pinnedPackages,
                    mutedAlertPackages = prefs.mutedAlertPackages,
                    disabledSyncPackages = prefs.disabledSyncPackages
                )
            }.getOrDefault(emptyList())
            _rawApps.value = loaded
            _uiState.update { it.copy(appsLoading = false) }
        }
    }

    fun setForegroundActive(active: Boolean) {
        isForegroundActive = active
        syncSensorMode()
        if (active) {
            viewModelScope.launch {
                telemetryManager.refreshSystemSnapshotAsync()
                _uiState.update { it.copy(clock = buildClockSnapshot()) }
            }
        }
    }

    private fun syncSensorMode() {
        val st = _uiState.value
        val needsHighRate = st.mode == StarkMode.HALF_DIAL && st.activeModule == HubModule.HEALTH
        telemetryManager.setSensorActivityState(
            foreground = isForegroundActive,
            highRateNeeded = needsHighRate
        )
    }

    fun onConfigurationChanged(isLandscape: Boolean) {
        val prev = _uiState.value
        val autoMorph = prev.preferences.autoMorphOrientation
        val nextMode = if (!autoMorph) {
            prev.mode
        } else when {
            isLandscape && prev.mode == StarkMode.PORTRAIT -> StarkMode.HUB
            !isLandscape && prev.mode == StarkMode.HUB -> StarkMode.PORTRAIT
            else -> prev.mode
        }
        if (prev.isDeviceLandscape == isLandscape && prev.mode == nextMode) return
        _uiState.update { state ->
            state.copy(
                isDeviceLandscape = isLandscape,
                mode = nextMode
            )
        }
    }

    fun onPermissionsUpdated() {
        telemetryManager.refreshCameraCapabilities()
        telemetryManager.registerSensors()
        viewModelScope.launch { telemetryManager.refreshSystemSnapshotAsync() }
    }

    fun markPermissionsOnboarded() {
        viewModelScope.launch { prefsRepo.setPermissionsOnboarded(true) }
    }

    fun markFirstBootCompleted() {
        viewModelScope.launch { prefsRepo.setFirstBootCompleted(true) }
    }

    fun replayFirstBoot() {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setFirstBootCompleted(false) }
    }

    fun setOrbUseHtmlWebGl(useHtml: Boolean) {
        playSelectCue()
        viewModelScope.launch {
            prefsRepo.setOrbUseHtmlWebGl(useHtml)
            _uiState.update {
                it.copy(statusBanner = if (useHtml) "STX CORE: 3D WIREFRAME ORB" else "STX CORE: HOLO GIRL")
            }
        }
    }

    fun setOrbRotationSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setOrbRotationSpeed(speed) }
    }

    fun setOrbBloomStrength(bloom: Float) {
        viewModelScope.launch { prefsRepo.setOrbBloomStrength(bloom) }
    }

    fun setOrbScale(scale: Float) {
        viewModelScope.launch { prefsRepo.setOrbScale(scale) }
    }

    fun setOrbAutoRotate(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setOrbAutoRotate(enabled) }
    }

    fun setOrbShowTextNodes(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setOrbShowTextNodes(enabled) }
    }

    fun setOrbShowDebris(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setOrbShowDebris(enabled) }
    }

    fun setOrbSpeechGlowReactive(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setOrbSpeechGlowReactive(enabled) }
    }

    fun resetOrbDefaults() {
        playSelectCue()
        viewModelScope.launch {
            prefsRepo.resetOrbDefaults()
            _uiState.update { it.copy(statusBanner = "3D ORB STUDIO RESET TO DEFAULTS") }
        }
    }

    fun setMode(mode: StarkMode) {
        playSelectCue()
        _uiState.update {
            it.copy(
                mode = mode,
                activeModule = if (mode == StarkMode.HALF_DIAL && it.activeModule == HubModule.STX_AI) {
                    HubModule.APPS
                } else {
                    it.activeModule
                },
                stxOverlayOpen = false,
                allAppsDrawerOpen = false
            )
        }
        syncSensorMode()
    }

    fun cycleFoldMode() {
        playSelectCue()
        val current = _uiState.value.mode
        val next = when (current) {
            StarkMode.PORTRAIT -> StarkMode.HUB
            StarkMode.HUB -> StarkMode.HALF_DIAL
            StarkMode.HALF_DIAL -> StarkMode.STX_AI
            StarkMode.STX_AI -> StarkMode.PORTRAIT
        }
        _uiState.update { it.copy(mode = next, stxOverlayOpen = false, allAppsDrawerOpen = false) }
        syncSensorMode()
    }

    fun openAllAppsDrawer() {
        playSelectCue()
        _uiState.update { it.copy(allAppsDrawerOpen = true, stxOverlayOpen = false) }
    }

    fun closeAllAppsDrawer() {
        playBackCue()
        _uiState.update { it.copy(allAppsDrawerOpen = false) }
    }

    fun toggleStxOverlay() {
        playSelectCue()
        _uiState.update { it.copy(stxOverlayOpen = !it.stxOverlayOpen) }
    }

    fun openStxOverlay() {
        playSelectCue()
        _uiState.update { it.copy(stxOverlayOpen = true) }
    }

    fun closeStxOverlay() {
        playBackCue()
        _uiState.update { it.copy(stxOverlayOpen = false) }
    }

    fun selectProtocol(index: Int) {
        val protocols = listOf("SLEEP", "STEALTH", "LIFE", "LAB", "FOCUS")
        val clamped = ((index % protocols.size) + protocols.size) % protocols.size
        if (clamped != _uiState.value.protocolIndex) {
            playDetentCue()
            _uiState.update {
                it.copy(
                    protocolIndex = clamped,
                    statusBanner = "PROTOCOL ${protocols[clamped]} ENGAGED"
                )
            }
        }
    }

    fun selectHubIndex(index: Int) {
        val clamped = ((index % hubModules.size) + hubModules.size) % hubModules.size
        if (clamped != _uiState.value.hubSelectedIndex) {
            playDetentCue()
            _uiState.update { it.copy(hubSelectedIndex = clamped) }
        }
    }

    fun openModule(module: HubModule) {
        playSelectCue()
        val idx = hubModules.indexOf(module).coerceAtLeast(0)
        if (module == HubModule.STX_AI) {
            _uiState.update {
                it.copy(
                    hubSelectedIndex = idx,
                    activeModule = module,
                    mode = StarkMode.STX_AI,
                    stxOverlayOpen = false,
                    allAppsDrawerOpen = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    hubSelectedIndex = idx,
                    activeModule = module,
                    subItemIndex = 0,
                    mode = StarkMode.HALF_DIAL,
                    allAppsDrawerOpen = false
                )
            }
        }
        syncSensorMode()
    }

    fun openSettingsSection(section: SettingsSection) {
        playSelectCue()
        val idx = hubModules.indexOf(HubModule.SETTINGS).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                hubSelectedIndex = idx,
                activeModule = HubModule.SETTINGS,
                subItemIndex = section.ordinal,
                mode = StarkMode.HALF_DIAL,
                stxOverlayOpen = false,
                allAppsDrawerOpen = false
            )
        }
        syncSensorMode()
    }

    fun selectSubIndex(index: Int, count: Int) {
        if (count <= 0) return
        val clamped = ((index % count) + count) % count
        val state = _uiState.value
        if (clamped != state.subItemIndex) {
            playDetentCue()
            _uiState.update { it.copy(subItemIndex = clamped) }
        }
    }

    fun stepBackFromHalfDialCenter(): Boolean {
        val state = _uiState.value
        if (state.allAppsDrawerOpen) {
            closeAllAppsDrawer()
            return true
        }
        if (state.stxOverlayOpen) {
            closeStxOverlay()
            return true
        }
        if (state.mode == StarkMode.HALF_DIAL || state.mode == StarkMode.STX_AI) {
            playBackCue()
            _uiState.update {
                it.copy(
                    mode = if (it.isDeviceLandscape) StarkMode.HUB else StarkMode.PORTRAIT
                )
            }
            syncSensorMode()
            return true
        }
        return false
    }

    fun setLeftDialLaunchMode(launchMode: AppLaunchMode) {
        playSelectCue()
        _uiState.update {
            it.copy(
                leftDialLaunchMode = launchMode,
                statusBanner = if (launchMode == AppLaunchMode.FLOATING) {
                    "LEFT DIAL: FLOATING WINDOW MODE"
                } else {
                    "LEFT DIAL: FULLSCREEN MODE"
                }
            )
        }
    }

    fun launchAppWithLeftDialMode(app: InstalledApp) {
        when (_uiState.value.leftDialLaunchMode) {
            AppLaunchMode.FLOATING -> launchAppWindowed(app)
            AppLaunchMode.FULLSCREEN -> launchAppFullscreen(app)
        }
    }

    fun focusApp(app: InstalledApp) {
        val idx = _uiState.value.installedApps.indexOfFirst {
            it.packageName == app.packageName && it.activityName == app.activityName
        }
        if (idx >= 0) {
            selectSubIndex(idx, _uiState.value.installedApps.size)
        }
    }

    fun launchAppFullscreen(app: InstalledApp) {
        playSelectCue()
        val ok = appsRepo.launchFullscreen(app)
        if (ok) {
            viewModelScope.launch { prefsRepo.recordAppLaunch(app.packageName) }
        }
        _uiState.update {
            it.copy(
                statusBanner = if (ok) "OPENED ${app.label.uppercase()}" else "UNABLE TO LAUNCH ${app.label.uppercase()}"
            )
        }
    }

    fun launchAppWindowed(app: InstalledApp) {
        playSelectCue()
        viewModelScope.launch {
            val ok = appsRepo.launchWindowedAsync(app)
            if (ok) {
                prefsRepo.recordAppLaunch(app.packageName)
            }
            val telemetry = com.example.hardware.MiuiFreeformLauncher.getBridgeTelemetry(getApplication())
            _uiState.update {
                it.copy(
                    statusBanner = when {
                        !ok -> "UNABLE TO LAUNCH ${app.label.uppercase()}"
                        telemetry.isPrivilegedBridgeReady -> "FLOAT MODE 5 · ${app.label.uppercase()}"
                        telemetry.isGlobalFreeformReady -> "FLOATING · ${app.label.uppercase()}"
                        else -> "MIUI BLOCKED FLOAT · ARM SHIZUKU OR DEV FREEFORM IN SETTINGS"
                    }
                )
            }
        }
    }

    fun openAppInfo(packageName: String) {
        playSelectCue()
        appsRepo.openAppSystemInfo(packageName)
    }

    fun openDefaultLauncherSettings() {
        playSelectCue()
        appsRepo.openDefaultLauncherSettings()
    }

    fun openWebSearch(query: String) {
        playSelectCue()
        appsRepo.openWebSearch(query)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFlashlight() {
        val on = telemetryManager.toggleFlashlight()
        playSelectCue()
        _uiState.update {
            it.copy(statusBanner = if (on) "OPTICAL TORCH & 4-EDGE GLOW ACTIVE" else "TORCH DISENGAGED")
        }
    }

    fun openWifiPanel() {
        playSelectCue()
        telemetryManager.toggleWifiPanel()
    }

    fun openBluetoothPanel() {
        playSelectCue()
        telemetryManager.toggleBluetoothPanel()
    }

    fun toggleMute() {
        val muted = telemetryManager.toggleMute()
        if (muted) {
            speechEngine.stop()
        }
        _uiState.update {
            it.copy(statusBanner = if (muted) "AUDIO MUTED" else "AUDIO ONLINE")
        }
    }

    fun warmUpSpeech() {
        if (_uiState.value.ttsEnabled) {
            speechEngine.warmUp()
        }
    }

    fun toggleTtsEnabled() {
        val next = !_uiState.value.ttsEnabled
        playSelectCue()
        if (!next) {
            speechEngine.stop()
        } else {
            speechEngine.warmUp()
        }
        _uiState.update {
            it.copy(
                ttsEnabled = next,
                statusBanner = if (next) "STX VOICE OUTPUT ONLINE" else "STX VOICE OUTPUT MUTED"
            )
        }
    }

    fun onHologramAvatarTap() {
        val snap = _uiState.value
        playSelectCue()
        if (snap.aiSpeaking) {
            speechEngine.stop()
            return
        }
        if (snap.ttsEnabled && !snap.telemetry.audioMuted) {
            val greet = "STX Core online, ${snap.preferences.operatorCallsign}. " +
                "Power at ${snap.telemetry.batteryPercent} percent. How can I assist?"
            speechEngine.speak(greet)
        }
    }

    fun toggleTouchLock() {
        val next = !_uiState.value.telemetry.touchLocked
        if (next) {
            engageLockCover()
        } else {
            unlockSuccess()
        }
    }

    fun engageLockCover(allowPhysicalScreenOff: Boolean = _uiState.value.preferences.useOsPhysicalLock) {
        telemetryManager.setTouchLocked(true)
        val osLocked = if (allowPhysicalScreenOff) {
            StarkDeviceAdminReceiver.tryLockScreenNow(getApplication())
        } else {
            false
        }
        playSelectCue()
        _uiState.update {
            it.copy(
                stxOverlayOpen = false,
                allAppsDrawerOpen = false,
                lockScreenError = null,
                lockAiReply = null,
                statusBanner = if (osLocked) {
                    "STX OS SCREEN LOCKED"
                } else {
                    "STX LOCK COVER ARMED"
                }
            )
        }
    }

    fun onLockscreenIdleSleep() {
        if (!_uiState.value.telemetry.touchLocked) return
        val adminLocked = StarkDeviceAdminReceiver.tryLockScreenNow(getApplication())
        if (adminLocked) {
            _uiState.update { it.copy(statusBanner = "LOCKSCREEN AUTO-SLEEP") }
        }
    }

    fun showStatusBanner(message: String) {
        _uiState.update { it.copy(statusBanner = message) }
    }

    fun onScreenOffEvent() {
        val prefs = _uiState.value.preferences
        if (prefs.lockCoverEnabled && prefs.autoLockOnScreenOff) {
            // Screen is already turning off; arm STX cover cleanly without recursive lockNow
            telemetryManager.setTouchLocked(true)
            _uiState.update {
                it.copy(
                    stxOverlayOpen = false,
                    allAppsDrawerOpen = false,
                    lockScreenError = null,
                    lockAiReply = null
                )
            }
        }
    }

    fun unlockWithPasscode(candidate: String): Boolean {
        val prefs = _uiState.value.preferences
        if (!prefs.hasPasscode) {
            unlockSuccess()
            return true
        }
        val valid = PasscodeCrypto.verifyPasscode(
            candidate = candidate,
            saltHex = prefs.passcodeSalt,
            expectedHashHex = prefs.passcodeHash
        )
        return if (valid) {
            unlockSuccess()
            true
        } else {
            playBackCue()
            _uiState.update { it.copy(lockScreenError = "INVALID PASSCODE") }
            false
        }
    }

    fun unlockSuccess() {
        playSelectCue()
        telemetryManager.setTouchLocked(false)
        _uiState.update {
            it.copy(
                lockScreenError = null,
                lockAiReply = null,
                statusBanner = "STX OS UNLOCKED · WELCOME BACK"
            )
        }
    }

    fun clearLockError() {
        _uiState.update { it.copy(lockScreenError = null) }
    }

    fun askStxAiOnLockscreen(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        playSelectCue()
        viewModelScope.launch {
            val snapshot = _uiState.value
            val replyText = runCatching {
                aiRepo.query(clean, snapshot.telemetry, snapshot.installedApps).text
            }.getOrElse {
                "STX AI temporarily offline. Try again when network is active."
            }
            _uiState.update { it.copy(lockAiReply = replyText) }
        }
    }

    fun setLockCoverEnabled(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setLockCoverEnabled(enabled) }
    }

    fun setAutoLockOnScreenOff(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setAutoLockOnScreenOff(enabled) }
    }

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setBiometricUnlockEnabled(enabled) }
    }

    fun setUseOsPhysicalLock(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setUseOsPhysicalLock(enabled) }
    }

    fun savePasscode(passcode: String, isAlphanumeric: Boolean) {
        if (passcode.length < 4) {
            _uiState.update { it.copy(statusBanner = "PASSCODE MUST BE AT LEAST 4 CHARS") }
            return
        }
        playSelectCue()
        viewModelScope.launch {
            prefsRepo.setPasscode(passcode, isAlphanumeric)
            _uiState.update { it.copy(statusBanner = "PASSCODE & STX LOCK COVER ARMED") }
        }
    }

    fun clearPasscode() {
        playBackCue()
        viewModelScope.launch {
            prefsRepo.clearPasscode()
            _uiState.update { it.copy(statusBanner = "PASSCODE CLEARED (SWIPE MODE)") }
        }
    }

    fun setLockWidgetVisibility(
        showSteps: Boolean? = null,
        showChargingRing: Boolean? = null,
        showStxAi: Boolean? = null
    ) {
        playSelectCue()
        viewModelScope.launch {
            prefsRepo.setLockWidgetVisibility(showSteps, showChargingRing, showStxAi)
        }
    }

    fun setLockHeadline(headline: String) {
        playSelectCue()
        viewModelScope.launch {
            prefsRepo.setLockHeadline(headline)
            _uiState.update { it.copy(statusBanner = "LOCKSCREEN BANNER UPDATED") }
        }
    }

    fun syncHardwareAndApps() {
        playSelectCue()
        viewModelScope.launch { telemetryManager.refreshSystemSnapshotAsync() }
        reloadInstalledApps()
        _uiState.update { it.copy(statusBanner = "TELEMETRY & APP MATRIX SYNCED") }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun toggleCameraLens() {
        playSelectCue()
        _uiState.update { it.copy(cameraFacingBack = !it.cameraFacingBack) }
    }

    fun setCameraZoom(zoom: Float) {
        _uiState.update { it.copy(cameraZoom = zoom.coerceIn(1f, 5f)) }
    }

    fun setOperatorCallsign(callsign: String) {
        viewModelScope.launch {
            prefsRepo.setOperatorCallsign(callsign)
            _uiState.update { it.copy(statusBanner = "CALLSIGN UPDATED") }
        }
    }

    fun togglePinApp(packageName: String) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.togglePinnedPackage(packageName) }
    }

    fun setFullscreenImmersive(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setFullscreenImmersive(enabled) }
    }

    fun setSfxEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSfxEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setKeepScreenOn(enabled) }
    }

    fun setAutoMorphOrientation(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setAutoMorphOrientation(enabled) }
    }

    fun setEdgeGlowCrimson(crimson: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setEdgeGlowCrimson(crimson) }
    }

    fun setSystemFloatingAiEnabled(enabled: Boolean) {
        playSelectCue()
        viewModelScope.launch { prefsRepo.setSystemFloatingAiEnabled(enabled) }
    }

    fun addVaultNote(title: String, body: String) {
        if (body.isBlank() && title.isBlank()) return
        playSelectCue()
        viewModelScope.launch { prefsRepo.addVaultNote(title, body) }
    }

    fun deleteVaultNote(id: String) {
        playBackCue()
        viewModelScope.launch { prefsRepo.deleteVaultNote(id) }
    }

    fun startVaultTimer(seconds: Int) {
        playSelectCue()
        _uiState.update { it.copy(vaultTimerSeconds = seconds, vaultTimerRunning = true) }
    }

    fun stopVaultTimer() {
        playBackCue()
        _uiState.update { it.copy(vaultTimerRunning = false, vaultTimerSeconds = 0) }
    }

    fun sendAiPrompt(
        prompt: String,
        switchToAiView: Boolean = false,
        openOverlay: Boolean = false
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        playSelectCue()

        val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = clean,
            timestamp = stamp
        )
        _uiState.update {
            it.copy(
                mode = if (switchToAiView) StarkMode.STX_AI else it.mode,
                stxOverlayOpen = if (openOverlay) true else it.stxOverlayOpen,
                aiMessages = it.aiMessages + userMsg,
                aiThinking = true
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = _uiState.value
                val reply = aiRepo.query(clean, snapshot.telemetry, snapshot.installedApps)
                val replyStamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val actionTag = when (val act = reply.action) {
                    StxAction.ToggleFlashlight -> {
                        toggleFlashlight()
                        "TORCH TOGGLED"
                    }
                    StxAction.OpenWifi -> {
                        openWifiPanel()
                        "WIFI PANEL"
                    }
                    StxAction.OpenBluetooth -> {
                        openBluetoothPanel()
                        "BLUETOOTH"
                    }
                    StxAction.ToggleMute -> {
                        toggleMute()
                        "AUDIO MUTE"
                    }
                    StxAction.ToggleFullscreen -> {
                        setFullscreenImmersive(!snapshot.preferences.fullscreenImmersive)
                        "FULLSCREEN"
                    }
                    StxAction.LockDeck -> {
                        engageLockCover()
                        "LOCK ARMED"
                    }
                    StxAction.SyncHardware -> {
                        syncHardwareAndApps()
                        "SYNCED"
                    }
                    is StxAction.SwitchMode -> {
                        setMode(act.mode)
                        "MODE · ${act.mode.name}"
                    }
                    is StxAction.OpenModule -> {
                        openModule(act.module)
                        "MODULE · ${act.module.title}"
                    }
                    is StxAction.StartTimer -> {
                        startVaultTimer(act.seconds)
                        openModule(HubModule.VAULT)
                        "TIMER · ${act.seconds}S"
                    }
                    is StxAction.LaunchApp -> {
                        if (act.windowed) launchAppWindowed(act.app) else launchAppFullscreen(act.app)
                        "${if (act.windowed) "WINDOW" else "LAUNCH"} · ${act.app.label.uppercase()}"
                    }
                    null -> null
                }

                val aiMsg = AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = reply.text,
                    timestamp = replyStamp,
                    isError = reply.isError,
                    executedAction = actionTag
                )
                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + aiMsg,
                        aiThinking = false
                    )
                }
                val after = _uiState.value
                if (after.ttsEnabled && !after.telemetry.audioMuted) {
                    speechEngine.speak(reply.text)
                }
            } catch (_: Throwable) {
                val errStamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + AiMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "STX AI link encountered a transient error. Ready for next command.",
                            timestamp = errStamp,
                            isError = true
                        ),
                        aiThinking = false
                    )
                }
            }
        }
    }

    fun currentHalfDialItems(): List<DialWedgeItem> {
        val state = _uiState.value
        return when (state.activeModule) {
            HubModule.APPS -> {
                val quickApps = state.topRecentApps
                if (quickApps.isEmpty()) {
                    listOf(DialWedgeItem("empty", "00", "SCANNING", "RECENT"))
                } else {
                    quickApps.mapIndexed { idx, app ->
                        val badge = if (app.launchCount > 0) "${app.launchCount}X" else "%02d".format(idx + 1)
                        DialWedgeItem(
                            id = "${app.packageName}/${app.activityName}",
                            badge = badge,
                            label = app.label.uppercase().take(10),
                            subLabel = if (app.lastUsedMs > 0L) "RECENT" else app.categoryTag,
                            isAccentCrimson = app.isPinnedToDock || app.launchCount > 0
                        )
                    }
                }
            }
            HubModule.HEALTH -> listOf(
                DialWedgeItem("steps", "01", "STEPS", "${state.telemetry.stepsCount}"),
                DialWedgeItem("gyro", "02", "GYROSCOPE", "3-AXIS"),
                DialWedgeItem("compass", "03", "COMPASS", "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°"),
                DialWedgeItem("power", "04", "POWER", "${state.telemetry.batteryPercent}%", isAccentCrimson = true)
            )
            HubModule.WIFI -> listOf(
                DialWedgeItem("link", "01", "LINK", state.telemetry.networkTransport),
                DialWedgeItem("rf", "02", "SIGNAL", "${state.telemetry.wifiSignalPercent}%"),
                DialWedgeItem("ip", "03", "IP / DNS", state.telemetry.ipv4Address),
                DialWedgeItem("bt", "04", "BLUETOOTH", state.telemetry.bluetoothStateLabel),
                DialWedgeItem("panel", "05", "RF PANEL", "OS WIFI", isAccentCrimson = true)
            )
            HubModule.CAMERA -> listOf(
                DialWedgeItem("optical", "01", "OPTICAL", "LIVE CAM"),
                DialWedgeItem("torch", "02", "TORCH", if (state.telemetry.flashlightOn) "ACTIVE" else "OFF", isAccentCrimson = state.telemetry.flashlightOn),
                DialWedgeItem("lens", "03", "FLIP LENS", if (state.cameraFacingBack) "REAR" else "FRONT"),
                DialWedgeItem("zoom", "04", "ZOOM", "${"%.1f".format(state.cameraZoom)}X")
            )
            HubModule.VAULT -> listOf(
                DialWedgeItem("notes", "01", "NOTES", "${state.preferences.vaultNotes.size} SAVED"),
                DialWedgeItem("timer", "02", "TIMER", if (state.vaultTimerRunning) "${state.vaultTimerSeconds}S" else "STANDBY", isAccentCrimson = state.vaultTimerRunning),
                DialWedgeItem("compose", "03", "NEW NOTE", "SAVE LOCAL")
            )
            HubModule.SEARCH -> listOf(
                DialWedgeItem("apps_q", "01", "APP QUERY", "${state.installedApps.size} APPS"),
                DialWedgeItem("web_q", "02", "WEB LINK", "BROWSER"),
                DialWedgeItem("stx_q", "03", "ASK STX AI", "GEMINI", isAccentCrimson = true)
            )
            HubModule.BRIEF -> listOf(
                DialWedgeItem("overview", "01", "SUMMARY", "LIVE OS"),
                DialWedgeItem("hardware", "02", "TELEMETRY", "${state.telemetry.ramUsedPercent}% RAM"),
                DialWedgeItem("dock", "03", "PINNED", "${state.preferences.pinnedPackages.size} APPS")
            )
            HubModule.SETTINGS -> SettingsSection.entries.map { section ->
                DialWedgeItem(
                    id = section.name,
                    badge = section.code,
                    label = section.title,
                    subLabel = section.subtitle,
                    isAccentCrimson = section == SettingsSection.LAUNCHER
                )
            }
            HubModule.STX_AI -> listOf(
                DialWedgeItem("stx", "01", "STX AI", "GEMINI")
            )
        }
    }

    private fun buildClockSnapshot(): StarkClockSnapshot {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val greeting = when {
            hour < 12 -> "GOOD MORNING"
            hour < 17 -> "GOOD AFTERNOON"
            else -> "GOOD EVENING"
        }
        val fraction = ((hour * 60 + minute) / 1440f).coerceIn(0f, 1f)
        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val secondsText = SimpleDateFormat("ss", Locale.getDefault()).format(Date())
        val dateText = SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date()).uppercase()
        return StarkClockSnapshot(
            timeText = timeText,
            secondsText = secondsText,
            dateText = dateText,
            greeting = greeting,
            dayProgressFraction = fraction
        )
    }

    private fun playDetentCue() {
        val s = _uiState.value
        soundEngine.playDetentTick(s.preferences.sfxEnabled, s.telemetry.audioMuted)
    }

    private fun playSelectCue() {
        val s = _uiState.value
        soundEngine.playSelectConfirm(s.preferences.sfxEnabled, s.telemetry.audioMuted)
    }

    private fun playBackCue() {
        val s = _uiState.value
        soundEngine.playBackCue(s.preferences.sfxEnabled, s.telemetry.audioMuted)
    }

    private fun computeTopRecentApps(apps: List<InstalledApp>): List<InstalledApp> {
        if (apps.isEmpty()) return emptyList()
        val distinctByPkg = apps.distinctBy { it.packageName }
        val commonPkgTokens = setOf("camera", "chrome", "youtube", "whatsapp", "dialer", "messages", "maps", "settings", "gallery", "music")
        val sorted = distinctByPkg.sortedWith(
            compareByDescending<InstalledApp> { it.lastUsedMs }
                .thenByDescending { it.launchCount }
                .thenByDescending { it.isPinnedToDock }
                .thenBy { it.isSystemApp }
                .thenByDescending { app ->
                    val lower = app.packageName.lowercase()
                    commonPkgTokens.any { lower.contains(it) }
                }
                .thenBy { it.label.lowercase() }
        )
        return sorted.take(6)
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.destroy()
        telemetryManager.stopMonitoring()
    }
}
