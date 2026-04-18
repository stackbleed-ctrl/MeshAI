package com.meshai.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RoutingTable — peer reachability map for multi-hop envelope forwarding.
 *
 * ## Why this exists (Option B — Multi-hop)
 *
 * With single-hop only, Device A can reach Device B only if they're directly
 * connected. A 3-device mesh (A — B — C) can't route A→C without B forwarding.
 *
 * [RoutingTable] tracks which nodes this device can reach, and via which
 * directly-connected peer. When [HopForwarder] needs to forward an envelope
 * to a destination that isn't directly connected, it consults the routing
 * table to find the best next hop.
 *
 * ## Population
 *
 * Entries are added when:
 * 1. A peer connects directly → [addDirectPeer] (hop count = 1).
 * 2. A NODE_ADVERTISE envelope arrives from a node we're not directly connected
 *    to → [addRouteViaPeer] (hop count = hopTrace.size + 1). This lets us
 *    discover multi-hop routes passively.
 *
 * Entries expire after [STALE_THRESHOLD_MS] (60s).
 *
 * ## Route selection
 *
 * [bestNextHop] returns the directly-connected peer that offers the lowest
 * hop count to [destinationNodeId], breaking ties by hop count then recency.
 *
 * SPEC_REF: ROUTE-002 / OPTION-B
 */
@Singleton
class RoutingTable @Inject constructor() {

    companion object {
        const val STALE_THRESHOLD_MS = 60_000L
    }

    data class RouteEntry(
        val destinationNodeId: String,
        val viaNodeId: String,       // directly-connected peer that can reach destination
        val hopCount: Int,           // 1 = direct, 2+ = multi-hop
        val updatedAtMs: Long = System.currentTimeMillis()
    ) {
        fun isStale(): Boolean = System.currentTimeMillis() - updatedAtMs > STALE_THRESHOLD_MS
    }

    /** destinationNodeId → best known route to that destination. */
    private val _table = MutableStateFlow<Map<String, RouteEntry>>(emptyMap())
    val table: StateFlow<Map<String, RouteEntry>> = _table.asStateFlow()

    /**
     * Register a directly-connected peer (hop count = 1).
     * Called by [NearbyLayer] / [BleGattLayer] on connection.
     */
    fun addDirectPeer(nodeId: String) {
        _table.update { it + (nodeId to RouteEntry(nodeId, nodeId, 1)) }
        Timber.d("[RoutingTable] Direct peer: $nodeId")
    }

    /**
     * Register a multi-hop route discovered from a NODE_ADVERTISE envelope.
     * Only updates if the new route is better (fewer hops) than what we have.
     *
     * @param destinationNodeId  The node whose advertisement we received.
     * @param viaPeer            The directly-connected peer that forwarded it.
     * @param hopCount           Number of hops to reach destination via viaPeer.
     */
    fun addRouteViaPeer(destinationNodeId: String, viaPeer: String, hopCount: Int) {
        val existing = _table.value[destinationNodeId]
        if (existing == null || hopCount < existing.hopCount) {
            _table.update { it + (destinationNodeId to RouteEntry(destinationNodeId, viaPeer, hopCount)) }
            Timber.d("[RoutingTable] Route to $destinationNodeId via $viaPeer (${hopCount} hops)")
        }
    }

    /**
     * Remove all routes that go via [peerNodeId] (peer disconnected).
     * Also removes the direct peer entry.
     */
    fun removePeer(peerNodeId: String) {
        _table.update { current ->
            current.filterValues { it.viaNodeId != peerNodeId && it.destinationNodeId != peerNodeId }
        }
        Timber.d("[RoutingTable] Removed routes via/to $peerNodeId")
    }

    /**
     * Find the best directly-connected next hop to reach [destinationNodeId].
     * Returns null if no route is known (envelope must be broadcast).
     */
    fun bestNextHop(destinationNodeId: String): RouteEntry? {
        pruneStale()
        return _table.value[destinationNodeId]?.takeIf { !it.isStale() }
    }

    /** All currently known reachable nodes (direct and multi-hop). */
    fun reachableNodes(): List<String> {
        pruneStale()
        return _table.value.keys.toList()
    }

    fun isReachable(nodeId: String): Boolean = bestNextHop(nodeId) != null

    fun directPeers(): List<String> = _table.value.values
        .filter { it.hopCount == 1 && !it.isStale() }
        .map { it.destinationNodeId }

    private fun pruneStale() {
        _table.update { current -> current.filterValues { !it.isStale() } }
    }
}
