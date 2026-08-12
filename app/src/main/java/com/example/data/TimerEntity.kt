package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationSeconds: Int,
    val label: String = "JARVIS Timer",
    val endTimeMillis: Long,
    val isFinished: Boolean = false
)
