package com.example.myapplication

import android.graphics.drawable.Icon
import android.media.Image
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.forEach


@Composable
fun ChatScreen(returnToChat: () -> Unit,onOpenASLInput: () -> Unit) {
    val context = LocalContext.current
    val recorder = remember { audio(context) }
    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val messageCount = GlobalState.userPrompts.size
    val responseCount = GlobalState.assistantResponses.size
    var textClick by remember { mutableStateOf(true) }
    var voiceClick by remember { mutableStateOf(false) }
    var aslClick by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }

    LaunchedEffect(messageCount, responseCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    if (GlobalState.hideResponse.value){
        ASLRenderer(tokens = GlobalState.aslTokens.value,onReturn = {GlobalState.hideResponse.value = false})
    }

    if (!GlobalState.hideResponse.value) {

        Column(

        ) {

            Row(

            ) {
                Button(
                    onClick = returnToChat,
                    modifier = Modifier.height(100.dp).width(200.dp).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDE0F0F),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )
                {
                    Row() {
                        Icon(
                            imageVector = Icons.Default.KeyboardReturn,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )

                        Text(
                            text = "Return Home",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                }

                Button(
                    onClick = {
                        GlobalState.userPrompts.clear()
                        GlobalState.assistantResponses.clear()
                        GlobalState.assistantIntents.clear()
                    },
                    modifier = Modifier.height(100.dp).width(200.dp).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )
                {
                    Row() {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )

                        Text(
                            text = "Clear Chat",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }


                }


            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),

                ) {


                items(messageCount) { index ->
                    chatBubble(
                        GlobalState.userPrompts[index],
                        isUser = true,
                        GlobalState.userTimes[index]
                    )
                    val intent = GlobalState.assistantIntents.getOrNull(index) ?: ""
                    val response = GlobalState.assistantResponses.getOrNull(index) ?: ""


                    when {
                        intent == "weather" -> weatherBubble(
                            GlobalState.weather.value,
                            GlobalState.assistantTimes[index]
                        )

                        intent == "news" -> newsBubble(
                            GlobalState.newsList.value,
                            GlobalState.assistantTimes[index]
                        )

                        response.isNotBlank() -> chatBubble(
                            response,
                            isUser = false,
                            GlobalState.assistantTimes[index]
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom

            ) {
                Button(
                    onClick = {
                        textClick = true
                        voiceClick = false
                        aslClick = false
                    },
                    modifier = Modifier.height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (textClick) Color(0xFFFFC107) else Color(0xFF403E37),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )

                {
                    Row() {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Text",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        textClick = false
                        voiceClick = true
                        aslClick = false
                    },
                    modifier = Modifier.height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (voiceClick) Color(0xFFFFC107) else Color(0xFF3E3D36),
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
                            text = "Voice",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        textClick = false
                        voiceClick = false
                        aslClick = true
                    },
                    modifier = Modifier.height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (aslClick) Color(0xFFFFC107) else Color(0xFF44433C),
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
                            text = "ASL",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }



            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (textClick) {

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Type your message...",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 20.sp

                            )
                        },
                        modifier = Modifier.height(90.dp).padding(8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF),
                            unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF000000),
                            focusedBorderColor = Color(0xFFDBBE0E),
                            unfocusedBorderColor = Color(0xFF423B3B),
                            unfocusedPlaceholderColor = Color(0xFF716E6E)
                        )
                    )

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                recorder.sendTextToBackend(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.height(70.dp).width(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE7B912),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }


                } else if (voiceClick) {
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
                        modifier = Modifier.height(100.dp).width(500.dp).padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recording) Color(0xFFF80000) else Color(0xFFE7B912),
                            contentColor = Color(0xFFFFFFFF),
                        )
                    )
                    {
                        Row() {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Text(
                                text = if (recording) "Stop recording" else "Tap to speak",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }


                    }

                } else if (aslClick) {
                    Button(
                        onClick = {
                            onOpenASLInput()
                        },
                        modifier = Modifier.height(100.dp).width(500.dp).padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE7B912),
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
                                text = "Open ASL Input",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                    }

                }
            }


        }
    }

}


