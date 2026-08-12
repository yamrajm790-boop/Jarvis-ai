# JÁRVIS AI Voice Assistant for Android

[![JARVIS Android Build](../../actions/workflows/android-build.yml/badge.svg)](../../actions/workflows/android-build.yml)

A complete personal JARVIS-style AI voice assistant Android application built with **Kotlin**, **Jetpack Compose**, and a dedicated **Node.js/Express Render Backend** leveraging the **Groq API**.

---

## 📲 Download APK

1. Open the GitHub repository.
2. Open the **Actions** tab.
3. Select the latest successful **Android CI Build**.
4. Scroll down to the **Artifacts** section.
5. Click **JARVIS-Android-APK** to download the ZIP file.
6. Extract the downloaded ZIP archive on your device or computer.
7. Transfer and install `JARVIS-debug.apk` on your Android phone.

> **Note**: GitHub Actions build artifacts are temporary and retained for **14 days**. For persistent APK downloads, check the **Releases** tab on GitHub.

---

## ⚙️ Automated CI/CD Workflows

This repository features fully automated GitHub Actions workflows:

- **Debug CI Build (`.github/workflows/android-build.yml`)**: Automatically compiles the Android project on every push to `main`, pull request, or manual trigger, producing the `JARVIS-debug.apk` artifact.
- **Release Build (`.github/workflows/android-release.yml`)**: On manual `workflow_dispatch` trigger, builds a signed release APK (`JARVIS.apk`) using GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and optionally publishes a GitHub Release.

---

## 🌟 Key Features & Architecture

- **Futuristic HUD UI**:
  - Central animated AI Orb (`JarvisOrb`) with glowing concentric rings, rotating arcs, and pulsing status indicators.
  - Dynamic audio frequency waveform (`JarvisWaveform`) reacting to speech input.
  - Dark glassmorphic aesthetic matching the JARVIS design language.
- **Voice & Speech Capabilities**:
  - Real-time Speech-to-Text (`SpeechRecognizerManager`) and Text-to-Speech (`TTSManager`).
  - Configurable wake word ("Hey Jarvis") and fallback Push-to-Talk button.
- **Local Fast Command Engine**:
  - Instant local parser (`LocalCommandParser`) for device controls (`volume up/down`, `pause music`, `play music`, `go home`, `go back`, `battery status`, `settings`) executing locally without network latency.
- **System Access & Permissions Center**:
  - Dedicated Permissions Center UI (`PermissionsScreen.kt`) and Background Assistant Setup (`BackgroundSetupScreen.kt`) for total hands-free control.
  - Integrated Quick Settings Tile (`JarvisTileService`) for instant voice assistant invocation.
- **Strict Security Guarantee**:
  - Groq API keys reside strictly on the Render Node.js backend.
  - Zero hardcoded secrets in the Android application source code.

---

## 🚀 Setup & Backend Deployment

### 1. Deploy Node.js Backend to Render
1. Go to [Render.com](https://render.com) and create a new **Web Service** pointing to the `backend/` directory.
2. Set Environment Variables:
   - `GROQ_API_KEY`: Your Groq API Key from [Groq Console](https://console.groq.com).
   - `GROQ_MODEL`: `llama-3.3-70b-versatile`
   - `PORT`: `10000`
   - `DEVICE_TOKEN`: `jarvis_secret_device_token_2026`
3. Deploy and obtain your Render service URL (e.g. `https://jarvis-backend.onrender.com`).

### 2. Configure Android App
1. Open the JARVIS app on your Android device.
2. Navigate to **Settings** → **Render Backend URL**.
3. Enter your deployed backend URL and tap **Save Connection Settings**.
4. Grant required permissions in the **Permissions Center**.

---

## 📁 Repository Structure

```
JARVIS/
├── .github/
│   └── workflows/
│       ├── android-build.yml     # Automated CI Debug APK build
│       └── android-release.yml   # Signed Release APK & GitHub Release
├── app/                          # Main Android App Module
│   ├── src/main/
│   │   ├── java/com/example/     # App Source Code (UI, Voice, Tools, Service)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── backend/                      # Node.js + Express Groq API Service
│   ├── src/
│   ├── package.json
│   └── render.yaml
├── gradlew                       # Gradle Wrapper Executable
├── build.gradle.kts              # Top-level Gradle configuration
├── settings.gradle.kts           # Gradle Settings & Module inclusions
├── .gitignore
└── README.md
```
