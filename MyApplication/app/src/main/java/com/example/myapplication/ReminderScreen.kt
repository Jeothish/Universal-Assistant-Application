package com.example.myapplication

import android.R.attr.right
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Paint
import android.icu.util.Calendar
import android.provider.Settings
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.gestures.snapping.SnapPosition

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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


import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.String
import kotlin.text.forEach


/**
 * Main screen for displaying and managing reminders

 * @param returnToChat Callback to navigate back to the home interface.
 * @param openRemindersScreen Callback to navigate to the add reminders screen
 */
@Composable
fun RemindersScreenDisplay(returnToChat: () -> Unit,openRemindersScreen: (existingReminder: Reminder?) -> Unit) {
    val context = LocalContext.current

    // Initialize Database and Repository instances
    val database = remember { DatabaseProvider.getDatabase(context) }
    val repository = remember {
        ReminderRepository(database.reminderDao())
    }

    // Observe reminders as a State to trigger recomposition on DB changes
    val reminders by repository.allReminders.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    val courotineScope = rememberCoroutineScope()


    val totalReminders by remember { derivedStateOf{reminders.size} }
    val completedReminders by remember { derivedStateOf{reminders.count{reminder -> reminder.is_complete == true}} }
    val remainingReminders by remember { derivedStateOf{reminders.count{reminder -> reminder.is_complete == false}} }

    //Filters reminders based on search bar
    val filteredReminders by remember {derivedStateOf {
        if(searchQuery.isBlank()) reminders
        else reminders.filter{
            r -> r.reminder_title?.contains(searchQuery, ignoreCase = true) == true ||
                r.reminder_description?.contains(searchQuery, ignoreCase = true) == true
        }
    } }

    // Logic to toggle between the List View and the ASL Output View
    if(!GlobalState.hideResponse.value) {
        Column {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search reminders...",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 20.sp

                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().height(90.dp).padding(8.dp),
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
                onClick = returnToChat,
                modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),
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




            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            {
                Spacer(modifier = Modifier.height(80.dp))

                statsBox(
                    icon = Icons.Default.Storage,
                    iconColour = Color(0xFF1046D0),
                    value = totalReminders,
                    label = "Total",
                    circleBackground = Color(
                        0x92435AAF
                    ),
                    modifier = Modifier.weight(1f)
                )
                statsBox(
                    icon = Icons.Default.CheckCircleOutline,
                    iconColour = Color(0xFF11CB14),
                    value = completedReminders,
                    label = "Done",
                    circleBackground = Color(
                        0xFF214B1A
                    ),
                    modifier = Modifier.weight(1f)
                )
                statsBox(
                    icon = Icons.Default.Pending,
                    iconColour = Color(0xFFFF5722),
                    value = remainingReminders,
                    label = "Remaining",
                    circleBackground = Color(
                        0xFFEA9049
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(filteredReminders) { reminder ->
                    ReminderCard(
                        repository = repository,
                        reminder = reminder,
                        onEdit = { reminder ->
                            courotineScope.launch {
                                openRemindersScreen(reminder)

                            }
                        },
                        onDelete = { id ->
                            courotineScope.launch {
                                val reminder = reminders.find{it.reminder_id == id}
                                if(reminder != null){
                                    repository.deleteReminder(reminder,context)
                                }

                            }
                        },
                        onToggleComplete = { id, isComplete ->
                            courotineScope.launch {
                                val reminder = reminders.find { it.reminder_id == id }
                                if (reminder != null) {
                                    repository.updateReminder(reminder.copy(is_complete = isComplete),context)
                                }
                            }
                        }
                    )
                }

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
                onClick = { openRemindersScreen(null) },
                modifier = Modifier.size(70.dp)
                    .background(color = Color(0xFFE7A23C), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFFFFFFFF)
                )
            }
        }
    }

    if (GlobalState.hideResponse.value){
        ASLOutputScreen(returnToChat = { GlobalState.hideResponse.value = false })
    }
}

