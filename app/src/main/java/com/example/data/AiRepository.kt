package com.example.data

import android.util.Log
import com.example.model.DeviceTelemetry
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.StarkMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "StxAiRepository"

sealed interface StxAction {
    data object ToggleFlashlight : StxAction
    data object OpenWifi : StxAction
    data object OpenBluetooth : StxAction
    data object ToggleMute : StxAction
    data object ToggleFullscreen : StxAction
    data object LockDeck : StxAction
    data object SyncHardware : StxAction
    data class SwitchMode(val mode: StarkMode) : StxAction
    data class OpenModule(val module: HubModule) : StxAction
    data class StartTimer(val seconds: Int) : StxAction
    data class LaunchApp(val app: InstalledApp, val windowed: Boolean = false) : StxAction
}

data class AiReply(
    val text: String,
    val isError: Boolean = false,
    val action: StxAction? = null
)

interface AiRepository {
    suspend fun query(
        prompt: String,
        telemetry: DeviceTelemetry,
        installedApps: List<InstalledApp>
    ): AiReply
}

/**
 * Deterministic instant router for hardware toggles, mode switches, timers, and installed app launches.
 */
object LocalStxCommandRouter {

    fun tryMatchLocalCommand(
        prompt: String,
        telemetry: DeviceTelemetry,
        installedApps: List<InstalledApp>
    ): AiReply? {
        val q = prompt.trim().lowercase()
        if (q.isBlank()) return null

        // Windowed or fullscreen app launch by voice/text command
        val wantsWindow = q.startsWith("window ") || q.startsWith("float ")
        val appPrefix = when {
            q.startsWith("open ") -> q.removePrefix("open ").trim()
            q.startsWith("launch ") -> q.removePrefix("launch ").trim()
            q.startsWith("start ") -> q.removePrefix("start ").trim()
            q.startsWith("window ") -> q.removePrefix("window ").trim()
            q.startsWith("float ") -> q.removePrefix("float ").trim()
            else -> null
        }

        if ("flash" in q || "torch" in q || "flashlight" in q) {
            return AiReply(
                text = "Toggling optical torch and full-edge perimeter illumination.",
                action = StxAction.ToggleFlashlight
            )
        }
        if ("wifi" in q || "wi-fi" in q || "network" in q || "signal" in q) {
            return AiReply(
                text = "Network link: ${telemetry.networkTransport} (${telemetry.wifiSsid}, ${telemetry.wifiSignalPercent}%, IP ${telemetry.ipv4Address}). Opening RF panel.",
                action = StxAction.OpenModule(HubModule.WIFI)
            )
        }
        if ("bluetooth" in q || q == "bt") {
            return AiReply(
                text = "Bluetooth status: ${telemetry.bluetoothStateLabel}. Opening Bluetooth controls.",
                action = StxAction.OpenBluetooth
            )
        }
        if ("mute" in q || "silent" in q || "unmute" in q) {
            return AiReply(
                text = "Updating audio mute state.",
                action = StxAction.ToggleMute
            )
        }
        if ("fullscreen" in q || "immersive" in q) {
            return AiReply(
                text = "Toggling edge-to-edge immersive HUD mode.",
                action = StxAction.ToggleFullscreen
            )
        }
        if (q == "lock" || "lock screen" in q || "lock deck" in q) {
            return AiReply(
                text = "Engaging STX OS Lock Cover.",
                action = StxAction.LockDeck
            )
        }
        if ("sync" in q || "refresh apps" in q || "rescan" in q) {
            return AiReply(
                text = "Resyncing device sensors, RF telemetry, and installed app matrix.",
                action = StxAction.SyncHardware
            )
        }
        if ("portrait" in q && "mode" in q || q == "portrait") {
            return AiReply(
                text = "Switching HUD layout to Portrait Command Deck.",
                action = StxAction.SwitchMode(StarkMode.PORTRAIT)
            )
        }
        if ("hub" in q || "home" in q) {
            return AiReply(
                text = "Returning to STX Arc-Reactor Hub.",
                action = StxAction.SwitchMode(StarkMode.HUB)
            )
        }
        if ("camera" in q || "optical" in q) {
            return AiReply(
                text = "Engaging live CameraX optical HUD.",
                action = StxAction.OpenModule(HubModule.CAMERA)
            )
        }
        if ("setting" in q || "launcher" in q || "lockscreen setup" in q) {
            return AiReply(
                text = "Opening STX OS deep settings matrix.",
                action = StxAction.OpenModule(HubModule.SETTINGS)
            )
        }
        if ("vault" in q || "note" in q) {
            return AiReply(
                text = "Opening local Vault notes and timer.",
                action = StxAction.OpenModule(HubModule.VAULT)
            )
        }
        val timerMatch = Regex("""(?:timer|countdown)\s+(\d+)\s*(m|min|mins|minute|minutes|s|sec|secs|second|seconds)?""").find(q)
        if (timerMatch != null) {
            val num = timerMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = timerMatch.groupValues[2]
            val sec = if (unit.startsWith("s")) num else num * 60
            return AiReply(
                text = "Starting tactical timer for ${sec}s in Vault.",
                action = StxAction.StartTimer(sec)
            )
        }
        if (q == "timer") {
            return AiReply(
                text = "Opening Vault tactical timer.",
                action = StxAction.OpenModule(HubModule.VAULT)
            )
        }
        if ("apps" in q || "drawer" in q || "all apps" in q) {
            return AiReply(
                text = "${installedApps.size} installed Android apps ready. Opening dual-dial App Matrix.",
                action = StxAction.OpenModule(HubModule.APPS)
            )
        }
        if ("battery" in q || "power" in q || "charging" in q || q == "charge") {
            return AiReply(
                text = "Power cell at ${telemetry.batteryPercent}% (${telemetry.chargingSource}), ${telemetry.batteryTempC}°C, ${telemetry.batteryVoltageMv} mV (${telemetry.batteryHealth})."
            )
        }
        if ("step" in q || "health" in q || "gyro" in q || "compass" in q || "direction" in q) {
            return AiReply(
                text = "Live telemetry: ${telemetry.stepsCount} steps logged, heading ${telemetry.cardinalDirection} ${telemetry.azimuthDeg}°, gyro Z ${"%.2f".format(telemetry.gyroZ)} rad/s.",
                action = StxAction.OpenModule(HubModule.HEALTH)
            )
        }
        if ("brief" in q || "status" in q || "system brief" in q) {
            return AiReply(
                text = "System Brief: Battery ${telemetry.batteryPercent}% (${telemetry.chargingSource}) · RAM ${telemetry.ramUsedPercent}% (${telemetry.ramUsedMb}/${telemetry.ramTotalMb} MB) · Net ${telemetry.networkTransport} · Heading ${telemetry.cardinalDirection} ${telemetry.azimuthDeg}° · ${installedApps.size} apps indexed.",
                action = StxAction.OpenModule(HubModule.BRIEF)
            )
        }

        if (!appPrefix.isNullOrBlank()) {
            val match = installedApps.firstOrNull {
                it.label.lowercase() == appPrefix
            } ?: installedApps.firstOrNull {
                it.label.lowercase().contains(appPrefix) || it.packageName.lowercase().contains(appPrefix)
            }
            if (match != null) {
                return AiReply(
                    text = if (wantsWindow) {
                        "Launching ${match.label} in floating windowed mode."
                    } else {
                        "Launching ${match.label} directly."
                    },
                    action = StxAction.LaunchApp(match, windowed = wantsWindow)
                )
            }
        }

        return null
    }
}

