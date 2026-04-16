package com.meshai.tools.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SmsMessage
import androidx.core.content.ContextCompat
import com.meshai.tools.AgentTool
import com.meshai.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent tool for sending and reading SMS messages.
 *
 * Permissions required:
 * - SEND_SMS — for sending
 * - READ_SMS + RECEIVE_SMS — for reading
 *
 * Gracefully fails with a clear message if permissions are not granted.
 */
@Singleton
class SmsTool @Inject constructor(
    @ApplicationContext private val context: Context
) : AgentTool {

    override val name = "send_sms"
    override val description = "Send an SMS message to a phone number"
    override val inputSchema = """{"to": "phone number string", "message": "text to send"}"""

    @Suppress("DEPRECATION")
    override suspend fun execute(jsonInput: String): ToolResult {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return ToolResult.failure("SEND_SMS permission not granted. Ask user to grant it in settings.")
        }

        return try {
            val input = Json.decodeFromString<SmsInput>(jsonInput)
            if (input.to.isBlank() || input.message.isBlank()) {
                return ToolResult.failure("'to' and 'message' fields are required")
            }

            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
                    ?: return ToolResult.failure("SmsManager not available")
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split message if it exceeds 160 chars
            val parts = smsManager.divideMessage(input.message)
            smsManager.sendMultipartTextMessage(
                input.to,
                null,
                parts,
                null,
                null
            )

            Timber.i("[SmsTool] Sent SMS to ${input.to} (${input.message.length} chars)")
            ToolResult.success(
                "SMS sent to ${input.to}",
                mapOf("to" to input.to, "chars" to input.message.length.toString())
            )
        } catch (e: Exception) {
            Timber.e(e, "[SmsTool] Failed to send SMS")
            ToolResult.failure("SMS send failed: ${e.message}")
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @Serializable
    private data class SmsInput(val to: String, val message: String)
}
