require('dotenv').config();

module.exports = {
  groqApiKey: process.env.GROQ_API_KEY || '',
  groqModel: process.env.GROQ_MODEL || 'llama-3.3-70b-versatile',
  port: parseInt(process.env.PORT || '10000', 10),
  clientOrigin: process.env.CLIENT_ORIGIN || '*',
  deviceToken: process.env.DEVICE_TOKEN || 'jarvis_secret_device_token_2026'
};