/**
 * Primary Firebase AI Logic implementation backed by Gemini 3.8 Flash + local tactical command execution.
 */
class FirebaseAiStxRepository : AiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    override suspend fun query(
        prompt: String,
        telemetry: DeviceTelemetry,
        installedApps: List<InstalledApp>
    ): AiReply {
        val clean = prompt.trim()
        if (clean.isEmpty()) {
            return AiReply("STX AI standing by. Issue a voice or text command.")
        }

        // Execute direct hardware/app commands instantaneously before network call
        LocalStxCommandRouter.tryMatchLocalCommand(clean, telemetry, installedApps)?.let {
            return it
        }

        val appSample = installedApps.take(40).joinToString(", ") { "${it.label}(${it.packageName})" }
        val contextualPrompt = """
            You are STX AI, the concise tactical holographic launcher assistant inside STX OS on Android.
            Live device telemetry:
            - Battery: ${telemetry.batteryPercent}% (${telemetry.chargingSource}, charging=${telemetry.isCharging}, temp=${telemetry.batteryTempC}C)
            - RAM: ${telemetry.ramUsedMb}/${telemetry.ramTotalMb}MB (${telemetry.ramUsedPercent}%)
            - Network: ${telemetry.networkTransport}, SSID=${telemetry.wifiSsid}, Signal=${telemetry.wifiSignalPercent}%
            - Sensors: Steps=${telemetry.stepsCount}, Compass=${telemetry.cardinalDirection} ${telemetry.azimuthDeg}deg
            - FlashlightOn=${telemetry.flashlightOn}, Muted=${telemetry.audioMuted}
            - Sample installed apps: $appSample

            If the user wants to execute an in-app task, end your concise (1-3 sentence) reply with ONE command tag on a new line:
            [CMD:FLASH] | [CMD:WIFI] | [CMD:BT] | [CMD:MUTE] | [CMD:FULLSCREEN] | [CMD:LOCK] | [CMD:SYNC] | [CMD:APPS] | [CMD:SETTINGS] | [CMD:CAMERA] | [CMD:VAULT] | [CMD:HEALTH] | [CMD:BRIEF] | [CMD:OPEN:<packageName>] | [CMD:WIN:<packageName>]
            Otherwise just answer concisely and helpfully in plain text without markdown code blocks.

            User request: $clean
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(contextualPrompt)
            }
            val raw = response.text?.trim().orEmpty()
            if (raw.isEmpty()) {
                AiReply("STX AI received an empty response. Try again.", isError = true)
            } else {
                parseGeminiCommandTag(raw, installedApps)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            AiReply("STX AI request timed out. Check network connection and try again.", isError = true)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Firebase AI Logic inference failed", t)
            AiReply(
                text = "STX AI link error: ${t.localizedMessage ?: "Unable to reach Gemini backend."}",
                isError = true
            )
        }
    }

    private fun parseGeminiCommandTag(raw: String, installedApps: List<InstalledApp>): AiReply {
        val tagRegex = Regex("""\[CMD:([^\]]+)\]""")
        val match = tagRegex.find(raw)
        val cleanText = raw.replace(tagRegex, "").trim().ifBlank { "Executing command." }
        val tag = match?.groupValues?.getOrNull(1)?.trim() ?: return AiReply(text = cleanText)

        val action: StxAction? = when {
            tag == "FLASH" -> StxAction.ToggleFlashlight
            tag == "WIFI" -> StxAction.OpenWifi
            tag == "BT" -> StxAction.OpenBluetooth
            tag == "MUTE" -> StxAction.ToggleMute
            tag == "FULLSCREEN" -> StxAction.ToggleFullscreen
            tag == "LOCK" -> StxAction.LockDeck
            tag == "SYNC" -> StxAction.SyncHardware
            tag == "APPS" -> StxAction.OpenModule(HubModule.APPS)
            tag == "SETTINGS" -> StxAction.OpenModule(HubModule.SETTINGS)
            tag == "CAMERA" -> StxAction.OpenModule(HubModule.CAMERA)
            tag == "VAULT" -> StxAction.OpenModule(HubModule.VAULT)
            tag == "HEALTH" -> StxAction.OpenModule(HubModule.HEALTH)
            tag == "BRIEF" -> StxAction.OpenModule(HubModule.BRIEF)
            tag.startsWith("OPEN:") -> {
                val pkg = tag.removePrefix("OPEN:").trim()
                installedApps.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) || it.label.equals(pkg, ignoreCase = true) }
                    ?.let { StxAction.LaunchApp(it, windowed = false) }
            }
            tag.startsWith("WIN:") -> {
                val pkg = tag.removePrefix("WIN:").trim()
                installedApps.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) || it.label.equals(pkg, ignoreCase = true) }
                    ?.let { StxAction.LaunchApp(it, windowed = true) }
            }
            else -> null
        }
        return AiReply(text = cleanText, action = action)
    }
}
