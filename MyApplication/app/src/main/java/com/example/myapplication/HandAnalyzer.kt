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

class HandAnalyzer(context: Context, private val overlayView: OverlayView) : ImageAnalysis.Analyzer {

    private val handLandmarker: HandLandmarker //landmarker object
    private val TAG = "HandAnalyzer" //tag for logs

    private val aslPrompt = GlobalState.aslPrompt //user prompt built via asl

    private var prevLetter = "" // last detected sign

    private var prevCall=0 //time before last time model is called
    private var delay = 10 // delay between asl model detection

    private var timer: Long =0 // time between when letter is added to message
    private val lock = Any() //lock to prevent race conditions

    //left hand model
    private val classifierL = ASLProcessing(context, "asl_mediapipe_model_finalL.tflite", "asl_labels_finalL.txt")
    //right hand model
    private val classifierR = ASLProcessing(context, "asl_mediapipe_model_finalR.tflite", "asl_labels_finalR.txt")

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder() //hand landmarker options
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")//load mediapipe model
                    .build()
            )
            .setNumHands(2)//max hands mp can detect in a frame
            .setMinHandDetectionConfidence(0.7f)
            .setMinHandPresenceConfidence(0.7f)//mp confidence
            .setMinTrackingConfidence(0.7f)
            .setRunningMode(RunningMode.LIVE_STREAM)//live stream mode for live video input
            .setResultListener { result, input ->
                if (result.landmarks().isNotEmpty()) {
                    GlobalState.aslHands.value = result.landmarks().size//check how many hands the user is shwoing
                    if (GlobalState.aslHands.value >=2){
                        GlobalState.aslHandsError.value = true//if >2 show warning
                    }
                    else{
                        GlobalState.aslHandsError.value = false
                    }

                    overlayView.post {//purple hand overlay skeleton
                        overlayView.setResults(
                            result,
                            input.height,
                            input.width,
                            RunningMode.LIVE_STREAM
                        )
                    }

                    val landmarks = result.landmarks()[0]//landmarks

                    //get wgich hand is on screen
                    val handedness = result.handedness()[0][0]
                    val detectedHand = handedness.categoryName()

                   // Log.d(TAG, "MediaPipe detected: $detectedHand hand (score: ${handedness.score()})")


                    val normalizedFeatures = normalizeLandmarks(landmarks)//normalize
                    if (prevCall > delay-1) {//ensure enough time has elapsed since model was last called

                        localPredict(normalizedFeatures, detectedHand)//call model
                        prevCall=0

                    }
                    else{
                        prevCall++
                    }


                } else {//if no hands on screen reset all timers
                    overlayView.post { overlayView.clear() }//clear hand skeleton
                    GlobalState.letter.value = ""
                    timer = System.currentTimeMillis()
                    prevCall = 0

                }
            }
            .setErrorListener { error ->
                Log.e(TAG, "Hand Landmarker Error: ${error.message}")
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)//intiialize landmarker
    }

    override fun analyze(imageProxy: ImageProxy) {//called by cameraX every frame
        //image proxy =  raw camra data of a image  (frame)

        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees//how many degress to be upright (raw image not always upright)
        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
        val flippedBitmap = flipBitmap(rotatedBitmap)//flip image (front cams are inverted)

        val mpImage = BitmapImageBuilder(flippedBitmap).build()//wrap for mediapipe processing
        val frameTime = System.currentTimeMillis()
        handLandmarker.detectAsync(mpImage, frameTime)//send image to mp for lm extractuion

        imageProxy.close()//close to prevent mem leaks
    }

    private fun normalizeLandmarks(landmarks: List<NormalizedLandmark>): FloatArray { //normalize landmarks to wrist to prevent issues with where hand is on scren
        //21 lms(w/ x,y,z coords) from mediapipe in list

        var points = landmarks.map { //convert each lm to flat xyz float array
            floatArrayOf(it.x(), it.y(), it.z())
        }.toTypedArray()

        // normalize to wrist
        val wrist = points[0]
        val normalized = points.map { point ->
            floatArrayOf(
                point[0] - wrist[0], //x
                point[1] - wrist[1], //y
                point[2] - wrist[2] //z
            )
        }.toTypedArray()

        val middleFingerMCP = normalized[9] //use middle finger knuckle (mcp) as reference for scaling and distance
        val scale = sqrt(
            middleFingerMCP[0] * middleFingerMCP[0] +
                    middleFingerMCP[1] * middleFingerMCP[1] +
                    middleFingerMCP[2] * middleFingerMCP[2]
        )//dist from mcp to wrist to get size(scale) of hand


        val scaled = if (scale > 0f) {//divide every coord by scale to ensure distance from camera makes no diff
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

        return scaled.flatMap { it.toList() }.toFloatArray()//21 xyz arrays to 1 floatt array of 63 fp
    }

    private fun localPredict(features: FloatArray, detHand: String){ //call asl model
        Thread {
            try {

                var prediction: Pair<String, Float>//prediction from model string = letter, float = confidence

                if (detHand.lowercase() == "right") {
                    //call left hand model since mediapipe inverts
                    prediction = classifierL.predict(features)
                    GlobalState.letter.value = prediction.first.lowercase()
                    //Log.d(TAG, "Predicted: $prediction ")
                    println(prediction)

                } else if (detHand.lowercase() == "left") {
                    //call roght hand model
                    prediction = classifierR.predict(features)
                    GlobalState.letter.value = prediction.first.lowercase()
                    //Log.d("PRED", "Predicted: $prediction ")
                    println(prediction)
                }
                val letter = GlobalState.letter.value
                println(timer)
                synchronized(lock) { //prevent race cond by only allowing 1 thread at atime

                    // asl senetnce construction using delay
                    if (prevLetter == "") {  //init
                        prevLetter = letter
                        timer = System.currentTimeMillis()
                    }
                    if (letter == prevLetter) {//if sign hasnt changed

                        if ((System.currentTimeMillis() - timer)/1000 >= GlobalState.aslTimer.value) { //if sign held up for long enough add tro message

                            if (letter == "del" && aslPrompt.value.isNotEmpty()) { //delete a letter from the message
                                aslPrompt.value = aslPrompt.value.dropLast(1).toMutableList()
                                GlobalState.letterDeleted.value = true

                            } else if (letter == "space") {//add space to msg
                                aslPrompt.value = (aslPrompt.value + " ").toMutableList()
                                GlobalState.spaceAdded.value = true

                            } else if (letter != "del" && prevLetter != "del") {//add letter to msg

                                aslPrompt.value = (aslPrompt.value + prevLetter).toMutableList()
                            }

                            timer = System.currentTimeMillis()
                        }
//
                    } else {//if sign changed reset timer
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

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {//helper to roatate bitmap
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }//make transform matrix to rotate
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)//new rotated bitmap using trans. matrix
    }

    private fun flipBitmap(bitmap: Bitmap): Bitmap {//helper to flip bitmap (front cams are inverted)
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)//make matrix that scales x by -1, horz mirror
            //px & py = pivot point to flip from center
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)//new flipped bitmap
    }

    fun close() {
        handLandmarker.close()
    }
}

private fun ImageProxy.toBitmap(): Bitmap { //helper to convert image proxy to bitmap (conv camX raw img to bitmap)
    val yBuffer = planes[0].buffer //bright ness read as buffer
    val uBuffer = planes[1].buffer //colour
    val vBuffer = planes[2].buffer //colour

    val ySize = yBuffer.remaining() //size of each plane
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize) //single byte array to hold all 3 planes (nv21 format)

    yBuffer.get(nv21, 0, ySize) //copy planes into array
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)//wrap in androids yuv imageclass
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)//convert to jpeg
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)//conv jpeg to bitmap
}
