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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.meshai.R
import com.meshai.agent.AgentMemory
import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.GoalEngine
import com.meshai.agent.OwnerPresenceDetector
import com.meshai.agent.ReActLoop
import com.meshai.agent.TaskStatus
import com.meshai.data.repository.AgentRepository
import com.meshai.mesh.MeshNetwork
import com.meshai.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Persistent Foreground Service that keeps the MeshAI agent running 24/7.
 *
 * Responsibilities:
 * 1. Initialize and start the mesh network
 * 2. Run the ReAct agent loop for queued tasks
 * 3. Monitor owner presence and switch to Agent Mode when away
 * 4. Handle mesh message routing (delegate tasks, sync memory)
 * 5. Manage the persistent foreground notification
 *
 * The service is started at boot via [BootReceiver] and re-started by
 * WorkManager if killed by the system.
 *
 * Battery optimizations:
 * - Tasks are batched when owner is present
 * - Agent loop sleeps when task queue is empty (exponential backoff)
 * - BLE advertising uses ADVERTISE_MODE_LOW_POWER
 * - CPU WakeLock is released between task executions
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "meshai_agent"
        private const val CHANNEL_NAME = "MeshAI Agent"

        const val ACTION_START = "com.meshai.START_AGENT"
        const val ACTION_STOP = "com.meshai.STOP_AGENT"
        const val ACTION_TOGGLE_AGENT_MODE = "com.meshai.TOGGLE_AGENT_MODE"

        fun startIntent(context: Context) =
            Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_START
            }
    }

    @Inject lateinit var meshNetwork: MeshNetwork
    @Inject lateinit var reActLoop: ReActLoop
    @Inject lateinit var agentMemory: AgentMemory
    @Inject lateinit var ownerPresenceDetector: OwnerPresenceDetector
    @Inject lateinit var agentRepository: AgentRepository
    @Inject lateinit var goalEngine: GoalEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var agentLoopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var localNode: AgentNode
    private var screenStateReceiver: ScreenStateReceiver? = null

    // -----------------------------------------------------------------------
    // Service lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Timber.i("[Service] AgentForegroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_AGENT_MODE -> {
                serviceScope.launch {
                    val current = ownerPresenceDetector.isOwnerPresent.value
                    ownerPresenceDetector.setAgentModeOverride(current)
                }
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("Agent initializing..."))
        initializeAgent()
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.i("[Service] AgentForegroundService destroyed")
        agentLoopJob?.cancel()
        serviceScope.cancel()
        meshNetwork.stop()
        wakeLock?.release()
        screenStateReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    private fun initializeAgent() {
        serviceScope.launch {
            // Build local node identity from persistent preferences
            localNode = agentRepository.getOrCreateLocalNode()
            Timber.i("[Service] Local node: ${localNode.displayName} (${localNode.nodeId})")

            // Register screen state broadcast receiver
            registerScreenReceiver()

            // Start mesh networking
            meshNetwork.start(localNode)

            // Observe presence changes and update notification
            launch {
                ownerPresenceDetector.isOwnerPresent.collectLatest { present ->
                    updateNotification(
                        if (present) "Agent standby — owner present"
                        else "Agent Mode ACTIVE — owner away"
                    )
                    agentRepository.updateLocalNodeStatus(present)
                }
            }

            // Run the main agent task loop
            startAgentLoop()
        }
    }

    private fun registerScreenReceiver() {
        screenStateReceiver = ScreenStateReceiver(
            onScreenOff = {
                serviceScope.launch { ownerPresenceDetector.onScreenOff() }
            },
            onScreenOn = {
                serviceScope.launch { ownerPresenceDetector.onScreenOn() }
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    // -----------------------------------------------------------------------
    // Agent task loop
    // -----------------------------------------------------------------------

    private fun startAgentLoop() {
        agentLoopJob = serviceScope.launch {
            var backoffMs = 5_000L
            val maxBackoffMs = 60_000L

            while (true) {
                val pendingTasks = agentRepository.getPendingTasks()

                if (pendingTasks.isEmpty()) {
                    // Nothing to do — sleep with exponential backoff
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(maxBackoffMs)
                    continue
                }

                // Reset backoff when there's work
                backoffMs = 5_000L

                val task = pendingTasks.first()
                Timber.d("[Service] Processing task: ${task.title}")

                // Acquire partial wake lock for task execution
                acquireWakeLock()

                try {
                    agentRepository.updateTaskStatus(task.taskId, TaskStatus.IN_PROGRESS)
                    val result = reActLoop.execute(task, localNode)
                    agentRepository.completeTask(task.taskId, result)
                    Timber.i("[Service] Task completed: ${task.title}")
                } catch (e: Exception) {
                    Timber.e(e, "[Service] Task failed: ${task.title}")
                    agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
                } finally {
                    releaseWakeLock()
                }

                // Small delay between tasks to avoid hammering the LLM
                delay(2_000L)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Wake lock
    // -----------------------------------------------------------------------

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MeshAI:AgentTaskWakeLock"
        ).also {
            it.acquire(5 * 60 * 1000L) // Max 5 minutes per task
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // -----------------------------------------------------------------------
    // Notification
    // -----------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "MeshAI autonomous agent status"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AgentForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeshAI Agent")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_mesh_node)
            .setContentIntent(tapIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
    }
}
