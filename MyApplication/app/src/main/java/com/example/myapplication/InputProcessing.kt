package com.example.myapplication

import android.content.Context
import android.media.MediaRecorder
import java.io.File

import java.net.URL
import java.net.HttpURLConnection
import android.util.Log
import com.chaquo.python.Python
import com.google.gson.Gson

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.time.LocalDate

import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext


data class NewsItem(
    val Title: String,
    val Link: String,
    val Description: String,
    val Published: String
)



data class WeatherItem(
    val city: String,
    @SerializedName("Temperature (°C)")
    val temperature: Double,

    @SerializedName("Wind Speed (km/h)")
    val windSpeed: Double,

    @SerializedName("Weather Condition")
    val forecast: String,

    @SerializedName("Local Time")
    val time: String
)


class InputProcessing(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRec() {
        outputFile = File(context.cacheDir, "voice_input.m4a")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRec(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }


    fun getIntent(prompt: String): String {
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("get_intent", prompt).toString()
    }

    fun weather(prompt: String, default: String = GlobalState.userCity.value): String {
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("getWeather", prompt, default).toString()

    }

    fun news(prompt: String): String {
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("getNews", prompt).toString()

    }

    fun sendTextToBackend(prompt: String) {
        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newMessage =
            ChatMessage(prompt = prompt, time = messageTime, response = null, intent = null)
        GlobalState.chatMessages.add(newMessage)
        val messageIndex = GlobalState.chatMessages.lastIndex
        GlobalState.thinking.value = true

        val llm = GlobalState.localLLM
        CoroutineScope(Dispatchers.IO).launch {
            while (!GlobalState.llmReady.value) {
                Log.d("LLM", "Wait, LLM is Loading...")
                kotlinx.coroutines.delay(100)
            }
            Log.d("LLM", "LLM ready, Sending prompt: $prompt")

            try {
                val intent = withContext(Dispatchers.Main) { getIntent(prompt) }
                Log.d("LLM", "Intent: $intent")

                val jsonString: String
                if (intent == "weather") {
                    jsonString = withContext(Dispatchers.Main) { weather(prompt) }
                    Log.d("LLM_RESPONSE", jsonString)
                } else if (intent == "news") {
                    jsonString = withContext(Dispatchers.Main) { news(prompt) }
                    Log.d("LLM_RESPONSE", jsonString)
                } else {
                    val fullResponse = StringBuilder()
                    GlobalState.localLLM!!.generateStream(prompt).collect { token ->
                        fullResponse.append(token)
                        withContext(Dispatchers.Main) {
                            if (messageIndex <= GlobalState.chatMessages.lastIndex) {
                                GlobalState.chatMessages[messageIndex] =
                                    GlobalState.chatMessages[messageIndex].copy(
                                        response = fullResponse.toString()
                                    )
                            }
                            GlobalState.llmResponse.value = fullResponse.toString()
                        }
                    }
                    jsonString = Gson().toJson(
                        mapOf(
                            "intent" to "chat",
                            "prompt" to prompt,
                            "result" to fullResponse.toString()
                        )
                    )
                    Log.d("LLM_RESPONSE", jsonString)
                }

                val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)

                withContext(Dispatchers.Main) {
                    handleResponse(jsonObject, messageIndex)
                }

            } catch (e: Exception) {
                Log.e("TEXT_ERROR", e.toString())
                withContext(Dispatchers.Main) { GlobalState.thinking.value = false }
            }
        }

    }

    fun handleResponse(jsonObject: JsonObject, messageIndex: Int) {
        //val jsonObject = Gson().fromJson(response, JsonObject::class.java)

        val intent = jsonObject.get("intent")?.asString ?: "chat"
        //val prompt = jsonObject.get("prompt")?.asString ?: ""
        var result = ""

        if (intent == "weather") {

            val resultObj = jsonObject.getAsJsonObject("result")
            val city = jsonObject.get("city")?.asString ?: "Unknown"
            val weather = Gson().fromJson(
                resultObj,
                WeatherItem::class.java
            ).copy(city = city)

            GlobalState.chatMessages[messageIndex] = GlobalState.chatMessages[messageIndex].copy(
                intent = "weather",
                weatherData = weather,
                response = "Weather loaded"
            )

        } else if (intent == "news") {
            val newsArray = jsonObject.getAsJsonArray("result")
            val newsList = Gson().fromJson(
                newsArray,
                Array<NewsItem>::class.java
            ).toList()

            GlobalState.newsList.value = newsList
            if (messageIndex <= GlobalState.chatMessages.lastIndex) {
                GlobalState.chatMessages[messageIndex] =
                    GlobalState.chatMessages[messageIndex].copy(
                        intent = "news",
                        newsData = newsList,
                        response = "News loaded"
                    )
            }


        } else {
            result = jsonObject.get("result")?.asString ?: ""
        }


        android.os.Handler(android.os.Looper.getMainLooper()).post {//update main thread

            if (messageIndex <= GlobalState.chatMessages.lastIndex) {

                if (intent == "chat") {
                    if (messageIndex <= GlobalState.chatMessages.lastIndex) {
                        GlobalState.chatMessages[messageIndex] =
                            GlobalState.chatMessages[messageIndex].copy(
                                response = result,
                                intent = intent,
                            )
                    }
                }
                GlobalState.thinking.value = false
            }
        }
    }
}