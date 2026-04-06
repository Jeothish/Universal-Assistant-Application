package com.example.myapplication

import kotlinx.serialization.Contextual
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*


/**
 * Schedules an alarm at a specific date and time.
 *
 * @param context Application context
 * @param reminderId Unique identifier for the reminder
 * @param title Reminder title
 * @param description Reminder description
 * @param date Date in format yyyy-MM-dd
 * @param time Time in format HH:mm:ss
 */

object AlarmScheuduler {

    fun scheduleAlarm(
        context: Context,
        reminderId: Int,
        title: String,
        description: String,
        date: String,
        time: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val dateTime = "$date $time"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val triggerTime = dateFormat.parse(dateTime)?.time ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    fun cancelAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            Log.d("AlarmCancel", "Cancelling alarm for id: $reminderId")
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmCancel", "Done cancelling for id: $reminderId")
            pendingIntent.cancel()
        }
        else{
            Log.d("AlarmCancel", "No pending intent found for id: $reminderId — nothing to cancel")
        }
    }
}
