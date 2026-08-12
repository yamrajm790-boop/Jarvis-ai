const express = require('express');
const http = require('http');
const cors = require('cors');
const helmet = require('helmet');
const env = require('./config/env');
const healthRoute = require('./routes/health');
const chatRoute = require('./routes/chat');
const { apiLimiter } = require('./middleware/security');
const setupWebSocketServer = require('./websocket/socketServer');

const app = express();
const server = http.createServer(app);

// Security and middleware
app.use(helmet());
app.use(cors({ origin: env.clientOrigin || '*' }));
app.use(express.json({ limit: '1mb' }));
app.use('/api/', apiLimiter);

// Routes
app.use('/health', healthRoute);
app.use('/api/chat', chatRoute);

// Setup WebSocket server
setupWebSocketServer(server);

// Start listening
const PORT = env.port;
server.listen(PORT, () => {
  console.log(`================================================`);
  console.log(`  JARVIS AI Voice Assistant Backend Running`);
  console.log(`  Port: ${PORT}`);
  console.log(`  Groq Model: ${env.groqModel}`);
  console.log(`================================================`);
});