@Composable
fun chatBubble(text: String, isUser: Boolean,time:String){
    val context = LocalContext.current
    val ttsManager = remember { TTSManager(context) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if(isUser) Alignment.End else Alignment.Start

    ) {
        Box(
            modifier = Modifier

                //.wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isUser) Color(0xFFFFC107) else Color(0xFF2A2A38))
                .border(
                    4.dp,
                    if (isUser) Color(0xFF000000) else Color(0xFFFFC107),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                fontSize = 20.sp,
                modifier = Modifier.padding(19.dp),
                color = if (isUser) Color(0xFF000000) else Color(0xFFFFFFFF)
            )
        }

        if(!isUser) {
            Row(modifier = Modifier.padding(6.dp)) {
                Button(
                    onClick = {
                        GlobalState.ttsReading.value = !GlobalState.ttsReading.value
                        if(GlobalState.ttsReading.value) ttsManager.speak(text)
                        else ttsManager.stop()
                    },
                    modifier = Modifier.height(80.dp),
                    shape = RoundedCornerShape(18.dp),
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
                            imageVector =  if(GlobalState.ttsReading.value) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = if(GlobalState.ttsReading.value) "Stop reading" else "Read",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top=6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val tokens = mutableListOf<String>()

                        text.forEach { t ->
                            if (t.isLetter()) tokens.add(
                                t.uppercaseChar().toString()
                            )
                        }

                        GlobalState.aslTokens.value = tokens
                        GlobalState.hideResponse.value = true
                    },
                    modifier = Modifier.height(80.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB80AE8),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )
                {
                    Row() {
                        Icon(
                            imageVector = Icons.Default.SignLanguage,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text ="Show ASL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top=6.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = time,
            fontSize = 20.sp,
            modifier = Modifier.padding(start=9.dp, end=9.dp, top=4.dp, bottom=22.dp),
            color = Color(0xFF636161)
        )


    }

}


fun getWeatherCategory(forecast: String): String{
    return when{
        forecast.contains("snow", ignoreCase = true) -> "snow"
        forecast.contains("rain", ignoreCase = true) -> "rain"
        forecast.contains("thunderstorm", ignoreCase = true) -> "storm"
        forecast.contains("fog", ignoreCase = true) -> "fog"
        forecast.contains("cloud", ignoreCase = true) -> "cloud"
        forecast.contains("clear", ignoreCase = true) -> "clear"
        else -> "unknown"
    }
}

fun getWeatherColour(category: String): Color{
    return when(category.lowercase()){
        "snow" -> Color(0xFFB3E5FC)
        "rain" -> Color(0xFF2C5DE5)
        "storm" -> Color(0xFF09194C)
        "fog" -> Color(0xFFBDBDBD)
        "cloud" -> Color(0xFF3E4449)
        "clear" -> Color(0xFFFFC107)
        else -> Color(0xFFB3E5FC)

    }
}

fun getWeatherIcon(category: String): ImageVector{
    return when(category.lowercase()){
        "snow" -> Icons.Default.AcUnit
        "rain" -> Icons.Default.Opacity
        "storm" -> Icons.Default.FlashOn
        "fog" -> Icons.Default.CloudQueue
        "cloud" -> Icons.Default.Cloud
        "clear" -> Icons.Default.WbSunny
        else -> Icons.Default.Help

    }
}

@Composable
fun weatherBubble(weather: WeatherItem,time:String){
    val context = LocalContext.current
    val ttsManager = remember { TTSManager(context) }
    val category = getWeatherCategory(weather.forecast)
    val color = getWeatherColour(category)
    val icon = getWeatherIcon(category)

    Column() {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(24.dp))
                .background(color.copy(alpha = 0.2f))
                .border(4.dp, color, RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {

                    Text(
                        "Weather in ${GlobalState.city.value}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "${weather.temperature}°C",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp
                    )
                    Text(weather.forecast, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(36.dp))

                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(72.dp))
            }
        }

        Row(modifier = Modifier.padding(6.dp)) {
            Button(
                onClick = {
                    GlobalState.ttsReading.value = !GlobalState.ttsReading.value
                    if(GlobalState.ttsReading.value)
                    {ttsManager.speak(weather.forecast)}
                    else ttsManager.stop()
                },
                modifier = Modifier.height(80.dp),
                shape = RoundedCornerShape(18.dp),
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
                        imageVector =  if(GlobalState.ttsReading.value) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if(GlobalState.ttsReading.value) "Stop reading" else "Read",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top=6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    val tokens = mutableListOf<String>()

                    weather.forecast.forEach { w ->
                        if (w.isLetter()) tokens.add(
                            w.uppercaseChar().toString()
                        )
                    }

                    GlobalState.aslTokens.value = tokens
                    GlobalState.hideResponse.value = true
                },
                modifier = Modifier.height(80.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB80AE8),
                    contentColor = Color(0xFFFFFFFF),
                )
            )
            {
                Row() {
                    Icon(
                        imageVector = Icons.Default.SignLanguage,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text ="Show ASL",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top=6.dp)
                    )
                }
            }
        }


        Text(
            text = time,
            fontSize = 20.sp,
            modifier = Modifier.padding(start=9.dp, end=9.dp, top=4.dp, bottom=22.dp),
            color = Color(0xFF636161)
        )

    }
}


