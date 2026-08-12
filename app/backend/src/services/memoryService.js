class MemoryService {
  constructor() {
    this.memoryStore = new Map();
  }

  getMemory(deviceId) {
    if (!this.memoryStore.has(deviceId)) {
      this.memoryStore.set(deviceId, {
        assistantName: 'Jarvis',
        responseStyle: 'Short',
        conversationHistory: []
      });
    }
    return this.memoryStore.get(deviceId);
  }

  addConversation(deviceId, role, content) {
    const mem = this.getMemory(deviceId);
    mem.conversationHistory.push({ role, content });
    // Keep context short to minimize token usage
    if (mem.conversationHistory.length > 8) {
      mem.conversationHistory = mem.conversationHistory.slice(-8);
    }
  }

  clearMemory(deviceId) {
    if (this.memoryStore.has(deviceId)) {
      this.memoryStore.delete(deviceId);
    }
  }
}

module.exports = new MemoryService();
