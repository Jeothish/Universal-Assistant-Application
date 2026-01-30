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
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
import OverlayView
class HandAnalyzer(
    context: Context,
    private val overlayView: OverlayView
) : ImageAnalysis.Analyzer {

    private val handLandmarker: HandLandmarker
    private val TAG = "HandAnalyzer"

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build()
            )
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.7f)
            .setMinHandPresenceConfidence(0.7f)
            .setMinTrackingConfidence(0.7f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, input ->
                if (result.landmarks().isNotEmpty()) {
                    overlayView.post {
                        overlayView.setResults(
                            result,
                            input.height,
                            input.width,
                            RunningMode.LIVE_STREAM
                        )
                    }

                    val landmarks = result.landmarks()[0]

                    //if right hand flip landmarks (front cam flips anyways so it sees left as right)
                    val handedness = result.handedness()[0][0]
                    val detectedHand = handedness.categoryName()

                    Log.d(TAG, "MediaPipe detected: $detectedHand hand (score: ${handedness.score()})")




                    val normalizedFeatures = normalizeLandmarks(landmarks)
                    sendLandmarksToBackend(normalizedFeatures, detectedHand)
                } else {
                    overlayView.post { overlayView.clear() }
                    GlobalState.letter.value = ""
                }
            }
            .setErrorListener { error ->
                Log.e(TAG, "Hand Landmarker Error: ${error.message}")
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
        val flippedBitmap = flipBitmap(rotatedBitmap)

        val mpImage = BitmapImageBuilder(flippedBitmap).build()
        val frameTime = System.currentTimeMillis()
        handLandmarker.detectAsync(mpImage, frameTime)

        imageProxy.close()
    }

    private fun normalizeLandmarks(landmarks: List<NormalizedLandmark>): FloatArray {
        var points = landmarks.map {
            floatArrayOf(it.x(), it.y(), it.z())
        }.toTypedArray()

        // normalize to wrist
        val wrist = points[0]
        val normalized = points.map { point ->
            floatArrayOf(
                point[0] - wrist[0],
                point[1] - wrist[1],
                point[2] - wrist[2]
            )
        }.toTypedArray()

        val middleFingerMCP = normalized[9]
        val scale = sqrt(
            middleFingerMCP[0] * middleFingerMCP[0] +
                    middleFingerMCP[1] * middleFingerMCP[1] +
                    middleFingerMCP[2] * middleFingerMCP[2]
        )


        val scaled = if (scale > 0f) {
            normalized.map { point ->
                floatArrayOf(
                    point[0] / scale,
                    point[1] / scale,
                    point[2] / scale
                )
            }
        } else {
            normalized.toList()
        }

        return scaled.flatMap { it.toList() }.toFloatArray()
    }

    private fun sendLandmarksToBackend(features: FloatArray, detHand: String) {
        Thread {
            try {
                val url = java.net.URL("http://192.168.1.11:8000/predict")


                val conn = url.openConnection() as java.net.HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val json = mapOf("features" to features.toList(), "hand" to detHand)
                val body = Gson().toJson(json)

                conn.outputStream.use {
                    it.write(body.toByteArray())
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()

                    val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                    val letter = jsonObject.get("letter")?.asString ?: ""
                    val confidence = jsonObject.get("confidence")?.asFloat ?: 0f

                    Log.d(TAG, "Predicted: $letter (confidence: $confidence)")
                    GlobalState.letter.value = letter
                } else {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown"
                    Log.e(TAG, "HTTP Error $responseCode: $errorBody")
                }

                conn.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Backend Error: ${e.message}", e)
            }
        }.start()
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun close() {
        handLandmarker.close()
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

