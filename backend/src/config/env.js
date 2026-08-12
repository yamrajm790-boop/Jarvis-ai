const dotenv = require('dotenv');
const path = require('path');

// Load .env if present
dotenv.config({ path: path.join(__dirname, '../../.env') });

module.exports = {
  port: process.env.PORT || 10000,
  groqApiKey: process.env.GROQ_API_KEY || '',
  groqModel: process.env.GROQ_MODEL || 'llama-3.3-70b-versatile',
  deviceToken: process.env.DEVICE_TOKEN || 'jarvis_secure_personal_token_12345',
  clientOrigin: process.env.CLIENT_ORIGIN || '*'
};