@Composable
fun newsBubble(newsList: List<NewsItem>,time:String) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager(context) }
    Column(){
    newsList.forEach { news ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2A2A38))
                .border(4.dp, Color(0xFFFFC107), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(news.Title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "${news.Description}°C",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp
                    )
                    Text(news.Published, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(news.Link, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(36.dp))
                Icon(
                    Icons.Default.Newspaper,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Row(modifier = Modifier.padding(6.dp)) {
            Button(
                onClick = {
                    GlobalState.ttsReading.value = !GlobalState.ttsReading.value
                    if(GlobalState.ttsReading.value){
                        ttsManager.speak(news.Title)
                        ttsManager.speak(news.Description)
                    }
                    else ttsManager.stop()
                },
                modifier = Modifier.height(80.dp).padding(top=16.dp),
                shape = RoundedCornerShape(18.dp),
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
                        imageVector =  if(GlobalState.ttsReading.value) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if(GlobalState.ttsReading.value) "Stop reading" else "Read",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top=6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    val tokens = mutableListOf<String>()

                    news.Title.forEach { t ->
                        if (t.isLetter()) tokens.add(
                            t.uppercaseChar().toString()
                        )
                    }

                    news.Description.forEach { d ->
                        if (d.isLetter()) tokens.add(
                            d.uppercaseChar().toString()
                        )
                    }

                    GlobalState.aslTokens.value = tokens
                    GlobalState.hideResponse.value = true
                },
                modifier = Modifier.height(80.dp).padding(top=16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB80AE8),
                    contentColor = Color(0xFFFFFFFF),
                )
            )
            {
                Row() {
                    Icon(
                        imageVector = Icons.Default.SignLanguage,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text ="Show ASL",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top=6.dp)
                    )
                }
            }
        }

        Text(
            text = time,
            fontSize = 20.sp,
            modifier = Modifier.padding(start=9.dp, end=9.dp, top=4.dp, bottom=22.dp),
            color = Color(0xFF636161)
        )

    }
}
}


@Composable
fun ASLInputScreen(returnToChat: () -> Unit){
    val scrollState = rememberScrollState()
    var showCamera by remember { mutableStateOf(false) }
    val letter by GlobalState.letter
    val aslInput by GlobalState.aslPrompt
    val context = LocalContext.current
    val recorder = remember { audio(context) }

    Column(
        modifier = Modifier.padding(8.dp).verticalScroll(scrollState)
    ){
        Row() {
            Button(
                onClick = returnToChat,
                modifier = Modifier.height(100.dp).width(500.dp).padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDE0F0F),
                    contentColor = Color(0xFFFFFFFF),
                )
            )
            {
                Row() {
                    Icon(
                        imageVector = Icons.Default.KeyboardReturn,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Text(
                        text = "Return To Chat",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Row() {
            Text(
                text = "Camera View",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(19.dp),
                color = Color(0xFFFFC63A)
            )

            Button(
                onClick = {showCamera = !showCamera},
                modifier = Modifier.height(80.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(showCamera) Color(0xFFE71212) else (Color(0xFFFFBF00)),
                    contentColor = Color(0xFF000000),
                )
            )

            {
                Row() {
                    Icon(
                        imageVector = if(showCamera) Icons.Default.Cancel else Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if(showCamera) "Stop camera" else "Start camera",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF2A2A38))
            .border(4.dp, Color(0xFFE7B212), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center



        ){
            if(showCamera){
                CameraDet()
            }

            else{
                Column(horizontalAlignment = Alignment.CenterHorizontally){

                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)

                    )

                    Text(
                        text ="Camera Off",
                        fontSize = 36.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                }

            }

        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Detected letter",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(19.dp),
            color = Color(0xFFFFC63A)
        )

        Box(
            modifier = Modifier.fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2A2A38))
                .border(4.dp, Color(0xFFE7B212), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = letter,
                fontSize = 55.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(19.dp),
                color = Color(0xFFFFC63A)
            )
        }



            Button(
                onClick = { GlobalState.aslPrompt.value = GlobalState.aslPrompt.value + letter},
                modifier = Modifier.height(80.dp).fillMaxWidth().padding(top=16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF27C212),
                    contentColor = Color(0xFFFFFFFF),
                )
            )

            {
                Row(modifier = Modifier.padding()){

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)

                    )

                    Text(
                        text ="Add Letter",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top=8.dp)
                    )


            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Your Message",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(19.dp),
            color = Color(0xFFFFC63A)
        )


        Box(
            modifier = Modifier.fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFFFFF))
                .border(4.dp, Color(0xFFE7B212), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = if (aslInput.isEmpty()) "Your message will appear here..." else aslInput.joinToString(""),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(19.dp),
                color = Color(0xFF848478)
            )
        }

        Row(modifier = Modifier.padding()){

            Button(
                onClick = { GlobalState.aslPrompt.value = GlobalState.aslPrompt.value + " "},
                modifier = Modifier.height(80.dp).width(200.dp).padding(top=16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF494944),
                    contentColor = Color(0xFFFFFFFF),
                )
            )

            {
                Text(
                    text ="Add space",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,

                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = { GlobalState.aslPrompt.value = GlobalState.aslPrompt.value.dropLast(1)},
                modifier = Modifier.height(80.dp).width(200.dp).padding(top=16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC23607),
                    contentColor = Color(0xFFFFFFFF),
                )
            )

            {
                Text(
                    text ="Delete Last",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row() {


            Button(
                onClick = {
                    recorder.sendTextToBackend(aslInput.joinToString(""))
                    Toast.makeText(context,"Message Sent!", Toast.LENGTH_SHORT).show()
                          },
                modifier = Modifier.height(120.dp).width(400.dp).padding(top = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color(0xFFFFFFFF),
                )
            )


            {

                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)

                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Send message",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

        }
    }

}

