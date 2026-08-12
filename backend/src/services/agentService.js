const groqService = require('./groqService');
const { validateToolCall } = require('../tools/toolValidator');

async function handleIncomingMessage({ message, history = [] }) {
  if (!message || typeof message !== 'string') {
    return {
      status: 'error',
      type: 'response',
      message: 'Sir, I did not receive a valid message.'
    };
  }

  // Send to Groq AI
  const aiResult = await groqService.processUserCommand(message, history);

  if (aiResult.type === 'tool_call') {
    const validation = validateToolCall(aiResult);
    if (!validation.valid) {
      console.warn('Rejected invalid tool call from AI:', validation.error);
      return {
        status: 'warning',
        type: 'response',
        message: 'I could not execute that action, sir.'
      };
    }

    return {
      status: 'success',
      type: 'tool_call',
      tool: validation.validatedCall.tool,
      arguments: validation.validatedCall.arguments,
      speak_message: aiResult.speak_message || `Executing ${validation.validatedCall.tool.replace(/_/g, ' ')}, sir.`
    };
  }

  // Normal textual response
  return {
    status: 'success',
    type: 'response',
    message: aiResult.message || 'At your service, sir.'
  };
}

module.exports = {
  handleIncomingMessage
};
