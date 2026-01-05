package com.example.myapplication

import android.content.Context
import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.io.ByteArrayOutputStream
import android.util.Log
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.gson.Gson

import com.google.gson.JsonObject



class HandAnalyzer(
    context: Context,
    private val overlayView: OverlayView
) : ImageAnalysis.Analyzer {

    private val handLandmarker: HandLandmarker

    private val TAG = "HandAnalyzer"
    private val sequenceBuffer: MutableList<FloatArray> = mutableListOf()
    private val SEQUENCE_LENGTH = 30


    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build()
            )
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.7f)
            .setMinTrackingConfidence(0.7f)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    override fun analyze(imageProxy: ImageProxy) {

        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        val result = handLandmarker.detect(mpImage)

        if (result.landmarks().isNotEmpty()) {

            overlayView.post {
                overlayView.setResults(
                    result,
                    rotatedBitmap.height,
                    rotatedBitmap.width,
                    RunningMode.LIVE_STREAM
                )
            }

            val frameData = landmarksToFloatArray(result.landmarks()) // pass full list for both hands

            sequenceBuffer.add(frameData)
            if (sequenceBuffer.size > SEQUENCE_LENGTH) {
                sequenceBuffer.removeAt(0)
            }

            if (sequenceBuffer.size == SEQUENCE_LENGTH) {
                sendSequenceToBackend(sequenceBuffer.toList())
            }

        } else {
            overlayView.post { overlayView.clear() }
        }


        imageProxy.close()
    }


    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun landmarksToFloatArray(
        hands: List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>?
    ): FloatArray {
        val data = FloatArray(126)  // 42 landmarks * 3 coords
        var index = 0

        // left hand landmarks (first hand if available)
        if (hands != null && hands.size > 0) {
            for (lm in hands[0]) {
                data[index++] = lm.x()
                data[index++] = lm.y()
                data[index++] = lm.z()
            }
        } else {
            repeat(63) { data[index++] = 0f }
        }

        // right hand landmarks (second hand if available)
        if (hands != null && hands.size > 1) {
            for (lm in hands[1]) {
                data[index++] = lm.x()
                data[index++] = lm.y()
                data[index++] = lm.z()
            }
        } else {
            repeat(63) { data[index++] = 0f }
        }

        return data
    }

    private fun sendSequenceToBackend(sequence: List<FloatArray>) {

        val frames = sequence.map { it.toList() }

        val json = mapOf("frames" to frames)

        Thread {
            try {
                val url = java.net.URL("http://192.168.1.18:8000/sign")
                val conn = url.openConnection() as java.net.HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = Gson().toJson(json)

                conn.outputStream.use {
                    it.write(body.toByteArray())
                }

                val response = conn.inputStream.bufferedReader().readText()
                Log.d("SIGN_RESPONSE", response)
                val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                val letter = jsonObject.get("letter")?.asString ?: ""
                GlobalState.letter.value = letter

            } catch (e: Exception) {
                Log.e("SIGN_ERROR", e.toString())
            }
        }.start()
    }


}

private fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
data class HandPoint(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float
)
