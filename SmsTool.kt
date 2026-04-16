package com.meshai.tools.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
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
 * Agent tool for sending SMS messages.
 *
 * SPEC_REF: SAFETY-002 / INV-002
 * This tool is DENIED by SafetyGate when the task origin is REMOTE.
 * That check occurs in ToolExecutionGuard → SafetyGate before this class
 * is ever called.
 *
 * SPEC_REF: SAFETY-004 / INV-006
 * [isIrreversible] = true signals SafetyGate to deny execution when the
 * owner is absent and has not pre-approved the task.
 *
 * SPEC_REF: TOOL-002 / TOOL-003 / TOOL-004
 * Input validated; permission checked; returns ToolResult in all paths.
 */
@Singleton
class SmsTool @Inject constructor(
    @ApplicationContext private val context: Context
) : AgentTool {

    override val name = "send_sms"
    override val description = "Send an SMS message to a phone number"
    override val inputSchema = """{"to": "phone number string", "message": "text to send"}"""

    /**
     * SPEC_REF: SAFETY-004 — SMS is irreversible; owner must be present or pre-approve.
     */
    override val isIrreversible = true

    @Suppress("DEPRECATION")
    override suspend fun execute(jsonInput: String): ToolResult {
        // SPEC_REF: TOOL-003 — permission gate before any action
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return ToolResult.failure("SEND_SMS permission not granted. Ask user to grant it in settings.")
        }

        return try {
            // SPEC_REF: TOOL-002 — validate input before execution
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

            val parts = smsManager.divideMessage(input.message)
            smsManager.sendMultipartTextMessage(input.to, null, parts, null, null)

            Timber.i("[SmsTool] Sent SMS to ${input.to} (${input.message.length} chars)")

            // SPEC_REF: TOOL-004 — success only when action completes
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
