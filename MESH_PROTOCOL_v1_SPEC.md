# MeshAI — Mesh Protocol v1 Specification
_Revision: v1.0 | Status: DRAFT | Last updated: 2025_

---

## 1. Overview

The Mesh Protocol v1 defines the **complete contract** for inter-node communication
in a MeshAI deployment. Every byte transmitted between Android devices MUST be
wrapped in a `MeshEnvelope`.

**Design goals:**
- Strict version negotiation (nodes reject incompatible peers)
- Type-tagged dispatch (no content sniffing)
- Loop-safe multi-hop routing (TTL + hop trace)
- Economy-aware (cost/latency embedded per hop)
- Forward-compatible (new `EnvelopeType` values are ignored by old nodes)

---

## 2. Envelope Structure

```
MeshEnvelope {
  version:            Int         // MUST be 1 for this spec
  envelopeId:         UUID        // dedup + ack correlation
  type:               EnvelopeType
  originNodeId:       String
  destinationNodeId:  String?     // null = broadcast
  hopTrace:           List<String>// appended per hop
  ttl:                Int         // decremented per hop; drop when ≤ 0
  payload:            String      // JSON-serialised inner type
  metadata:           Map<String,String>
  accumulatedCostUsd: Double
  originTimestampMs:  Long
  noiseSessionId:     String      // stub; populated by SEC-001
  payloadHmac:        String      // stub; populated by SEC-001
}
```

---

## 3. Envelope Types

| Type | Payload | Direction |
|------|---------|-----------|
| `TASK_DELEGATE` | `AgentTask` | origin → peer |
| `TASK_RESULT` | `TaskResult` | peer → origin |
| `NODE_ADVERTISE` | `AgentNode` | broadcast |
| `CAPABILITY_QUERY` | `CapabilityQuery` | unicast |
| `CAPABILITY_RESPONSE` | `List<Capability>` | unicast |
| `TELEMETRY_EVENT` | `MeshEvent` | peer → coordinator |
| `CONTROL_SIGNAL` | `ControlSignal` | coordinator → any |
| `ACK` | envelopeId string | any → any |
| `NACK` | `ErrorPayload` | any → any |

---

## 4. Routing Rules

1. Receiver checks `version <= MAX_SUPPORTED_VERSION`. Reject with NACK(VERSION_MISMATCH) if false.
2. Receiver checks `ttl > 0`. Drop silently if expired.
3. Receiver checks `hopTrace` does not contain `thisNodeId`. Drop with NACK(LOOP_DETECTED) if found.
4. Receiver stamps own nodeId onto `hopTrace` and decrements `ttl` before forwarding.
5. If `destinationNodeId == thisNodeId` or `destinationNodeId == null` (broadcast): dispatch to EnvelopeDispatcher.
6. Else: forward via TransportManager.

---

## 5. Transport Priority

```
Priority 1: Meshrabiya (Wi-Fi Direct multi-hop)
  - Max bandwidth, multi-hop capable, requires NEARBY_WIFI_DEVICES (API 33+)

Priority 2: Google Nearby Connections (BLE + Wi-Fi P2P cluster)
  - ~100m range, good fallback, requires ACCESS_FINE_LOCATION

Priority 3: BLE GATT
  - Ultra-low-power, short range, limited payload (~512 bytes)
  - Used for node discovery beaconing even when higher transports are unavailable
```

---

## 6. Capability Advertisement

Nodes MUST broadcast a `NODE_ADVERTISE` envelope:
- On startup
- Every 15 seconds (keepalive)
- Immediately after any capability change (battery drop, tool registration)

Stale threshold: 30 seconds.  Receivers evict nodes not re-advertised within this window.

---

## 7. Security (SPEC_REF: SEC-001 — FUTURE)

Current status: **stub only**. `noiseSessionId` and `payloadHmac` fields are empty.

Planned implementation:
- Noise_XX handshake for peer authentication + forward secrecy
- HMAC-SHA256 over (envelopeId ‖ type ‖ payload) for integrity
- Per-session key rotation every 60 minutes

---

## 8. Economy Layer

Each hop SHOULD append its execution cost to `accumulatedCostUsd`.
The origin can set a `maxCostUsd` constraint in `AgentTask.constraints`.
`TransportManager` MUST NOT forward an envelope if `accumulatedCostUsd > task.constraints.maxCostUsd`.

---

## 9. Error Codes

| Code | Meaning |
|------|---------|
| `VERSION_MISMATCH` | Receiver cannot handle envelope version |
| `TTL_EXCEEDED` | Envelope expired in transit |
| `POLICY_DENIED` | PolicyEngine rejected the task |
| `LOOP_DETECTED` | Hop trace cycle detected |
| `UNKNOWN_TYPE` | Unrecognised EnvelopeType |
| `INTERNAL_ERROR` | Unexpected receiver failure |

---

## 10. Versioning

Minor additions (new EnvelopeType, new metadata keys) increment `version` by 0 (no change needed).
Breaking changes (field removal, type rename) MUST increment `version`.
Nodes advertise their `MAX_SUPPORTED_VERSION` in `NODE_ADVERTISE` envelopes.
