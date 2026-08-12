package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoryKey: String,
    val memoryValue: String,
    val category: String = "preference",
    val updatedAt: Long = System.currentTimeMillis()
)
