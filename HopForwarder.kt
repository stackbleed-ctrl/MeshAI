package com.meshai.mesh

import com.meshai.mesh.MeshEnvelope.Companion.forwarded
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HopForwarder — multi-hop envelope forwarding for mesh networks.
 *
 * ## Why this exists (Option B — Multi-hop)
 *
 * Previously, every envelope's [MeshEnvelope.destinationNodeId] had to be
 * a directly-connected peer. In a 3-device mesh:
 *
 *   Device A ←──── Nearby ────→ Device B ←──── BLE ────→ Device C
 *
 * A could reach B directly. But A could NOT delegate a task to C — even
 * though B could reach C and could forward the envelope.
 *
 * [HopForwarder] plugs into [EnvelopeDispatcher]: when an envelope arrives
 * whose [MeshEnvelope.destinationNodeId] is NOT this device, forward it
 * toward the destination via the best known next hop from [RoutingTable].
 *
 * ## Forwarding logic
 *
 * 1. Check TTL — drop if ≤ 0 (loop prevention).
 * 2. Check hop trace — drop if this node appears (already forwarded).
 * 3. Look up next hop in [RoutingTable].
 * 4. Stamp this node onto hop trace and decrement TTL via [MeshEnvelope.forwarded].
 * 5. Send via [TransportManager].
 *
 * Null destinationNodeId = broadcast — flood to all direct peers.
 *
 * SPEC_REF: ROUTE-003 / OPTION-B
 */
@Singleton
class HopForwarder @Inject constructor(
    private val routingTable: RoutingTable,
    private val transportManager: TransportManager,
    private val codec: MeshCodec
) {

    /**
     * Attempt to forward [envelope] toward its destination.
     *
     * @param envelope     The envelope to forward (already validated by [EnvelopeDispatcher]).
     * @param thisNodeId   This device's nodeId — stamped onto hopTrace.
     * @return [ForwardResult] indicating what happened.
     */
    suspend fun forward(envelope: MeshEnvelope, thisNodeId: String): ForwardResult {
        if (envelope.ttl <= 1) {
            Timber.w("[HopForwarder] TTL exhausted for ${envelope.envelopeId.take(8)} — dropping")
            return ForwardResult.Dropped("TTL exhausted")
        }

        // Stamp this node and decrement TTL
        val stamped = envelope.forwarded(thisNodeId)

        val destination = stamped.destinationNodeId

        return if (destination == null) {
            // Broadcast: flood to all direct peers except the originating node
            val peers = routingTable.directPeers().filter { it != envelope.originNodeId }
            if (peers.isEmpty()) {
                Timber.d("[HopForwarder] No peers for broadcast flood")
                return ForwardResult.Dropped("No peers available for broadcast")
            }
            var forwarded = 0
            peers.forEach { peerId ->
                val result = transportManager.sendEnvelope(stamped.copy(destinationNodeId = peerId))
                if (result.success) forwarded++
            }
            Timber.d("[HopForwarder] Broadcast flooded to $forwarded/${peers.size} peers")
            ForwardResult.Forwarded(nextHopNodeId = "broadcast", hopCount = stamped.hopTrace.size)
        } else {
            // Unicast: find next hop
            val route = routingTable.bestNextHop(destination)
            if (route == null) {
                Timber.w("[HopForwarder] No route to $destination — dropping ${envelope.envelopeId.take(8)}")
                return ForwardResult.Dropped("No route to $destination")
            }

            // Forward to the directly-connected next hop with destination still set
            val result = transportManager.sendEnvelope(stamped)
            if (result.success) {
                Timber.d("[HopForwarder] Forwarded ${envelope.type} → $destination via ${route.viaNodeId} (${stamped.hopTrace.size} hops)")
                ForwardResult.Forwarded(nextHopNodeId = route.viaNodeId, hopCount = stamped.hopTrace.size)
            } else {
                Timber.w("[HopForwarder] Forward failed to ${route.viaNodeId}: ${result.errorMessage}")
                ForwardResult.Failed(reason = result.errorMessage ?: "transport error")
            }
        }
    }

    /**
     * Returns true if [envelope] should be forwarded by this node.
     * Conditions: the envelope is not addressed to us, not expired, not looped.
     */
    fun shouldForward(envelope: MeshEnvelope, thisNodeId: String): Boolean {
        if (envelope.destinationNodeId == thisNodeId) return false  // it's for us
        if (envelope.ttl <= 0) return false                         // expired
        if (envelope.hopTrace.count { it == thisNodeId } > 0) return false  // loop
        return true
    }
}

sealed class ForwardResult {
    data class Forwarded(val nextHopNodeId: String, val hopCount: Int) : ForwardResult()
    data class Dropped(val reason: String) : ForwardResult()
    data class Failed(val reason: String) : ForwardResult()
}
