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
import org.json.JSONObject
import java.lang.StringBuilder


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

    private val db by lazy { DatabaseProvider.getDatabase(context) }
    private val weatherRepo by lazy { WeatherRepository(db.weatherDao()) }

    //call python via chaquopy
    fun getIntent(prompt: String): String { //call KW intent detection
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("get_intent", prompt).toString()
    }

    fun weather(prompt: String, default: String = GlobalState.userCity.value): String {
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("getWeather", prompt, default).toString()

    }

    fun news(prompt: String): String {// call python for news
        val py = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("getNews", prompt).toString()

    }



    //send user query to LLM (backend)
    fun sendTextToBackend(prompt: String) {
        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())//time of query

        val newMessage =
            ChatMessage(prompt = prompt, time = messageTime, response = null, intent = null)

        GlobalState.chatMessages.add(newMessage)//add message to chat history

        val messageIndex = GlobalState.chatMessages.lastIndex

        GlobalState.thinking.value = true //set llm processing state to true

        val llm = GlobalState.localLLM

        CoroutineScope(Dispatchers.IO).launch { //run on coroutine to prevent freezing main thread while waiting for response
            while (!GlobalState.llmReady.value) { //if llm init hasnt finished
                Log.d("LLM", "Wait, LLM is Loading...")
                kotlinx.coroutines.delay(100)
            }
            Log.d("LLM", "LLM ready, Sending prompt: $prompt")

            try {
                val intent = withContext(Dispatchers.Main) { getIntent(prompt) }//call python intent detection
                Log.d("LLM", "Intent: $intent")

                val jsonString: String
                if (intent == "weather") {
                    Log.d("WEATHER_PERFORMANCE", "Processing Weather Request")
                    jsonString = withContext(Dispatchers.Main) { weather(prompt, GlobalState.userCity.value)}
                    //jsonString = weatherRepo.getWeatherData(prompt, GlobalState.userCity.value) //get weather forecast through cache/api
                    Log.d("WEATHER_PERFORMANCE", "Fetched weather data")
                }
                else if (intent == "news") {
                    jsonString = withContext(Dispatchers.IO) { news(prompt) }//get news articles
                    Log.d("LLM_RESPONSE", jsonString)
                }
                else { //call llm if chat intent
                   val fullResponse = StringBuilder()//streaming mode
                    GlobalState.localLLM!!.generateStream(prompt).collect { token ->
                        fullResponse.append(token)//add token to llm message
                        withContext(Dispatchers.Main) {
                            if (messageIndex <= GlobalState.chatMessages.lastIndex) {
                                GlobalState.chatMessages[messageIndex] =
                                    GlobalState.chatMessages[messageIndex].copy(
                                        response = fullResponse.toString()
                                    )
                            }
                            //GlobalState.llmResponse.value = fullResponse.toString()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        GlobalState.thinking.value = false //once llm responds set thinking to false
                    }

                    jsonString = Gson().toJson( //json obj for hadnling response
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

        val intent = jsonObject.get("intent")?.asString ?: "chat" //extract intent from json, iof empty set to chat
        //val prompt = jsonObject.get("prompt")?.asString ?: ""
        var result = ""

        if (intent == "weather") {//if user asks for weather

            val resultObj = jsonObject.get("result")//extract forecast
            val city = jsonObject.get("city")?.asString ?: "Unknown"//extract city

            if (resultObj.isJsonArray) {
                val forecastList =
                    Gson().fromJson(resultObj.asJsonArray, Array<WeatherItem>::class.java).toList()
                        .map { it.copy(city = city) }

                GlobalState.chatMessages[messageIndex] = //update chat message
                    GlobalState.chatMessages[messageIndex].copy(
                        intent = "weather",
                        weatherForecast = forecastList,
                        response = "Here is the forecast for ${city}" //for tts
                    )
            }
            else{
                val weather = Gson().fromJson(
                    resultObj,
                    WeatherItem::class.java
                ).copy(city = city)
                val speechText = "The weather in ${weather.city} is ${weather.temperature} degrees and ${weather.forecast}"
                GlobalState.chatMessages[messageIndex] = GlobalState.chatMessages[messageIndex].copy(
                    intent = "weather",
                    weatherData = weather,
                    response = speechText
                )

            }


        } else if (intent == "news") {
            val newsArray = jsonObject.getAsJsonArray("result")
            val newsList = Gson().fromJson(
                newsArray,
                Array<NewsItem>::class.java
            ).toList()
            GlobalState.newsList.value = newsList
            val newsCount = newsList.size
            val firstHeadline = newsList.firstOrNull()?.Title ?: ""
            val newsSpeech = if(newsCount > 0){
                "I found $newsCount news stories for you. The top headline is: $firstHeadline"
            } else {
                "I couldn't find any news stories right now."
            }

            if (messageIndex <= GlobalState.chatMessages.lastIndex) {
                GlobalState.chatMessages[messageIndex] =
                    GlobalState.chatMessages[messageIndex].copy(
                        intent = "news",
                        newsData = newsList,
                        response = newsSpeech

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