package com.example.myapplication


import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

object GlobalState {
    var asl = mutableStateOf(false)
    var letter = mutableStateOf(" ")

    var thinking =  mutableStateOf(false)


    var city = mutableStateOf("")
    var vc_result = mutableStateOf("")
    var vc_prompt = mutableStateOf("")
    var vc_intent = mutableStateOf("")

    val userPrompts = mutableStateListOf<String>()
    val userTimes = mutableStateListOf<String>()
    val assistantResponses = mutableStateListOf<String>()
    val assistantIntents = mutableStateListOf<String>()

    val assistantTimes = mutableStateListOf<String>()


    val weatherHistory = mutableStateListOf<WeatherItem>()

    var newsList = mutableStateOf<List<NewsItem>>(emptyList())
    var greeting = mutableStateOf(true)
    val aslTokens = mutableStateOf<List<String>>(emptyList())

    val aslPrompt = mutableStateOf(listOf<String>())

    val hideResponse = mutableStateOf(false)

    val userCity = mutableStateOf("Dublin")

    var serverIP = mutableStateOf("192.168.1.135")

    var ttsReading = mutableStateOf(false)

    var localLLM: LocalLLM? = null

    var llmReady = mutableStateOf(false)

    val llmResponse = mutableStateOf("")

    var aslTimer = mutableStateOf(0)

    val ttsPitch = mutableStateOf(1.0f)

    val ttsSpeechRate = mutableStateOf(1.0f)

    val ttsLanguage = mutableStateOf(Locale.UK)
    //val ttsPitch = mutableStateOf("")
    var aslHands = mutableStateOf(0)
    var aslHandsError = mutableStateOf(false)

    var spaceAdded = mutableStateOf(false)
    var letterDeleted = mutableStateOf(false)


}
