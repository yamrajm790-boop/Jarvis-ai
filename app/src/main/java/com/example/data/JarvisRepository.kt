package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val dao: JarvisDao) {
    val allConversations: Flow<List<ConversationEntity>> = dao.getAllConversations()
    val allCustomCommands: Flow<List<CustomCommandEntity>> = dao.getAllCustomCommands()
    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()
    val allAlarms: Flow<List<AlarmEntity>> = dao.getAllAlarms()
    val allTimers: Flow<List<TimerEntity>> = dao.getAllTimers()

    suspend fun saveConversation(
        userMessage: String,
        assistantResponse: String,
        toolCalled: String? = null,
        toolArguments: String? = null,
        isOfflineMode: Boolean = false
    ) {
        val entity = ConversationEntity(
            userMessage = userMessage,
            assistantResponse = assistantResponse,
            toolCalled = toolCalled,
            toolArguments = toolArguments,
            isOfflineMode = isOfflineMode
        )
        dao.insertConversation(entity)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun saveCustomCommand(trigger: String, description: String, actionsJson: String) {
        val cmd = CustomCommandEntity(
            triggerPhrase = trigger.lowercase().trim(),
            description = description,
            actionsJson = actionsJson
        )
        dao.insertCustomCommand(cmd)
    }

    suspend fun deleteCustomCommand(id: Long) {
        dao.deleteCustomCommand(id)
    }

    suspend fun getActiveCustomCommands(): List<CustomCommandEntity> {
        return dao.getActiveCustomCommands()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "preference") {
        dao.insertMemory(MemoryEntity(memoryKey = key, memoryValue = value, category = category))
    }

    suspend fun deleteMemory(id: Long) {
        dao.deleteMemory(id)
    }

    suspend fun clearAllMemory() {
        dao.clearAllMemory()
    }

    // Alarms
    suspend fun saveAlarm(alarm: AlarmEntity): Long {
        return dao.insertAlarm(alarm)
    }

    suspend fun getActiveAlarms(): List<AlarmEntity> {
        return dao.getActiveAlarms()
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return dao.getAlarmById(id)
    }

    suspend fun updateAlarmEnabled(id: Long, isEnabled: Boolean) {
        dao.updateAlarmEnabled(id, isEnabled)
    }

    suspend fun deleteAlarm(id: Long) {
        dao.deleteAlarm(id)
    }

    suspend fun clearAllAlarms() {
        dao.clearAllAlarms()
    }

    // Timers
    suspend fun saveTimer(timer: TimerEntity): Long {
        return dao.insertTimer(timer)
    }

    suspend fun getActiveTimers(): List<TimerEntity> {
        return dao.getActiveTimers()
    }

    suspend fun updateTimerFinished(id: Long, isFinished: Boolean) {
        dao.updateTimerFinished(id, isFinished)
    }

    suspend fun deleteTimer(id: Long) {
        dao.deleteTimer(id)
    }
}