/**
 * Individual cards to display reminders neatly
 * Allows for safe editing,deletion,TTS,.ASL translation

 * @param repository The ReminderRepository used for direct DB updates.
 * @param reminder The reminder data object to display.
 * @param onEdit Callback triggered too navigate to the add/edit reminders screen.
 * @param onDelete Callback triggered to remove the reminder by its ID.
 * @param onToggleComplete Callback triggered to update the completion status in the DB.
 */
@Composable
fun ReminderCard(repository: ReminderRepository,reminder: Reminder,onEdit: (Reminder) -> Unit, onDelete: (Int) -> Unit,onToggleComplete: (Int,Boolean) -> Unit){

    var isComplete by remember { mutableStateOf(reminder.is_complete) }
    val courotineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val ttsManager = remember { TTSManager(context) }

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
                        onToggleComplete(reminder.reminder_id, isComplete)

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
                        color =  if(isComplete) Color(0xFF666666) else Color(0xFFFFFFFF),
                        textDecoration = if(isComplete) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Divider(
                        color = Color.Gray,
                        thickness = 2.dp,
                        modifier = Modifier.padding(vertical=8.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = reminder.reminder_description ?: "No description",
                        fontSize = 18.sp,
                        color =  if(isComplete) Color(0xFF444444) else Color(0xFF7B7676),
                        textDecoration = if(isComplete) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    Spacer(modifier = Modifier.height(16.dp))


                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(
                                    if (isComplete) Color(0xFF666666) else Color(0xFFFFFFFF),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "${reminder.reminder_date ?: "No date"} | ${reminder.reminder_time ?: "No time"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) Color(0xFF888888) else Color(0xFF0E0E0E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis

                            )
                        }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                Button(
                    onClick = {
                        GlobalState.ttsReading.value = !GlobalState.ttsReading.value
                        if(GlobalState.ttsReading.value) ttsManager.speak(reminder.reminder_title!!)
                        else ttsManager.stop()
                    },
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(GlobalState.ttsReading.value) Color(0xFFE01212) else Color(
                            0xFF4CAF50
                        ),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )
                {
                    Column() {
                        Icon(
                            imageVector =  if(GlobalState.ttsReading.value) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            if(GlobalState.ttsReading.value) "Stop reading" else "Read",
                            fontSize = 15.sp
                        )
                    }

                }

                //Spacer(modifier = Modifier.width(25.dp))

                Button(
                    onClick = { onEdit(reminder) },
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0C512),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )

                {
                    Column() {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Edit",
                            fontSize = 15.sp
                        )

                    }
                }

                //Spacer(modifier = Modifier.width(25.dp))

                Button(
                    onClick = { onDelete(reminder.reminder_id) },
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC0A0A),
                        contentColor = Color(0xFFFFFFFF),
                    )
                )

                {
                    Column() {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Delete",
                            fontSize = 15.sp)
                    }
                }
            }

            /**
             * Gets letters from title and description to create a sequence of tokens for the ASLOutputScreen
             */
            Button(
                onClick = {
                    val tokens = mutableListOf<String>()

                    reminder.reminder_title?.forEach { t ->
                        if (t.isLetter()) tokens.add(
                            t.uppercaseChar().toString()
                        )
                    }

                    reminder.reminder_description?.forEach { d ->
                        if (d.isLetter()) tokens.add(
                            d.uppercaseChar().toString()
                        )
                    }

                    GlobalState.aslTokens.value = tokens
                    GlobalState.hideResponse.value = true

                          },
                modifier = Modifier.height(100.dp).fillMaxWidth().padding(16.dp),
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
        }
    }

/**
 * Screen used to create a reminder or edit an existing one
 * Supports multi-modal input
 *
 * @param repository The repository for adding/updating the reminder.
 * @param navController Navigation controller for managing back-stack.
 * @param returnToChat Callback to return to the reminder screen.
 * @param existingReminder If given, the screen is filled with the reminders data you wish to edit
 */
