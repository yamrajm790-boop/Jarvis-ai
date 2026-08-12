# JÁRVIS Backend Service (Express + Groq API)

This is the official Node.js + Express backend service for the **JÁRVIS AI Voice Assistant** Android application, designed for single-click deployment to **Render**.

## Architecture & Principles
1. **Groq API Security**: The Groq API key exists ONLY on this backend service and is never embedded in the Android app.
2. **Structured Tool Calling**: Groq uses function calling to request valid Android tools (e.g. `open_app`, `set_volume`, `take_screenshot`).
3. **Model Configuration**: Model selection is controlled via `GROQ_MODEL` environment variable.
4. **Render Ready**: Includes `render.yaml` for automatic build and deployment.

---

## Deployment Instructions (Render)

1. Push this repository or the `backend` folder to GitHub.
2. Log into [Render Dashboard](https://dashboard.render.com).
3. Click **New +** → **Web Service**.
4. Connect your GitHub repository and set the Root Directory to `backend`.
5. Set the Environment Variables:
   - `GROQ_API_KEY`: Your Groq API Key (`gsk_...`) from [console.groq.com](https://console.groq.com).
   - `GROQ_MODEL`: `llama-3.3-70b-versatile`
   - `PORT`: `10000`
   - `DEVICE_TOKEN`: `jarvis_secret_device_token_2026`
6. Click **Create Web Service**.
7. Copy your deployed web service URL (e.g. `https://jarvis-backend.onrender.com`).
8. Paste this URL into the **JARVIS Android App** under **Settings → Render Backend URL**.

---

## Local Development

```bash
cd backend
npm install
cp .env.example .env
# Edit .env and enter your GROQ_API_KEY
npm start
```

Health check endpoint: `GET http://localhost:10000/health`
Chat API endpoint: `POST http://localhost:10000/api/chat`
WebSocket endpoint: `ws://localhost:10000/ws`
