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


data class NewsItem(
    val Title: String,
    val Link: String,
    val Description: String,
    val Published: String
)



data class WeatherItem(
    @SerializedName("Temperature (°C)")
    val temperature: Double,

    @SerializedName("Wind Speed (km/h)")
    val windSpeed: Double,

    @SerializedName("Weather Condition")
    val forecast: String,

    @SerializedName("Local Time")
    val time: String
)


class audio(private val context: Context) {

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

//    fun sendTextToBackend(text: String){
//        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
//        GlobalState.userTimes.add(messageTime)
//        GlobalState.userPrompts.add(text)
//        GlobalState.thinking.value = true
//        Thread{
//            try{
//                Log.d("SERVER_IP", GlobalState.serverIP.value)
//                val url = URL("http://${GlobalState.serverIP.value}:8000/text")
//                val conn = url.openConnection() as HttpURLConnection
//
//                conn.requestMethod= "POST"
//                conn.setRequestProperty("Content-Type","application/json")
//                conn.doOutput = true
//
//                val payload = """
//                    {
//                        "text": "${text.replace("\"","\\\"")}", "time": "${LocalDateTime.now().toString()} ${LocalDate.now().dayOfWeek}", "city": "${GlobalState.userCity.value}"
//                        }
//                """.trimIndent()
//
//                conn.outputStream.use{it.write(payload.toByteArray())}
//
//                val response = conn.inputStream.bufferedReader().readText()
//
//                handleResponse(response)
//
//            }
//            catch (e: Exception){
//                Log.e("TEXT_ERROR",e.toString())
//                GlobalState.thinking.value= false
//            }
//        }.start()
//    }
private fun parseJsonFromLLM(raw: String): JsonObject? {
    val cleaned = raw
        .replace("```json", "")
        .replace("```", "")
        .trim()

    Log.d("LLM_CLEANED", cleaned)  // add this

    return try {
        Gson().fromJson(cleaned, JsonObject::class.java)
    } catch (e: Exception) {
        // regex fallback
        val jsonRegex = Regex("\\{[\\s\\S]*}", RegexOption.MULTILINE)
        val match = jsonRegex.find(cleaned)?.value
        Log.d("LLM_MATCH", match ?: "no match")
        try {
            Gson().fromJson(match, JsonObject::class.java)
        } catch (e2: Exception) {
            Log.e("LLM_PARSE", "Failed: $cleaned")
            null
        }
    }
}

//    fun sendTextToBackend(text: String){
//        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
//        GlobalState.userTimes.add(messageTime)
//        GlobalState.userPrompts.add(text)
//        GlobalState.vc_prompt.value = text
//        GlobalState.thinking.value = true
//        val llm = GlobalState.localLLM
//        Thread{
//            try{
//                val response = llm!!.generate(text)
//                Log.d("LLM_RESPONSE", response)
//
//                println(response)
//                val parsed = parseJsonFromLLM(response)
//                Log.d("LLM_PARSED", parsed?.toString() ?: "NULL - parse failed")
//                handleResponse(parsed)
//
//            }
//            catch (e: Exception){
//                Log.e("TEXT_ERROR",e.toString())
//                GlobalState.thinking.value= false
//            }
//        }.start()
//    }

    fun getIntent(prompt: String): String {
        val py  = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("get_intent", prompt).toString()
    }

    fun weather(prompt: String, default: String = GlobalState.userCity.value): String
        {
            val py  = Python.getInstance()
            val mod = py.getModule("intent")
            return mod.callAttr("getWeather", prompt,default).toString()

    }
    fun news(prompt: String): String
    {
        val py  = Python.getInstance()
        val mod = py.getModule("intent")
        return mod.callAttr("getNews", prompt).toString()

    }

