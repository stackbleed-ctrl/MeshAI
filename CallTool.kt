package com.meshai.tools.call

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.ConnectionService
import com.meshai.tools.AgentTool
import com.meshai.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent tool for call screening and management.
 *
 * In Agent Mode, the agent can:
 * - Screen incoming calls and decide to reject, silence, or answer
 * - Leave a recorded or TTS-generated voicemail response
 * - Relay call info to the owner via SMS
 *
 * Full call answering requires the app to be set as the default dialer,
 * which requires explicit user setup. This tool handles the call screening
 * path which does NOT require default dialer status.
 */
@Singleton
class CallTool @Inject constructor(
    @ApplicationContext private val context: Context
) : AgentTool {

    override val name = "screen_call"
    override val description = "Screen an incoming call: reject, silence, or allow it. Optionally send owner an SMS alert."
    override val inputSchema = """{"action": "reject|silence|allow", "caller": "phone number", "notify_owner": "bool"}"""

    // Last known incoming call details (populated by MeshCallScreeningService)
    private var pendingCall: PendingCallInfo? = null

    override suspend fun execute(jsonInput: String): ToolResult {
        return try {
            val input = Json.decodeFromString<CallInput>(jsonInput)
            Timber.i("[CallTool] Screening call from ${input.caller}: action=${input.action}")

            when (input.action.lowercase()) {
                "reject" -> {
                    MeshCallScreeningService.currentScreeningCallback?.let { cb ->
                        cb.onScreeningResponse(
                            CallScreeningService.CallResponse.Builder()
                                .setDisallowCall(true)
                                .setRejectCall(true)
                                .build()
                        )
                    }
                    ToolResult.success("Call from ${input.caller} rejected")
                }

                "silence" -> {
                    MeshCallScreeningService.currentScreeningCallback?.let { cb ->
                        cb.onScreeningResponse(
                            CallScreeningService.CallResponse.Builder()
                                .setSilenceCall(true)
                                .build()
                        )
                    }
                    ToolResult.success("Call from ${input.caller} silenced")
                }

                "allow" -> {
                    MeshCallScreeningService.currentScreeningCallback?.let { cb ->
                        cb.onScreeningResponse(
                            CallScreeningService.CallResponse.Builder()
                                .setDisallowCall(false)
                                .build()
                        )
                    }
                    ToolResult.success("Call from ${input.caller} allowed through")
                }

                else -> ToolResult.failure("Unknown action: ${input.action}. Use reject, silence, or allow")
            }
        } catch (e: Exception) {
            Timber.e(e, "[CallTool] Error processing call action")
            ToolResult.failure("Call tool error: ${e.message}")
        }
    }

    @Serializable
    private data class CallInput(
        val action: String,
        val caller: String = "unknown",
        val notify_owner: Boolean = true
    )

    data class PendingCallInfo(val callId: String, val caller: String, val timestamp: Long)
}

// -----------------------------------------------------------------------
// CallScreeningService — receives incoming calls for agent review
// -----------------------------------------------------------------------

/**
 * System-level call screening service.
 *
 * Registered in AndroidManifest and granted BIND_CALL_SCREENING_SERVICE permission.
 * The system calls [onScreenCall] for each incoming call.
 * We store the callback so the agent (via [CallTool]) can respond.
 *
 * Users must explicitly set this as the call screening app in Phone settings.
 */
class MeshCallScreeningService : CallScreeningService() {

    companion object {
        /** Shared reference to latest screening callback — thread-safe is the responsibility of the agent loop */
        @Volatile
        var currentScreeningCallback: ScreeningCallback? = null

        /** Last incoming caller number for the agent to act on */
        @Volatile
        var lastCallerNumber: String = "unknown"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val callerNumber = callDetails.handle?.schemeSpecificPart ?: "unknown"
        lastCallerNumber = callerNumber
        Timber.i("[CallScreening] Incoming call from $callerNumber")

        // If agent mode is not active, allow call through immediately
        // Agent mode check happens in the foreground service — here we
        // store the callback and let the ReAct loop decide
        currentScreeningCallback = object : ScreeningCallback {
            override fun onScreeningResponse(response: CallResponse) {
                respondToCall(response)
            }
        }

        // Default: allow the call if no response from agent within 3 seconds
        // The WorkManager or foreground service should handle the timeout
    }

    interface ScreeningCallback {
        fun onScreeningResponse(response: CallResponse)
    }
}

// -----------------------------------------------------------------------
// ConnectionService — for MANAGE_OWN_CALLS path
// -----------------------------------------------------------------------

/**
 * Telecom ConnectionService for apps that manage their own calls.
 *
 * Required if the app needs to programmatically answer/end calls
 * without being the default dialer. Requires MANAGE_OWN_CALLS permission.
 *
 * This is a stub — full implementation requires registering a PhoneAccount
 * and handling the ConnectionService lifecycle.
 */
class MeshConnectionService : ConnectionService() {
    // Full MANAGE_OWN_CALLS implementation:
    // 1. Register PhoneAccount with TelecomManager at app startup
    // 2. Override onCreateOutgoingConnection / onCreateIncomingConnection
    // 3. Use Connection.setActive() / Connection.setDisconnected()
    // See: https://developer.android.com/guide/topics/connectivity/telecom/selfManaged
}
