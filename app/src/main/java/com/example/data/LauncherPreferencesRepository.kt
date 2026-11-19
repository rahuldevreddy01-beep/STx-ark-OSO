package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LauncherPreferences
import com.example.model.VaultNote
import com.example.security.PasscodeCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.stxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stark_os_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val OPERATOR_CALLSIGN = stringPreferencesKey("operator_callsign")
        val PINNED_PACKAGES = stringSetPreferencesKey("pinned_packages")
        val MUTED_ALERT_PACKAGES = stringSetPreferencesKey("muted_alert_packages")
        val DISABLED_SYNC_PACKAGES = stringSetPreferencesKey("disabled_sync_packages")
        val FULLSCREEN_IMMERSIVE = booleanPreferencesKey("fullscreen_immersive")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_MORPH_ORIENTATION = booleanPreferencesKey("auto_morph_orientation")
        val EDGE_GLOW_CRIMSON = booleanPreferencesKey("edge_glow_crimson")
        val VAULT_NOTES_JSON = stringPreferencesKey("vault_notes_json")
        val LOCK_COVER_ENABLED = booleanPreferencesKey("lock_cover_enabled")
        val AUTO_LOCK_ON_SCREEN_OFF = booleanPreferencesKey("auto_lock_on_screen_off")
        val BIOMETRIC_UNLOCK_ENABLED = booleanPreferencesKey("biometric_unlock_enabled")
        val USE_OS_PHYSICAL_LOCK = booleanPreferencesKey("use_os_physical_lock")
        val PASSCODE_HASH = stringPreferencesKey("passcode_hash")
        val PASSCODE_SALT = stringPreferencesKey("passcode_salt")
        val PASSCODE_IS_ALPHANUMERIC = booleanPreferencesKey("passcode_is_alphanumeric")
        val LOCK_SHOW_STEPS = booleanPreferencesKey("lock_show_steps")
        val LOCK_SHOW_CHARGING_RING = booleanPreferencesKey("lock_show_charging_ring")
        val LOCK_SHOW_STX_AI = booleanPreferencesKey("lock_show_stx_ai")
        val LOCK_HEADLINE = stringPreferencesKey("lock_headline")
        val SYSTEM_FLOATING_AI = booleanPreferencesKey("system_floating_ai")
        val APP_USAGE_JSON = stringPreferencesKey("app_usage_json")
    }

    val preferencesFlow: Flow<LauncherPreferences> = context.stxDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
        val rawNotes = prefs[Keys.VAULT_NOTES_JSON]
        val notes = if (rawNotes.isNullOrBlank()) {
            defaultVaultNotes()
        } else {
            decodeNotes(rawNotes)
        }
        val storedHeadline = prefs[Keys.LOCK_HEADLINE]
            ?.replace("STARK OS", "STX OS", ignoreCase = true)
            ?.ifBlank { "STX OS // SECURE DECK" }
            ?: "STX OS // SECURE DECK"

        val (usageCounts, usageLastMs) = decodeUsageStats(prefs[Keys.APP_USAGE_JSON])

        LauncherPreferences(
            operatorCallsign = prefs[Keys.OPERATOR_CALLSIGN]?.ifBlank { "SANJU" } ?: "SANJU",
            pinnedPackages = prefs[Keys.PINNED_PACKAGES] ?: emptySet(),
            mutedAlertPackages = prefs[Keys.MUTED_ALERT_PACKAGES] ?: emptySet(),
            disabledSyncPackages = prefs[Keys.DISABLED_SYNC_PACKAGES] ?: emptySet(),
            fullscreenImmersive = prefs[Keys.FULLSCREEN_IMMERSIVE] ?: true,
            sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            autoMorphOrientation = prefs[Keys.AUTO_MORPH_ORIENTATION] ?: true,
            edgeGlowCrimson = prefs[Keys.EDGE_GLOW_CRIMSON] ?: false,
            vaultNotes = notes,
            lockCoverEnabled = prefs[Keys.LOCK_COVER_ENABLED] ?: true,
            autoLockOnScreenOff = prefs[Keys.AUTO_LOCK_ON_SCREEN_OFF] ?: true,
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] ?: true,
            useOsPhysicalLock = prefs[Keys.USE_OS_PHYSICAL_LOCK] ?: true,
            passcodeHash = prefs[Keys.PASSCODE_HASH] ?: "",
            passcodeSalt = prefs[Keys.PASSCODE_SALT] ?: "",
            passcodeIsAlphanumeric = prefs[Keys.PASSCODE_IS_ALPHANUMERIC] ?: false,
            lockShowSteps = prefs[Keys.LOCK_SHOW_STEPS] ?: true,
            lockShowChargingRing = prefs[Keys.LOCK_SHOW_CHARGING_RING] ?: true,
            lockShowStxAiTrigger = prefs[Keys.LOCK_SHOW_STX_AI] ?: true,
            lockHeadline = storedHeadline,
            systemFloatingAiEnabled = prefs[Keys.SYSTEM_FLOATING_AI] ?: false,
            appLaunchCounts = usageCounts,
            appLastUsedMs = usageLastMs
        )
    }

    suspend fun setOperatorCallsign(callsign: String) {
        val clean = callsign.trim().uppercase().take(18).ifBlank { "SANJU" }
        context.stxDataStore.edit { it[Keys.OPERATOR_CALLSIGN] = clean }
    }

    suspend fun togglePinnedPackage(packageName: String) {
        context.stxDataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (packageName in current) current.remove(packageName) else current.add(packageName)
            prefs[Keys.PINNED_PACKAGES] = current
        }
    }

    suspend fun toggleAppAlerts(packageName: String) {
        context.stxDataStore.edit { prefs ->
            val current = prefs[Keys.MUTED_ALERT_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (packageName in current) current.remove(packageName) else current.add(packageName)
            prefs[Keys.MUTED_ALERT_PACKAGES] = current
        }
    }

    suspend fun toggleAppSync(packageName: String) {
        context.stxDataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_SYNC_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (packageName in current) current.remove(packageName) else current.add(packageName)
            prefs[Keys.DISABLED_SYNC_PACKAGES] = current
        }
    }

    suspend fun setFullscreenImmersive(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.FULLSCREEN_IMMERSIVE] = enabled }
    }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.SFX_ENABLED] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setAutoMorphOrientation(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.AUTO_MORPH_ORIENTATION] = enabled }
    }

    suspend fun setEdgeGlowCrimson(crimson: Boolean) {
        context.stxDataStore.edit { it[Keys.EDGE_GLOW_CRIMSON] = crimson }
    }

    suspend fun setLockCoverEnabled(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.LOCK_COVER_ENABLED] = enabled }
    }

    suspend fun setAutoLockOnScreenOff(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.AUTO_LOCK_ON_SCREEN_OFF] = enabled }
    }

    suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.BIOMETRIC_UNLOCK_ENABLED] = enabled }
    }

    suspend fun setUseOsPhysicalLock(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.USE_OS_PHYSICAL_LOCK] = enabled }
    }

    suspend fun setPasscode(passcode: String, isAlphanumeric: Boolean) {
        val salt = PasscodeCrypto.generateSaltHex()
        val hash = PasscodeCrypto.hashPasscode(passcode, salt)
        context.stxDataStore.edit {
            it[Keys.PASSCODE_SALT] = salt
            it[Keys.PASSCODE_HASH] = hash
            it[Keys.PASSCODE_IS_ALPHANUMERIC] = isAlphanumeric
            it[Keys.LOCK_COVER_ENABLED] = true
        }
    }

    suspend fun clearPasscode() {
        context.stxDataStore.edit {
            it[Keys.PASSCODE_SALT] = ""
            it[Keys.PASSCODE_HASH] = ""
        }
    }

    suspend fun setLockWidgetVisibility(
        showSteps: Boolean? = null,
        showChargingRing: Boolean? = null,
        showStxAi: Boolean? = null
    ) {
        context.stxDataStore.edit { prefs ->
            showSteps?.let { prefs[Keys.LOCK_SHOW_STEPS] = it }
            showChargingRing?.let { prefs[Keys.LOCK_SHOW_CHARGING_RING] = it }
            showStxAi?.let { prefs[Keys.LOCK_SHOW_STX_AI] = it }
        }
    }

    suspend fun setLockHeadline(headline: String) {
        val clean = headline.trim().uppercase().take(36).ifBlank { "STX OS // SECURE DECK" }
        context.stxDataStore.edit { it[Keys.LOCK_HEADLINE] = clean }
    }

    suspend fun addVaultNote(title: String, body: String) {
        val stamp = SimpleDateFormat("HH:mm · dd MMM", Locale.getDefault()).format(Date())
        val note = VaultNote(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "TACTICAL NOTE" }.uppercase(),
            body = body.trim(),
            timestampLabel = stamp.uppercase()
        )
        context.stxDataStore.edit { prefs ->
            val existing = prefs[Keys.VAULT_NOTES_JSON]?.let(::decodeNotes) ?: defaultVaultNotes()
            prefs[Keys.VAULT_NOTES_JSON] = encodeNotes(listOf(note) + existing)
        }
    }

    suspend fun deleteVaultNote(id: String) {
        context.stxDataStore.edit { prefs ->
            val existing = prefs[Keys.VAULT_NOTES_JSON]?.let(::decodeNotes) ?: defaultVaultNotes()
            prefs[Keys.VAULT_NOTES_JSON] = encodeNotes(existing.filterNot { it.id == id })
        }
    }

    private fun encodeNotes(notes: List<VaultNote>): String {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("body", n.body)
                    put("ts", n.timestampLabel)
                }
            )
        }
        return array.toString()
    }

    private fun decodeNotes(json: String): List<VaultNote> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            VaultNote(
                id = obj.getString("id"),
                title = obj.getString("title").replace("STARK OS", "STX OS"),
                body = obj.getString("body").replace("Stark OS", "STX OS"),
                timestampLabel = obj.optString("ts", "LOCAL")
            )
        }
    }.getOrDefault(defaultVaultNotes())

    suspend fun setSystemFloatingAiEnabled(enabled: Boolean) {
        context.stxDataStore.edit { it[Keys.SYSTEM_FLOATING_AI] = enabled }
    }

    suspend fun recordAppLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        context.stxDataStore.edit { prefs ->
            val root = runCatching {
                val raw = prefs[Keys.APP_USAGE_JSON]
                if (raw.isNullOrBlank()) JSONObject() else JSONObject(raw)
            }.getOrElse { JSONObject() }
            val entry = root.optJSONObject(packageName) ?: JSONObject()
            val count = entry.optInt("c", 0) + 1
            entry.put("c", count)
            entry.put("t", now)
            root.put(packageName, entry)
            prefs[Keys.APP_USAGE_JSON] = root.toString()
        }
    }

    private fun decodeUsageStats(raw: String?): Pair<Map<String, Int>, Map<String, Long>> {
        if (raw.isNullOrBlank()) return emptyMap<String, Int>() to emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val counts = HashMap<String, Int>()
            val times = HashMap<String, Long>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val obj = root.optJSONObject(pkg) ?: continue
                counts[pkg] = obj.optInt("c", 0)
                times[pkg] = obj.optLong("t", 0L)
            }
            counts to times
        }.getOrDefault(emptyMap<String, Int>() to emptyMap())
    }

    private fun defaultVaultNotes(): List<VaultNote> = listOf(
        VaultNote(
            id = "vault_default_1",
            title = "STX OS LAUNCHER",
            body = "Set STX OS as Home Launcher inside Settings -> Home Launcher for instant wake access.",
            timestampLabel = "SYSTEM"
        )
    )
}
