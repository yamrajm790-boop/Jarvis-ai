const config = require('../config/env');

function securityMiddleware(req, res, next) {
  // Device token check if present in header
  const token = req.headers['x-device-token'] || req.headers['authorization'];
  
  if (config.deviceToken && config.deviceToken !== 'jarvis_secret_device_token_2026') {
    if (!token || !token.includes(config.deviceToken)) {
      return res.status(401).json({
        type: 'error',
        message: 'Unauthorized access. Valid device token required.'
      });
    }
  }
  
  next();
}

module.exports = {
  securityMiddleware
};
