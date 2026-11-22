package com.example.hardware

import android.os.Process
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shizuku UserService running inside the privileged shell UID (2000) process.
 * Executes MIUI 12.5 native freeform commands directly without launching fullscreen first:
 *   1. settings put global enable_freeform_support 1
 *   2. settings put global force_resizable_activities 1
 *   3. cmd package resolve-activity --brief <PACKAGE_NAME>
 *   4. am start --windowingMode 5 -n <PACKAGE_NAME>/<ACTIVITY_NAME>
 */
class ShizukuShellService : IShizukuShellService.Stub() {

    private val safeNameRegex = Regex("^[A-Za-z0-9._$]+$")
    private val safeComponentRegex = Regex("^[A-Za-z0-9._$]+/[A-Za-z0-9._$]+$")

    private val amBin: String by lazy { pickBin("/system/bin/am", "am") }
    private val cmdBin: String by lazy { pickBin("/system/bin/cmd", "cmd") }
    private val settingsBin: String by lazy { pickBin("/system/bin/settings", "settings") }

    @Volatile
    private var globalSettingsArmed = false

    private fun pickBin(abs: String, fallback: String): String {
        return if (runCatching { File(abs).canExecute() }.getOrDefault(false)) abs else fallback
    }

    override fun enableGlobalFreeformSettings(): Int {
        val rc1 = runCommand(listOf(settingsBin, "put", "global", "enable_freeform_support", "1")).first
        val rc2 = runCommand(listOf(settingsBin, "put", "global", "force_resizable_activities", "1")).first
        if (rc1 == 0 && rc2 == 0) {
            globalSettingsArmed = true
            return 0
        }
        return if (rc1 != 0) rc1 else rc2
    }

    override fun resolveLaunchComponent(packageName: String?): String {
        val pkg = packageName?.trim().orEmpty()
        if (!safeNameRegex.matches(pkg)) return ""
        val (rc, output) = runCommand(listOf(cmdBin, "package", "resolve-activity", "--brief", pkg))
        if (rc != 0) return ""
        val componentLine = output
            .lineSequence()
            .map { it.trim() }
            .lastOrNull { it.contains('/') && safeComponentRegex.matches(it) }
        return componentLine.orEmpty()
    }

    override fun launchWindowMode5(packageName: String?, activityName: String?): Int {
        val pkg = packageName?.trim().orEmpty()
        if (!safeNameRegex.matches(pkg)) return -1

        if (!globalSettingsArmed) {
            enableGlobalFreeformSettings()
        }

        val rawAct = activityName?.trim().orEmpty()
        val explicitComponent = if (rawAct.isNotEmpty() && safeNameRegex.matches(rawAct)) {
            "$pkg/$rawAct"
        } else {
            ""
        }
        val component = explicitComponent.ifEmpty { resolveLaunchComponent(pkg) }
        if (!safeComponentRegex.matches(component)) {
            return -2
        }

        var (rc, out) = runCommand(
            listOf(amBin, "start", "--windowingMode", "5", "-n", component)
        )

        // If explicit activity failed, re-resolve via cmd package resolve-activity --brief
        if ((rc != 0 || hasAmError(out)) && explicitComponent.isNotEmpty()) {
            val resolved = resolveLaunchComponent(pkg)
            if (resolved.isNotEmpty() && resolved != component) {
                val retry = runCommand(
                    listOf(amBin, "start", "--windowingMode", "5", "-n", resolved)
                )
                rc = retry.first
                out = retry.second
            }
        }

        return if (rc == 0 && !hasAmError(out)) 0 else (if (rc != 0) rc else -3)
    }

    override fun destroy() {
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    private fun hasAmError(output: String): Boolean {
        val lower = output.lowercase()
        return lower.contains("error:") ||
            lower.contains("securityexception") ||
            lower.contains("unable to resolve") ||
            lower.contains("does not exist")
    }

    private fun runCommand(args: List<String>): Pair<Int, String> {
        return try {
            val proc = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
            val finished = proc.waitFor(1800, TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return -9 to "timeout"
            }
            proc.exitValue() to out.trim()
        } catch (t: Throwable) {
            -10 to (t.message ?: "exec failed")
        }
    }
}
