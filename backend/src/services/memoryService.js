/**
 * In-memory / lightweight store for user preferences and personal facts
 */

class MemoryService {
  constructor() {
    this.preferences = {
      userName: 'Sir',
      assistantName: 'JARVIS',
      conciseResponses: true,
      userLikesShortAnswers: true
    };
    this.memoryItems = [];
  }

  getSystemContext() {
    return `User preferences:
- Assistant Name: ${this.preferences.assistantName}
- Call user: ${this.preferences.userName}
- Tone: Highly concise, direct, witty, JARVIS-style assistant (Iron Man theme). Never respond with long verbose explanations unless explicitly asked. Always keep voice responses short (1-2 short sentences max).`;
  }

  addMemory(key, value) {
    this.memoryItems.push({ key, value, timestamp: Date.now() });
  }

  getMemories() {
    return this.memoryItems;
  }

  clearMemory() {
    this.memoryItems = [];
  }
}

module.exports = new MemoryService();
