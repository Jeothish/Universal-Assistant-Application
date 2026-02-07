package com.example.myapplication

data class ReminderGet(
    val reminder_id: Int,
    val reminder_title: String?,
    val reminder_date: String?,
    val reminder_description: String?,
    val is_complete: Boolean?,
    val recurrence_type: String?,
    val reminder_time: String?,
)

data class ReminderCreate(
    val reminder_title: String,
    val reminder_date: String,
    val reminder_description: String? = null,
    val is_complete: Boolean,
    val recurrence_type: String? = null,
    val reminder_time: String? = null,
)

data class ReminderEdit(
    val reminder_title: String?,
    val reminder_date: String?,
    val reminder_description: String?,
    val is_complete: Boolean?,
    val recurrence_type: String?,
    val reminder_time: String?,
)
