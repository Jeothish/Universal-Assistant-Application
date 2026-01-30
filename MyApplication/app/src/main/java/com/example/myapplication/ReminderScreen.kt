package com.example.myapplication

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemindersScreenDisplay(returnToChat: () -> Unit){
    Text("Reminders Display")
    Spacer(modifier = Modifier.height(16.dp))

    //Button to return to homepage
    Button(onClick = returnToChat){
        Text("Return to Chat")
    }
}