package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HomePage(modifier: Modifier = Modifier){


    val navController = rememberNavController() //Keeps track of which screen is being displayed

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
                }
            )
        }
        //Defines the reminders screen
        composable(HomeRoutes.REMINDERS){
            RemindersScreenDisplay(returnToChat = {navController.popBackStack()}, openRemindersScreen = {navController.navigate(
                HomeRoutes.ADD_REMINDERS)})
        }

        composable(HomeRoutes.ADD_REMINDERS){
            AddReminderScreen(returnToChat = {navController.popBackStack()}) //Goes back to previous screen
        }

    }
}



