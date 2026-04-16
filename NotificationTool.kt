package com.meshai.tools.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.meshai.tools.AgentTool
import com.meshai.tools.ToolResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent tool for reading and responding to notifications.
 *
 * Uses [MeshNotificationListenerService] to receive all status bar notifications.
 * Requires the user to grant Notification Access in Android Settings.
 *
 * In Agent Mode, the agent can:
 * - Read incoming notifications (messages, emails, alerts)
 * - Dismiss notifications
 * - Relay important ones to the owner via SMS/mesh message
 */
@Singleton
class NotificationTool @Inject constructor() : AgentTool {

    override val name = "read_notifications"
    override val description = "Read recent notifications from the device. Returns latest unread notifications as JSON."
    override val inputSchema = """{"limit": "number (1-20, default 5)", "package_filter": "optional app package name"}"""

    override suspend fun execute(jsonInput: String): ToolResult {
        return try {
            val input = runCatching { Json.decodeFromString<NotifInput>(jsonInput) }
                .getOrDefault(NotifInput())

            val notifications = MeshNotificationListenerService.recentNotifications
                .let { list ->
                    if (input.package_filter != null) {
                        list.filter { it.packageName == input.package_filter }
                    } else list
                }
                .takeLast(input.limit)

            if (notifications.isEmpty()) {
                return ToolResult.success("No recent notifications found")
            }

            val summary = notifications.joinToString("\n") {
                "[${it.appLabel}] ${it.title}: ${it.text}"
            }

            ToolResult.success(
                "Found ${notifications.size} notifications:\n$summary",
                mapOf("count" to notifications.size.toString())
            )
        } catch (e: Exception) {
            Timber.e(e, "[NotificationTool] Error reading notifications")
            ToolResult.failure("Notification read error: ${e.message}")
        }
    }

    @Serializable
    private data class NotifInput(
        val limit: Int = 5,
        val package_filter: String? = null
    )
}

// -----------------------------------------------------------------------
// NotificationListenerService
// -----------------------------------------------------------------------

/**
 * Background listener that captures all status bar notifications.
 *
 * Must be granted Notification Access via:
 *   Settings > Apps > Special app access > Notification access > MeshAI
 *
 * Stores a rolling window of recent notifications in memory.
 * Agent tools read from this buffer.
 */
class MeshNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val MAX_CACHED = 50

        /** Thread-safe rolling buffer of recent notifications */
        val recentNotifications: ArrayDeque<CapturedNotification> = ArrayDeque(MAX_CACHED)

        /** Hot flow for real-time notification events in the agent loop */
        val notificationFlow = MutableSharedFlow<CapturedNotification>(
            replay = 10,
            extraBufferCapacity = 20
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val captured = CapturedNotification(
            packageName = sbn.packageName,
            appLabel = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(sbn.packageName, 0)
                ).toString()
            } catch (e: Exception) { sbn.packageName },
            title = extras.getString("android.title") ?: "",
            text = extras.getCharSequence("android.text")?.toString() ?: "",
            timestamp = sbn.postTime,
            key = sbn.key
        )

        Timber.d("[NotifListener] Posted: [${captured.appLabel}] ${captured.title}")

        synchronized(recentNotifications) {
            if (recentNotifications.size >= MAX_CACHED) {
                recentNotifications.removeFirst()
            }
            recentNotifications.addLast(captured)
        }

        // Emit to the flow (non-blocking)
        notificationFlow.tryEmit(captured)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        synchronized(recentNotifications) {
            recentNotifications.removeIf { it.key == sbn.key }
        }
    }

    override fun onListenerConnected() {
        Timber.i("[NotifListener] Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Timber.w("[NotifListener] Notification listener disconnected")
    }
}

// -----------------------------------------------------------------------
// Data model
// -----------------------------------------------------------------------

data class CapturedNotification(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val key: String
)
