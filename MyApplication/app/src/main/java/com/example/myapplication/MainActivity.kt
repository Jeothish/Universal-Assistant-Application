package com.example.myapplication


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

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


class MainActivity : ComponentActivity() {

    private val cameraPermission = Manifest.permission.CAMERA

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setContent {
                MyApplicationTheme {
                    MyApplicationApp()
                }
            }
        } else {
            // Permission denied: show message or handle gracefully
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted — show app content
            setContent {
                MyApplicationTheme {
                    MyApplicationApp()
                }
            }
        } else {
            // Request permission
            requestPermissionLauncher.launch(cameraPermission)
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
fun Chat(modifier: Modifier){
    Box(modifier = Modifier.fillMaxSize())
    {
        CameraDet()
        Box(modifier=Modifier.fillMaxSize().padding(16.dp)) {
            Greeting(
                time = "Evening",
                modifier = Modifier.align(Alignment.Center)

            )
            Button("Voice Chat", Alignment.BottomStart)
            Button("Reminders", Alignment.BottomEnd)
        }
    }

}

@Composable
fun CameraDet() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).also {
                previewView = it
            }
        }
    )

    LaunchedEffect(previewView) {
        previewView?.let {
            startCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = it
            )
        }
    }
}

fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
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
            HandAnalyzer(context)
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
        Text(text = "Good $time,\n\nHow can I help?",color= Color(222,172,255), fontSize = 32.sp)
    }
}


@Composable
fun Button(text : String,  contentAlignment: Alignment) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Button(
            onClick = { /* handle click here */ },
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




