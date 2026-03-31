package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.util.Locale



class TTSManager(context: Context) {

    private var tts: TextToSpeech? = null
    //val supportedLanguages = mutableStateListOf<Locale>()

    init{
        tts = TextToSpeech(context){
                status->
            if(status == TextToSpeech.SUCCESS){
                tts?.language = Locale.UK
                tts?.setPitch(GlobalState.ttsPitch.value)
                tts?.setSpeechRate(GlobalState.ttsSpeechRate.value)


                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String?) {
                        Log.d("TTS_DEBUG", "onStart: $utteranceId")
                        GlobalState.ttsReading.value = true
                    }

                    override  fun onDone(utteranceId: String?){
                        Log.d("TTS_DEBUG", "onStart: $utteranceId")
                        GlobalState.ttsReading.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        Log.d("TTS_DEBUG", "onStart: $utteranceId")
                        GlobalState.ttsReading.value = false
                    }
                })

//                supportedLanguages.clear()
//                supportedLanguages.addAll(Locale.getAvailableLocales().filter { tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE}
//                    .distinctBy { it.language }
//                    .sortedBy { it.displayLanguage }
//                )
            }


        }
    }
    fun speak(text: String?){

        if (text.isNullOrBlank()) return



        tts?.language = Locale.UK
        tts?.setPitch(GlobalState.ttsPitch.value)
        tts?.setSpeechRate(GlobalState.ttsSpeechRate.value)

        val utteranceId = System.currentTimeMillis().toString()

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop(){
        Log.d("TTS_DEBUG", "Stop Clicked")
        tts?.stop()

        GlobalState.ttsReading.value = false
        GlobalState.stopRequested.value = true
    }

    fun shutdown(){
        tts?.stop()
        tts?.shutdown()
    }

//    fun getSupportedLanguages(): List<Locale> {
//        return Locale.getAvailableLocales().filter {
//            tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE
//        }.distinctBy { it.language }
//    }
}