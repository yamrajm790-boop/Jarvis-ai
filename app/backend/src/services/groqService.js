const Groq = require('groq-sdk');
const config = require('../config/env');
const { registeredTools } = require('../tools/toolRegistry');

class GroqService {
  constructor() {
    this.client = null;
    this.initClient();
  }

  initClient() {
    if (config.groqApiKey) {
      this.client = new Groq({
        apiKey: config.groqApiKey
      });
    } else {
      console.warn('⚠️ GROQ_API_KEY is not set. GroqService running in mock/offline mode.');
    }
  }

  async processChat({ userMessage, history = [], systemPrompt }) {
    if (!this.client) {
      // Fallback mock if key is not configured on backend yet
      return this.handleFallbackMock(userMessage);
    }

    const messages = [
      {
        role: 'system',
        content: systemPrompt || `You are JÁRVIS, a personal futuristic AI assistant.
Your answers are concise, calm, intelligent, fast, and slightly futuristic. Always address the user politely as 'sir'.
For device actions like opening apps, setting volume, alarms, etc., call the provided tool functions.
Keep verbal responses under 15-20 words for simple requests.`
      },
      ...history.map(h => ({ role: h.role, content: h.content })),
      { role: 'user', content: userMessage }
    ];

    try {
      const completion = await this.client.chat.completions.create({
        model: config.groqModel || 'llama-3.3-70b-versatile',
        messages: messages,
        tools: registeredTools,
        tool_choice: 'auto',
        temperature: 0.3,
        max_tokens: 250
      });

      const responseMessage = completion.choices[0].message;

      // If Groq selected a tool call
      if (responseMessage.tool_calls && responseMessage.tool_calls.length > 0) {
        const toolCall = responseMessage.tool_calls[0];
        let parsedArgs = {};
        try {
          parsedArgs = JSON.parse(toolCall.function.arguments || '{}');
        } catch (e) {
          parsedArgs = {};
        }

        return {
          type: 'tool_call',
          tool: toolCall.function.name,
          arguments: parsedArgs,
          responseMessage: responseMessage.content || `Executing ${toolCall.function.name}, sir.`
        };
      }

      // Normal text response
      return {
        type: 'response',
        message: responseMessage.content || 'At your service, sir.'
      };
    } catch (error) {
      console.error('Groq API Error:', error.message);
      return {
        type: 'error',
        message: 'Sorry sir, AI service is temporarily unavailable.'
      };
    }
  }

  handleFallbackMock(userMessage) {
    const lower = userMessage.toLowerCase();
    if (lower.includes('youtube')) {
      return {
        type: 'tool_call',
        tool: 'open_app',
        arguments: { package: 'com.google.android.youtube' },
        responseMessage: 'Opening YouTube for you, sir.'
      };
    }
    if (lower.includes('battery')) {
      return {
        type: 'tool_call',
        tool: 'get_battery',
        arguments: {},
        responseMessage: 'Checking battery level, sir.'
      };
    }
    return {
      type: 'response',
      message: `I have processed your request: "${userMessage}", sir.`
    };
  }
}

module.exports = new GroqService();
