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

    fun sendAudioToBackend(audioFile: File) {
        Thread {
            try {
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = URL("http://192.168.1.18:8000/voice")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )
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
                val jsonObject = Gson().fromJson(response, JsonObject::class.java)

                val intent = jsonObject.get("intent")?.asString ?: ""
                var result = ""
                var city=""
                if (intent == "weather") {
                    city = jsonObject.get("city")?.asString ?: ""


                    val resultObj = jsonObject.getAsJsonObject("result")
                    result = resultObj?.toString() ?: ""
                }
                else{
                    result = jsonObject.get("result")?.asString ?: ""
                }




                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    GlobalState.vc_result.value = result
                    GlobalState.vc_intent.value = intent
                    if(intent == "weather") {
                        GlobalState.city.value = city
                    }
                }



            } catch (e: Exception) {
                Log.e("VOICE_ERROR", e.toString())
            }
        }.start()
    }
}
