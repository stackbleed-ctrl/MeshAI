package com.meshai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshai.R
import com.meshai.agent.AgentNode
import com.meshai.agent.OwnerPresenceDetector
import com.meshai.data.repository.AgentRepository
import com.meshai.mesh.MeshNetwork
import com.meshai.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AgentForegroundService — pure Android lifecycle boilerplate.
 *
 * ## What this does now (Part A)
 *
 * Everything this class used to do (task loop, wake lock management,
 * receiver registration race condition) is gone. That logic lives in:
 * - [RuntimeController] — owns the kernel loop
 * - [MeshKernel]        — execution, routing, scheduling, decision gate
 * - [TaskScheduler]     — priority queue + retry
 * - [DecisionEngine]    — adaptive policy
 *
 * This class does exactly four things:
 * 1. Start the foreground notification.
 * 2. Call [runtimeController.start] with the local node identity.
 * 3. Register the screen-state receiver (must be on main thread).
 * 4. Update the notification when owner presence changes.
 *
 * SPEC_REF: LIFE-002 / PART-A
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID    = 1001
        private const val CHANNEL_ID         = "meshai_agent"
        private const val CHANNEL_NAME       = "MeshAI Agent"
        const val ACTION_START               = "com.meshai.START_AGENT"
        const val ACTION_STOP                = "com.meshai.STOP_AGENT"
        const val ACTION_TOGGLE_AGENT_MODE   = "com.meshai.TOGGLE_AGENT_MODE"

        fun startIntent(context: Context) =
            Intent(context, AgentForegroundService::class.java).apply { action = ACTION_START }
    }

    @Inject lateinit var runtimeController: RuntimeController
    @Inject lateinit var ownerPresenceDetector: OwnerPresenceDetector
    @Inject lateinit var agentRepository: AgentRepository
    @Inject lateinit var meshNetwork: MeshNetwork

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var screenReceiver: ScreenStateReceiver? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.i("[Service] Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_TOGGLE_AGENT_MODE -> serviceScope.launch {
                ownerPresenceDetector.setAgentModeOverride(ownerPresenceDetector.isOwnerPresent.value)
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("Agent initializing…"))

        // Screen receiver MUST be registered on the main thread (here, not in a coroutine)
        registerScreenReceiver()

        serviceScope.launch {
            val localNode = agentRepository.getOrCreateLocalNode()
            meshNetwork.start(localNode)

            // Observe presence for notification updates
            launch {
                ownerPresenceDetector.isOwnerPresent.collectLatest { present ->
                    updateNotification(if (present) "Agent standby — owner present" else "Agent Mode ACTIVE")
                    agentRepository.updateLocalNodeStatus(present)
                }
            }

            // Hand off to RuntimeController — this is the only call that matters
            runtimeController.start(localNode)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        runtimeController.stop()
        serviceScope.cancel()
        meshNetwork.stop()
        screenReceiver?.let { unregisterReceiver(it) }
        Timber.i("[Service] Destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -----------------------------------------------------------------------
    // Screen receiver — on main thread as required
    // -----------------------------------------------------------------------

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        screenReceiver = ScreenStateReceiver(
            onScreenOff = { serviceScope.launch { ownerPresenceDetector.onScreenOff() } },
            onScreenOn  = { serviceScope.launch { ownerPresenceDetector.onScreenOn() } }
        )
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
    }

    // -----------------------------------------------------------------------
    // Notification
    // -----------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = "MeshAI autonomous agent status"; setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val tap  = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, AgentForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeshAI Agent").setContentText(status)
            .setSmallIcon(R.drawable.ic_mesh_node)
            .setContentIntent(tap).addAction(0, "Stop", stop)
            .setOngoing(true).setSilent(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun updateNotification(status: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(status))
    }
}
