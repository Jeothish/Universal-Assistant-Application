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
import androidx.compose.material3.Icon

import androidx.compose.ui.text.style.TextAlign



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
fun HomeMainScreen(onOpenReminders: () -> Unit,onOpenChat: () -> Unit,onOpenSettings:() -> Unit,onOpenProfile: () -> Unit) {

    var isASLPlaying by remember { mutableStateOf(true) }
    var hideResponse by remember { mutableStateOf(false) }
    var showASL by remember { mutableStateOf(false) }
    var replayKey by remember { mutableStateOf(0) }
    var aslTokens by GlobalState.aslTokens
    val asl by GlobalState.asl
    val prompt by GlobalState.vc_prompt
    var greeting by GlobalState.greeting
    val thinking by GlobalState.thinking
    val news by GlobalState.newsList
    val letter by GlobalState.letter
    val context = LocalContext.current
    val recorder = remember { audio(context) }
    var recording by remember { mutableStateOf(false) }
    var showTestInput by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val aslInput by GlobalState.aslPrompt



    Box(modifier = Modifier.fillMaxSize())
    {

        if (asl) {
            CameraDet()
        }

        if (aslTokens.isNotEmpty() && hideResponse) {
            ASLRenderer(tokens = aslTokens, onReturn = { hideResponse = false })
        }




        if (asl || recording || GlobalState.vc_intent.value != "") {
            greeting = false

        }
        if (greeting) {
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
        } else if (asl) {
            Text(
                text = "Detected Sign: $letter",
                color = Color.Magenta,
                fontSize = 24.sp,
                modifier = Modifier.align(
                    Alignment.TopEnd
                ).padding(end = 80.dp, top = 20.dp, start = 40.dp)
            )

            Text(
                text = "Prompt: ${aslInput.joinToString("")}",
                color = Color.Magenta,
                fontSize = 24.sp,
                modifier = Modifier.align(
                    Alignment.TopEnd
                ).padding(end = 80.dp, top = 200.dp, start = 40.dp)
            )

        }

        val r = GlobalState.vc_result.value
        val w = GlobalState.weather.value
        val city = GlobalState.city.value
        val intent = GlobalState.vc_intent.value

        print(r)


        if ((intent == "weather" || intent == "chat") && !asl) {


            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .verticalScroll(scrollState)
                    .padding(end = 0.dp, start = 0.dp, bottom = 200.dp, top = 0.dp)
            )

            {
                Text(
                    text = prompt.uppercase(),
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        bottom = 20.dp,
                        start = 10.dp,
                        end = 0.dp,
                        top = 20.dp
                    )
                )

                if (intent == "chat") {
                    if (!hideResponse) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {

                            Text(
                                text = r,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f).padding(
                                    end = 50.dp,
                                    start = 20.dp,
                                    top = 100.dp,
                                    bottom = 10.dp
                                )
                            )


                        }
                        Button(
                            onClick = {
                                val tokens = mutableListOf<String>()
                                r.forEach { c ->
                                    if (c.isLetter()) tokens.add(
                                        c.uppercaseChar().toString()
                                    )
                                }
                                GlobalState.aslTokens.value = tokens
                                hideResponse = true
                            },
                            modifier = Modifier.height(40.dp)
                                .padding(end = 0.dp, start = 20.dp)
                        )
                        { Text("Translate to ASL") }

                    }
                } else {
                    if (!hideResponse) {
                        Text(text = city.uppercase(), color = Color.Magenta)

                        Text(text = "Temperature: ${w.temperature} °C")
                        Text("Wind Speed: ${w.windSpeed} km/h")
                        Text("Forecast: ${w.forecast}")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Time: ${w.time}")
                            Button(
                                onClick = {
                                    val tokens = mutableListOf<String>()
                                    w.forecast.forEach { c ->
                                        if (c.isLetter()) tokens.add(
                                            c.uppercaseChar().toString()
                                        )
                                    }
                                    GlobalState.aslTokens.value = tokens
                                    hideResponse = true
                                },
                                modifier = Modifier.height(40.dp)
                            )
                            { Text("Translate to ASL") }

                        }

                    }

                }


            }
        } else if (intent == "news" && !asl) {

            if (!hideResponse) {

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 220.dp)
                ) {

                    Text(
                        text = prompt.uppercase(),
                        color = Color.Green,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            bottom = 30.dp,
                            start = 0.dp,
                            end = 0.dp,
                            top = 20.dp
                        )
                    )

                    LazyColumn {
                        items(news) { item ->


                            Text(
                                text = "Title: ${item.Title}", fontSize = 20.sp,
                                color = Color.Magenta,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Text(
                                text = "${item.Link}",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "Published: ${item.Published}\n",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Button(
                                    onClick = {
                                        val tokens = mutableListOf<String>()
                                        item.Title.forEach { c ->
                                            if (c.isLetter()) tokens.add(
                                                c.uppercaseChar().toString()
                                            )
                                        }
                                        GlobalState.aslTokens.value = tokens
                                        hideResponse = true
                                    },
                                    modifier = Modifier.height(40.dp)
                                )
                                { Text("Translate to ASL") }
                            }
                        }

                    }
                }

            }
        }





            textBar { input ->
                GlobalState.thinking.value = true
                GlobalState.vc_prompt.value = input

                recorder.sendTextToBackend(input)
            }

    }


            if (!hideResponse) {

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


                        item {
                            homeButtons(
                                icon = Icons.Default.Chat,
                                iconColour = Color(0xFF0F8BDE),
                                title = "AI Assistant",
                                description = "Here to help with anything",
                                onClick = {onOpenChat()})
                        }

                        item {
                            homeButtons(
                                icon = Icons.Default.Doorbell,
                                iconColour = Color(0xFFDE8B0F),
                                title = "Reminders",
                                description = "Manage your daily tasks",
                                onClick = { onOpenReminders() })
                        }

//                        item {
//                            homeButtons(
//                                icon = Icons.Default.Mic,
//                                iconColour = if (recording) Color(0xFFE70707) else Color(0xFF2BFF00),
//                                title = if (recording) "Stop recording" else "Voice Chat",
//                                description = "**MOVE TO ASSISTANT**",
//                                onClick = {
//
//                                    if (!recording) {
//                                        recorder.startRec()
//                                        recording = true
//                                    } else {
//
//                                        val file = recorder.stopRec()
//
//                                        file?.let { recorder.sendAudioToBackend(it) }
//                                        recording = false
//
//
//                                    }
//                                },
//                            )
//                        }

                        item {
                            homeButtons(
                                icon = Icons.Default.FrontHand,
                                iconColour = Color(0xFFBF0FDE),
                                title = "Sign Language",
                                description = "**MOVE TO ASSISTANT**",
                                onClick = {
                                    var asl by GlobalState.asl
                                    val aslInput by GlobalState.aslPrompt
                                    val thinking by GlobalState.thinking

                                    if (!thinking) {
                                        if (aslInput.joinToString("").isNotBlank()) {
                                            if (asl) {
                                                recorder.sendTextToBackend(aslInput.joinToString(""))
                                                GlobalState.aslPrompt.value =
                                                    mutableListOf<String>()

                                            }
                                        }
                                        asl = !asl
                                    }


                                }
                            )
                        }

                        item {
                            homeButtons(
                                icon = Icons.Default.Settings,
                                iconColour = Color(0xFF3E3A37),
                                title = "Settings",
                                description = "Customise your app preferences",
                                onClick = {onOpenSettings})
                        }

                        item {
                            homeButtons(
                                icon = Icons.Default.Person,
                                iconColour = Color(0xFFE2D813),
                                title = "Profile",
                                description = "Manage your account",
                                onClick = {onOpenProfile})
                        }

                        item {
                            homeButtons(
                                icon = Icons.Default.PhoneAndroid,
                                iconColour = Color(0xFFE2135B),
                                title = "Phone helper",
                                description = "**COMING SOON**",
                                onClick = {})
                        }



//UN COMMENT FOR ASL FUNCTIONALITY THROUGH BUTTON
//                    item {
//                        Button(
//                            "Sign Language", Alignment.BottomEnd,
//                            { recorder.sendTextToBackend(aslInput.joinToString("")) })
//                    }


                    }
                }
            }
}

