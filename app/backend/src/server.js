const http = require('http');
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const config = require('./config/env');
const healthRoutes = require('./routes/health');
const chatRoutes = require('./routes/chat');
const { securityMiddleware } = require('./middleware/security');
const { setupWebSocket } = require('./websocket/socketServer');

const app = express();
const server = http.createServer(app);

// Security Headers
app.use(helmet());

// CORS Configuration
app.use(cors({
  origin: config.clientOrigin === '*' ? true : config.clientOrigin,
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 60, // 60 requests per minute
  message: { type: 'error', message: 'Too many requests. Please slow down, sir.' }
});
app.use(limiter);

// Body Parsing
app.use(express.json({ limit: '100kb' }));

// Health Check (unauthenticated for Render monitors)
app.use('/', healthRoutes);

// Protected API Routes
app.use('/api', securityMiddleware, chatRoutes);

// Initialize WebSocket
setupWebSocket(server);

const PORT = config.port;
server.listen(PORT, () => {
  console.log(`==========================================`);
  console.log(`🤖 JÁRVIS Express Backend Running on Port ${PORT}`);
  console.log(`⚡ Groq Model Configured: ${config.groqModel}`);
  console.log(`==========================================`);
});
