package com.example.myapplication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Paint
import android.icu.util.Calendar
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.widget.Toast

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextFieldColors
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Snackbar

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.String




@Composable
fun RemindersScreenDisplay(returnToChat: () -> Unit,openRemindersScreen: (existingReminder: ReminderGet?) -> Unit) {
    var reminders by remember { mutableStateOf<List<ReminderGet>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val totalReminders by remember { derivedStateOf{reminders.size} }
    val completedReminders by remember { derivedStateOf{reminders.count{reminder -> reminder.is_complete == true}} }
    val remainingReminders by remember { derivedStateOf{reminders.count{reminder -> reminder.is_complete == false}} }
    val courotineScope = rememberCoroutineScope()


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
            onValueChange = {searchQuery= it},
            placeholder = {
                Text(
                    text="Search reminders...",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 20.sp

                )},
            leadingIcon = {Icon(Icons.Default.Search, contentDescription = null)},
            modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =  Color(0xFFFFFFFF),
                unfocusedContainerColor =  Color(0xFFFFFFFF),
                focusedTextColor =  Color(0xFF000000),
                focusedBorderColor =  Color(0xFFDBBE0E),
                unfocusedBorderColor =  Color(0xFF423B3B),
                unfocusedPlaceholderColor =  Color(0xFF716E6E)
            )
        )


        Button(onClick = returnToChat, modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDE0F0F),
            contentColor = Color(0xFFFFFFFF),
        ))

        {
            Row() {
                Icon(imageVector = Icons.Default.KeyboardReturn , contentDescription = null,modifier = Modifier.size(36.dp))
                Text(text="Return to Chat", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))

            }
        }




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
        Row(){
            Text(text= "Active",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color=Color(0xFFFFC107),
                modifier = Modifier.padding(8.dp)
            )


        }



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

            items(reminders) {reminder -> ReminderCard(
                reminder = reminder,
                onEdit = {reminder ->
                    courotineScope.launch {
                        openRemindersScreen(reminder)
                        reminders = getReminders()

                    }
                         },
                onDelete = {id -> courotineScope.launch{
                    deleteReminder(id)
                    reminders = getReminders()

                }}
            )}

        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        IconButton(
            onClick = {openRemindersScreen(null)},
            modifier = Modifier.size(70.dp).background(color = Color(0xFFE7A23C), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFFFFFFFF)
            )
        }
    }
}
@Composable
fun customPopup(
    snackbarHostState: SnackbarHostState,
    message: String
){
    val courotineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()){
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.Center)
        ){
            data -> Snackbar(containerColor = Color(0xFFD01515)){
                Row(verticalAlignment = Alignment.CenterVertically){
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = data.visuals.message,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
             }
        }

        LaunchedEffect(message) {
            courotineScope.launch{
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}
@Composable
fun ReminderCard(reminder: ReminderGet, onEdit: (ReminderGet) -> Unit, onDelete: (Int) -> Unit){
    Log.d("ReminderCard", "Date: ${reminder.reminder_date}, Time: ${reminder.reminder_time}")
    var isComplete by remember { mutableStateOf(reminder.is_complete == true) }
    val courotineScope = rememberCoroutineScope()

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
                selected = isComplete,
                onClick = {
                    isComplete = !isComplete
                    courotineScope.launch {
                        try {
                            updateReminders(
                                reminder.reminder_id,
                                ReminderEdit(
                                    reminder_title = reminder.reminder_title,
                                    reminder_date = reminder.reminder_date,
                                    reminder_description = reminder.reminder_description,
                                    is_complete = isComplete,
                                    recurrence_type = reminder.recurrence_type,
                                    reminder_time = reminder.reminder_time
                                )
                            )
                        }
                        catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                          },
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
                            text = "${reminder.reminder_date ?: "No date"} | ${reminder.reminder_time ?: "No time"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0E0E0E)

                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Repeats ${reminder.recurrence_type ?: "does not repeat"}",
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

            Button(onClick = {onEdit(reminder)}, modifier = Modifier.height(100.dp).width(100.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
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

            Button(onClick = {onDelete(reminder.reminder_id)},
             modifier = Modifier.height(100.dp).width(100.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
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
                Text(text="Translate to ASL", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))

            }
        }
    }
}
@Composable
fun AddReminderScreen(returnToChat: () -> Unit,existingReminder: ReminderGet? = null){
    var title by remember { mutableStateOf(existingReminder?.reminder_title ?:"") }
    var description by remember { mutableStateOf(existingReminder?.reminder_description ?:"") }
    var date by remember { mutableStateOf(existingReminder?.reminder_date ?:"") }
    var time by remember { mutableStateOf(existingReminder?.reminder_time ?:"") }
    var type by remember { mutableStateOf(existingReminder?.recurrence_type ?:"") }
    val scrollState = rememberScrollState()
    val couroutineScope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    val context = LocalContext.current
    val isEditing = existingReminder != null
    val snackbarHostState = remember{ SnackbarHostState()}


    Column(modifier = Modifier.padding(12.dp).verticalScroll(scrollState))
    {

        Button(onClick = returnToChat, modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDE0F0F),
            contentColor = Color(0xFFFFFFFF),
        ))

        {
            Row() {
                Icon(imageVector = Icons.Default.KeyboardReturn , contentDescription = null,modifier = Modifier.size(36.dp))
                Text(text="Return to Chat", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(25.dp))

        Text(text= "Title *",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {title= it},
            placeholder = {
                Text(
                    text="What do you need to remember?",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 20.sp

                )},
            modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =  Color(0xFFFFFFFF),
                unfocusedContainerColor =  Color(0xFFFFFFFF),
                focusedTextColor =  Color(0xFF000000),
                focusedBorderColor =  Color(0xFFDBBE0E),
                unfocusedBorderColor =  Color(0xFF423B3B),
                unfocusedPlaceholderColor =  Color(0xFF716E6E)
            )
        )
        Row() {
            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "Speak Title",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "ASL Title",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Text(text= "Description ",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = {description= it},
            placeholder = {
                Text(
                    text="Add some details...",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 20.sp

                )},
            modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =  Color(0xFFFFFFFF),
                unfocusedContainerColor =  Color(0xFFFFFFFF),
                focusedTextColor =  Color(0xFF000000),
                focusedBorderColor =  Color(0xFFDBBE0E),
                unfocusedBorderColor =  Color(0xFF423B3B),
                unfocusedPlaceholderColor =  Color(0xFF716E6E)
            )
        )
        Row() {
            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "Speak Description",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "ASL Description",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }



        }

        Text(text= "Date *",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )

        val datePicker = DatePickerDialog(

            context,
            {_,year,month,dayOfMonth ->
                calendar.set(year,month,dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                date = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable{datePicker.show()}) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                placeholder = {
                    Text(
                        text = "Select Date (YYYY-MM-DD)",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 20.sp

                    )
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedTextColor = Color(0xFF000000),
                    focusedBorderColor = Color(0xFFDBBE0E),
                    unfocusedBorderColor = Color(0xFF423B3B),
                    unfocusedPlaceholderColor = Color(0xFF716E6E),
                    disabledContainerColor = Color(0xFFFFFFFF),
                    disabledTextColor = Color(0xFF000000),
                    unfocusedTextColor =  Color(0xFF000000)
                ),
                enabled = false
            )
        }

        Row() {
            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "Speak Date",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "ASL Date",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        val timePicker = TimePickerDialog(

            context,
            {_,hourOfDay,minute ->

                time = String.format("%02d:%02d:00", hourOfDay,minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable{timePicker.show()}) {
            OutlinedTextField(
                value = time,
                onValueChange = {},
                placeholder = {
                    Text(
                        text = "Select Time (HH:MM:SS)",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 20.sp

                    )
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedTextColor = Color(0xFF000000),
                    focusedBorderColor = Color(0xFFDBBE0E),
                    unfocusedBorderColor = Color(0xFF423B3B),
                    unfocusedPlaceholderColor = Color(0xFF716E6E),
                    disabledContainerColor = Color(0xFFFFFFFF),
                    disabledTextColor = Color(0xFF000000),
                    unfocusedTextColor =  Color(0xFF000000)
                ),
                enabled = false
            )
        }
        Row() {
            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "Speak Time",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "ASL Time",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }



        }



        Text(text= "Repeating ",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )

        OutlinedTextField(
            value = type,
            onValueChange = {type= it},
            placeholder = {
                Text(
                    text="Would you like this to repeat?",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 20.sp

                )},
            modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =  Color(0xFFFFFFFF),
                unfocusedContainerColor =  Color(0xFFFFFFFF),
                focusedTextColor =  Color(0xFF000000),
                focusedBorderColor =  Color(0xFFDBBE0E),
                unfocusedBorderColor =  Color(0xFF423B3B),
                unfocusedPlaceholderColor =  Color(0xFF716E6E)
            )
        )
        Row() {
            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "Speak Repeating",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.height(100.dp).width(200.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7D112),
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
                        text = "ASL Repeating",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

        }

        Button(onClick = {

            if(title.isBlank()){
                Toast.makeText(context,"Enter a date!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            if(date.isBlank()){
                Toast.makeText(context,"Enter a date!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            if(time.isBlank()){
                Toast.makeText(context,"Enter a time!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            couroutineScope.launch {
                try {
                    if(isEditing){
                       val  updateReminders = ReminderEdit(
                           reminder_title = title.ifBlank { null },
                           reminder_date = date.ifBlank { null },
                           reminder_description = description.ifBlank { null },
                           is_complete = false,
                           recurrence_type = type.ifBlank { null },
                           reminder_time = time.ifBlank { null }
                                    )
                        updateReminders(reminderId = existingReminder.reminder_id, reminder = updateReminders)
                        returnToChat()

                        if(date.isNotBlank() && time.isNotBlank()){
                            AlarmScheuduler.scheduleAlarm(context,existingReminder.reminder_id,title,description,date,time)
                        }
                    }
                    else{
                        val newReminderID = createReminder(ReminderCreate(
                            reminder_title = title,
                            reminder_date = date,
                            reminder_description = description,
                            is_complete = false,
                            recurrence_type = type,
                            reminder_time = time
                        ))
                        returnToChat()
                        if(date.isNotBlank() && time.isNotBlank()){
                            AlarmScheuduler.scheduleAlarm(context,newReminderID,title,description,date,time)
                        }


                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        },
            modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF16E70F),
                contentColor = Color(0xFFFFFFFF),
            ))

        {
            Row() {
                Icon(imageVector = if (isEditing) Icons.Default.Edit else  Icons.Default.Add , contentDescription = null,modifier = Modifier.size(36.dp))
                Text(text= if (isEditing) "Edit reminder" else "Add reminder", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))
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