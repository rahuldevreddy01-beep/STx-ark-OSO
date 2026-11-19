package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FirebaseAiStxRepository
import com.example.data.LauncherAppsRepository
import com.example.data.LocalStxCommandRouter
import com.example.data.StxAction
import com.example.model.DeviceTelemetry
import com.example.model.HubModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * Persistent System-Wide Floating STX AI Overlay Service.
 * Floats cleanly above any external Android app via WindowManager and hides automatically
 * whenever STX OS MainActivity itself is in the foreground (where Compose overlay is shown).
 */
class StxFloatingAiService : Service() {

    private var windowManager: WindowManager? = null
    private var rootContainer: FrameLayout? = null
    private var orbView: FrameLayout? = null
    private var expandedCard: LinearLayout? = null
    private var replyLogView: TextView? = null
    private var replyScrollView: ScrollView? = null
    private var statusSubView: TextView? = null
    private var promptInput: EditText? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var torchState = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val aiRepo by lazy { FirebaseAiStxRepository() }
    private val appsRepo by lazy { LauncherAppsRepository(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = WeakReference(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        startForegroundSafely()
        createOverlayUi()
        syncVisibility()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        syncVisibility()
        return START_STICKY
    }

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        serviceScope.cancel()
        removeOverlaySafely()
        super.onDestroy()
    }

    private fun startForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STX AI Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the STX AI assistant accessible over any app"
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("STX AI · System Overlay Active")
            .setContentText("Floating assistant ready across all apps")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }
    }

    private fun createOverlayUi() {
        val wm = windowManager ?: return
        if (!Settings.canDrawOverlays(this)) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(180)
        }
        layoutParams = params

        val root = FrameLayout(this)
        rootContainer = root

        // 1. Collapsed Draggable STX Orb
        val orb = FrameLayout(this).apply {
            val sizePx = dp(52)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_BG_DARK)
                setStroke(dp(2), COLOR_CYAN)
            }
            val img = ImageView(this@StxFloatingAiService).apply {
                setImageResource(R.drawable.img_stx_hologram)
                scaleType = ImageView.ScaleType.CENTER_CROP
                val pad = dp(6)
                setPadding(pad, pad, pad, pad)
                clipToOutline = true
            }
            addView(
                img,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            val dot = View(this@StxFloatingAiService).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(COLOR_EMERALD)
                    setStroke(dp(1), Color.BLACK)
                }
            }
            addView(
                dot,
                FrameLayout.LayoutParams(dp(10), dp(10), Gravity.BOTTOM or Gravity.END).apply {
                    setMargins(0, 0, dp(3), dp(3))
                }
            )
        }
        attachOrbDragAndClick(orb)
        orbView = orb
        root.addView(orb)

        // 2. Expanded Tactical STX AI Floating Panel
        val card = buildExpandedCard()
        card.visibility = View.GONE
        expandedCard = card
        root.addView(card)

        runCatching {
            wm.addView(root, params)
        }
    }

    private fun buildExpandedCard(): LinearLayout {
        val cardWidth = dp(312)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                cardWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(COLOR_BG_CARD)
                setStroke(dp(1), COLOR_CYAN)
            }
        }

        // Header row (draggable bar)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTxt = TextView(this).apply {
            text = "STX AI // GLOBAL HUD"
            setTextColor(COLOR_ICE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val subTxt = TextView(this).apply {
            text = "FLOATING OVER ALL APPS"
            setTextColor(COLOR_CYAN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.MONOSPACE
            maxLines = 1
        }
        statusSubView = subTxt
        titleCol.addView(titleTxt)
        titleCol.addView(subTxt)
        header.addView(titleCol)

        val homeBtn = makePillButton("HOME", COLOR_CYAN_SUBTLE, COLOR_CYAN, COLOR_ICE) {
            openMainActivity()
        }
        val closeBtn = makePillButton("HIDE", COLOR_CRIMSON_SUBTLE, COLOR_CRIMSON, COLOR_CRIMSON) {
            setExpandedState(false)
        }
        header.addView(homeBtn)
        header.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(6), dp(1))
        })
        header.addView(closeBtn)
        container.addView(header)

        // Scrollable Reply Log
        val logText = TextView(this).apply {
            text = "STX AI ready across any app.\nAsk a question or tap a quick command below."
            setTextColor(COLOR_ICE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        replyLogView = logText

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(132)
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#CC06101B"))
                setStroke(dp(1), Color.parseColor("#4400F0FF"))
            }
            addView(logText)
        }
        replyScrollView = scroll
        container.addView(scroll)

        // Quick Action Chips Row
        val chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, dp(8))
        }
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf("FLASH", "BATTERY", "APPS", "WIFI", "BT", "HOME").forEach { chip ->
            val chipBtn = makePillButton(chip, COLOR_CYAN_SUBTLE, COLOR_CYAN, COLOR_ICE) {
                submitGlobalPrompt(chip)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
            }
            chipRow.addView(chipBtn, lp)
        }
        chipScroll.addView(chipRow)
        container.addView(chipScroll)

        // Input Row: EditText + MIC + SEND
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#FF040A12"))
                setStroke(dp(1), COLOR_CYAN)
            }
        }
        val edit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "Ask STX AI or command..."
            setHintTextColor(Color.parseColor("#FF6A889E"))
            setTextColor(COLOR_ICE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setBackgroundColor(Color.TRANSPARENT)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val q = text?.toString().orEmpty().trim()
                    if (q.isNotEmpty()) {
                        setText("")
                        submitGlobalPrompt(q)
                    }
                    true
                } else {
                    false
                }
            }
        }
        promptInput = edit
        inputRow.addView(edit)

        val micBtn = makePillButton("MIC", COLOR_CRIMSON_SUBTLE, COLOR_CRIMSON, COLOR_ICE) {
            openMainActivity(triggerVoice = true)
        }
        val sendBtn = makePillButton("GO", COLOR_CYAN_SUBTLE, COLOR_CYAN, COLOR_ICE) {
            val q = edit.text?.toString().orEmpty().trim()
            if (q.isNotEmpty()) {
                edit.setText("")
                submitGlobalPrompt(q)
            }
        }
        inputRow.addView(micBtn)
        inputRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), dp(1))
        })
        inputRow.addView(sendBtn)
        container.addView(inputRow)

        return container
    }

    private fun makePillButton(
        label: String,
        bgColor: Int,
        borderColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(9), dp(6), dp(9), dp(6))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(5).toFloat()
                setColor(bgColor)
                setStroke(dp(1), borderColor)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun attachOrbDragAndClick(orb: View) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        orb.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (touchX - event.rawX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) {
                        moved = true
                        lp.x = (initialX + dx).coerceAtLeast(0)
                        lp.y = (initialY + dy).coerceAtLeast(0)
                        runCatching {
                            windowManager?.updateViewLayout(rootContainer, lp)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        setExpandedState(true)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setExpandedState(expanded: Boolean) {
        isExpanded = expanded
        orbView?.visibility = if (expanded) View.GONE else View.VISIBLE
        expandedCard?.visibility = if (expanded) View.VISIBLE else View.GONE

        val lp = layoutParams
        val root = rootContainer
        if (lp != null && root != null) {
            lp.flags = if (expanded) {
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
            runCatching { windowManager?.updateViewLayout(root, lp) }
        }

        if (!expanded) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            promptInput?.windowToken?.let { token ->
                runCatching { imm?.hideSoftInputFromWindow(token, 0) }
            }
        }
    }

    private fun submitGlobalPrompt(prompt: String) {
        statusSubView?.text = "PROCESSING…"
        statusSubView?.setTextColor(COLOR_CRIMSON)
        appendLog("YOU: $prompt")

        serviceScope.launch {
            val snapshot = readSnapshotTelemetry()
            val apps = runCatching {
                appsRepo.loadInstalledApps(emptySet(), emptySet(), emptySet())
            }.getOrDefault(emptyList())

            val reply = LocalStxCommandRouter.tryMatchLocalCommand(prompt, snapshot, apps)
                ?: aiRepo.query(prompt, snapshot, apps)

            executeGlobalAction(reply.action)

            appendLog("STX: ${reply.text}")
            statusSubView?.text = "FLOATING OVER ALL APPS"
            statusSubView?.setTextColor(COLOR_CYAN)
        }
    }

    private fun executeGlobalAction(action: StxAction?) {
        when (action) {
            StxAction.ToggleFlashlight -> toggleSystemTorch()
            StxAction.OpenWifi -> runCatching {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                setExpandedState(false)
            }
            StxAction.OpenBluetooth -> runCatching {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                setExpandedState(false)
            }
            is StxAction.LaunchApp -> {
                if (action.windowed) appsRepo.launchWindowed(action.app)
                else appsRepo.launchFullscreen(action.app)
                setExpandedState(false)
            }
            is StxAction.OpenModule -> {
                openMainActivity(module = action.module)
            }
            is StxAction.SwitchMode -> {
                openMainActivity()
            }
            StxAction.LockDeck -> openMainActivity()
            else -> Unit
        }
    }

    private fun toggleSystemTorch() {
        runCatching {
            val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchState = !torchState
            cm.setTorchMode(camId, torchState)
        }
    }

    private fun openMainActivity(triggerVoice: Boolean = false, module: HubModule? = null) {
        setExpandedState(false)
        runCatching {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (triggerVoice) putExtra(EXTRA_TRIGGER_VOICE, true)
                if (module != null) putExtra(EXTRA_OPEN_MODULE, module.name)
            }
            startActivity(intent)
        }
    }

    private fun readSnapshotTelemetry(): DeviceTelemetry {
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        return DeviceTelemetry(
            batteryPercent = pct,
            chargingSource = if (bm?.isCharging == true) "CHARGING" else "BATTERY",
            isCharging = bm?.isCharging == true
        )
    }

    private fun appendLog(line: String) {
        val existing = replyLogView?.text?.toString().orEmpty()
        val combined = (existing.lines().takeLast(7) + line).joinToString("\n")
        replyLogView?.text = combined
        replyScrollView?.post { replyScrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun syncVisibility() {
        mainHandler.post {
            val shouldHide = appInForeground || screenLocked
            rootContainer?.visibility = if (shouldHide) View.GONE else View.VISIBLE
            if (shouldHide && isExpanded) {
                setExpandedState(false)
            }
        }
    }

    private fun removeOverlaySafely() {
        val root = rootContainer ?: return
        runCatching { windowManager?.removeViewImmediate(root) }
            .onFailure { runCatching { windowManager?.removeView(root) } }
        rootContainer = null
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val CHANNEL_ID = "stx_floating_ai"
        const val NOTIF_ID = 7071
        const val EXTRA_TRIGGER_VOICE = "extra_stx_trigger_voice"
        const val EXTRA_OPEN_MODULE = "extra_stx_open_module"

        private val COLOR_BG_DARK = Color.parseColor("#EE040C16")
        private val COLOR_BG_CARD = Color.parseColor("#F0040C16")
        private val COLOR_CYAN = Color.parseColor("#FF00F0FF")
        private val COLOR_CYAN_SUBTLE = Color.parseColor("#3300F0FF")
        private val COLOR_CRIMSON = Color.parseColor("#FFFF2A4B")
        private val COLOR_CRIMSON_SUBTLE = Color.parseColor("#33FF2A4B")
        private val COLOR_ICE = Color.parseColor("#FFF0FAFF")
        private val COLOR_EMERALD = Color.parseColor("#FF00E59B")

        @Volatile
        private var appInForeground: Boolean = false

        @Volatile
        private var screenLocked: Boolean = false

        private var activeInstance: WeakReference<StxFloatingAiService>? = null

        fun setAppInForeground(inForeground: Boolean) {
            appInForeground = inForeground
            activeInstance?.get()?.syncVisibility()
        }

        fun setScreenLocked(locked: Boolean) {
            screenLocked = locked
            activeInstance?.get()?.syncVisibility()
        }

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, StxFloatingAiService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, StxFloatingAiService::class.java))
            }
        }
    }
}
