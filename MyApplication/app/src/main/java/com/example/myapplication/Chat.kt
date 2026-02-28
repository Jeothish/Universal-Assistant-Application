package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatScreen(returnToChat: () -> Unit){
    val context = LocalContext.current
    val recorder = remember { audio(context) }
    var inputText  by remember { mutableStateOf("") }
    val thinking by GlobalState.thinking
    val listState = rememberLazyListState()
    var textClick by remember { mutableStateOf(true) }
    var voiceClick by remember { mutableStateOf(false) }
    var aslClick by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }


    Column(

    ) {
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
                    text = "Return Home",
                    fontSize = 25.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }


        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF03051A)), //Placeholder colour
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(8.dp),

        ) {

           item{
               Text(
                   text = "PLACEHOLDER",

                   fontSize = 25.sp,
                   modifier = Modifier.padding(top = 4.dp)
               )
           }

        }

        Spacer(modifier = Modifier.height(16.dp))


        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom

        ){
            Button(
                onClick = {
                    textClick = true
                    voiceClick = false
                    aslClick = false
                },
                modifier = Modifier.height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =  if(textClick) Color(0xFF504F48) else  Color(0xFFE7D112),
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
                    containerColor = if(voiceClick) Color(0xFF504F48) else  Color(0xFFE7D112),
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
                    containerColor = if(aslClick) Color(0xFF504F48) else  Color(0xFFE7D112),
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


            }
            else if(voiceClick){
                Button(
                    onClick = {
                        recording = !recording
                              },
                    modifier = Modifier.height(100.dp).width(500.dp).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(recording) Color(0xFFF80000) else  Color(0xFFE7B912),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )
                {
                    Row() {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver ,
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

            }

            else if(aslClick){
                Button(
                    onClick = {

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
                            imageVector = Icons.Default.FrontHand ,
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

