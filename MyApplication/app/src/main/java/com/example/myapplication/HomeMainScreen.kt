package com.example.myapplication

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalTime
import kotlin.text.forEach
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.LogoDev
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material3.Icon

import androidx.compose.ui.text.style.TextAlign


// main screen buttons
@Composable
fun homeButtons(icon: ImageVector, iconColour:Color = Color.DarkGray, title:String, description:String, onClick:() -> Unit){
        Box(
            modifier = Modifier
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(width = 2.dp, color = iconColour, shape = RoundedCornerShape(20.dp))
                .clickable{onClick()}
                .padding(16.dp)
        ){
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColour,
                    modifier = Modifier.size(88.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF6F6F6)
                )

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFFBABABA),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(iconColour)
                )

            }

        }
}

@Composable
fun HomeMainScreen(onOpenReminders: () -> Unit,onOpenChat: () -> Unit,onOpenSettings:() -> Unit,onOpenASLInput: () -> Unit) {

            //greeting on main screen
            val hour = LocalTime.now().hour
            var time = ""
            if (hour > 4 && hour < 11) {
                time = "morning"
            } else if (hour > 11 && hour < 18) {
                time = "afternoon"
            } else {
                time = "evening"
            }

            Greeting(time = time)


                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {

                    Spacer(modifier = Modifier.height(170.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().height(600.dp)
                    ) {

                        item {//chat button
                            homeButtons(
                                icon = Icons.Default.Chat,
                                iconColour = Color(0xFF0F8BDE),
                                title = "AI Assistant",
                                description = "Here to help with anything",
                                onClick = {onOpenChat()})
                        }

                        item {//reminders button
                            homeButtons(
                                icon = Icons.Default.Doorbell,
                                iconColour = Color(0xFFDE8B0F),
                                title = "Reminders",
                                description = "Manage your daily tasks",
                                onClick = { onOpenReminders() })
                        }

                        item (span = { GridItemSpan(2) }) {//settings
                            homeButtons(
                                icon = Icons.Default.Settings,
                                iconColour = Color(0xFF9D978E),
                                title = "Settings",
                                description = "Customise your app preferences",
                                onClick = {onOpenSettings()})
                        }


                    }
                }
}

