package com.meshai.mesh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AckTracker — envelope acknowledgement and retransmission for mesh reliability.
 *
 * ## Why this exists (Option A — Resilience)
 *
 * The Codex review correctly identified: "send → assume success. That's not
 * safe in mesh networks." We have ACK/NACK in [MeshEnvelope] types but nothing
 * ever sends or tracks them.
 *
 * Without ACK tracking, a delegated task can be lost silently:
 * - We send TASK_DELEGATE to Device B
 * - Device B's Nearby layer drops the packet
 * - Device A marks the task as "delegated" and moves on
 * - Task never executes. No error. No retry.
 *
 * ## Protocol
 *
 * **Sender side:** Call [trackSent] immediately after sending a TASK_DELEGATE
 * or other critical envelope. A watchdog coroutine retransmits via [retransmit]
 * after [ACK_TIMEOUT_MS] if no ACK arrives.
 *
 * **Receiver side:** [EnvelopeDispatcher] calls [sendAck] when a TASK_DELEGATE
 * is successfully decoded and submitted to the kernel. The ACK envelope is sent
 * back to the origin node.
 *
 * **Confirmation:** When an ACK envelope arrives, [EnvelopeDispatcher] calls
 * [onAckReceived], which cancels the watchdog and clears the pending entry.
 *
 * Max retries: 3. After exhaustion, the task is marked FAILED in the kernel.
 *
 * SPEC_REF: ACK-001 / OPTION-A
 */
@Singleton
class AckTracker @Inject constructor() {

    companion object {
        const val ACK_TIMEOUT_MS = 8_000L
        const val MAX_RETRANSMITS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class PendingEnvelope(
        val envelope: MeshEnvelope,
        val sentAtMs: Long   = System.currentTimeMillis(),
        val retryCount: Int  = 0
    )

    /** envelopeId → PendingEnvelope. Cleared when ACK arrives or retries exhausted. */
    private val pending = ConcurrentHashMap<String, PendingEnvelope>()

    /** Called when an ACK envelope is received — resolves pending entry. */
    var onRetransmit: ((MeshEnvelope) -> Unit)? = null
    var onExhausted:  ((String) -> Unit)? = null   // envelopeId of failed delivery

    /**
     * Register [envelope] as pending acknowledgement.
     * Starts a watchdog that retransmits after [ACK_TIMEOUT_MS].
     */
    fun trackSent(envelope: MeshEnvelope) {
        if (envelope.type != EnvelopeType.TASK_DELEGATE &&
            envelope.type != EnvelopeType.TASK_RESULT) return  // only track critical types

        pending[envelope.envelopeId] = PendingEnvelope(envelope)
        Timber.d("[AckTracker] Tracking ${envelope.type} ${envelope.envelopeId.take(8)}")
        startWatchdog(envelope.envelopeId)
    }

    /**
     * Called by [EnvelopeDispatcher] when an ACK envelope is received.
     * Clears the pending entry — stops retransmission.
     */
    fun onAckReceived(ackedEnvelopeId: String) {
        val removed = pending.remove(ackedEnvelopeId)
        if (removed != null) {
            Timber.d("[AckTracker] ACK received for ${ackedEnvelopeId.take(8)} — confirmed")
        }
    }

    /**
     * Build an ACK envelope to send back to [originalEnvelope.originNodeId].
     * Called by [EnvelopeDispatcher] on successful TASK_DELEGATE receipt.
     */
    fun buildAck(originalEnvelope: MeshEnvelope, thisNodeId: String): MeshEnvelope =
        MeshEnvelope(
            type              = EnvelopeType.ACK,
            originNodeId      = thisNodeId,
            destinationNodeId = originalEnvelope.originNodeId,
            payload           = originalEnvelope.envelopeId
        )

    fun pendingCount(): Int = pending.size

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private fun startWatchdog(envelopeId: String) {
        scope.launch {
            delay(ACK_TIMEOUT_MS)
            val entry = pending[envelopeId] ?: return@launch  // already acked

            if (entry.retryCount >= MAX_RETRANSMITS) {
                pending.remove(envelopeId)
                Timber.e("[AckTracker] ${envelopeId.take(8)} exhausted retransmits — delivery failed")
                onExhausted?.invoke(envelopeId)
                return@launch
            }

            // Retransmit
            val updated = entry.copy(retryCount = entry.retryCount + 1, sentAtMs = System.currentTimeMillis())
            pending[envelopeId] = updated
            Timber.w("[AckTracker] Retransmitting ${entry.envelope.type} ${envelopeId.take(8)} (attempt ${updated.retryCount}/$MAX_RETRANSMITS)")
            onRetransmit?.invoke(entry.envelope)
            startWatchdog(envelopeId)  // re-arm watchdog
        }
    }
}
