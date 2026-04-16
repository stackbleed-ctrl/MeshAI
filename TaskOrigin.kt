package com.meshai.agent

/**
 * Describes where a task originated.
 *
 * SPEC_REF: SAFETY-002
 * Origin is used by SafetyGate to deny high-risk tool calls (e.g. SMS)
 * for tasks that arrived from remote mesh nodes.
 */
enum class TaskOrigin {
    /** Task was created locally by the device owner. */
    LOCAL,

    /** Task was delegated from another mesh node. */
    REMOTE
}
