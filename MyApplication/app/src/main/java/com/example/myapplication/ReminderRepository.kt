package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import com.example.myapplication.Reminder
import com.example.myapplication.ReminderDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository providing a higher-level interface for Reminder operations.
 *
 * Handles validation and business logic before going to ReminderDao.
 */

class ReminderRepository(private val dao: ReminderDao) {

    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()


    suspend fun addReminder(reminder: Reminder,context: Context){
        Log.d("ReminderRepo", "addReminder called for: ${reminder.reminder_title}")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedDate = LocalDate.parse(reminder.reminder_date, formatter)

        if(reminder.reminder_title.isBlank()){
            throw IllegalArgumentException("Reminder title can not be empty")
        }

        if(parsedDate.isBefore(LocalDate.now()) || reminder.reminder_date.isBlank()){
            throw IllegalArgumentException("Reminder date can not be empty or in the past")
        }


        val assignedId = dao.insertReminder(reminder).toInt()
        AlarmScheuduler.scheduleAlarm(
            context,
            assignedId,
            reminder.reminder_title,
            reminder.reminder_description ?: "",
            reminder.reminder_date,
            reminder.reminder_time ?: ""
        )
    }

    suspend fun updateReminder(reminder:Reminder,context: Context){
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedDate = LocalDate.parse(reminder.reminder_date, formatter)

        if(reminder.reminder_title.isBlank()){
            throw IllegalArgumentException("Reminder title can not be empty")
        }

        if(reminder.reminder_date.isBlank() || parsedDate.isBefore(LocalDate.now()) ){
            throw IllegalArgumentException("Reminder date can not be empty or in the past")
        }
        AlarmScheuduler.cancelAlarm(context, reminder.reminder_id)
        dao.updateReminder(reminder)
        AlarmScheuduler.scheduleAlarm(context, reminder.reminder_id, reminder.reminder_title,
            reminder.reminder_description ?: "", reminder.reminder_date, reminder.reminder_time ?: "")
    }


    suspend fun deleteReminder(reminder: Reminder,context: Context){
        AlarmScheuduler.cancelAlarm(context,reminder.reminder_id)
        dao.deleteReminder(reminder)
    }

}