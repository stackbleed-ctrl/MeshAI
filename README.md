# 🕸️ MeshAI

### Decentralized Autonomous AI Agents with Multi-Radio Mesh Networking

> **Every Android phone is a node. Every node is an agent. The mesh never sleeps.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-16%2B-brightgreen)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-31-blue)](https://developer.android.com/about/versions/12)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple)](https://kotlinlang.org)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue)](https://developer.android.com/about/versions/16)

---

## Overview

MeshAI transforms Android devices into **autonomous AI agent nodes** that self-organize into a resilient, multi-radio mesh network — no internet, no central server, no cloud. Phones communicate with phones, run on-device LLMs, decompose goals into tasks, execute tools, and collaborate across the mesh 24/7.

### Why Android-only?

iOS sandboxing prohibits background Wi-Fi Direct sockets, persistent BLE GATT servers, background peer-to-peer socket listeners, and foreground service semantics for mesh networking. Apple explicitly blocks the low-level APIs required for true mesh. **Android is the only mobile platform where this architecture is possible.**

---

## Features

### 🌐 Multi-Radio Mesh Networking

Three radio layers form an adaptive hybrid transport:

| Layer | Technology | Use Case |
|---|---|---|
| **Tier 1** | Meshrabiya (Wi-Fi Direct multi-hop) | High-bandwidth multi-hop routing, virtual IPs, TCP/UDP |
| **Tier 2** | Google Nearby Connections (BLE + Wi-Fi) | Peer discovery, cross-device data relay |
| **Tier 3** | BLE GATT | Ultra-low-power presence beaconing, small payloads |

The network stack auto-selects the best available transport per peer. Mesh → Nearby → Cellular/Wi-Fi fallback is handled transparently. All mesh payloads are encrypted using the **Noise protocol**.

### 🤖 Autonomous Agent Core

The agent runtime is built on three cooperating subsystems:

**`ReActLoop`** — The heart of each node. Runs a Reasoning + Acting loop capped at 12 steps:
```
Thought → Action (tool call) → Observation → Thought → ... → FINAL ANSWER
```
- Builds structured context per step (task, memory, available tools, node capabilities, battery, owner presence)
- Parses `Action: <tool>` / `Action Input: <json>` from LLM output
- Persists each step's observation to `AgentMemory` for cross-session recall
- Exposes `loopState: StateFlow<LoopState>` for UI binding

**`GoalEngine`** — Decomposes free-text user goals into typed `AgentTask` objects via LLM. Supported task types:

`SEND_SMS` · `ANSWER_CALL` · `TAKE_PHOTO` · `GET_LOCATION` · `MONITOR` · `RESPOND_TO_MESSAGE` · `LLM_REASONING` · `DELEGATE` · `CUSTOM`

Falls back to a single `CUSTOM` task if the LLM returns unparseable JSON.

**`OwnerPresenceDetector`** — Polls every 60 seconds to determine whether the owner is available. Owner is declared **unavailable** when any of the following are true:
- Screen has been off for > 30 minutes (persisted via DataStore across reboots)
- Do Not Disturb is in `INTERRUPTION_FILTER_NONE` or `INTERRUPTION_FILTER_ALARMS` state
- User has explicitly toggled **Agent Mode** from the dashboard

When unavailable, the `ReActLoop` becomes fully proactive — answering messages, screening calls, and completing queued tasks autonomously.

### 🧠 On-Device LLM Engine

`LlmEngine` implements tiered inference with automatic fallback:

| Tier | Backend | Requirement |
|---|---|---|
| **1 — Gemini Nano** | Android AICore (ML Kit GenAI) | Pixel 8+ / Android 16+ AICore device |
| **2 — Gemma 2B** | MediaPipe LLM Inference | Any Android 12+ device with model file |
| **3 — Degraded** | Graceful no-op response | Fallback when no model is available |

Gemma prompt format uses the standard `<start_of_turn>` / `<end_of_turn>` instruction template. Model file: `gemma2b-it-cpu-int4.bin` placed in the app's external files directory.

> **Gemini Nano binding:** The AICore stub is present but disabled pending stable production API surface. See the inline comments in `LlmEngine.kt` for the binding pattern to follow when deploying on Pixel 8+.

### 🛠️ Device Tool Use

The `ToolRegistry` dispatches tool calls from the `ReActLoop` to registered tool implementations:

| Tool | File | Android API |
|---|---|---|
| Send SMS | `SmsTool.kt` | `SmsManager` |
| Call screening | `CallTool.kt` | `CallScreeningService`, `ConnectionService` |
| Camera capture | `CameraTool.kt` | CameraX |
| GPS location | `LocationTool.kt` | Fused Location Provider |
| Notification R/W | `NotificationTool.kt` | `NotificationListenerService` |

Tools return a typed `ToolResult` with a `.summary` string consumed by the `ReActLoop` as the observation.

### 🔒 Security Model

| Layer | Mechanism |
|---|---|
| Mesh transport | Noise protocol (session encryption per peer) |
| Local storage | Jetpack Security · AES-256-GCM via `EncryptedSharedPreferences` / `EncryptedFile` |
| Cross-node key exchange | Age encryption |
| Permissions | Gated at runtime with graceful degradation on denial |

### ⚙️ Lifecycle & Persistence

- **`AgentForegroundService`** — Persistent foreground service providing the coroutine scope for all agent loops. Survives app background and system memory pressure.
- **`AgentWorker`** — WorkManager `CoroutineWorker` for deferrable, constraint-aware background tasks.
- **`BootReceiver`** — `BOOT_COMPLETED` / `QUICKBOOT_POWERON` receiver auto-restarts the agent service after device reboot.
- **`MeshAIDatabase`** — Room database backing `AgentMemory`, `AgentTask`, and `AgentNode` persistence.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                             UI Layer                                 │
│   Jetpack Compose · Material 3 · DashboardScreen · MeshAINavGraph   │
├─────────────────────────────────────────────────────────────────────┤
│                         Agent Core Layer                             │
│        ReActLoop · GoalEngine · AgentMemory · OwnerPresenceDetector  │
├────────────────┬───────────────────────────┬────────────────────────┤
│   LLM Layer    │        Tool Layer          │      Mesh Layer        │
│                │                            │                        │
│  LlmEngine     │  ToolRegistry              │  MeshNetwork           │
│  ├ Gemini Nano │  ├ SmsTool                 │  ├ MeshrabiyaLayer     │
│  ├ Gemma 2B    │  ├ CallTool                │  ├ NearbyLayer         │
│  └ Degraded    │  ├ CameraTool              │  └ BleGattLayer        │
│                │  ├ LocationTool            │                        │
│                │  └ NotificationTool        │  MeshEncryption        │
│                │                            │  MeshMessage           │
├────────────────┴───────────────────────────┴────────────────────────┤
│                       Infrastructure Layer                           │
│  Room (MeshAIDatabase) · DataStore · Hilt (AppModule)               │
│  Coroutines · Flow · WorkManager · ForegroundService · BootReceiver  │
└─────────────────────────────────────────────────────────────────────┘
```

**Dependency injection** is handled entirely by Hilt. `AppModule` provides the coroutine scope, DataStore instance, and all singleton bindings. `LlmEngine`, `ReActLoop`, `GoalEngine`, `ToolRegistry`, and `OwnerPresenceDetector` are all `@Singleton` and injected throughout.

---

## Project Structure

```
MeshAI/
├── agent/
│   ├── AgentNode.kt               # Node identity, capabilities, battery, owner state
│   ├── AgentTask.kt               # Task model: title, description, TaskType, TaskPriority
│   ├── AgentMemory.kt             # Short-term + persistent key-value memory
│   ├── AgentRepository.kt         # Repository layer for Room + DataStore
│   ├── AgentWorker.kt             # WorkManager CoroutineWorker
│   ├── AgentForegroundService.kt  # Persistent foreground service + coroutine scope
│   ├── ReActLoop.kt               # Core Thought→Action→Observation reasoning engine
│   ├── GoalEngine.kt              # LLM-based goal decomposition → AgentTask list
│   └── OwnerPresenceDetector.kt   # Screen-off / DND / Agent Mode detection
│
├── llm/
│   └── LlmEngine.kt               # Tiered inference: Gemini Nano → Gemma → Degraded
│
├── mesh/
│   ├── MeshNetwork.kt             # Unified mesh abstraction & routing
│   ├── MeshMessage.kt             # Encrypted message data model
│   ├── MeshEncryption.kt          # Noise protocol session encryption
│   ├── MeshrabiyaLayer.kt         # Wi-Fi Direct multi-hop mesh via Meshrabiya
│   ├── NearbyLayer.kt             # Google Nearby Connections BLE+Wi-Fi layer
│   └── BleGattLayer.kt            # BLE GATT low-power discovery & beaconing
│
├── tools/
│   ├── ToolRegistry.kt            # Tool registration + dispatch for ReActLoop
│   ├── SmsTool.kt                 # SmsManager send/receive
│   ├── CallTool.kt                # CallScreeningService + ConnectionService
│   ├── CameraTool.kt              # CameraX photo capture
│   ├── LocationTool.kt            # Fused Location Provider GPS
│   └── NotificationTool.kt        # NotificationListenerService R/W
│
├── data/
│   ├── MeshAIDatabase.kt          # Room database definition
│   └── AppModule.kt               # Hilt DI module
│
├── ui/
│   ├── DashboardScreen.kt         # Compose mesh map + agent status dashboard
│   ├── DashboardViewModel.kt      # ViewModel for dashboard state
│   ├── OtherScreens.kt            # Task queue, settings, node detail screens
│   ├── MeshAINavGraph.kt          # Compose navigation graph
│   ├── Theme.kt                   # Material 3 colour scheme
│   └── Typography.kt              # Type scale
│
├── MainActivity.kt
├── MeshAIApp.kt                   # Application class + Hilt entry point
├── BootReceiver.kt                # Auto-restart on BOOT_COMPLETED
├── AndroidManifest.xml
│
└── test/
    ├── ReActLoopTest.kt           # Unit tests for the reasoning loop
    └── MeshEncryptionTest.kt      # Unit tests for Noise protocol encryption
```

---

## Setup

### Prerequisites

- **Android Studio** Ladybug 2024.2+ or Meerkat 2025.1+
- **Android device** running Android 12+ (SDK 31); Android 16 recommended for full feature set
- **Wi-Fi Direct** support required for Meshrabiya mesh features
- **Gemini Nano** requires Pixel 8+ or a device with AICore; other devices use the Gemma MediaPipe fallback

### Build & Run

```bash
git clone https://github.com/stackbleed-ctrl/MeshAI.git
cd MeshAI
# Open in Android Studio, sync Gradle, then:
./gradlew assembleDebug
./gradlew installDebug
```

### Gemma Model Setup

The Gemma fallback requires the model file to be pushed to the device:

```bash
# Download gemma2b-it-cpu-int4.bin from https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference
adb push gemma2b-it-cpu-int4.bin \
  /sdcard/Android/data/com.meshai/files/gemma2b-it-cpu-int4.bin
```

The `LlmEngine` reads from `context.getExternalFilesDir(null)` — no root required.

### Required Permissions

All permissions are requested at runtime. Some require manual Settings grants:

| Permission | Grant Method | Used By |
|---|---|---|
| `SEND_SMS`, `RECEIVE_SMS` | Runtime dialog | `SmsTool` |
| `MANAGE_OWN_CALLS` | Runtime dialog | `CallTool` |
| `ACCESS_FINE_LOCATION` | Runtime dialog | `LocationTool` |
| `CAMERA` | Runtime dialog | `CameraTool` |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Settings → Notifications → App Access | `NotificationTool` |
| `BIND_CALL_SCREENING_SERVICE` | Settings → Phone → Call Screening | `CallTool` |
| `NEARBY_WIFI_DEVICES` | Runtime dialog | `NearbyLayer` |
| `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` | Runtime dialog | `BleGattLayer` |

Agent Mode and all tool use require explicit user consent. The app gracefully degrades if any permission is denied.

---

## Running Tests

```bash
# Unit tests (ReActLoopTest, MeshEncryptionTest)
./gradlew test

# Instrumented tests on device
./gradlew connectedAndroidTest
```

---

## Key Design Decisions

**Why Meshrabiya over Wi-Fi Direct raw sockets?**
Meshrabiya provides true multi-hop routing with virtual IP assignment and TCP/UDP socket support over Wi-Fi Direct, removing the 2-device limit and enabling a proper mesh topology.

**Why a tiered LLM strategy?**
On-device inference is hardware-dependent. Graceful degradation means MeshAI is functional on any Android 12+ device even without a downloaded model — it reports inability and queues the task rather than crashing.

**Why ReAct over a simpler prompt loop?**
The Thought/Action/Observation structure forces the model to reason explicitly before acting, produces auditable step-by-step traces stored in `AgentMemory`, and naturally handles multi-step tasks (monitor → detect → alert → confirm) with tool use at each step.

**Why `ForegroundService` + `WorkManager` together?**
The foreground service provides a persistent coroutine scope and keeps the mesh connections alive while the owner is away. WorkManager handles constraint-aware deferrable tasks (e.g. "check inventory when on Wi-Fi") with guaranteed execution even after process death.

---

## Roadmap

- [ ] Gemini Nano AICore binding (pending stable Android 16 GA API surface)
- [ ] Mesh topology map visualization in `DashboardScreen`
- [ ] Cross-node task delegation via `MeshNetwork` (`DELEGATE` task type)
- [ ] Shared encrypted mesh knowledge base (synchronized `AgentMemory` across nodes)
- [ ] Wake-word detection via `AudioRecord` for hands-free agent activation
- [ ] CI/CD pipeline with GitHub Actions (build, lint, unit tests)
- [ ] APK release artifact via GitHub Releases

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). PRs welcome — especially for Gemini Nano AICore binding, Meshrabiya topology improvements, and new `ToolRegistry` implementations.

---

## Disclaimer

MeshAI is a research and experimental project. Autonomous device control — including call answering, SMS sending, and notification access — requires explicit user consent at every step. Always comply with local telecommunications laws regarding automated communications and recording. The authors are not responsible for misuse.

---

## License

MIT License — see [LICENSE](LICENSE).

---

*Built with Kotlin · Jetpack Compose · Hilt · Room · MediaPipe · Meshrabiya · Noise Protocol*
