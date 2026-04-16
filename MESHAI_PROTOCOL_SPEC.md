# MeshAI Protocol Specification

```
Spec-Version:     1.0.0
Protocol-Version: 1
Last-Updated:     2026-04-16
Compatible-Code:  MESHAI_PROTO_V1
```

> **Authority**: This document is the source of truth. Code must conform to it.  
> **Drift policy**: If spec and code disagree, code is wrong. File an issue immediately.

---

## NON-NEGOTIABLE INVARIANTS

These rules **cannot be weakened, bypassed, or overridden at runtime**. Violation is a spec breach.

| ID | Invariant |
|----|-----------|
| **INV-001** | `SafetyGate.evaluate()` is mandatory for every tool execution. No bypass path exists. |
| **INV-002** | SMS tool (`send_sms`) MUST NOT execute when the task's `origin` is `REMOTE`. |
| **INV-003** | `KillSwitch.halt()` terminates ALL execution paths — loop, tools, mesh dispatch. |
| **INV-004** | Every `MeshMessage` MUST carry a cryptographic signature and a replay-protected nonce. |
| **INV-005** | `ToolRegistry.executeTool()` MUST route through `ToolExecutionGuard`. Direct tool calls are forbidden. |
| **INV-006** | Owner-absent autonomous actions require explicit `AgentTask.ownerApproved = false` handling — never silently escalate. |
| **INV-007** | `ReActLoop` MUST NOT exceed `MAX_STEPS = 12` iterations per task. |
| **INV-008** | `MeshMessage.ttl` MUST NOT exceed 5 at creation time. |

---

## 1. Overview

MeshAI turns Android devices into autonomous AI agent nodes that self-organize into a resilient, multi-radio mesh network.

**Design principles:**

- Safety-first: every outbound action passes through a mandatory gate
- Offline-capable: full functionality with zero internet
- Graceful degradation: missing permissions yield `ToolResult.failure`, never a crash
- Spec-code traceability: every invariant above maps to a `SPEC_REF` comment in code

---

## 2. Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│   Jetpack Compose · Dashboard · Mesh Map                 │
├─────────────────────────────────────────────────────────┤
│                    Agent Core Layer                      │
│   ReActLoop · GoalEngine · AgentMemory · SafetyGate      │
├──────────────┬──────────────────────┬───────────────────┤
│   LLM Layer  │   Tool Layer         │   Mesh Layer       │
│  Gemini Nano │  ToolExecutionGuard  │  Meshrabiya        │
│  Gemma 2B/7B │  ToolRegistry        │  Nearby Conn.      │
│  MediaPipe   │  SMS·Calls·Notif     │  BLE GATT          │
│              │  Camera·Location     │                    │
├──────────────┴──────────────────────┴───────────────────┤
│                   Infrastructure Layer                   │
│   Room · DataStore · Hilt · Coroutines · Flow            │
│   Jetpack Security · Noise/Age Protocol · WorkManager    │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Safety Gate (SAFETY-*)

### SAFETY-001 — Universal Tool Gate
**Rule:** ALL tool executions MUST pass through `SafetyGate.evaluate()`.  
**Maps to:** `ToolExecutionGuard.execute()` → `SafetyGate.evaluate()`  
**Enforcement:** `ToolRegistry.executeTool()` calls `ToolExecutionGuard`, which calls `SafetyGate`.  
**Violation:** Any direct call to `AgentTool.execute()` that bypasses the guard.

### SAFETY-002 — Remote Origin Denial for SMS
**Rule:** `send_sms` MUST be denied when `taskOrigin == REMOTE`.  
**Maps to:** `SafetyGate.evaluate()` checks `ToolRequest.origin`  
**Rationale:** Prevents mesh nodes from sending SMS on behalf of remote, potentially compromised peers.  
**Violation assertion:**
```kotlin
// SPEC_REF: SAFETY-002
check(!(toolId == "send_sms" && request.origin == TaskOrigin.REMOTE)) {
    "SPEC VIOLATION: SAFETY-002 — DenyRemoteOriginForSMS breached"
}
```

### SAFETY-003 — KillSwitch
**Rule:** `KillSwitch.halt()` MUST terminate: ReActLoop, all tool execution, mesh dispatch.  
**Maps to:** `AgentForegroundService` observes `KillSwitch.isHalted`; `ReActLoop` checks it each iteration.  
**Cannot be reset** without an explicit owner gesture (UI toggle or device restart).

### SAFETY-004 — Owner-Absent Escalation Prevention
**Rule:** Autonomous actions taken while owner is absent MUST NOT silently escalate privileges.  
**Maps to:** `ReActLoop.buildSystemPrompt()` includes `OWNER PRESENT` field; `SafetyGate` checks it for irreversible actions.

