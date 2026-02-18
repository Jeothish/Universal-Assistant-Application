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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlin.text.forEach


class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var reminderId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

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


    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val vibratorPattern = longArrayOf(0, 1000, 500, 1000, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(vibratorPattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun snoozeAlarm(reminderId: Int, title: String, description: String,date: String,time: String) {
        val snoozeTime = System.currentTimeMillis() + (15 * 60 * 1000)
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("date", date)
            putExtra("time", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
           this,
            reminderId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        stopAlarmSound()
        finish()
    }


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



    @Composable
    fun AlarmScreen(title: String, description: String, date: String, time: String,onDismiss: () -> Unit) {

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
                        onClick = {},
                        modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3DD712),
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
                                text = "Read Aloud",
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
                                text = "Snooze (15mins)",
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
            ASLRenderer(tokens = GlobalState.aslTokens.value,onReturn = {GlobalState.hideResponse.value = false})
        }
    }
}

