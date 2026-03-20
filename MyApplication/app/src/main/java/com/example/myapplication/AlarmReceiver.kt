package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: ""
        val description = intent?.getStringExtra("description") ?: ""
        val date = intent?.getStringExtra("date") ?: ""
        val time = intent?.getStringExtra("time") ?: ""
        val reminderId = intent?.getIntExtra("reminder_id", -1) ?: -1

        val AlarmForegroundIntent = Intent(context, AlarmForegroundService::class.java).apply{
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
        }
        context?.let { ContextCompat.startForegroundService(it, AlarmForegroundIntent) }
    }

}