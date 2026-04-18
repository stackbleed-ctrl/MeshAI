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
import com.meshai.agent.ExecutionBudget
import com.meshai.agent.GoalEngine
import com.meshai.agent.OwnerPresenceDetector
import com.meshai.agent.ReActLoop
import com.meshai.agent.TaskPriority
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
 * Persistent Foreground Service — keeps the MeshAI agent running 24/7.
 *
 * ## Bug fixes applied
 *
 * 1. **Idempotent init** — `agentLoopJob?.cancel()` before re-launch prevents
 *    multiple concurrent loops after OS-triggered START_STICKY restarts.
 *
 * 2. **Mutex'd wake lock** — `withWakeLock { }` serialises lock acquisition
 *    and always releases via try/finally, preventing orphaned 5-min locks.
 *
 * 3. **Receiver on main thread** — `registerScreenReceiver()` called from
 *    `onStartCommand()` (main thread), not inside a coroutine.
 *
 * ## Production additions
 *
 * 4. **Priority-based budgets** — CRITICAL tasks get 2× the default token
 *    budget; LOW tasks get half. Budget is passed into `ReActLoop.execute()`.
 *
 * 5. **ExecutionTrace storage** — The [ExecutionTrace] returned by the loop
 *    is forwarded to [AgentRepository.storeTrace] for dashboard display.
 *
 * 6. **Lease management** — [TaskLeaseManager] is called before and after
 *    each task to prevent duplicate execution across the mesh.
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID     = 1001
        private const val CHANNEL_ID          = "meshai_agent"
        private const val CHANNEL_NAME        = "MeshAI Agent"

        const val ACTION_START              = "com.meshai.START_AGENT"
        const val ACTION_STOP               = "com.meshai.STOP_AGENT"
        const val ACTION_TOGGLE_AGENT_MODE  = "com.meshai.TOGGLE_AGENT_MODE"

        fun startIntent(context: Context) =
            Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_START
            }

        /** Token budget multipliers by task priority. */
        private val BUDGET_BY_PRIORITY = mapOf(
            TaskPriority.LOW      to (ExecutionBudget.DEFAULT_MAX_TOKENS / 2),
            TaskPriority.NORMAL   to ExecutionBudget.DEFAULT_MAX_TOKENS,
            TaskPriority.HIGH     to (ExecutionBudget.DEFAULT_MAX_TOKENS * 3 / 2),
            TaskPriority.CRITICAL to (ExecutionBudget.DEFAULT_MAX_TOKENS * 2)
        )
    }

    @Inject lateinit var meshNetwork: MeshNetwork
    @Inject lateinit var reActLoop: ReActLoop
    @Inject lateinit var agentMemory: AgentMemory
    @Inject lateinit var ownerPresenceDetector: OwnerPresenceDetector
    @Inject lateinit var agentRepository: AgentRepository
    @Inject lateinit var goalEngine: GoalEngine
    @Inject lateinit var taskLeaseManager: TaskLeaseManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var agentLoopJob: Job? = null
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

        // Fix 3: registerReceiver must run on the main thread (here in onStartCommand)
        registerScreenReceiver()

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
     * Idempotent — safe to call on every START_STICKY restart.
     * Cancels any existing loop before creating a replacement.
     */
    private suspend fun initializeAgent() {
        // Fix 1: cancel before re-launch
        agentLoopJob?.cancel()
        agentLoopJob = null

        localNode = agentRepository.getOrCreateLocalNode()
        Timber.i("[Service] Local node: ${localNode.displayName} (${localNode.nodeId})")

        meshNetwork.start(localNode)

        serviceScope.launch {
            ownerPresenceDetector.isOwnerPresent.collectLatest { present ->
                updateNotification(
                    if (present) "Agent standby — owner present"
                    else "Agent Mode ACTIVE — owner away"
                )
                agentRepository.updateLocalNodeStatus(present)
            }
        }

        startAgentLoop()
    }

    private fun registerScreenReceiver() {
        if (screenStateReceiver != null) return
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
        agentLoopJob = serviceScope.launch {
            var backoffMs    = 5_000L
            val maxBackoffMs = 60_000L

            while (true) {
                val pendingTasks = agentRepository.getPendingTasks()

                if (pendingTasks.isEmpty()) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(maxBackoffMs)
                    continue
                }

                backoffMs = 5_000L
                val task = pendingTasks.first()

                // Distributed deduplication — skip if another node claimed it
                if (!taskLeaseManager.claimTask(task.taskId, localNode.nodeId)) {
                    Timber.d("[Service] Task ${task.taskId} already claimed — skipping")
                    delay(2_000L)
                    continue
                }

                Timber.d("[Service] Processing task: ${task.title} [${task.priority}]")

                // Fix 2: mutex'd wakelock — one lock at a time, always released
                withWakeLock {
                    try {
                        agentRepository.updateTaskStatus(task.taskId, TaskStatus.IN_PROGRESS)

                        val budget = ExecutionBudget(
                            maxTokens = BUDGET_BY_PRIORITY[task.priority]
                                ?: ExecutionBudget.DEFAULT_MAX_TOKENS
                        )

                        val execResult = reActLoop.execute(task, localNode, budget)

                        // Store trace for dashboard / debugging
                        agentRepository.storeTrace(task.taskId, execResult.trace)
                        agentRepository.completeTask(task.taskId, execResult.answer)

                        Timber.i("[Service] Task done: ${task.title} — ${execResult.trace.summary()}")
                    } catch (e: Exception) {
                        Timber.e(e, "[Service] Task failed: ${task.title}")
                        agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
                    } finally {
                        taskLeaseManager.releaseTask(task.taskId, localNode.nodeId)
                    }
                }

                delay(2_000L)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Fix 2: mutex-guarded wake lock
    // -----------------------------------------------------------------------

    private suspend fun withWakeLock(block: suspend () -> Unit) {
        wakeLockMutex.withLock {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeshAI:AgentTaskWakeLock"
            )
            wl.acquire(5 * 60 * 1000L)
            try { block() } finally { if (wl.isHeld) wl.release() }
        }
    }

    // -----------------------------------------------------------------------
    // Notification
    // -----------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "MeshAI autonomous agent status"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
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
