# JARVIS Personal Voice Assistant Backend

Node.js + Express + WebSocket backend proxying voice queries to the **Groq API** with strict structured tool validation for your personal Android device.

## Features
- **Groq API Integration**: Fast Llama 3.3 70B AI processing.
- **Strict Tool Output**: Output format validation prevents arbitrary code execution.
- **WebSocket & REST APIs**: WSS at `/ws` and REST at `/api/chat`.
- **Render Ready**: Included `render.yaml` with zero-config deployment on Render.
- **Device Security Token**: Personal auth token protection.

## Setup & Running Locally

1. Install dependencies:
   ```bash
   cd backend
   npm install
   ```
2. Create `.env` from `.env.example`:
   ```bash
   cp .env.example .env
   ```
3. Set your `GROQ_API_KEY`:
   ```env
   GROQ_API_KEY=gsk_your_groq_api_key
   PORT=10000
   DEVICE_TOKEN=jarvis_secure_personal_token_12345
   ```
4. Start server:
   ```bash
   npm start
   ```

## Deploying to Render

1. Push this repository to GitHub.
2. In Render Dashboard, click **New +** -> **Blueprint**.
3. Select your repository. Render will automatically detect `render.yaml`.
4. Fill in `GROQ_API_KEY` under Environment Variables in Render.
5. Deploy! Copy your service URL (e.g., `https://jarvis-backend-xyz.onrender.com`) into the JARVIS Android Settings screen.
