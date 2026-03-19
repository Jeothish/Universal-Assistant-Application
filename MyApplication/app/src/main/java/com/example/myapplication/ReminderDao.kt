package com.example.myapplication


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM Reminders ORDER BY reminder_date ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminders WHERE reminder_id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}