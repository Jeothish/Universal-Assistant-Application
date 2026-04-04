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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
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
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import OverlayView
import android.content.Intent
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.chaquo.python.android.AndroidPlatform
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import android.os.Handler
import android.os.Looper
import androidx.compose.material.icons.filled.MonitorHeart

class MainActivity : ComponentActivity() {

    private val cameraPermission = Manifest.permission.CAMERA //permissions
    private val micPermission = Manifest.permission.RECORD_AUDIO
    private val notificationPermission = Manifest.permission.POST_NOTIFICATIONS


    private val requestPermissionLauncher = //ask user for perms
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] == true
            val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
            val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true //location for weather
            if (cameraGranted && micGranted && notificationGranted) {
                if (locationGranted) fetchUserCity()//get user city if perm granted



                setContent {
                    MyApplicationTheme {
                        MyApplicationApp()
                    }
                }
            } else {
                // Permission denied add handling
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) { //init code that runs on app start
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Python.isStarted()) {//start python
            Python.start(AndroidPlatform(this))
        }

        lifecycleScope.launch(Dispatchers.IO) { //init llm, db + stored user settings
            try {
                GlobalState.aslTimer.value = AppPreferences.loadTimer(applicationContext)
                GlobalState.ttsSpeechRate.value = AppPreferences.loadSpeechSpeed(applicationContext)
                GlobalState.ttsPitch.value = AppPreferences.loadSpeechPitch(applicationContext)
                val db = DatabaseProvider.getDatabase(applicationContext)
                val wikiRepo = WikiRepository(db.wikiDao())
                val weatherRepo = WeatherRepository(db.weatherDao())
                val llm = LocalLLM(wikiRepo)
                llm.initialize(context = applicationContext)
                GlobalState.localLLM = llm
                GlobalState.llmReady.value = true
                Log.d("LLM", "LLM loaded successfully, llmReady = ${GlobalState.llmReady.value}")
            } catch(e: Exception) {
                Log.e("LLM", "LLM loading failed: $e")
            }
        }

            val cameraGranted = ContextCompat.checkSelfPermission( //check perms
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (android.os.Build.VERSION.SDK_INT >= 33){ //push notifs
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }


        else{
            true
        }
        val locationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED


        if (cameraGranted && micGranted && notificationGranted) {
            if (locationGranted) fetchUserCity()
            requestOverlayPermission()

            setContent {
                MyApplicationTheme {
                    MyApplicationApp()
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        val handler = Handler(Looper.getMainLooper()) //for ram monitoring
        val memoryRunnable = object : Runnable {
            override fun run() {
                GlobalState.ram.value = getMemoryUsage()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(memoryRunnable)

    }

    private fun requestOverlayPermission() { //ask for overlay permission for alarm screen
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun fetchUserCity() { //get user city from coords
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return//ensure permission is granted

        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault()) //convert coords to city name
                val city = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.locality
                if (city != null) {
                    GlobalState.userCity.value = city
                    Log.d("LOCATION", "City: $city")
                }
            }
        }
    }

    private fun getMemoryUsage(): String { //get ram usage
        val debug = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(debug)

        val total = debug.totalPss / 1024

        return total.toString()
    }

}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        HomePage(modifier = Modifier.padding(innerPadding))
    }


}

