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

    private var engine: Engine? = null //llm inference engine

    private var appContext: Context? = null

    suspend fun initialize(context: Context) {
        appContext = context.applicationContext //give application level context
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
            backend = Backend.GPU, //can be switched to cpu for lower end devices
            cacheDir = context.cacheDir.path, //store model cache, used for faster laoding of llm
            maxNumTokens = 512 //lower = less hallucination and shorter responses
        )
        engine = Engine(engineConfig)
        engine!!.initialize()

    }

    //used for extracting query topic to be sent to wikipedia (RAG)
    private fun getTopic(query:String): String { //pass in user query, return topic (string)

                val conv = engine!!.createConversation( //create new conversation everytime as topic extraction doesnt need context
                    ConversationConfig(
                        systemInstruction = Contents.of("""
                    Extract the main search topic from the user's query.
                    Reply with ONLY the topic, nothing else.
                    Examples:
                    "tell me about Taylor Swift" → "Taylor Swift"
                    "who is Elon Musk" → "Elon Musk"
                    "what is the Eiffel Tower" → "Eiffel Tower"
                """.trimIndent()),
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0) //sampler params to ensure predicatbility and reliabilty
                    )
                )
                val topic = conv.sendMessage(query).toString().trim()
                conv.close()//close conversation to free up resources
                return topic
            }


    suspend fun callWiki(query: String, wikiRepo: WikiRepository): String { //send extracted topic to wikipedia
        val topic = getTopic(query) //call topic extraction
        Log.d("LLM", "Extracted topic: $topic")

        val wikiText = wikiRepo.getWikiData(topic) //send to wiki

        return wikiText
    }

    suspend fun generateStream(prompt: String): Flow<String> { //generate response stream (output tokens to user one by one)

        val context = callWiki(prompt,wikiRepo) //get query context from wikipedia
        val conv = engine!!.createConversation( //start a new conversatiom
            ConversationConfig(
                systemInstruction = Contents.of("""
                    You are a helpful assistant. Answer using ONLY this Wikipedia context:
                    
                    $context
                """.trimIndent()),
                samplerConfig = SamplerConfig(topK = 5, topP = 0.95, temperature = 0.8) //topk = 5 reduces math and enhances performance, temp decides how much creativity is allowed
            )
        )

        return conv.sendMessageAsync(prompt) //send prompt to llm async
            .map { it.toString() }
            .onCompletion { conv.close() }//once full response is recieved by user close conversation to free up resources
    }
}

