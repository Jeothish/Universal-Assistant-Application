package com.example.myapplication

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale



class TTSManager(context: Context) {

    private var tts: TextToSpeech? = null

    init{
        tts = TextToSpeech(context){
                status->
            if(status == TextToSpeech.SUCCESS){
                tts?.language = Locale.UK
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
            }
        }
    }
    fun speak(text: String){
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stop(){
        tts?.stop()
    }

    fun shutdown(){
        tts?.stop()
        tts?.shutdown()
    }
}