@Composable
fun AddReminderScreen(repository: ReminderRepository, navController: NavController, returnToChat: () -> Unit, existingReminder: Reminder? = null){
    var title by rememberSaveable { mutableStateOf(existingReminder?.reminder_title ?:"") }
    var description by rememberSaveable { mutableStateOf(existingReminder?.reminder_description ?:"") }
    var date by rememberSaveable { mutableStateOf(existingReminder?.reminder_date ?:"") }
    var time by rememberSaveable { mutableStateOf(existingReminder?.reminder_time ?:"") }
    val scrollState = rememberScrollState()
    val couroutineScope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    val context = LocalContext.current
    val isEditing = existingReminder != null
    val snackbarHostState = remember{ SnackbarHostState()}
    val recorder = remember { InputProcessing(context) }
    var recording by remember { mutableStateOf(false) }
    val speechRecognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }
    var voiceClick by remember { mutableStateOf(false) }
    var aslClick by remember { mutableStateOf(false) }
    var targetField by rememberSaveable{ mutableStateOf("")}
    val currentMillis = System.currentTimeMillis()

    val aslResult by GlobalState.aslResult
    val aslTarget by GlobalState.aslTargetField

    /**
     * When the user provides input for a specific reminder field through ASL this ensures the correct field is updated
     */
    LaunchedEffect(aslResult,aslTarget) {
        if(aslResult.isNotBlank()){
            when(aslTarget){
                "title" -> title = aslResult
                "description" -> description = aslResult
            }

            GlobalState.aslResult.value = ""
            GlobalState.aslTargetField.value = ""

        }

    }

    Column(modifier = Modifier.padding(12.dp).verticalScroll(scrollState))
    {

        Button(onClick = returnToChat, modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDE0F0F),
            contentColor = Color(0xFFFFFFFF),
        ))

        {
            Row() {
                Icon(imageVector = Icons.Default.KeyboardReturn , contentDescription = null,modifier = Modifier.size(36.dp))
                Text(text="Return to Reminders", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))
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
            onValueChange = {title = it},
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
                unfocusedBorderColor =  Color(0xFF050404),
                unfocusedPlaceholderColor =  Color(0xFF716E6E)
            )
        )
        Row() {
            Button(
                onClick = {
                    if (!recording) {
                        targetField = "title"
                        speechRecognizer.value = startSTT(context) {spokenText ->
                            recording = false
                            speechRecognizer.value?.destroy()
                            speechRecognizer.value = null

                            if (spokenText.isNotBlank()) {

                                if(targetField.isNotBlank()){
                                    when (targetField) {
                                        "title" -> title = spokenText
                                        "description" -> description = spokenText
                                    }
                                    targetField=""
                                }
                                else{
                                    GlobalState.thinking.value = true
                                    GlobalState.vc_prompt.value = spokenText
                                    recorder.sendTextToBackend(spokenText)
                                }

                            }
                        }
                        recording = true
                    }
                    else {
                        speechRecognizer.value?.stopListening()
                        recording=false

                    }
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =  Color(0xFFFDDC05),
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
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {
                    GlobalState.aslTargetField.value = "title"
                    navController.navigate(HomeRoutes.ASL_INPUT_SCREEN)
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
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
                        fontSize = 14.sp,
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
                onClick = {
                    if (!recording) {

                        targetField = "description"
                        speechRecognizer.value = startSTT(context) {spokenText ->
                            recording = false
                            speechRecognizer.value?.destroy()
                            speechRecognizer.value = null

                            if (spokenText.isNotBlank()) {

                                if(targetField.isNotBlank()){
                                    when (targetField) {
                                        "title" -> title = spokenText
                                        "description" -> description = spokenText
                                    }
                                    targetField=""
                                }
                                else{
                                    GlobalState.thinking.value = true
                                    GlobalState.vc_prompt.value = spokenText
                                    recorder.sendTextToBackend(spokenText)
                                }

                            }
                        }
                        recording = true
                    }
                    else {
                        speechRecognizer.value?.stopListening()
                        recording=false

                    }
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
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
                        text ="Speak Description",
                        fontSize = 14.sp,

                    )
                }
            }

            Button(
                onClick = {
                    GlobalState.aslTargetField.value = "description"
                    navController.navigate(HomeRoutes.ASL_INPUT_SCREEN)
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
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
                        fontSize = 14.sp,
                        //modifier = Modifier.padding(top = 8.dp)
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
                onValueChange = {date= it},
                placeholder = {
                    Text(
                        text = "Select Date (YYYY-MM-DD)",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 20.sp,
                        color=Color(0xFF686560)

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
                onClick = {
                    if (!recording) {
                        speechRecognizer.value = startSTT(context) {spokenText ->
                            recording = false
                            speechRecognizer.value?.destroy()
                            speechRecognizer.value = null

                            if (spokenText.isNotBlank()) {
                                val parsedDate = NattyParsing.extractDate(spokenText)
                                Log.d("STT_PARSING","PARSED TIME $parsedDate")
                                if(parsedDate != null){
                                    date = parsedDate
                                }
                                GlobalState.thinking.value = true
                                GlobalState.vc_prompt.value = spokenText

                            }}
                        recording = true
                    }
                    else {
                        speechRecognizer.value?.stopListening()
                        recording=false
                    }
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
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


        }

        Text(text= "Time *",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color=Color(0xFFFFC107),
            modifier = Modifier.padding(8.dp)
        )

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
                onValueChange = {time= it},
                placeholder = {
                    Text(
                        text = "Select Time (HH:MM:SS)",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 20.sp,
                        color=Color(0xFF686560)

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
                onClick = {
                    if (!recording) {
                        speechRecognizer.value = startSTT(context) {spokenText ->
                            recording = false
                            speechRecognizer.value?.destroy()
                            speechRecognizer.value = null

                            if (spokenText.isNotBlank()) {
                                val parsedTime = NattyParsing.extractTime(spokenText)
                                Log.d("STT_PARSING","PARSED TIME $parsedTime")
                                if(parsedTime != null){
                                    time = parsedTime
                                }
                                GlobalState.thinking.value = true
                                GlobalState.vc_prompt.value = spokenText

                            }}
                        recording = true
                    }
                    else {
                        speechRecognizer.value?.stopListening()
                        recording=false
                    }
                },
                modifier = Modifier.weight(1f).height(100.dp).padding(16.dp),
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



        }

        Button(onClick = {
            val currentDate = java.time.LocalDate.now().toString()
            val currentTime =  java.time.LocalTime.now().minusMinutes(1).toString().substring(0, 8)

            if(title.isBlank()){
                Toast.makeText(context,"Enter a title!", Toast.LENGTH_SHORT).show()
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

            if (date < currentDate) {
                Toast.makeText(context, "Date is in the past! Select a valid date", Toast.LENGTH_LONG).show()
                return@Button
            }

            if (date == currentDate && time < currentTime) {
                Toast.makeText(context, "Time is in the past!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            couroutineScope.launch {
                try {
                    val reminder = Reminder(
                        reminder_id = existingReminder?.reminder_id ?: 0,
                        reminder_title = title,
                        reminder_date = date,
                        reminder_description = description,
                        is_complete = false,
                        reminder_time = time
                    )

                    if (isEditing) {
                        repository.updateReminder(reminder,context)
                    } else {
                        repository.addReminder(reminder,context)
                    }

                    returnToChat()

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


/**
 * Used to display key metrics on reminders
 *
 * @param icon The ImageVector to be displayed at the top of the card.
 * @param iconColour The color applied to the icon.
 * @param value The data to display.
 * @param label Label for the box.
 * @param circleBackground The background color container holding the icon.
 * @param modifier Modifier used for styling
 */
@Composable
fun statsBox(icon:ImageVector, iconColour:Color = Color.DarkGray , value:Int , label:String, circleBackground: Color = Color.Gray,modifier: Modifier = Modifier ){
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