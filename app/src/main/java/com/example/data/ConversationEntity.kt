package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_history")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userMessage: String,
    val assistantResponse: String,
    val toolCalled: String? = null,
    val toolArguments: String? = null,
    val isOfflineMode: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
