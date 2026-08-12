# JÁRVIS AI Voice Assistant for Android

A complete personal JARVIS-style AI voice assistant Android application built with **Kotlin**, **Jetpack Compose**, and a dedicated **Node.js/Express Render Backend** leveraging the **Groq API**.

---

## 🌟 Key Features & Visual Design
- **Futuristic HUD UI**:
  - Central animated AI Orb (`JarvisOrb`) with glowing concentric rings, rotating arcs, and pulsing states (IDLE, LISTENING, PROCESSING, ACTING, SPEAKING, ERROR).
  - Dynamic audio frequency waveform (`JarvisWaveform`) reacting to real-time speech and microphone levels.
  - Dark glassmorphic aesthetic matching the official JARVIS design reference.
- **Voice & Speech**:
  - Speech-to-Text (`SpeechRecognizerManager`) and Text-to-Speech (`TTSManager`).
  - Configurable wake word ("Hey Jarvis") and fallback Push-to-Talk button.
- **Local Command Fast-Path**:
  - Instant local parser for device controls (`volume up/down`, `pause music`, `play music`, `go home`, `go back`, `battery status`, `settings`) without needing network roundtrips.
- **Android Device Tool Executor**:
  - Secure tool calls (`open_app`, `open_url`, `search_web`, `set_volume`, `take_screenshot`, `set_alarm`, etc.).
  - Accessibility Service (`JarvisAccessibilityService`) integration for system navigation and screenshots.
- **Command Hub & History**:
  - Full History screen with time filters and replay capabilities.
  - Command Hub with built-in actions and local custom automation routines.
- **Strict Groq API Security**:
  - Groq API keys reside strictly on the Node.js backend.
  - Zero hardcoded keys in the Android app.

---

## 🚀 Setup & Deployment

### 1. Deploy Node.js Backend to Render
1. Go to [Render.com](https://render.com) and create a new **Web Service** pointing to the `backend/` directory.
2. Set Environment Variables:
   - `GROQ_API_KEY`: Your Groq API Key from [Groq Console](https://console.groq.com).
   - `GROQ_MODEL`: `llama-3.3-70b-versatile`
   - `PORT`: `10000`
   - `DEVICE_TOKEN`: `jarvis_secret_device_token_2026`
3. Deploy and obtain your Render service URL (e.g. `https://jarvis-backend.onrender.com`).

### 2. Configure Android App
1. Open the JARVIS app on your Android device or streaming emulator.
2. Navigate to **Settings** → **Render Backend URL**.
3. Enter your deployed backend URL and tap **Update Server**.
4. Grant requested Microphone and Phone permissions.
5. Enable **Jarvis Accessibility Service** in System Settings for home/back gestures and screenshots.

---

## 📁 Repository Structure

```
/app
├── backend/                  # Node.js + Express Backend for Render
│   ├── src/
│   │   ├── server.js         # Main Express Server & WebSocket
│   │   ├── config/env.js     # Config & Environment Parser
│   │   ├── routes/           # /health and /api/chat Routes
│   │   ├── services/         # Groq API, Agent, and Memory Services
│   │   ├── tools/            # Tool Registry and Validator
│   │   └── websocket/        # WebSocket Streaming Server
│   ├── package.json
│   ├── render.yaml
│   └── README.md
└── src/main/java/com/example/  # Android Application
    ├── MainActivity.kt        # Entry Point & Navigation
    ├── JarvisApplication.kt   # App Singleton Context
    ├── accessibility/         # Accessibility Service
    ├── ai/                    # Groq API Client & Fallback Engine
    ├── data/                  # Room Database & Local Preferences
    ├── tools/                 # Tool Executor & Local Command Parser
    ├── ui/                    # Jetpack Compose Views, AI Orb & Waveform
    └── voice/                 # Speech Recognizer, Wake Word & TTS
```
