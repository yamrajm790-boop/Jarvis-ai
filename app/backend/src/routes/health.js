const express = require('express');
const router = express.Router();
const config = require('../config/env');

router.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'jarvis-backend',
    timestamp: new Date().toISOString(),
    groqModel: config.groqModel,
    groqConfigured: Boolean(config.groqApiKey)
  });
});

module.exports = router;
