package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // Conversation History
    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversation_history")
    suspend fun clearHistory()

    // Custom Commands
    @Query("SELECT * FROM custom_commands ORDER BY triggerPhrase ASC")
    fun getAllCustomCommands(): Flow<List<CustomCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommand(command: CustomCommandEntity): Long

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCustomCommand(id: Long)

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getActiveCustomCommands(): List<CustomCommandEntity>

    // Personal Memory
    @Query("SELECT * FROM personal_memory ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM personal_memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM personal_memory")
    suspend fun clearAllMemory()

    // Alarms
    @Query("SELECT * FROM alarms ORDER BY triggerAtMillis ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getActiveAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Query("UPDATE alarms SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateAlarmEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    @Query("DELETE FROM alarms")
    suspend fun clearAllAlarms()

    // Timers
    @Query("SELECT * FROM timers ORDER BY endTimeMillis ASC")
    fun getAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE isFinished = 0")
    suspend fun getActiveTimers(): List<TimerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: TimerEntity): Long

    @Query("UPDATE timers SET isFinished = :isFinished WHERE id = :id")
    suspend fun updateTimerFinished(id: Long, isFinished: Boolean)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimer(id: Long)
}
