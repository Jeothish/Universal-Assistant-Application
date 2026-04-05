package com.example.myapplication

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


    suspend fun addReminder(reminder: Reminder){
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedDate = LocalDate.parse(reminder.reminder_date, formatter)

        if(reminder.reminder_title.isBlank()){
            throw IllegalArgumentException("Reminder title can not be empty")
        }

        if(parsedDate.isBefore(LocalDate.now()) || reminder.reminder_date.isBlank()){
            throw IllegalArgumentException("Reminder date can not be empty or in the past")
        }

        dao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder:Reminder){
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedDate = LocalDate.parse(reminder.reminder_date, formatter)

        if(reminder.reminder_title.isBlank()){
            throw IllegalArgumentException("Reminder title can not be empty")
        }

        if(reminder.reminder_date.isBlank() || parsedDate.isBefore(LocalDate.now()) ){
            throw IllegalArgumentException("Reminder date can not be empty or in the past")
        }

        dao.updateReminder(reminder)
    }


    suspend fun deleteReminder(reminder: Reminder){
        
        dao.deleteReminder(reminder)
    }

}