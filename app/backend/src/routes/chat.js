const express = require('express');
const router = express.Router();
const agentService = require('../services/agentService');

router.post('/chat', async (req, res) => {
  try {
    const { message, deviceId = 'default_device' } = req.body;

    if (!message || typeof message !== 'string' || !message.trim()) {
      return res.status(400).json({
        type: 'error',
        message: 'Invalid message provided, sir.'
      });
    }

    const result = await agentService.processRequest(deviceId, message.trim());
    return res.json(result);
  } catch (error) {
    console.error('Chat endpoint error:', error);
    return res.status(500).json({
      type: 'error',
      message: 'Sorry sir, AI service encountered an internal error.'
    });
  }
});

module.exports = router;
