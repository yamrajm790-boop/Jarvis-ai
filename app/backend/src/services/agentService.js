const groqService = require('./groqService');
const memoryService = require('./memoryService');
const { validateToolCall } = require('../tools/toolValidator');

class AgentService {
  async processRequest(deviceId, userMessage) {
    const memory = memoryService.getMemory(deviceId);
    
    // Call Groq AI service with short history context
    const result = await groqService.processChat({
      userMessage,
      history: memory.conversationHistory
    });

    if (result.type === 'tool_call') {
      const validation = validateToolCall(result.tool, result.arguments);
      if (!validation.isValid) {
        return {
          type: 'error',
          message: 'Requested action is invalid or untrusted, sir.'
        };
      }
    }

    // Record in short term memory
    memoryService.addConversation(deviceId, 'user', userMessage);
    if (result.type === 'response') {
      memoryService.addConversation(deviceId, 'assistant', result.message);
    } else if (result.type === 'tool_call') {
      memoryService.addConversation(deviceId, 'assistant', result.responseMessage || `Executed tool ${result.tool}`);
    }

    return result;
  }
}

module.exports = new AgentService();
