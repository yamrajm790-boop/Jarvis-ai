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
}
