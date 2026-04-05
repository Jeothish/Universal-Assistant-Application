package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.view.WindowManager
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccessAlarms
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning


import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlin.text.forEach

/**
 * Activity displayed when an alarm is triggered
 *
 * Handles UI, alarm sound, vibration, and user actions
 */
class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var reminderId: Int = -1

    //Called when the activity starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensures the alarm screen appears over the lock screen and wakes the phone
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or //Allows activity to appear even if phone is locked
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or //Removes lock screen
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or //Prevents screen from going off
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON //Turns screen on if its off
        )

        //Get reminder data passed from service
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        reminderId = intent.getIntExtra("reminder_id", -1)


        startAlarmSound()

        setContent {
            MyApplicationTheme {
                AlarmScreen(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    onDismiss = {
                        stopAlarmSound()
                        finish()
                    },
                )
            }
        }

    }

    //Starts the alarm ringtone and vibration pattern
    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri) //Converts default alarm URI into a playable ringtone object
            ringtone?.play()

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val vibratorPattern = longArrayOf(0, 1000, 500, 1000, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(vibratorPattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //Stops the currently playing alarm sound and vibration
    private fun stopAlarmSound() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    /**
     * @Snoozes the current alarm by 15 minutes
     *
     * Reschedules the alarm using AlarmManager with the same reminder data
     *
     * @param reminderId Unique identifier of the reminder
     * @param title Reminder title
     * @param description Reminder description
     * @param date Reminder date
     * @param time Reminder time
     */
    private fun snoozeAlarm(reminderId: Int, title: String, description: String,date: String,time: String) {
        val snoozeTime = System.currentTimeMillis() + (60 * 60 * 1000)
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
        }
        //Wraps the intent so Android can execute it later
        val pendingIntent = PendingIntent.getBroadcast(
           this,
            reminderId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //Schedules new alarm
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        stopAlarmSound()
        finish()
    }

    //Displays an animated pulsing alarm icon f
    @Composable
    fun pulsingIcon() {

        val infiniteTransition = rememberInfiniteTransition(label = "")

        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )

        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = Color(0xFFFFE925),
            modifier = Modifier
                .size(95.dp)
                .scale(scale)
        )
    }


    /**
     * Main UI screen displayed when an alarm is triggered
     * Provides user actions (TTS,ASL translation, snooze,dismiss)
     *
     * @param title Reminder title
     * @param description Reminder description
     * @param date Reminder date
     * @param time Reminder time
     * @param onDismiss Callback invoked when alarm is dismissed
     */

    @Composable
    fun AlarmScreen(title: String, description: String, date: String, time: String,onDismiss: () -> Unit) {
        val context = LocalContext.current
        val ttsManager = remember { TTSManager(context) }

        if (!GlobalState.hideResponse.value) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF020C26)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pulsingIcon()

                Spacer(modifier = Modifier.height(5.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D1D)),
                )
                {
                    Column(modifier = Modifier.padding(16.dp))
                    {

                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 35.sp,
                            color = Color(0xFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Divider(
                            color = Color.Gray,
                            thickness = 2.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = description,
                            fontSize = 25.sp,
                            color = Color(0xFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(27.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFCFC0B),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row() {
                                    /*
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF000000),
                                        modifier = Modifier
                                            .size(55.dp)

                                    )
                                    */

                                    Text(
                                        text = "Due: $date | $time",
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)

                                    )
                                }
                            }

                        }
                    }


                }

                Spacer(modifier = Modifier.height(27.dp))



                Column(
                    modifier = Modifier.fillMaxWidth(),

                    ) {

                    Button(
                        onClick = {
                            GlobalState.ttsReading.value = !GlobalState.ttsReading.value
                            if(GlobalState.ttsReading.value) ttsManager.speak(title)
                            else ttsManager.stop()
                        },
                        modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(GlobalState.ttsReading.value) Color(0xFFE01212) else Color(
                                0xFF0C31EC
                            ),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )

                    {
                        Row() {
                            Icon(
                                imageVector = Icons.Default.KeyboardVoice,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                if(GlobalState.ttsReading.value) "Stop reading" else "Read Aloud",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                        }
                    }



                    Button(
                        onClick = {
                            val tokens = mutableListOf<String>()

                            title.forEach { t ->
                                if (t.isLetter()) tokens.add(
                                    t.uppercaseChar().toString()
                                )
                            }

                          description.forEach { d ->
                                if (d.isLetter()) tokens.add(
                                    d.uppercaseChar().toString()
                                )
                            }

                            GlobalState.aslTokens.value = tokens
                            GlobalState.hideResponse.value = true
                                  },
                        modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC30AEC),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )

                    {
                        Row() {
                            Icon(
                                imageVector = Icons.Default.FrontHand,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Translate to ASL",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                        }
                    }



                    Button(
                        onClick = {snoozeAlarm(reminderId,title,description,date,time)},
                        modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1762E3),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )

                    {
                        Row() {
                            Icon(
                                imageVector = Icons.Default.AccessAlarms,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Snooze (1 hour)",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                        }
                    }



                    Button(
                        onClick = {onDismiss()},
                        modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE80F0F),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )

                    {
                        Row() {
                            Icon(
                                imageVector = Icons.Default.AlarmOff,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Stop Alarm",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                        }
                    }
                }
            }
        }

        if(GlobalState.hideResponse.value){
            ASLOutputScreen(returnToChat = {GlobalState.hideResponse.value = false})
        }
    }
}

