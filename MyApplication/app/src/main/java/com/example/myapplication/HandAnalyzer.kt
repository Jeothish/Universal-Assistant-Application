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
    private var deleted = false
    private var confirmed = false
    private var prevLetter = ""

    private var prevCall=0
    private var delay = 10

    private var prevAction =""
    private var timer =0
    private val lock = Any()

    private val classifierL = ASLProcessing(context, "asl_mediapipe_model_L.tflite", "asl_labels_Left.txt")
    private val classifierR = ASLProcessing(context, "asl_mediapipe_model_R.tflite", "asl_labels_Right.txt")

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
                    timer = 0
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
                        timer = 0
                    }
                    if (letter == prevLetter) {


                        if (timer >= 3) {

                            if (letter == "del" && aslPrompt.value.isNotEmpty()) {
                                aslPrompt.value = aslPrompt.value.dropLast(1).toMutableList()
                            } else if (letter == "space") {
                                aslPrompt.value = (aslPrompt.value + " ").toMutableList()
                            } else if (letter != "del" && prevLetter != "del") {

                                aslPrompt.value = (aslPrompt.value + prevLetter).toMutableList()
                            }

                            timer = -13
                        } else {
                            timer++

                        }
                    } else {
                        prevCall = 0
                        timer = 0
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

    private fun sendLandmarksToBackend(features: FloatArray, detHand: String) {
        Thread {
            try {
                val url = java.net.URL("http://${GlobalState.serverIP.value}:8000/predict")


                val conn = url.openConnection() as java.net.HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val json = mapOf("features" to features.toList(), "hand" to detHand)//json object to send to backend
                val body = Gson().toJson(json)

                conn.outputStream.use {
                    it.write(body.toByteArray())
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()

                    val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                    var letter = jsonObject.get("letter")?.asString ?: ""
                    val confidence = jsonObject.get("confidence")?.asFloat ?: 0f

                    Log.d(TAG, "Predicted: $letter (confidence: $confidence)")
                    GlobalState.letter.value = letter
                    letter = letter.lowercase()


                    println(timer)
                    synchronized(lock) {
                        if (prevLetter == "") { // asl senetnce construction using delay
                            prevLetter = letter
                            timer = 0
                        }
                        if (letter == prevLetter) {

                            if (timer >= 13) {

                                if (letter == "del" && aslPrompt.value.isNotEmpty()) {
                                    aslPrompt.value = aslPrompt.value.dropLast(1).toMutableList()
                                } else if (letter == "space") {
                                    aslPrompt.value = (aslPrompt.value + " ").toMutableList()
                                } else if (letter != "del" && prevLetter != "del") {

                                    aslPrompt.value = (aslPrompt.value + prevLetter).toMutableList()
                                }

                                timer = -13
                            } else {
                                timer++

                            }
                        } else {
                            prevCall = 0
                            timer = 0
                        }
                        prevLetter = letter
                    }

                    //asl word construction using "space" as confirm sign

//                    if (prevLetter == "" && letter != "space" && letter!="del"){ //1st time condition
//                        prevLetter = letter
//                    }
//
//                    if (letter != "space" && letter != "del"){ //
//                        confirmed = false
//                        deleted = false
//
//                    }
//                    else if (letter == "space" && !deleted && prevAction == "del"){
//                        aslPrompt.removeAt(aslPrompt.lastIndex)
//                        deleted = true
//                        confirmed =true
//                    }
//
//                    if (letter == "space" && !confirmed && prevLetter != ""){
//
//                        aslPrompt.add(prevLetter)
//                        confirmed = true
//
//                        if (prevAction == "del") {
//                            deleted = false
//                        }
//
//                    }
//
//                    if (letter!="space" && letter!= "del") {
//                        prevLetter = letter
//                    }
//                    else{
//                        if (prevAction == "space"){
//                            deleted = false
//                        }
//                        prevAction = letter
//
//                    }

                    println(prevLetter)
                    println(letter)
                    println(aslPrompt)

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
