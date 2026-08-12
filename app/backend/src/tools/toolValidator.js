const { registeredTools } = require('./toolRegistry');

const validToolNames = new Set(registeredTools.map(t => t.function.name));

function validateToolCall(toolName, args) {
  if (!validToolNames.has(toolName)) {
    return {
      isValid: false,
      error: `Tool '${toolName}' is not registered in Jarvis Tool System.`
    };
  }

  // Basic argument validation
  if (args === null || typeof args !== 'object') {
    return {
      isValid: false,
      error: `Invalid arguments format for tool '${toolName}'.`
    };
  }

  return { isValid: true };
}

module.exports = {
  validateToolCall,
  validToolNames
};
