package com.meshai.core.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CapabilityRegistry — the brain of mesh routing.
 *
 * Nodes self-advertise their capabilities via NODE_ADVERTISE envelopes.
 * MeshRouter consults this registry before delegating any task.
 *
 * SPEC_REF: CAP-002
 */
@Singleton
class CapabilityRegistry @Inject constructor() {

    // nodeId → list of capabilities for that node
    private val _registry = MutableStateFlow<Map<String, List<Capability>>>(emptyMap())
    val registry: StateFlow<Map<String, List<Capability>>> = _registry.asStateFlow()

    /**
     * Register (or refresh) capabilities advertised by a remote node.
     * Called when a NODE_ADVERTISE envelope is received.
     */
    fun advertise(capabilities: List<Capability>) {
        if (capabilities.isEmpty()) return
        val nodeId = capabilities.first().deviceId
        _registry.update { it + (nodeId to capabilities) }
        Timber.d("[CapabilityRegistry] Registered ${capabilities.size} caps for node $nodeId")
    }

    /**
     * Remove a node that has gone offline.
     */
    fun evict(nodeId: String) {
        _registry.update { it - nodeId }
        Timber.d("[CapabilityRegistry] Evicted node $nodeId")
    }

    /**
     * Find the best node to handle a given task.
     *
     * Selection criteria (in order):
     * 1. Must have the required capability name.
     * 2. Must not be stale.
     * 3. Must be available.
     * 4. Lowest routing score (latency + cost + battery penalty).
     *
     * Returns null if no suitable node exists → task must run locally or be queued.
     */
    fun bestNode(
        capabilityName: String,
        excludeNodeId: String? = null,
        constraints: TaskConstraints = TaskConstraints()
    ): Capability? {
        pruneStale()
        return _registry.value
            .values
            .flatten()
            .filter { cap ->
                cap.name == capabilityName &&
                cap.availability &&
                !cap.isStale() &&
                cap.deviceId != excludeNodeId &&
                cap.latencyMs <= constraints.maxLatencyMs &&
                cap.costUsd   <= constraints.maxCostUsd
            }
            .minByOrNull { it.routingScore() }
            .also {
                if (it != null)
                    Timber.d("[CapabilityRegistry] Best node for '$capabilityName': ${it.deviceId} (score=${it.routingScore()})")
                else
                    Timber.w("[CapabilityRegistry] No node found for '$capabilityName'")
            }
    }

    /**
     * Return all currently known nodes and their capability sets.
     * Used by the dashboard to populate the node grid.
     */
    fun allNodes(): Map<String, List<Capability>> =
        _registry.value.filterValues { caps -> caps.any { !it.isStale() } }

    /** Count of live (non-stale) remote nodes. */
    fun liveNodeCount(): Int = allNodes().size

    private fun pruneStale() {
        _registry.update { current ->
            current.mapValues { (_, caps) -> caps.filter { !it.isStale() } }
                   .filterValues { it.isNotEmpty() }
        }
    }
}

/** Constraints passed alongside a task for routing decisions. */
data class TaskConstraints(
    val maxCostUsd: Double = 0.05,
    val maxLatencyMs: Int  = 5_000
)
