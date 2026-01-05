package com.example.myapplication

import androidx.compose.runtime.mutableStateOf

object GlobalState {
    var asl = mutableStateOf(false)
    var letter = mutableStateOf(" ")
}
