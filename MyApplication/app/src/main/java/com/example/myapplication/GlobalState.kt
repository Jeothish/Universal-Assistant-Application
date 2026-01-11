package com.example.myapplication


import androidx.compose.runtime.mutableStateOf

object GlobalState {
    var asl = mutableStateOf(false)
    var letter = mutableStateOf(" ")

    var thinking =  mutableStateOf(false)


    var city = mutableStateOf("")
    var vc_result = mutableStateOf("")
    var vc_prompt = mutableStateOf("")
    var vc_intent = mutableStateOf("")

    var weather = mutableStateOf(WeatherItem(0.0,0.0,"",""))

    var newsList = mutableStateOf<List<NewsItem>>(emptyList())
    var greeting = mutableStateOf(true)
}
