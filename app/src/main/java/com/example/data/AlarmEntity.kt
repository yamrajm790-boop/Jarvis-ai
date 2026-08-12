package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "JARVIS Alarm",
    val isEnabled: Boolean = true,
    val triggerAtMillis: Long = 0L,
    val isRecurring: Boolean = false,
    val repeatDays: String = ""
)
