const { WebSocketServer } = require('ws');
const env = require('../config/env');
const agentService = require('../services/agentService');

function setupWebSocketServer(httpServer) {
  const wss = new WebSocketServer({ server: httpServer, path: '/ws' });

  wss.on('connection', (ws, req) => {
    const urlParams = new URLSearchParams(req.url.split('?')[1]);
    const clientToken = urlParams.get('token') || req.headers['x-device-token'];

    if (env.deviceToken && clientToken !== env.deviceToken) {
      ws.send(JSON.stringify({ type: 'error', message: 'Unauthorized connection.' }));
      ws.close(1008, 'Unauthorized');
      return;
    }

    console.log('Client connected to JARVIS WebSocket');

    // Send welcome status
    ws.send(JSON.stringify({
      type: 'status',
      message: 'JARVIS WebSocket connection established, sir.'
    }));

    ws.on('message', async (data) => {
      try {
        const payload = JSON.parse(data.toString());

        if (payload.type === 'ping') {
          ws.send(JSON.stringify({ type: 'pong' }));
          return;
        }

        if (payload.type === 'user_command' || payload.message) {
          const userMessage = payload.message || payload.text;
          const history = payload.history || [];

          // Notify processing state
          ws.send(JSON.stringify({ type: 'state', state: 'processing' }));

          const result = await agentService.handleIncomingMessage({ message: userMessage, history });
          
          ws.send(JSON.stringify(result));
        }
      } catch (err) {
        console.error('WebSocket Message Error:', err);
        ws.send(JSON.stringify({
          type: 'response',
          message: 'Sorry sir, I failed to parse that command.'
        }));
      }
    });

    ws.on('close', () => {
      console.log('Client disconnected from JARVIS WebSocket');
    });
  });

  return wss;
}

module.exports = setupWebSocketServer;
