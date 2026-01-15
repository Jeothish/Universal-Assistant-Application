package com.example.myapplication


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors


import android.Manifest
import android.R
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.zIndex
import java.time.LocalTime

import com.example.myapplication.GlobalState
import com.example.myapplication.audio.*



class MainActivity : ComponentActivity() {

    private val cameraPermission = Manifest.permission.CAMERA
    private val micPermission = Manifest.permission.RECORD_AUDIO



    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] == true
            val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

            if (cameraGranted && micGranted) {
                setContent {
                    MyApplicationTheme {
                        MyApplicationApp()
                    }
                }
            } else {
                // Permission denied — handle gracefully
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (cameraGranted && micGranted) {
            setContent {
                MyApplicationTheme {
                    MyApplicationApp()
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CHAT) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label,
                            tint = Color(222,172,255)
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },

                )
            }
        }
    )


    {
        Scaffold(modifier = Modifier.fillMaxSize())

        { innerPadding ->
            when (currentDestination){
                AppDestinations.CHAT ->{
                    Chat(modifier = Modifier.padding(innerPadding))
                }
                AppDestinations.SETTINGS -> {
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppDestinations.PROFILE -> {
                    ProfileScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }


            }

        }
    }

}
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings")
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile")
    }
}

@Composable
fun textBar(onSend: (String) -> Unit){
    var text by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 560.dp), verticalAlignment = Alignment.CenterVertically)
    {
        OutlinedTextField(value = text, onValueChange = {text=it},modifier=Modifier.weight(1f), placeholder = {Text("Ask Anything..")}, singleLine = true)
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text=""
                    }})
        {Text("send")}
    }
}

