# 🕸️ MeshAI — Decentralized Autonomous AI Agents with Multi-Radio Mesh Networking

> **Every Android phone is a node. Every node is an agent. The mesh never sleeps.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-16%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple)](https://kotlinlang.org)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue)](https://developer.android.com/about/versions/16)
[![Build](https://github.com/yourusername/MeshAI/actions/workflows/ci.yml/badge.svg)](https://github.com/yourusername/MeshAI/actions)

---

## 📖 Overview

MeshAI transforms Android devices into **autonomous AI agent nodes** that self-organize into a resilient, multi-radio mesh network. No internet required. No central server. Just phones talking to phones, running local LLMs, completing tasks, and collaborating — 24/7.

**Why Android-only?**
iOS sandboxing prevents background Wi-Fi Direct usage, peer-to-peer socket servers, persistent background services, and low-level BLE GATT server operation. Apple explicitly blocks the APIs needed for true mesh networking. **Android is the only mobile platform where this is possible.**

---

## ✨ Features

### 🌐 Multi-Radio Mesh Network
- **Meshrabiya** — true multi-hop Wi-Fi mesh with virtual IPs, TCP/UDP sockets, and WPA3 encryption
- **Google Nearby Connections** — Bluetooth + Wi-Fi peer discovery and data transfer
- **BLE GATT** — ultra-low-power discovery and small payload beaconing
- Automatic hybrid routing: Mesh → Nearby → Cellular/Wi-Fi fallback
- Offline-first: full functionality with zero internet

### 🤖 Autonomous AI Agent Core
- **Gemini Nano** via Android AICore (on-device, no API key)
- **Gemma 2B/7B** via MediaPipe LLM Inference (fallback)
- **ReAct-style reasoning loop**: Think → Act → Observe → Repeat
- Short-term memory + shared encrypted mesh knowledge base
- Persistent `ForegroundService` + `WorkManager` for 24/7 operation

### 🛠️ Device Tool Use
Agents can autonomously:
- 📱 Send SMS via `SmsManager`
- 📞 Answer/screen calls via `CallScreeningService` + `ConnectionService`
- 🔔 Read & respond to notifications via `NotificationListenerService`
- 📷 Access camera, GPS, accelerometer, microphone
- 🗂️ Delegate tasks across the mesh based on battery/capability

### 🧠 Human Hand-off & Autonomy
- **Owner Unavailable** detection: screen-off >30 min, DND mode, low battery, or explicit toggle
- Agents proactively complete queued tasks, respond to messages and calls on your behalf
- Define high-level goals: *"Monitor front door. Alert me if motion. Order groceries weekly."*
- Agents decompose goals into subtasks and execute across the mesh

### 🔒 Security
- Noise protocol for mesh message encryption
- Jetpack Security (AES-256-GCM) for local storage
- Age encryption for cross-node key exchange
- Permission-gated with graceful degradation

---

## 📸 Screenshots

| Mesh Map | Agent Dashboard | Task Queue |
|----------|----------------|------------|
| *(screenshot placeholder)* | *(screenshot placeholder)* | *(screenshot placeholder)* |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│   Jetpack Compose · Material 3 · Dashboard · Mesh Map    │
├─────────────────────────────────────────────────────────┤
│                    Agent Core Layer                      │
│   ReAct Loop · Goal Decomposition · Memory · Planning    │
├──────────────┬──────────────────────┬───────────────────┤
│   LLM Layer  │     Tool Layer       │   Mesh Layer       │
│  Gemini Nano │  SMS · Calls · Notif │  Meshrabiya        │
│  Gemma 2B/7B │  Camera · Location   │  Nearby Conn.      │
│  MediaPipe   │  Sensors · Shell     │  BLE GATT          │
├──────────────┴──────────────────────┴───────────────────┤
│                   Infrastructure Layer                   │
│   Room · DataStore · Hilt · Coroutines · Flow            │
│   Jetpack Security · Noise Protocol · WorkManager        │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Setup

### Prerequisites
- Android Studio Ladybug 2024.2+ (or Meerkat 2025.1+)
- Android device running **Android 12+** (SDK 31+); Android 16 recommended
- Device must support **Wi-Fi Direct** for Meshrabiya mesh features
- Gemini Nano requires **Pixel 8+** or compatible AICore device; other devices use Gemma fallback

### Build & Run

```bash
git clone https://github.com/yourusername/MeshAI.git
cd MeshAI
# Open in Android Studio, sync Gradle, run on device
./gradlew assembleDebug
```

### Required Permissions
Grant all permissions on first launch. Agent Mode requires:
- `MANAGE_OWN_CALLS` — call screening
- `BIND_NOTIFICATION_LISTENER_SERVICE` — notification access (grant in Settings)
- `BIND_CALL_SCREENING_SERVICE` — call screening (grant in Settings)

---

## 🗺️ Project Structure

```
MeshAI/
├── app/
│   ├── src/main/
│   │   ├── java/com/meshai/
│   │   │   ├── agent/          # ReAct loop, goal engine, memory
│   │   │   ├── mesh/           # Meshrabiya, Nearby, BLE networking
│   │   │   ├── tools/          # SMS, calls, camera, location tools
│   │   │   ├── llm/            # LLM abstraction, Gemini Nano, Gemma
│   │   │   ├── service/        # Foreground service, WorkManager
│   │   │   ├── data/           # Room DB, DataStore, repositories
│   │   │   ├── security/       # Noise protocol, encryption
│   │   │   ├── ui/             # Compose screens, ViewModels
│   │   │   └── di/             # Hilt modules
│   │   ├── res/                # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── .github/workflows/ci.yml
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📄 License

MIT License — see [LICENSE](LICENSE).

---

## ⚠️ Disclaimer

MeshAI is a research/experimental project. Autonomous device control (call answering, SMS sending) requires careful user consent. Always comply with local laws regarding automated communications. The authors are not responsible for misuse.
