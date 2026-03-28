package com.example.myapplication


import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import com.chaquo.python.Python
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withTimeout

class LocalLLM(private val wikiRepo: WikiRepository) {


    private var conversation: Conversation? = null
    private var engine: Engine? = null
    private var msg = 0
    private var max=3
    private var appContext: Context? = null

    suspend fun initialize(context: Context) {
        appContext = context.applicationContext
        val modelFile = File(context.filesDir, "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm")//model must be transferred from assets to local phone storage

        if (!modelFile.exists()){//trasnfer model if its not alr there
            context.assets.open("Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm").use { input ->
                modelFile.outputStream().use{output ->
                    input.copyTo(output)
                }

            }
        }

        val engineConfig = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU,
            cacheDir = context.cacheDir.path,
            maxNumTokens = 1024
        )
        engine = Engine(engineConfig)
        engine!!.initialize()

    }

    suspend fun restart() {

        try {
            engine?.close()
        } catch (e: Exception) {
            Log.e("LLM", "Error closing engine: $e")
        }
        engine = null
        conversation = null
        msg = 0
        initialize(appContext!!)
        Log.d("LLM", "LLM restarted")
    }
    private fun getTopic(query:String): String {

                val conv = engine!!.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of("""
                    Extract the main search topic from the user's query.
                    Reply with ONLY the topic, nothing else.
                    Examples:
                    "tell me about Taylor Swift" → "Taylor Swift"
                    "who is Elon Musk" → "Elon Musk"
                    "what is the Eiffel Tower" → "Eiffel Tower"
                """.trimIndent()),
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
                    )
                )
                val topic = conv.sendMessage(query).toString().trim()
                conv.close()
                return topic
            }





    suspend fun callWiki(query: String, wikiRepo: WikiRepository): String {
        val topic = getTopic(query)
        Log.d("LLM", "Extracted topic: $topic")

        val wikiText = wikiRepo.getWikiData(topic)

        return wikiText
    }

    suspend fun generateStream(prompt: String): Flow<String> {
        msg++
        if (msg > max) {
            conversation?.close()
            msg = 0
        }
        val context = callWiki(prompt,wikiRepo)
        val conv = engine!!.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of("""
                    You are a helpful assistant. Answer using ONLY this Wikipedia context:
                    
                    $context
                """.trimIndent()),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)
            )
        )

        return conv.sendMessageAsync(prompt)
            .map { it.toString() }
            .onCompletion { conv.close() }
    }
}

