package com.example.hardware

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.DeviceTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DeviceTelemetryManager(private val context: Context) : SensorEventListener {

    private val _telemetry = MutableStateFlow(
        DeviceTelemetry(
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            androidVersion = "ANDROID ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    )
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private var torchCameraId: String? = null
    private var lastSensorUiPushMs = 0L
    private val gyroHistoryBuffer = ArrayDeque<Float>(40)
    private var receiverRegistered = false

    private val rotationMatrix = FloatArray(9)
    private var stepBaseline: Float? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent != null) {
                parseBatteryIntent(intent)
            }
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                _telemetry.update { it.copy(flashlightOn = enabled) }
            }
        }
    }

    init {
        detectTorchCamera()
        refreshSystemSnapshot()
    }

    fun startMonitoring() {
        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(batteryReceiver, filter)
            }
            if (sticky != null) parseBatteryIntent(sticky)
            receiverRegistered = true
        }

        registerSensors()
        runCatching { cameraManager?.registerTorchCallback(torchCallback, null) }
        refreshSystemSnapshot()
    }

    fun stopMonitoring() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(batteryReceiver) }
            receiverRegistered = false
        }
        runCatching { sensorManager?.unregisterListener(this) }
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
    }

    fun registerSensors() {
        val sm = sensorManager ?: return
        runCatching { sm.unregisterListener(this) }

        val stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val rotSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)

        stepSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        accelSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        rotSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        lightSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

        _telemetry.update {
            it.copy(
                stepsSensorAvailable = stepSensor != null,
                lightSensorAvailable = lightSensor != null
            )
        }
    }

    fun refreshSystemSnapshot() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val ramPercent = ((usedMb * 100f) / totalMb).roundToInt().coerceIn(0, 100)

        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes ?: 1L
        val freeBytes = statFs?.availableBytes ?: 0L
        val totalGb = totalBytes / (1024f * 1024f * 1024f)
        val freeGb = freeBytes / (1024f * 1024f * 1024f)
        val storageUsedPct = (((totalBytes - freeBytes) * 100f) / totalBytes.coerceAtLeast(1L))
            .roundToInt().coerceIn(0, 100)

        val uptimeSec = SystemClock.elapsedRealtime() / 1000L
        val hours = uptimeSec / 3600L
        val mins = (uptimeSec % 3600L) / 60L
        val secs = uptimeSec % 60L
        val uptimeStr = "%02d:%02d:%02d".format(hours, mins, secs)

        val netSnapshot = readNetworkSnapshot()
        val btSnapshot = readBluetoothSnapshot()

        val ringerMuted = audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
            audioManager?.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        _telemetry.update {
            it.copy(
                ramUsedPercent = ramPercent,
                ramUsedMb = usedMb,
                ramTotalMb = totalMb,
                storageFreeGb = freeGb,
                storageTotalGb = totalGb,
                storageUsedPercent = storageUsedPct,
                uptimeFormatted = uptimeStr,
                uptimeSeconds = uptimeSec,
                wifiEnabled = netSnapshot.wifiEnabled,
                wifiConnected = netSnapshot.wifiConnected,
                wifiSsid = netSnapshot.ssid,
                wifiRssiDbm = netSnapshot.rssiDbm,
                wifiSignalPercent = netSnapshot.signalPercent,
                wifiLinkSpeedMbps = netSnapshot.linkSpeedMbps,
                wifiFrequencyMhz = netSnapshot.freqMhz,
                wifiBandLabel = netSnapshot.bandLabel,
                ipv4Address = netSnapshot.ipv4,
                dnsServer = netSnapshot.dns,
                networkTransport = netSnapshot.transport,
                bluetoothEnabled = btSnapshot.first,
                bluetoothPairedCount = btSnapshot.second,
                bluetoothStateLabel = btSnapshot.third,
                audioMuted = ringerMuted
            )
        }
    }

    fun toggleFlashlight(): Boolean {
        val id = torchCameraId
        val cm = cameraManager
        val nextState = !_telemetry.value.flashlightOn
        if (id != null && cm != null) {
            runCatching {
                cm.setTorchMode(id, nextState)
                _telemetry.update { it.copy(flashlightOn = nextState) }
                return nextState
            }
        }
        // Even on devices/emulators without physical camera flash hardware, update edge-flash state
        _telemetry.update { it.copy(flashlightOn = nextState) }
        return nextState
    }

    fun toggleWifiPanel() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.isWifiEnabled = !(wifiManager?.isWifiEnabled ?: false)
                refreshSystemSnapshot()
            }
        }.onFailure {
            openWifiSettings()
        }
    }

    fun openWifiSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun toggleBluetoothPanel() {
        runCatching {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun toggleMute(): Boolean {
        val nextMuted = !_telemetry.value.audioMuted
        runCatching {
            audioManager?.let { am ->
                if (nextMuted) {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                } else {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                }
            }
        }
        _telemetry.update { it.copy(audioMuted = nextMuted) }
        return nextMuted
    }

    fun setTouchLocked(locked: Boolean) {
        _telemetry.update { it.copy(touchLocked = locked) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val raw = event.values[0]
                if (stepBaseline == null) stepBaseline = raw
                val delta = (raw - (stepBaseline ?: raw)).roundToInt().coerceAtLeast(0)
                // Show device hardware counter plus session delta or direct hardware value if > 0
                val totalDisplay = raw.roundToInt().coerceAtLeast(delta)
                _telemetry.update {
                    it.copy(stepsCount = totalDisplay, stepsSensorAvailable = true)
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val mag = sqrt(gx * gx + gy * gy + gz * gz)
                synchronized(gyroHistoryBuffer) {
                    if (gyroHistoryBuffer.size >= 36) gyroHistoryBuffer.removeFirst()
                    gyroHistoryBuffer.addLast(mag)
                }
                pushThrottledSensorUpdate(gx = gx, gy = gy, gz = gz)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                pushThrottledSensorUpdate(
                    ax = event.values[0],
                    ay = event.values[1],
                    az = event.values[2]
                )
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthRad = orientation[0]
                val pitchRad = orientation[1]
                val rollRad = orientation[2]
                val deg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).roundToInt()
                val cardinal = azimuthToCardinal(deg)
                val pitch = Math.toDegrees(pitchRad.toDouble()).roundToInt()
                val roll = Math.toDegrees(rollRad.toDouble()).roundToInt()
                pushThrottledSensorUpdate(
                    azimuth = deg,
                    cardinal = cardinal,
                    pitch = pitch,
                    roll = roll
                )
            }

            Sensor.TYPE_LIGHT -> {
                pushThrottledSensorUpdate(lux = event.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun pushThrottledSensorUpdate(
        gx: Float? = null,
        gy: Float? = null,
        gz: Float? = null,
        ax: Float? = null,
        ay: Float? = null,
        az: Float? = null,
        azimuth: Int? = null,
        cardinal: String? = null,
        pitch: Int? = null,
        roll: Int? = null,
        lux: Float? = null
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSensorUiPushMs < 90L && gx == null && azimuth == null) return
        if (now - lastSensorUiPushMs < 75L) return
        lastSensorUiPushMs = now

        val historySnapshot = if (gx != null) {
            synchronized(gyroHistoryBuffer) { gyroHistoryBuffer.toList() }
        } else {
            null
        }

        _telemetry.update { cur ->
            cur.copy(
                gyroX = gx ?: cur.gyroX,
                gyroY = gy ?: cur.gyroY,
                gyroZ = gz ?: cur.gyroZ,
                gyroHistory = historySnapshot ?: cur.gyroHistory,
                accelX = ax ?: cur.accelX,
                accelY = ay ?: cur.accelY,
                accelZ = az ?: cur.accelZ,
                azimuthDeg = azimuth ?: cur.azimuthDeg,
                cardinalDirection = cardinal ?: cur.cardinalDirection,
                pitchDeg = pitch ?: cur.pitchDeg,
                rollDeg = roll ?: cur.rollDeg,
                ambientLux = lux ?: cur.ambientLux
            )
        }
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else 0
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC FAST"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB LINK"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "INDUCTIVE"
            else -> if (isCharging) "CHARGING" else "DISCHARGE"
        }
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3900)
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val healthLabel = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "OPTIMAL"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "THERMAL HIGH"
            BatteryManager.BATTERY_HEALTH_DEAD -> "CRITICAL"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            else -> "NOMINAL"
        }
        _telemetry.update {
            it.copy(
                batteryPercent = pct,
                isCharging = isCharging,
                chargingSource = source,
                batteryTempC = tempTenths / 10f,
                batteryVoltageMv = voltageMv,
                batteryHealth = healthLabel
            )
        }
    }

    private fun detectTorchCamera() {
        val cm = cameraManager ?: return
        runCatching {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    _telemetry.update { it.copy(flashlightAvailable = true) }
                    return
                }
            }
        }
    }

    private data class NetworkSnapshot(
        val wifiEnabled: Boolean,
        val wifiConnected: Boolean,
        val ssid: String,
        val rssiDbm: Int,
        val signalPercent: Int,
        val linkSpeedMbps: Int,
        val freqMhz: Int,
        val bandLabel: String,
        val ipv4: String,
        val dns: String,
        val transport: String
    )

    private fun readNetworkSnapshot(): NetworkSnapshot {
        val wifiEnabled = runCatching { wifiManager?.isWifiEnabled ?: false }.getOrDefault(false)
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProps = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val hasVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        val transport = when {
            hasVpn && hasWifi -> "WIFI + VPN"
            hasWifi -> "WIFI LINK"
            hasCell -> "CELLULAR"
            else -> if (wifiEnabled) "STANDBY" else "OFFLINE"
        }

        val ipv4 = linkProps?.linkAddresses
            ?.firstOrNull { it.address is java.net.Inet4Address }
            ?.address?.hostAddress ?: "--"
        val dns = linkProps?.dnsServers
            ?.firstOrNull { it is java.net.Inet4Address }
            ?.hostAddress ?: "--"

        @Suppress("DEPRECATION")
        val wifiInfo = runCatching { wifiManager?.connectionInfo }.getOrNull()
        val rawSsid = wifiInfo?.ssid?.replace("\"", "").orEmpty()
        val ssid = when {
            !hasWifi -> if (hasCell) "MOBILE DATA" else "NO LINK"
            rawSsid.isBlank() || rawSsid == "<unknown ssid>" -> "WIFI CONNECTED"
            else -> rawSsid
        }
        val rssi = if (hasWifi && wifiInfo != null && wifiInfo.rssi > -120) wifiInfo.rssi else -100
        val signalPct = if (hasWifi) {
            ((rssi + 100) * 2).coerceIn(10, 100)
        } else if (hasCell) {
            78
        } else {
            0
        }
        val speed = if (hasWifi) (wifiInfo?.linkSpeed ?: 0).coerceAtLeast(0) else 0
        val freq = if (hasWifi) (wifiInfo?.frequency ?: 0) else 0
        val band = when {
            freq >= 5925 -> "6 GHz"
            freq >= 4900 -> "5 GHz"
            freq >= 2400 -> "2.4 GHz"
            else -> "--"
        }

        return NetworkSnapshot(
            wifiEnabled = wifiEnabled,
            wifiConnected = hasWifi,
            ssid = ssid,
            rssiDbm = rssi,
            signalPercent = signalPct,
            linkSpeedMbps = speed,
            freqMhz = freq,
            bandLabel = band,
            ipv4 = ipv4,
            dns = dns,
            transport = transport
        )
    }

    private fun readBluetoothSnapshot(): Triple<Boolean, Int, String> {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = bm?.adapter
        if (adapter == null) return Triple(false, 0, "NO HW")
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        if (!enabled) return Triple(false, 0, "OFF")
        val hasPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return Triple(true, 0, "ON (PERM)")
        val paired = runCatching { adapter.bondedDevices?.size ?: 0 }.getOrDefault(0)
        return Triple(true, paired, if (paired > 0) "ON · $paired PAIRED" else "ON · READY")
    }

    private fun azimuthToCardinal(deg: Int): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((deg + 22.5) / 45.0).toInt()) % 8
        return dirs[idx]
    }
}
