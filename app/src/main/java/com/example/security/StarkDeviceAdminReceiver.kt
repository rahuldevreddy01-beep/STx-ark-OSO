package com.example.security

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class StarkDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context, StarkDeviceAdminReceiver::class.java)

        fun isLockAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isAdminActive(componentName(context))
        }

        fun tryLockScreenNow(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return if (dpm.isAdminActive(componentName(context))) {
                runCatching {
                    dpm.lockNow()
                    true
                }.getOrDefault(false)
            } else {
                false
            }
        }

        fun createEnableAdminIntent(context: Context): Intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Allows STX OS to physically turn off and lock your screen after timeout or when tapping LOCK."
                )
            }
    }
}
