package com.example.myapplication

import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalTime
import kotlin.text.forEach
@Composable
fun HomeMainScreen(onOpenReminders: () -> Unit){

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
        var showTestInput by remember {mutableStateOf(false)}
        val scrollState = rememberScrollState()
        val aslInput by GlobalState.aslPrompt



        Box(modifier = Modifier.fillMaxSize())
        {

            if (asl) {
                CameraDet()
            }

            if (aslTokens.isNotEmpty() && hideResponse) {
                ASLRenderer(tokens = aslTokens, isPlaying = isASLPlaying,replayTrigger = replayKey)
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {


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

                    Greeting(time = time, modifier = Modifier.align(Alignment.Center))
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
                            .padding(end = 0.dp, start = 0.dp, bottom = 200.dp, top=0.dp)
                    )

                    {
                        Text(
                            text = prompt.uppercase(),
                            color = Color.Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 20.dp, start = 10.dp, end = 0.dp, top=20.dp)
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
                            if(!hideResponse){
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

                    } else {
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {


                            Button(
                                onClick = { isASLPlaying = !isASLPlaying },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if(isASLPlaying) Color.Red else Color.Green,
                                    contentColor = Color.White
                                )
                            )
                            { Text(if (isASLPlaying) "Pause" else "Resume") }

                            Button(
                                onClick = { hideResponse = !hideResponse },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Blue,
                                    contentColor = Color.White
                                )
                            )
                            { Text("Return") }

                            Button(
                                onClick = {
                                    isASLPlaying = true
                                    replayKey++ },

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Magenta,
                                    contentColor = Color.White
                                )
                            )
                            { Text("Replay") }
                        }
                    }
                }

                if (hideResponse && intent == "chat") {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {


                        Button(
                            onClick = { isASLPlaying = !isASLPlaying },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(isASLPlaying) Color.Red else Color.Green,
                                contentColor = Color.White
                            )
                        )
                        { Text(if (isASLPlaying) "Pause" else "Resume") }

                        Button(
                            onClick = { hideResponse = !hideResponse },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue,
                                contentColor = Color.White
                            )
                        )
                        { Text("Return") }

                        Button(
                            onClick = {
                                isASLPlaying = true
                                replayKey++ },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Magenta,
                                contentColor = Color.White
                            )
                        )
                        { Text("Replay") }
                    }
                }

                if (hideResponse && intent == "weather") {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {


                        Button(
                            onClick = { isASLPlaying = !isASLPlaying },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(isASLPlaying) Color.Red else Color.Green,
                                contentColor = Color.White
                            )
                        )
                        { Text(if (isASLPlaying) "Pause" else "Resume") }

                        Button(
                            onClick = { hideResponse = !hideResponse },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue,
                                contentColor = Color.White
                            )
                        )
                        { Text("Return") }

                        Button(
                            onClick = {
                                isASLPlaying = true
                                replayKey++ },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Magenta,
                                contentColor = Color.White
                            )
                        )
                        { Text("Replay") }
                    }
                }




                Column(modifier = Modifier.fillMaxSize()) {

                    Spacer(modifier = Modifier.weight(1f))

                    textBar { input ->
                        GlobalState.thinking.value = true
                        GlobalState.vc_prompt.value = input

                        recorder.sendTextToBackend(input)
                    }
                }

                if (!hideResponse) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        LazyVerticalGrid(columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.wrapContentHeight()) {
                            item {
                                Button(
                                    onClick = {
                                        onOpenReminders()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        if (recording) Color.Red else Color(222, 172, 255),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .height(100.dp)
                                        .zIndex(1f)
                                        .width(130.dp),

                                    ) {
                                    Text("Reminders")
                                }
                            }

                            item{Spacer(modifier = Modifier.width((0.dp)))}

                            item {
                                Button(
                                    onClick = {

                                        if (!recording) {
                                            recorder.startRec()
                                            recording = true
                                        } else {

                                            val file = recorder.stopRec()

                                            file?.let { recorder.sendAudioToBackend(it) }
                                            recording = false


                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        if (recording) Color.Red else Color(222, 172, 255),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .height(100.dp)
                                        .zIndex(1f)
                                        .width(130.dp),

                                    ) {
                                    Text(if (recording) "Stop Recording" else "Voice Chat")
                                }
                            }


                            item {

                                Button("Sign Language", Alignment.BottomEnd,
                                    { recorder.sendTextToBackend(aslInput.joinToString("")) })
                            }
                        }


                    }
                }
            }
        }
    }

