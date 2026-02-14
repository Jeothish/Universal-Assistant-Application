package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: ""
        val description = intent?.getStringExtra("description") ?: ""
        val date = intent?.getStringExtra("date") ?: ""
        val time = intent?.getStringExtra("time") ?: ""
        val reminderId = intent?.getIntExtra("reminder_id", -1)

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply{
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context?.startActivity(alarmIntent)
    }

}