package com.example.myapplication

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Contents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class LocalLLM() {

    private var engine: Engine? = null

    suspend fun initialize(context: Context) {
        val modelFile = File(context.filesDir, "gemma3-1b-it-int4.litertlm")//model must be transferred from assets to local phone storage

        if (!modelFile.exists()){//trasnfer model if its not alr there
            context.assets.open("gemma3-1b-it-int4.litertlm").use { input ->
                modelFile.outputStream().use{output ->
                    input.copyTo(output)
                }

            }
        }

        val engineConfig = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.GPU,
            cacheDir = context.cacheDir.path,
            maxNumTokens = 512
        )
        engine = Engine(engineConfig)
        engine!!.initialize()
    }

    // streaming
    fun generateStream(prompt: String): Flow<String> {
        val conversation = engine!!.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of("""
        You are a chat assistant, respond conversationally to any user queries. 
        
        Current time: ${LocalDateTime.now()} ${LocalDate.now().dayOfWeek}
    """.trimIndent()),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)
            )
        )
        return conversation.sendMessageAsync(prompt)
            .map { it.toString() }
    }

    // non stream
    fun generate(prompt: String): String {
        engine!!.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of("""
        You are a chat assistant, respond conversationally to any user queries. 
        
        Current time: ${LocalDateTime.now()} ${LocalDate.now().dayOfWeek}
    """.trimIndent()),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.2) // low temp for JSON
            )
        ).use { conversation ->
            return conversation.sendMessage(prompt).toString()
        }
    }

    fun close() {
        engine?.close()
    }
}