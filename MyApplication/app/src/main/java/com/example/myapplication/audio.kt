package com.example.myapplication

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File

import java.net.URL
import java.net.HttpURLConnection
import android.util.Log
import com.google.gson.Gson

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

import java.time.LocalDateTime
import java.time.LocalTime
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

    fun sendTextToBackend(text: String){
        GlobalState.thinking.value = true
        Thread{
            try{
                val url = URL("http://192.168.1.11:8000/text")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod= "POST"
                conn.setRequestProperty("Content-Type","application/json")
                conn.doOutput = true

                val payload = """
                    {
                        "text": "${text.replace("\"","\\\"")}", "time": "${LocalDateTime.now().toString()}"
                        }
                """.trimIndent()

                conn.outputStream.use{it.write(payload.toByteArray())}

                val response = conn.inputStream.bufferedReader().readText()

                handleResponse(response)

            }
            catch (e: Exception){
                Log.e("TEXT_ERROR",e.toString())
                GlobalState.thinking.value= false
            }
        }.start()
    }

    fun handleResponse(response: String){
        val jsonObject = Gson().fromJson(response, JsonObject::class.java)

        val intent = jsonObject.get("intent")?.asString ?: ""
        val prompt = jsonObject.get("prompt")?.asString ?: ""
        var result = ""

        var city=""
        if (intent == "weather") {
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
            GlobalState.vc_result.value = result
            GlobalState.vc_intent.value = intent
            GlobalState.vc_prompt.value = prompt
            if(intent == "weather") {
                GlobalState.city.value = city
            }
            GlobalState.thinking.value = false


        }
    }

    fun sendAudioToBackend(audioFile: File) {
        GlobalState.thinking.value = true

        Thread {
            try {

                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = URL("http://192.168.1.11:8000/voice")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )
                conn.setChunkedStreamingMode(0)

                conn.doOutput = true

                val output = conn.outputStream
                val writer = output.bufferedWriter()

                writer.write("--$boundary\r\n")
                writer.write(
                    "Content-Disposition: form-data; name=\"audio\"; filename=\"audio.m4a\"\r\n"
                )
                writer.write("Content-Type: audio/mp4\r\n\r\n")
                writer.flush()

                audioFile.inputStream().copyTo(output)
                output.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                Log.d("VOICE_RESPONSE", response)

                handleResponse(response)



            } catch (e: Exception) {
                Log.e("VOICE_ERROR", e.toString())
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    GlobalState.thinking.value = false
            }}
        }.start()
    }
}
