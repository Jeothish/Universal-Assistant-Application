package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HomePage(modifier: Modifier = Modifier){


    val navController = rememberNavController() //Keeps track of which screen is being displayed
    var reminderToEdit by remember { mutableStateOf<ReminderGet?>(null)}
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
            AddReminderScreen(returnToChat = {
                navController.popBackStack()
                reminderToEdit = null },
                existingReminder = reminderToEdit) //Goes back to previous screen
        }

        composable(HomeRoutes.CHAT){
            ChatScreen(returnToChat = {navController.popBackStack()})
        }

        composable(HomeRoutes.SETTINGS){
            SettingsScreen(returnToChat = {navController.popBackStack()})
        }

        composable(HomeRoutes.PROFILE){
            ProfileScreen(returnToChat = {navController.popBackStack()})
        }


    }


}



