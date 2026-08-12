package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val description: String,
    val actionsJson: String, // Array of tool calls JSON string
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
