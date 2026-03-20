package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat



class AlarmForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: ""
        val description = intent?.getStringExtra("description") ?: ""
        val date = intent?.getStringExtra("date") ?: ""
        val time = intent?.getStringExtra("time") ?: ""
        val reminderId = intent?.getIntExtra("reminder_id", -1) ?: -1

        createNotificationChannel()

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, reminderId, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ $title")
            .setContentText(description)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .build()

        startForeground(reminderId, notification)
        startActivity(alarmIntent)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel(CHANNEL_ID,"Alarm Notifications", NotificationManager.IMPORTANCE_MAX)
            .apply {description = "Reminder alarms";enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC}

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object{const val CHANNEL_ID = "alarm_channel"}

    }
