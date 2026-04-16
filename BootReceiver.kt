package com.meshai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Starts the AgentForegroundService after device boot.
 *
 * Registered for:
 * - android.intent.action.BOOT_COMPLETED — normal boot
 * - android.intent.action.MY_PACKAGE_REPLACED — after app update
 *
 * Requires: RECEIVE_BOOT_COMPLETED permission
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Timber.i("[BootReceiver] Boot/update received — starting agent service")
            ContextCompat.startForegroundService(
                context,
                AgentForegroundService.startIntent(context)
            )
        }
    }
}

/**
 * Monitors screen on/off state for the OwnerPresenceDetector.
 *
 * Created dynamically by AgentForegroundService (not declared in manifest,
 * since ACTION_SCREEN_OFF/ON cannot be received by manifest-registered receivers).
 */
class ScreenStateReceiver(
    private val onScreenOff: () -> Unit,
    private val onScreenOn: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Timber.d("[ScreenReceiver] Screen OFF")
                onScreenOff()
            }
            Intent.ACTION_SCREEN_ON -> {
                Timber.d("[ScreenReceiver] Screen ON")
                onScreenOn()
            }
        }
    }
}
