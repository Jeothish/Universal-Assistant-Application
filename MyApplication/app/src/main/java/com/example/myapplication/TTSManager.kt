package com.example.myapplication

import android.content.Context
import android.provider.Settings
import android.speech.tts.TextToSpeech
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
            }

            supportedLanguages.clear()
            supportedLanguages.addAll(Locale.getAvailableLocales().filter { tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE}
                    .distinctBy { it.language }
                    .sortedBy { it.displayLanguage }
            )
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

    fun getSupportedLanguages(): List<Locale> {
        return Locale.getAvailableLocales().filter {
            tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE
        }.distinctBy { it.language }
    }
}