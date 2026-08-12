const env = require('../config/env');
const rateLimit = require('express-rate-limit');

/**
 * Middleware to verify device token for personal Android app
 */
function verifyDeviceToken(req, res, next) {
  const token = req.headers['x-device-token'] || req.query.token;
  
  // If device token is set in env, validate it
  if (env.deviceToken && token !== env.deviceToken) {
    return res.status(401).json({
      status: 'error',
      message: 'Unauthorized device access.'
    });
  }

  next();
}

/**
 * Rate Limiter for HTTP API
 */
const apiLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 60, // Limit each IP to 60 requests per minute
  standardHeaders: true,
  legacyHeaders: false,
  message: { status: 'error', message: 'Rate limit exceeded. Please wait a moment.' }
});

module.exports = {
  verifyDeviceToken,
  apiLimiter
};
