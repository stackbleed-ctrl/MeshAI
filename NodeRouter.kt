package com.meshai.mesh

import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.NodeStatus
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scores peer nodes and selects the best executor for a delegated task.
 *
 * ## Scoring model
 *
 * Each candidate node receives a composite score from four components:
 *
 * | Component            | Max pts | Rationale |
 * |----------------------|---------|-----------|
 * | Battery level        | 40      | Linear; a dead node cannot execute |
 * | Agent Mode active    | 30      | Owner-away nodes are more "available" |
 * | Capability match     | 20      | Each required cap matched = 4 pts |
 * | Extra capabilities   | 10      | Nodes with more tools solve more tasks |
 *
 * Nodes below [MIN_BATTERY_PCT] or [NodeStatus.OFFLINE] are always excluded.
 *
 * ## Usage
 *
 * Call [bestNodeForTask] from [MeshNetwork] before delegating:
 *
 * ```kotlin
 * val target = nodeRouter.bestNodeForTask(task, meshNetwork.connectedPeers())
 * if (target != null) {
 *     meshNetwork.delegateTask(task, target)
 * } else {
 *     // No suitable peer — execute locally or queue for later
 * }
 * ```
 */
@Singleton
class NodeRouter @Inject constructor() {

    companion object {
        /** Nodes with battery below this threshold are excluded from delegation. */
        const val MIN_BATTERY_PCT = 20

        // Score weights
        private const val WEIGHT_BATTERY      = 0.40   // per % point → max 40 at 100%
        private const val WEIGHT_AGENT_MODE   = 30.0   // flat bonus
        private const val WEIGHT_CAP_MATCH    = 4.0    // per required capability matched
        private const val WEIGHT_EXTRA_CAPS   = 0.5    // per extra capability beyond required
    }

    /**
     * Select the best available peer node to execute [task].
     *
     * @param task   The task to delegate — [AgentTask.requiredCapabilities]
     *               is used to filter and score candidates.
     * @param peers  The current set of reachable peer nodes from the
     *               transport layer.
     * @return       The highest-scoring eligible node, or null if no
     *               node meets the minimum criteria.
     */
    fun bestNodeForTask(task: AgentTask, peers: List<AgentNode>): AgentNode? {
        val candidates = peers
            .filter { it.status != NodeStatus.OFFLINE }
            .filter { it.batteryLevel >= MIN_BATTERY_PCT }
            .filter { task.requiredCapabilities.all { cap -> cap in it.capabilities } }

        if (candidates.isEmpty()) {
            Timber.d("[NodeRouter] No eligible peers for task '${task.title}'")
            return null
        }

        val scored = candidates.map { node ->
            node to scoreNode(node, task)
        }

        scored.sortedByDescending { it.second }.forEach { (node, score) ->
            Timber.d("[NodeRouter] Peer ${node.displayName}: score=${"%.1f".format(score)}")
        }

        val best = scored.maxByOrNull { it.second }?.first
        Timber.i("[NodeRouter] Best peer for '${task.title}': ${best?.displayName ?: "none"}")
        return best
    }

    /**
     * Compute a composite routing score for [node] relative to [task].
     *
     * Exposed as non-private for unit testing.
     */
    fun scoreNode(node: AgentNode, task: AgentTask): Double {
        var score = 0.0

        // Battery component — linear, 0–40 pts
        score += node.batteryLevel * WEIGHT_BATTERY

        // Agent Mode bonus — owner-away nodes are more available
        if (node.status == NodeStatus.AGENT_MODE || !node.isOwnerPresent) {
            score += WEIGHT_AGENT_MODE
        }

        // Required capability match — each matched cap adds 4 pts
        val matchedCaps = task.requiredCapabilities.count { it in node.capabilities }
        score += matchedCaps * WEIGHT_CAP_MATCH

        // Extra capabilities bonus — versatile nodes preferred as fallbacks
        val extraCaps = (node.capabilities.size - task.requiredCapabilities.size)
            .coerceAtLeast(0)
        score += extraCaps * WEIGHT_EXTRA_CAPS

        return score
    }

    /**
     * Rank all peers by their general utility score regardless of a specific
     * task. Useful for pre-emptive mesh topology decisions (e.g., which node
     * should be the gossip hub).
     */
    fun rankPeers(peers: List<AgentNode>): List<AgentNode> {
        return peers
            .filter { it.status != NodeStatus.OFFLINE }
            .sortedByDescending { node ->
                (node.batteryLevel * WEIGHT_BATTERY) +
                (if (!node.isOwnerPresent) WEIGHT_AGENT_MODE else 0.0) +
                (node.capabilities.size * WEIGHT_EXTRA_CAPS)
            }
    }
}
