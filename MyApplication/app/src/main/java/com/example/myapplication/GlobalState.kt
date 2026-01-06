package com.example.myapplication

import androidx.compose.runtime.mutableStateOf

object GlobalState {
    var asl = mutableStateOf(false)
    var letter = mutableStateOf(" ")
    var city = mutableStateOf("")
    var vc_result = mutableStateOf("")
    var vc_intent = mutableStateOf("")
    var greeting = mutableStateOf(true)
}