//settings
@Composable
fun SettingsScreen(modifier: Modifier = Modifier,returnToChat: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var speechSpeed by remember { mutableStateOf(GlobalState.ttsSpeechRate.value) } //values to be canged in settings
    var speechPitch by remember { mutableStateOf(GlobalState.ttsPitch.value) }
    var timer by remember { mutableStateOf(GlobalState.aslTimer.value.toFloat()) }
    var saved by remember { mutableStateOf(false) }
    val ttsManager = remember { TTSManager(context) }
    var expanded by remember { mutableStateOf(false) }
//    val languages = ttsManager.supportedLanguages
//    val selectedLanguage = GlobalState.ttsLanguage.value
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp)){

        //return button
    Button(onClick = returnToChat, modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFDE0F0F),
        contentColor = Color(0xFFFFFFFF),
    ))

    {
        Row() {
            Icon(imageVector = Icons.Default.KeyboardReturn , contentDescription = null,modifier = Modifier.size(36.dp))
            Text(text="Return to Home", fontSize = 25.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()

            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF2A2A38))
            .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(24.dp))
            .padding(26.dp),

    ) {

        Column()
        {
            Row( //tts settings
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2196F3))
                        .padding(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(106.dp),
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,

                        )
                }
                Text(
                    text = "Voice & Speech",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(19.dp),
                    color = Color(0xFFFFC63A)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row() { //speech speed

                Text(
                    text = "Speech Speed",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    //modifier = Modifier.padding(19.dp),
                    color = Color(0xFFFFFFFF)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${"%.2f".format(speechSpeed)}x",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    //modifier = Modifier.padding(19.dp),
                    color = Color(0xFFFFC63A),
                    textAlign = TextAlign.End

                )
            }

            Slider( //slider for speech speed
                value = speechSpeed,
                onValueChange = {speechSpeed = it;GlobalState.ttsSpeechRate.value = it },
                onValueChangeFinished = {scope.launch {AppPreferences.saveSpeechSpeed(context, speechSpeed)}},
                valueRange = 0.5f..2.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color(0xFFFFC63A),
                    inactiveTrackColor = Color(0xFF6F6F6D)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {

                Text(
                    text = "Slow",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    color = Color(0xFF6F6F6D)
                )

                Text(
                    text = "Normal",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6F6F6D)
                )

                Text(
                    text = "Fast",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    color = Color(0xFF6F6F6D)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row() { //speech pitch

                Text(
                    text = "Speech Pitch",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    //modifier = Modifier.padding(19.dp),
                    color = Color(0xFFFFFFFF)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${"%.2f".format(GlobalState.ttsPitch.value)}x",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    //modifier = Modifier.padding(19.dp),
                    color = Color(0xFFFFC63A),
                    textAlign = TextAlign.End

                )
            }

            Slider( //slider for speech pitch
                value = speechPitch,
                onValueChange = {speechPitch = it;GlobalState.ttsPitch.value = it },
                onValueChangeFinished = {scope.launch {AppPreferences.saveSpeechPitch(context, GlobalState.ttsPitch.value)}},
                valueRange = 0.7f..1.5f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color(0xFFFFC63A),
                    inactiveTrackColor = Color(0xFF6F6F6D)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {

                Text(
                    text = "Low",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    color = Color(0xFF6F6F6D)
                )

                Text(
                    text = "Normal",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6F6F6D)
                )

                Text(
                    text = "High",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    color = Color(0xFF6F6F6D)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))


        }
    }

        Spacer(modifier = Modifier.height(20.dp))

        //asl settings
        Box(
            modifier = Modifier.fillMaxWidth()

                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2A2A38))
                .border(2.dp, Color(0xFF673AB7), RoundedCornerShape(24.dp))
                .padding(26.dp),

            ) {

            Column() {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF673AB7))
                            .padding(16.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(106.dp),
                            imageVector = Icons.Default.SignLanguage,
                            contentDescription = null,

                            )
                    }
                    Text(
                        text = "ASL Settings",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(19.dp),
                        color = Color(0xFFFFC63A)
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row() { //addd letter to message delay

                    Text(
                        text = "Add Letter to Message Delay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        //modifier = Modifier.padding(19.dp),
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${timer.toInt()}s",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        //modifier = Modifier.padding(19.dp),
                        color = Color(0xFFFFC63A),
                        textAlign = TextAlign.End

                    )
                }


                Slider(
                    value = timer,
                    onValueChange = {timer = it;GlobalState.aslTimer.value = timer.toInt() },
                    onValueChangeFinished = {
                        scope.launch {
                            AppPreferences.saveTimer(context, timer.toInt())
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF673AB7),
                        activeTrackColor = Color(0xFFFFC107),
                        inactiveTrackColor = Color(0xFF6F6F6D)
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {

                    Text(
                        text = "Quick",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        color = Color(0xFF6F6F6D)
                    )

                    Text(
                        text = "Slow",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6F6F6D)
                    )

                    Text(
                        text = "Slower",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = Color(0xFF6F6F6D)
                    )
                }

                }
            }

        Spacer(modifier = Modifier.height(20.dp))

        //resoucre monitoring settings
        Box(
            modifier = Modifier.fillMaxWidth()

                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2A2A38))
                .border(2.dp, Color(0xFFF44336), RoundedCornerShape(24.dp))
                .padding(26.dp),

            ) {

            Column() {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF44336))
                            .padding(16.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(106.dp),
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = null,

                            )
                    }
                    Text(
                        text = "Resource Monitoring",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(19.dp),
                        color = Color(0xFFFFC63A)
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row() {

                    Text(
                        text = "Show app RAM usage on chat screen.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF)
                    )
                }

                //turn off on ram monitoring

                Button(onClick = {GlobalState.showRam.value = !GlobalState.showRam.value},
                    content = {if (GlobalState.showRam.value) Text(text = "Hide", fontSize = 25.sp) else Text(text = "Show", fontSize = 25.sp)},
                    modifier = Modifier.height(100.dp).width(500.dp).padding(16.dp),shape = RoundedCornerShape(12.dp)
                    , colors = if (GlobalState.showRam.value) {ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0707),
                    contentColor = Color(0xFFFFFFFF)
                    )} else{ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF02),
                        contentColor = Color(0xFFFFFFFF)
                    )}
                )
            }
        }
        }


    }


@Composable
fun ProfileScreen(modifier: Modifier = Modifier,returnToChat: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile")
    }
}


//launch camera for asl input
@Composable
fun CameraDet() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val overlayView = remember { OverlayView(context, null) } //purple skelton overlay for asl

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

    LaunchedEffect(Unit) { //launch cam
        startCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            overlayView = overlayView
        )
    }
}

//start cam analyzing for asl/mp
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
            setSurfaceProvider(previewView.surfaceProvider)//camera feed
        }

        val imageAnalysis = ImageAnalysis.Builder() //for mp
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) //yuv format to be conv to jpeg->bitmap for mp
            .build()

        val analyzer = HandAnalyzer(context, overlayView)//instantiate hand analyzer class for mp
        imageAnalysis.setAnalyzer(
            Executors.newSingleThreadExecutor(),
            analyzer
        )

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA, //use front cam
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("Camera", "Bind failed", e)
        }

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

@Composable //greeting text on home screen
fun Greeting(time: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ){
        Text(text = "Good $time,\n\nhow can I help?",
            color= Color(255, 193, 7, 255),
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 64.dp)
        )
    }
}







