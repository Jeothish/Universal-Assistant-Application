package com.example.myapplication

import WikiTools
import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class LocalLLM() {


    private var conversation: Conversation? = null
    private var engine: Engine? = null
    private var msg = 0
    private var max=3

    private fun startConversation(){

        conversation = engine!!.createConversation(
            ConversationConfig(
//                systemInstruction = Contents.of("""
//                    You are a chat assistant, respond conversationally to any user queries.
//                    Current time: ${LocalDateTime.now()} ${LocalDate.now().dayOfWeek}
//                """.trimIndent()),
//                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
                systemInstruction = Contents.of("""
                You are a chat assistant, respond conversationally to any user queries.
                Current time: ${LocalDateTime.now()} ${LocalDate.now().dayOfWeek}
                IMPORTANT: For ANY question about real people, places, events, or facts, 
                you MUST use the searchWikipedia tool. Never answer factual questions from memory.
            """.trimIndent()),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
                tools = listOf(WikiTools())

            )
        )
    }
    suspend fun initialize(context: Context) {
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
            backend = Backend.GPU,
            cacheDir = context.cacheDir.path,
            maxNumTokens = 1024
        )
        engine = Engine(engineConfig)
        engine!!.initialize()
        startConversation()
    }

    // streaming
    fun generateStream(prompt: String): Flow<String> {
        msg++
        if (msg > max) {
            conversation?.close()
            startConversation()
            msg = 0
        }

        return conversation!!.sendMessageAsync(prompt)
            .map { it.toString() }
    }

    // non stream
    fun generate(prompt: String): String {
        msg++
        if (msg > max) {
            conversation?.close()
            startConversation()
            msg = 0
        }

        return conversation!!.sendMessage(prompt).toString()

    }

    fun close() {
        engine?.close()
    }
}