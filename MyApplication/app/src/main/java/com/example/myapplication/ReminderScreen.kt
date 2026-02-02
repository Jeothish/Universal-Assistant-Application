package com.example.myapplication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
@Composable
fun RemindersScreenDisplay(returnToChat: () -> Unit){
    var reminders by remember { mutableStateOf<List<ReminderGet>>(emptyList()) }
    var loading by remember {mutableStateOf(true)}

    LaunchedEffect(Unit) {
        try{
            reminders = getReminders()
        }
        catch(e:Exception){
            e.printStackTrace()
        }
        finally{
            loading = false
        }
        }

    if(loading){

        Text("Loading Reminders")
    }
    else{
        Column{
            Spacer(modifier = Modifier.height(80.dp))
            for(reminder in reminders){
                Text("Title : ${reminder.reminder_title}")
                Text("Date : ${reminder.reminder_date}")
                Text("Description : ${reminder.reminder_description ?: "No description"}")
                Text("Completed: ${if (reminder.is_complete == true) "Yes" else "No"}")
                Text("Recurrence Type: ${reminder.recurrence_type}")
                Text("Recurrence Day: ${reminder.recurrence_day_of_week}")
                Text("Recurrence Time: ${reminder.recurrence_time}")
            }
        }
    }


    //Button to return to homepage
    Button(onClick = returnToChat){
        Text("Return to Chat")
    }

}

@Composable
fun formatReminderDisplay(){}

