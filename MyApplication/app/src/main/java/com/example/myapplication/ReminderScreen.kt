package com.example.myapplication

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RemindersScreenDisplay(returnToChat: () -> Unit) {
    var reminders by remember { mutableStateOf<List<ReminderGet>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var totalReminders by remember { mutableIntStateOf(0) }
    var completedReminders by remember { mutableIntStateOf(0) }
    var remainingReminders by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            reminders = getReminders()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    Column {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search reminders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )


        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
        )
        {
            Spacer(modifier = Modifier.height(80.dp))
            statsBox(icon = Icons.Default.Storage, iconColour = Color(0xFF1046D0), value = totalReminders , label = "Total", circleBackground = Color(
                0x92435AAF
            ), modifier = Modifier.weight(1f))
            statsBox(icon = Icons.Default.CheckCircleOutline, iconColour = Color(0xFF11CB14),value = completedReminders , label = "Done",circleBackground = Color(
                0xFF214B1A
            ),modifier = Modifier.weight(1f))
            statsBox(icon = Icons.Default.Pending,iconColour = Color(0xFFFF5722), value = remainingReminders , label = "Remaining", circleBackground = Color(
                0xFFEA9049
            ), modifier = Modifier.weight(1f))
        }

        Text(text= "Active",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )


    /**
        for (reminder in reminders) {
            Text("Title : ${reminder.reminder_title}")
            Text("Date : ${reminder.reminder_date}")
            Text("Description : ${reminder.reminder_description ?: "No description"}")
            Text("Completed: ${if (reminder.is_complete == true) "Yes" else "No"}")
            Text("Recurrence Type: ${reminder.recurrence_type}")
            Text("Recurrence Day: ${reminder.recurrence_day_of_week}")
            Text("Recurrence Time: ${reminder.recurrence_time}")
        }
        */

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){

            items(reminders) {reminder -> ReminderCard(reminder)}

            }
        }






        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            //Button to return to homepage
            Button(
                onClick = returnToChat,
            ) {
                Text("Return to Chat")
            }

        }
    }

@Composable
fun ReminderCard(reminder: ReminderGet){
    var checkTest by remember { mutableStateOf(false) } //Bind to is_complete later
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D1D)),
    )
    {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                RadioButton(
                    selected = checkTest,
                    onClick = { checkTest = !checkTest },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF0DF108),
                        unselectedColor = Color(0xFFFFFFFF),
                        disabledSelectedColor = Color(0xFF1D1D1D),
                        disabledUnselectedColor = Color(0xFF383434)
                    ),
                    modifier = Modifier.scale(2f)
                )

                Spacer(modifier = Modifier.width(35.dp))

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = reminder.reminder_title ?: "No title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        color = Color(0xFFFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = reminder.reminder_description ?: "No description",
                        fontSize = 18.sp,
                        color = Color(0xFF7B7676)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFACC1C7),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = reminder.reminder_date ?: "No date",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0E0E0E)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = " Repeats ${reminder.recurrence_type ?: "No date"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF),
                        )

                    }
                }
            }
                Row(Modifier.padding(16.dp)){

                    Button(onClick = {}, modifier = Modifier.height(100.dp).width(100.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1255E0),
                        contentColor = Color(0xFFFFFFFF),
                    ))
                    {
                        Column() {
                            Icon(imageVector = Icons.Default.RecordVoiceOver , contentDescription = null,modifier = Modifier.size(36.dp))
                            Text("Read")
                        }

                    }

                    Spacer(modifier = Modifier.width(25.dp))

                    Button(onClick = {}, modifier = Modifier.height(100.dp).width(100.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0C512),
                        contentColor = Color(0xFFFFFFFF),
                    ))

                    {
                        Column() {
                            Icon(imageVector = Icons.Default.Edit , contentDescription = null,modifier = Modifier.size(36.dp))
                            Text("Edit")

                        }
                    }

                    Spacer(modifier = Modifier.width(25.dp))

                    Button(onClick = {}, modifier = Modifier.height(100.dp).width(100.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC0A0A),
                        contentColor = Color(0xFFFFFFFF),
                    ))

                    {
                        Column() {
                            Icon(imageVector = Icons.Default.Delete , contentDescription = null,modifier = Modifier.size(36.dp))
                            Text("Delete")

                        }
                    }
                }


        Button(onClick = {}, modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFC30AEC),
            contentColor = Color(0xFFFFFFFF),
        ))

        {
            Row() {
                Icon(imageVector = Icons.Default.FrontHand , contentDescription = null,modifier = Modifier.size(36.dp))
                Text(text="Translate to ASL", fontSize = 25.sp)

            }
        }
        }
}

@Composable
fun statsBox(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColour:Color = Color.DarkGray , value:Int , label:String, circleBackground: Color = Color.Gray,modifier: Modifier = Modifier ){
    Column(
        modifier = modifier
            .padding(4.dp)
            .background(Color(0xFF1D1D1D), shape = RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Box(
            modifier = Modifier
                .size(61.dp)
                .background(circleBackground, shape = CircleShape),
            contentAlignment = Alignment.Center
        ){
            Icon(
                imageVector = icon,
                null,
                tint = iconColour,
                modifier = Modifier.size(45.dp)
            )
        }


        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value.toString(),
            fontSize = 33.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.LightGray
        )

    }

}

