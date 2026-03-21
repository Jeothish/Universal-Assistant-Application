package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun HomePage(modifier: Modifier = Modifier){

    val context = LocalContext.current
    val database = remember { DatabaseProvider.getDatabase(context) }
    val repository = remember {
        ReminderRepository(database.reminderDao())
    }
    val navController = rememberNavController() //Keeps track of which screen is being displayed
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null)}
    //Connects screen tracker with the screens
    NavHost(
        navController = navController,
        startDestination = HomeRoutes.MAIN,
        modifier = modifier
    ){
        //Defines the home screen
        composable(HomeRoutes.MAIN){
            HomeMainScreen(
                onOpenReminders = {
                    navController.navigate(HomeRoutes.REMINDERS)
                },
                onOpenChat = {
                    navController.navigate(HomeRoutes.CHAT)
                },
                onOpenSettings = {
                    navController.navigate(HomeRoutes.SETTINGS)
                },
                onOpenProfile = {
                    navController.navigate(HomeRoutes.PROFILE)
                },
                onOpenAslOutput = {
                    navController.navigate(HomeRoutes.ASL_OUTPUT_SCREEN)
                }
            )
        }
        //Defines the reminders screen
        composable(HomeRoutes.REMINDERS) {
            RemindersScreenDisplay(
                returnToChat = { navController.popBackStack() },
                openRemindersScreen = { reminder ->
                    reminderToEdit = reminder
                    navController.navigate(HomeRoutes.ADD_REMINDERS)
                }
            )
        }

        composable(HomeRoutes.ADD_REMINDERS){
            AddReminderScreen(
                repository,
                returnToChat = {
                navController.popBackStack()
                reminderToEdit = null },
                existingReminder = reminderToEdit) //Goes back to previous screen
        }

        composable(HomeRoutes.CHAT){
            ChatScreen(
                returnToChat = {navController.popBackStack()},
                onOpenASLInput = {navController.navigate(HomeRoutes.ASL_INPUT_SCREEN)}
            )
        }

        composable(HomeRoutes.SETTINGS){
            SettingsScreen(returnToChat = {navController.popBackStack()})

        }

        composable(HomeRoutes.PROFILE){
            ProfileScreen(returnToChat = {navController.popBackStack()})
        }

        composable(HomeRoutes.ASL_INPUT_SCREEN){
            ASLInputScreen(returnToChat = {navController.popBackStack()})
        }

        composable(HomeRoutes.ASL_OUTPUT_SCREEN){
            ASLOutputScreen(returnToChat = {navController.popBackStack()})
        }

    }


}



