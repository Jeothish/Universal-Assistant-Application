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
    private val inputs = arrayOf("","","","")
    private val aslPrompt = GlobalState.aslPrompt

    private var prevLetter = ""

    private var prevCall=0
    private var delay = 10

    private var timer: Long =0
    private val lock = Any()

    private val classifierL = ASLProcessing(context, "asl_mediapipe_model_finalL.tflite", "asl_labels_finalL.txt")
    private val classifierR = ASLProcessing(context, "asl_mediapipe_model_finalR.tflite", "asl_labels_finalR.txt")

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
                    GlobalState.aslHands.value = result.landmarks().size
                    if (GlobalState.aslHands.value >=2){
                        GlobalState.aslHandsError.value = true
                    }
                    else{
                        GlobalState.aslHandsError.value = false
                    }

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
                    if (prevCall > delay-1) {
                       //sendLandmarksToBackend(normalizedFeatures, detectedHand)
                        localPredict(normalizedFeatures, detectedHand)
                        prevCall=0

                    }
                    else{
                        prevCall++
                    }


                } else {
                    overlayView.post { overlayView.clear() }
                    GlobalState.letter.value = ""
                    timer = System.currentTimeMillis()
                    prevCall = 0

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

    private fun localPredict(features: FloatArray, detHand: String){
        Thread {
            try {

                var prediction: Pair<String, Float>
                if (detHand.lowercase() == "right") {
                    //use left since mediapipe inverts
                    prediction = classifierL.predict(features)
                    GlobalState.letter.value = prediction.first.lowercase()
                    Log.d(TAG, "Predicted: $prediction ")
                    println(prediction)

                } else if (detHand.lowercase() == "left") {
                    //use roght
                    prediction = classifierR.predict(features)
                    GlobalState.letter.value = prediction.first.lowercase()
                    Log.d("PRED", "Predicted: $prediction ")
                    println(prediction)
                }
                val letter = GlobalState.letter.value
                println(timer)
                synchronized(lock) {

                    if (prevLetter == "") { // asl senetnce construction using delay
                        prevLetter = letter
                        timer = System.currentTimeMillis()
                    }
                    if (letter == prevLetter) {


                        if ((System.currentTimeMillis() - timer)/1000 >= GlobalState.aslTimer.value) {

                            if (letter == "del" && aslPrompt.value.isNotEmpty()) {
                                aslPrompt.value = aslPrompt.value.dropLast(1).toMutableList()
                                GlobalState.letterDeleted.value = true
                            } else if (letter == "space") {
                                aslPrompt.value = (aslPrompt.value + " ").toMutableList()
                                GlobalState.spaceAdded.value = true
                            } else if (letter != "del" && prevLetter != "del") {

                                aslPrompt.value = (aslPrompt.value + prevLetter).toMutableList()
                            }

                            timer = System.currentTimeMillis()
                        }
//
                    } else {
                        prevCall = 0
                        timer = System.currentTimeMillis()
                    }
                    prevLetter = letter
                }
            }
            catch(e: Exception){
                Log.e("ASLTHREAD", "Error: ${e.message}", e)
            }
        }
            .start()


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