    fun sendTextToBackend(prompt: String){
        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        GlobalState.userTimes.add(messageTime)
        GlobalState.userPrompts.add(prompt)
        GlobalState.vc_prompt.value = prompt
        GlobalState.thinking.value = true
        val llm = GlobalState.localLLM
        Thread{
            try{
                val intent = getIntent(prompt)

                val jsonString: String


                if (intent == "weather"){
                    jsonString = weather(prompt)
                    Log.d("LLM_RESPONSE", jsonString)

                }
                else if (intent == "news"){
                    jsonString = news(prompt)
                    Log.d("LLM_RESPONSE", jsonString)

                }

                else{
                    val llmResponse = llm!!.generate(prompt)
                    jsonString = Gson().toJson(mapOf(
                        "intent" to "chat",
                        "prompt" to prompt,
                        "result" to llmResponse))


                    Log.d("LLM_RESPONSE", jsonString)
                }
                val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)
                handleResponse(jsonObject)


            }
            catch (e: Exception){
                Log.e("TEXT_ERROR",e.toString())
                GlobalState.thinking.value= false
            }

        }.start()

    }

    fun handleResponse(response: JsonObject?){
        val jsonObject = Gson().fromJson(response, JsonObject::class.java)

        val intent = jsonObject.get("intent")?.asString ?: ""
        //val prompt = jsonObject.get("prompt")?.asString ?: ""
        var result = ""

        var city=""
        if (intent == "weather") {


            // ADD RULE BASED KW FOR WEATHER






            city = jsonObject.get("city")?.asString ?: ""


            val resultObj = jsonObject.getAsJsonObject("result")
            val weather = Gson().fromJson(
                resultObj,
                WeatherItem::class.java
            )
            GlobalState.weather.value = weather
        }
        else if (intent == "news"){
            val newsArray = jsonObject.getAsJsonArray("result")
            val newsList = Gson().fromJson(
                newsArray,
                Array<NewsItem>::class.java
            ).toList()

            GlobalState.newsList.value = newsList


        }
        else{
            result = jsonObject.get("result")?.asString ?: ""
        }


        android.os.Handler(android.os.Looper.getMainLooper()).post {//update main thread
            val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            GlobalState.vc_result.value = result
            GlobalState.vc_intent.value = intent
            //GlobalState.vc_prompt.value = prompt

            GlobalState.assistantResponses.add(result)
            GlobalState.assistantIntents.add(intent)
            GlobalState.assistantTimes.add(messageTime)
            if(intent == "weather") {
                GlobalState.city.value = city
            }
            GlobalState.thinking.value = false


        }
    }

//    fun sendAudioToBackend(audioFile: File) {
//
//        GlobalState.thinking.value = true
//
//        Thread {
//            try {
//
//                val boundary = "Boundary-${System.currentTimeMillis()}"
//                val url = URL("http://${GlobalState.serverIP.value}:8000/voice")
//                val conn = url.openConnection() as HttpURLConnection
//
//                conn.requestMethod = "POST"
//                conn.setRequestProperty(
//                    "Content-Type",
//                    "multipart/form-data; boundary=$boundary"
//                )
//                conn.setChunkedStreamingMode(0)
//
//                conn.doOutput = true
//
//                val output = conn.outputStream
//                val writer = output.bufferedWriter()
//
//                //send user time
//                writer.write("--$boundary\r\n")
//                writer.write("Content-Disposition: form-data; name=\"timestamp\"\r\n\r\n")
//                writer.write(LocalDateTime.now().toString() + " " +LocalDate.now().dayOfWeek)
//                writer.write("\r\n")
//                writer.flush()
//
//                //send user city
//                writer.write("--$boundary\r\n")
//                writer.write("Content-Disposition: form-data; name=\"city\"\r\n\r\n")
//                writer.write(GlobalState.userCity.value)
//                writer.write("\r\n")
//                writer.flush()
//
//                //send user prompts
//                writer.write("--$boundary\r\n")
//                writer.write("Content-Disposition: form-data; name=\"user_prompts\"\r\n\r\n")
//                writer.write(GlobalState.userPrompts.joinToString("|"))
//                writer.write("\r\n")
//                writer.flush()
//
//                //send user times
//                writer.write("--$boundary\r\n")
//                writer.write("Content-Disposition: form-data; name=\"user_times\"\r\n\r\n")
//                writer.write(GlobalState.userTimes.joinToString("|"))
//                writer.write("\r\n")
//                writer.flush()
//
//                //send the audio
//                writer.write("--$boundary\r\n")
//                writer.write(
//                    "Content-Disposition: form-data; name=\"audio\"; filename=\"audio.m4a\"\r\n"
//                )
//                writer.write("Content-Type: audio/mp4\r\n\r\n")
//                writer.flush()
//
//                audioFile.inputStream().copyTo(output)
//                output.flush()
//
//                writer.write("\r\n--$boundary--\r\n")
//                writer.flush()
//                writer.close()
//
//                val response = conn.inputStream.bufferedReader().readText()
//                Log.d("VOICE_RESPONSE", response)
//
//                handleResponse(response)
//
//
//
//            } catch (e: Exception) {
//                Log.e("VOICE_ERROR", e.toString())
//                android.os.Handler(android.os.Looper.getMainLooper()).post {
//                    GlobalState.thinking.value = false
//            }}
//        }.start()
//    }
}
