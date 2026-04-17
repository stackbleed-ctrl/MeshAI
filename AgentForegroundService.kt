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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * ## Bug fixes applied
 *
 * Fix 1 — Idempotent [initializeAgent]:
 * The service uses START_STICKY, so the OS can restart it after being killed.
 * Previously [initializeAgent] spawned a new [agentLoopJob] without cancelling
 * the old one, resulting in multiple concurrent loops pulling tasks from the
 * same Room queue after 3+ OS restarts. Now [agentLoopJob] is cancelled before
 * any new launch.
 *
 * Fix 2 — Mutex'd wake lock via [withWakeLock]:
 * The original [acquireWakeLock] overwrote the [wakeLock] reference without
 * releasing the previous lock if called concurrently (e.g., two tasks queued).
 * Each orphaned lock held for up to 5 minutes. [withWakeLock] serialises lock
 * acquisition and always releases via try/finally.
 *
 * Fix 3 — Receiver on main thread:
 * [registerReceiver] must be called from the main thread. The original code
 * called it inside a [serviceScope] coroutine dispatched on [Dispatchers.Default]
 * (thread pool). On several OEMs this silently failed, leaving owner presence
 * undetected. The call is now made directly from [onStartCommand] on the main
 * thread before the coroutine is launched.
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID      = "meshai_agent"
        private const val CHANNEL_NAME    = "MeshAI Agent"

        const val ACTION_START              = "com.meshai.START_AGENT"
        const val ACTION_STOP               = "com.meshai.STOP_AGENT"
        const val ACTION_TOGGLE_AGENT_MODE  = "com.meshai.TOGGLE_AGENT_MODE"

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

    // Bug fix 1: tracked so we can cancel before re-launching
    private var agentLoopJob: Job? = null

    // Bug fix 2: mutex ensures only one wakelock is held at a time
    private val wakeLockMutex = Mutex()

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

        // Bug fix 3: registerReceiver MUST run on the main thread.
        // onStartCommand is called on the main thread, so register here
        // before launching any coroutine.
        registerScreenReceiver()

        // Launch agent init on background dispatcher
        serviceScope.launch { initializeAgent() }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.i("[Service] AgentForegroundService destroyed")
        agentLoopJob?.cancel()
        serviceScope.cancel()
        meshNetwork.stop()
        screenStateReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    /**
     * Idempotent agent initialization.
     *
     * Safe to call multiple times (e.g., on START_STICKY restarts).
     * Always cancels any existing [agentLoopJob] before starting a new one,
     * preventing duplicate concurrent loops after OS-triggered restarts.
     */
    private suspend fun initializeAgent() {
        // Bug fix 1: cancel existing loop job before creating a new one
        agentLoopJob?.cancel()
        agentLoopJob = null

        localNode = agentRepository.getOrCreateLocalNode()
        Timber.i("[Service] Local node: ${localNode.displayName} (${localNode.nodeId})")

        // Start mesh networking
        meshNetwork.start(localNode)

        // Observe presence changes and update notification
        serviceScope.launch {
            ownerPresenceDetector.isOwnerPresent.collectLatest { present ->
                updateNotification(
                    if (present) "Agent standby — owner present"
                    else "Agent Mode ACTIVE — owner away"
                )
                agentRepository.updateLocalNodeStatus(present)
            }
        }

        // Start the main task loop
        startAgentLoop()
    }

    // Bug fix 3: called from onStartCommand (main thread), not from coroutine
    private fun registerScreenReceiver() {
        if (screenStateReceiver != null) return  // already registered (idempotent guard)

        screenStateReceiver = ScreenStateReceiver(
            onScreenOff = { serviceScope.launch { ownerPresenceDetector.onScreenOff() } },
            onScreenOn  = { serviceScope.launch { ownerPresenceDetector.onScreenOn() } }
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
        // Bug fix 1: assign to agentLoopJob so subsequent initializeAgent()
        // calls can cancel this before launching a replacement.
        agentLoopJob = serviceScope.launch {
            var backoffMs     = 5_000L
            val maxBackoffMs  = 60_000L

            while (true) {
                val pendingTasks = agentRepository.getPendingTasks()

                if (pendingTasks.isEmpty()) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(maxBackoffMs)
                    continue
                }

                backoffMs = 5_000L
                val task = pendingTasks.first()
                Timber.d("[Service] Processing task: ${task.title}")

                // Bug fix 2: serialised wakelock — only one held at a time,
                // always released even on exception
                withWakeLock {
                    try {
                        agentRepository.updateTaskStatus(task.taskId, TaskStatus.IN_PROGRESS)
                        val result = reActLoop.execute(task, localNode)
                        agentRepository.completeTask(task.taskId, result)
                        Timber.i("[Service] Task completed: ${task.title}")
                    } catch (e: Exception) {
                        Timber.e(e, "[Service] Task failed: ${task.title}")
                        agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
                    }
                }

                delay(2_000L)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Bug fix 2: mutex-guarded wakelock — replaces acquireWakeLock /
    // releaseWakeLock which could orphan locks if called concurrently.
    // -----------------------------------------------------------------------

    /**
     * Acquires a partial wake lock, executes [block], then unconditionally
     * releases the lock — even if [block] throws. Serialised via [wakeLockMutex]
     * so only one lock can be live at a time regardless of concurrency.
     *
     * Max hold time is 5 minutes as a safety ceiling.
     */
    private suspend fun withWakeLock(block: suspend () -> Unit) {
        wakeLockMutex.withLock {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeshAI:AgentTaskWakeLock"
            )
            wl.acquire(5 * 60 * 1000L)  // safety ceiling — released in finally
            try {
                block()
            } finally {
                if (wl.isHeld) wl.release()
            }
        }
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(status))
    }
}