---

## 4. Tool Execution Protocol (TOOL-*)

### TOOL-001 — Tool Registration
All tools MUST be registered in `ToolRegistry` to be callable. Ad-hoc tool instantiation in `ReActLoop` is forbidden.

### TOOL-002 — Input Validation
Each tool MUST validate its `jsonInput` and return `ToolResult.failure(reason)` on bad input — never throw an unhandled exception.

### TOOL-003 — Permission Gate
Tools MUST check Android permissions before execution and return a descriptive `ToolResult.failure` if not granted.

### TOOL-004 — Result Contract
Every tool MUST return a `ToolResult`. `success` = true only if the external action completed. The `summary` field MUST be human-readable.

---

## 5. ReAct Loop Protocol (LOOP-*)

### LOOP-001 — Step Limit
**Rule:** Maximum iterations per task is **12** (`MAX_STEPS`).  
**Enforcement:** Hard loop ceiling; returns graceful message on breach.

### LOOP-002 — Final Answer Signal
The loop terminates on `"FINAL ANSWER:"` prefix in LLM response. No other termination signal is valid.

### LOOP-003 — Tool Call Routing
The loop MUST invoke tools via `ToolRegistry.executeTool()`. Direct tool object calls are forbidden.

### LOOP-004 — Memory Persistence
Each tool observation MUST be stored in `AgentMemory` with key pattern `task_{id}_step_{n}`.

---

## 6. Mesh Message Protocol (MESH-*)

### MESH-001 — Signature Requirement
**Rule:** Every `MeshMessage` MUST be signed before transmission.  
**Maps to:** `MeshEncryption.sign(message)`  
**Violation assertion:**
```kotlin
// SPEC_REF: MESH-001
require(message.signature != null) {
    "SPEC VIOLATION: MESH-001 — unsigned message rejected"
}
```

### MESH-002 — Replay Protection
**Rule:** Every message MUST include a nonce. Received messages with a previously seen nonce MUST be silently dropped.  
**Maps to:** `MeshNetwork.inboundFilter()` checks nonce against `SeenNonceCache`.

### MESH-003 — TTL Enforcement
**Rule:** `MeshMessage.ttl` MUST be ≤ 5 at origin. Nodes MUST decrement TTL on forward; drop at 0.  
**Enforcement assertion:**
```kotlin
// SPEC_REF: MESH-003
require(message.ttl in 1..5) {
    "SPEC VIOLATION: MESH-003 — TTL out of range: ${message.ttl}"
}
```

### MESH-004 — Encryption in Transit
All mesh transport (Meshrabiya, Nearby, BLE) MUST encrypt payloads with the active session key before transmission.

---

## 7. Version Negotiation (VER-*)

### VER-001 — Protocol Version Declaration
All nodes MUST declare `MESHAI_PROTO_V1` compatibility in their `NODE_ANNOUNCEMENT` payload.

### VER-002 — Incompatible Node Rejection
Nodes receiving an announcement with an unknown `protocolVersion` MUST refuse task delegation to that node and log a warning.

---

## 8. Spec-Code Traceability Map

| Spec ID | Code Location | Enforcement Type |
|---------|--------------|-----------------|
| INV-001 / SAFETY-001 | `ToolExecutionGuard.execute()` | Runtime guard |
| INV-002 / SAFETY-002 | `SafetyGate.evaluate()` | Runtime assertion |
| INV-003 / SAFETY-003 | `KillSwitch` + `AgentForegroundService` | State check |
| INV-004 / MESH-001 | `MeshEncryption.sign()` + `MeshNetwork` | Runtime assertion |
| INV-005 / TOOL-001 | `ToolRegistry.executeTool()` | Structural enforcement |
| INV-006 / SAFETY-004 | `SafetyGate` + `ReActLoop.buildSystemPrompt()` | Logic gate |
| INV-007 / LOOP-001 | `ReActLoop.MAX_STEPS` | Compile-time constant |
| INV-008 / MESH-003 | `MeshMessage` init + `MeshNetwork` | Runtime assertion |

---

## 9. Test Coverage Requirements

Every invariant in Section 2 MUST have at least one corresponding unit test in `SpecInvariantsTest.kt`.  
Every `SPEC_REF` comment in code MUST appear in the test suite's coverage.

---

## 10. Spec Maintenance

- Any weakening of a NON-NEGOTIABLE INVARIANT requires a dedicated PR with `[SPEC-CHANGE]` prefix and explicit sign-off.
- Spec version MUST be incremented on any change to Sections 3–7.
- Code declaring `MESHAI_PROTO_V1` compatibility MUST satisfy all invariants in this version of the spec.