@Composable
fun Chat(modifier: Modifier){
    val asl by GlobalState.asl
    val prompt by GlobalState.vc_prompt
    var greeting by GlobalState.greeting
    val thinking by GlobalState.thinking
    val news by GlobalState.newsList
    val letter by GlobalState.letter
    val context = LocalContext.current
    val recorder = remember { audio(context) }
    var recording by remember { mutableStateOf(false) }
    var showTestInput by remember {mutableStateOf(false)}
    var aslTokens by GlobalState.aslTokens

    Box(modifier = Modifier.fillMaxSize())
    {

        if (asl) {
            CameraDet()
        }

        if (aslTokens.isNotEmpty()){
            ASLRenderer(tokens = aslTokens)
        }

        Box(modifier=Modifier.fillMaxSize().padding(16.dp)) {


            if(asl || recording || GlobalState.vc_intent.value != ""){
                greeting = false

            }
            if (greeting){
                val hour = LocalTime.now().hour
                var time = ""
                if (hour > 4 && hour <11){
                    time = "morning"
                }
                else if (hour>11 && hour < 18){
                    time = "afternoon"
                }
                else{
                    time = "evening"
                }

                Greeting(time = time,modifier = Modifier.align(Alignment.Center))
            }
            else if (asl){
                Text(text="Detected Sign: $letter",color= Color.Magenta, fontSize = 24.sp,modifier=Modifier.align(
                    Alignment.TopEnd).padding(end = 80.dp, top=20.dp, start = 40.dp))
            }
            if (GlobalState.thinking.value){
                Text(text="Thinking...",color= Color.Magenta, fontSize = 32.sp,modifier=Modifier.align(
                    Alignment.TopEnd).padding(end = 90.dp, top=50.dp, start = 50.dp, bottom = 10.dp))


            }
            val r = GlobalState.vc_result.value
            val w = GlobalState.weather.value
            val city = GlobalState.city.value
            val intent = GlobalState.vc_intent.value

            print(r)


            if ((intent == "weather" || intent == "chat")  && !asl) {



                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end=20.dp, start =30.dp, bottom = 200.dp)
                )

                {
                    Text(
                        text = prompt.uppercase(),
                        color = Color.Green,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp, start = 10.dp, end = 0.dp)
                    )
                    if (intent == "chat") {
                        Text(
                            text = r,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 50.dp,start=20.dp)
                        )


                    }
                    else{
                        Text(text=city.uppercase(),color=Color.Magenta)

                        Text(text="Temperature: ${w.temperature} °C")
                        Text("Wind Speed: ${w.windSpeed} km/h")
                        Text("Forecast: ${w.forecast}")
                        Text("Time: ${w.time}")
                    }





                }
            }
            else if (intent=="news" && !asl){


                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(top=110.dp,start=20.dp,end=20.dp,bottom=220.dp)
                ) {

                    Text(
                        text = prompt.uppercase(),
                        color = Color.Green,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 30.dp, start = 0.dp, end=0.dp, top=20.dp)
                    )



                    LazyColumn { items(news){item ->



                        Text(text="Title: ${item.Title}",fontSize = 20.sp,
                        color = Color.Magenta,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp))





                        Text( text = "${item.Link}",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp))

                        Text(text = "Published: ${item.Published}\n",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp))} }


                }

            }

            if(showTestInput){
                ASLTestInput()
            }


            Column(modifier = Modifier.fillMaxSize()) {

                Spacer(modifier = Modifier.weight(1f))

                textBar { input ->
                    GlobalState.thinking.value = true
                    GlobalState.vc_prompt.value = input

                    recorder.sendTextToBackend(input)
                }
            }


            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ){
                Button(
                    onClick = {
                        if (!recording) {
                            recorder.startRec()
                            recording = true
                        } else {

                            val file = recorder.stopRec()

                            file?.let { recorder.sendAudioToBackend(it) }
                            recording = false



                        }
                    },colors = ButtonDefaults.buttonColors(
                        if (recording) Color.Red else Color(222,172,255) ,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(16.dp) // distance from screen edges
                        .height(100.dp)
                        .zIndex(1f)
                        .width(130.dp),

                    ) {
                    Text(if (recording) "Stop Recording" else "Voice Chat")
                }}

            // Button("Voice Chat", Alignment.BottomStart)
            Button("Sign Language", Alignment.BottomEnd)
            FutureButton(text = "Text -> ASL Testing", contentAlignment = Alignment.BottomCenter, onClick = {showTestInput = !showTestInput},modifier = Modifier.padding(bottom = 110.dp,end = 185.dp))
        }
    }

}

@Composable
fun CameraDet() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current


    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
    } }
    val overlayView = remember { OverlayView(context, null) }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            factory = { overlayView },
            modifier = Modifier.fillMaxSize()
        )
    }

    LaunchedEffect(Unit) {
        startCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            overlayView = overlayView
        )
    }
}


fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    overlayView: OverlayView
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            Executors.newSingleThreadExecutor(),
            HandAnalyzer(context, overlayView)
        )

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            imageAnalysis
        )

    }, ContextCompat.getMainExecutor(context))
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CHAT("Chat", Icons.Default.Edit),
    SETTINGS("Settings", Icons.Default.Settings),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(time: String, modifier: Modifier) {
    Box(

        modifier = Modifier.fillMaxSize(),contentAlignment = Alignment.Center
    ){
        Text(text = "Good $time,\n\nhow can I help?",color= Color(222,172,255), fontSize = 32.sp)
    }
}


@Composable
fun Button(text : String,  contentAlignment: Alignment) {
    var asl by GlobalState.asl
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Button(

            onClick = {asl= !asl},
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(222,172,255),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(16.dp) // distance from screen edges
                .height(100.dp)
                .width(160.dp)
        ) {
            Text(text)
        }
    }
}

@Composable
fun FutureButton(text:String,contentAlignment: Alignment,onClick: () -> Unit,modifier: Modifier = Modifier){
    Box(modifier = Modifier.fillMaxSize().then(modifier),contentAlignment = contentAlignment)
    {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(222,172,255),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(16.dp) // distance from screen edges
                .height(100.dp)
                .width(150.dp)
        )
        {
            Text(text)
        }
    }
}






