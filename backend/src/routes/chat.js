const express = require('express');
const router = express.Router();
const agentService = require('../services/agentService');
const { verifyDeviceToken } = require('../middleware/security');

router.post('/', verifyDeviceToken, async (req, res) => {
  try {
    const { message, history } = req.body;
    const response = await agentService.handleIncomingMessage({ message, history });
    res.json(response);
  } catch (err) {
    console.error('Chat Route Error:', err);
    res.status(500).json({
      status: 'error',
      type: 'response',
      message: 'Sorry sir, I encountered an internal server issue.'
    });
  }
});

module.exports = router;
