package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.mutableStateListOf
import java.util.Locale



class TTSManager(context: Context) {

    private var tts: TextToSpeech? = null
    val supportedLanguages = mutableStateListOf<Locale>()

    init{
        tts = TextToSpeech(context){
                status->
            if(status == TextToSpeech.SUCCESS){
                tts?.language = GlobalState.ttsLanguage.value
                tts?.setPitch(GlobalState.ttsPitch.value)
                tts?.setSpeechRate(GlobalState.ttsSpeechRate.value)


                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String?) {
                        GlobalState.ttsReading.value = true
                    }

                    override  fun onDone(utternaceId: String?){
                        GlobalState.ttsReading.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        GlobalState.ttsReading.value = false
                    }
                })

                supportedLanguages.clear()
                supportedLanguages.addAll(Locale.getAvailableLocales().filter { tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE}
                    .distinctBy { it.language }
                    .sortedBy { it.displayLanguage }
                )
            }


        }
    }
    fun speak(text: String?){

        if (text.isNullOrBlank()) return

        tts?.language = GlobalState.ttsLanguage.value
        tts?.setPitch(GlobalState.ttsPitch.value)
        tts?.setSpeechRate(GlobalState.ttsSpeechRate.value)

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CHAT_ID")

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "CHAT_ID")
    }

    fun stop(){
        tts?.stop()
    }

    fun shutdown(){
        tts?.stop()
        tts?.shutdown()
    }

    fun getSupportedLanguages(): List<Locale> {
        return Locale.getAvailableLocales().filter {
            tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE
        }.distinctBy { it.language }
    }
}