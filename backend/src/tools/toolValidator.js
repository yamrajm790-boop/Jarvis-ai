const toolRegistry = require('./toolRegistry');

/**
 * Validates a structured tool call JSON object.
 * Rejects unknown tools or invalid arguments.
 */
function validateToolCall(toolCall) {
  if (!toolCall || typeof toolCall !== 'object') {
    return { valid: false, error: 'Tool call must be an object' };
  }

  const { tool, arguments: args } = toolCall;

  if (!tool || typeof tool !== 'string') {
    return { valid: false, error: 'Missing or invalid "tool" name' };
  }

  const registeredTool = toolRegistry[tool];
  if (!registeredTool) {
    return { valid: false, error: `Unknown tool: ${tool}` };
  }

  const safeArgs = args && typeof args === 'object' ? args : {};

  // Check required arguments
  const reqs = registeredTool.parameters?.required || [];
  for (const req of reqs) {
    if (safeArgs[req] === undefined || safeArgs[req] === null) {
      return { valid: false, error: `Missing required argument "${req}" for tool "${tool}"` };
    }
  }

  return {
    valid: true,
    validatedCall: {
      type: 'tool_call',
      tool,
      arguments: safeArgs
    }
  };
}

module.exports = {
  validateToolCall
};
