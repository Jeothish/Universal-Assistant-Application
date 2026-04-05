package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val reminder_id: Int = 0,
    val reminder_title: String,
    val reminder_date: String,
    val reminder_description: String?,
    val is_complete: Boolean = false,
    val reminder_time: String?
)
