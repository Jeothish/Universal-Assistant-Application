package com.example.myapplication

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ASLProcessing(context: Context, modelFile: String, labelsFile: String) {
    private val interpreter: Interpreter
    val labels: List<String>

    init {
        val assetManager = context.assets

        // Load model
        val afd = assetManager.openFd(modelFile) //tf needs model in byte buffer format
        val inputStream = FileInputStream(afd.fileDescriptor)
        val modelBuffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
        interpreter = Interpreter(modelBuffer)

        // Load labels
        labels = assetManager.open(labelsFile).bufferedReader().readLines()
    }

    fun predict(features: FloatArray): Pair<String, Float> { //return letter and conf
        val inputBuffer = ByteBuffer.allocateDirect(63 * 4).apply {//tf lite doesnt take floatarray, needs bytebuffer
            order(ByteOrder.nativeOrder())
            features.forEach { putFloat(it) }
            rewind()
        }

        val outputSize = labels.size
        val output = Array(1) { FloatArray(outputSize) } //1 = tf needs batch dimesniosn

        interpreter.run(inputBuffer, output)

        val probs = output[0]
        val index = probs.indices.maxByOrNull { probs[it] } ?: 0//get highest conf index
        val confidence = probs[index]

        return Pair(labels[index], confidence)
    }

    fun close() = interpreter.close()


}