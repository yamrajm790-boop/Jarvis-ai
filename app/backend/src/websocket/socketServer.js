const WebSocket = require('ws');
const agentService = require('../services/agentService');

function setupWebSocket(server) {
  const wss = new WebSocket.Server({ server, path: '/ws' });

  wss.on('connection', (ws) => {
    console.log('📱 Android JARVIS Client connected via WebSocket');

    ws.on('message', async (message) => {
      try {
        const data = JSON.parse(message.toString());
        if (data.type === 'chat' && data.message) {
          const result = await agentService.processRequest(data.deviceId || 'default_device', data.message);
          ws.send(JSON.stringify(result));
        } else {
          ws.send(JSON.stringify({ type: 'pong', timestamp: Date.now() }));
        }
      } catch (err) {
        ws.send(JSON.stringify({
          type: 'error',
          message: 'Failed to process WebSocket payload.'
        }));
      }
    });

    ws.on('close', () => {
      console.log('📱 Android JARVIS Client disconnected');
    });
  });

  return wss;
}

module.exports = {
  setupWebSocket
};
