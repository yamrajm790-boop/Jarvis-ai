const Groq = require('groq-sdk');
const env = require('../config/env');
const toolRegistry = require('../tools/toolRegistry');
const memoryService = require('./memoryService');

let groqClient = null;
if (env.groqApiKey) {
  groqClient = new Groq({ apiKey: env.groqApiKey });
}

/**
 * System prompt that forces strict JSON response output with either tool_call or response.
 */
function buildSystemPrompt() {
  const toolsDescription = Object.entries(toolRegistry)
    .map(([name, spec]) => `- ${name}: ${spec.description} | args: ${JSON.stringify(spec.parameters.properties)}`)
    .join('\n');

  return `You are JARVIS, a personal AI voice assistant for Android.
${memoryService.getSystemContext()}

You have access to the following Android tools:
${toolsDescription}

CRITICAL RULES:
1. You MUST always output ONLY valid JSON without any markdown or wrapped codeblocks.
2. If the user's command requires an Android action or tool execution, output:
{
  "type": "tool_call",
  "tool": "TOOL_NAME",
  "arguments": { ... },
  "speak_message": "Concise spoken status message (e.g., 'Opening YouTube, sir.')"
}

3. If the user's input is general conversation or a question, output:
{
  "type": "response",
  "message": "Short, natural, concise response (e.g., 'Sir, it is 8:30 PM.')"
}

4. NEVER attempt to output arbitrary executable JavaScript, Python, shell code, or unauthorized commands.
5. Keep spoken text under 20 words whenever possible.
6. Always address the user as 'sir' or according to their preference.`;
}

async function processUserCommand(message, contextHistory = []) {
  if (!env.groqApiKey) {
    return {
      type: 'response',
      message: 'Sir, the Groq API key is not configured on the backend server.'
    };
  }

  const client = groqClient || new Groq({ apiKey: env.groqApiKey });

  // FIX: contextHistory can sometimes arrive as undefined/null/non-array
  // (e.g. malformed client payload). Guard against that before calling
  // .map() so the whole request doesn't crash with
  // "contextHistory.map is not a function".
  const safeHistory = Array.isArray(contextHistory) ? contextHistory : [];

  const messages = [
    { role: 'system', content: buildSystemPrompt() },
    ...safeHistory.map(item => ({
      role: item.role === 'user' ? 'user' : 'assistant',
      content: typeof item.content === 'string' ? item.content : JSON.stringify(item.content)
    })),
    { role: 'user', content: message }
  ];

  try {
    const chatCompletion = await client.chat.completions.create({
      messages,
      model: env.groqModel || 'llama-3.3-70b-versatile',
      temperature: 0.2,
      max_tokens: 350,
      response_format: { type: 'json_object' }
    });

    const rawOutput = chatCompletion.choices[0]?.message?.content || '{}';
    let parsed;
    try {
      parsed = JSON.parse(rawOutput);
    } catch (e) {
      // Fallback regex attempt if needed
      parsed = { type: 'response', message: rawOutput.replace(/[\{\}]/g, '').trim() || 'Command acknowledged, sir.' };
    }

    return parsed;
  } catch (err) {
    console.error('Groq Service Error:', err.message);
    return {
      type: 'response',
      message: 'Sorry sir, AI service is temporarily unavailable.'
    };
  }
}

module.exports = {
  processUserCommand
};
