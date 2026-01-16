package com.example.myapplication

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay


@Composable
fun ASLTestInput() {
    var testSentence by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ){
        OutlinedTextField(
            value = testSentence,
            onValueChange = { testSentence = it },
            label = { Text("Enter sentence") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val tokens = mutableListOf<String>()
                testSentence.text.forEach { c ->
                    if (c.isLetter()) tokens.add(c.uppercaseChar().toString())
                }
                GlobalState.aslTokens.value = tokens
                Log.d("ASL_TEST", "Tokens: $tokens")
            },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(50.dp)
        ){
            Text("Test ASL")
        }
    }
}

@Composable
fun ASLRenderer(tokens: List<String>, isPlaying: Boolean = true, replay: Int) {
    val context = LocalContext.current
    var currentTokenIndex by remember { mutableStateOf(0) }

    val videoView = remember {
        VideoView(context).apply {
            val uri = Uri.parse("android.resource://${context.packageName}/raw/aslanimationwithtwocameras")
            setVideoURI(uri)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                if(isPlaying) start() else pause()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { videoView },
            modifier = Modifier.fillMaxSize()
        )
    }

    LaunchedEffect(tokens, currentTokenIndex,isPlaying) {
        if(!isPlaying) return@LaunchedEffect
        if (tokens.isEmpty() || currentTokenIndex >= tokens.size) return@LaunchedEffect

        val letter = tokens[currentTokenIndex]

        // Calculate timestamp (A=0s, B=1s, C=2s, etc.)
        val letterIndex = letter.first() - 'A'
        val seekMs = letterIndex * 1000 // 1 second per letter (24 frames at 24fps)

        Log.d("ASL_RENDER", "Playing letter $letter at ${seekMs}ms (index: $letterIndex)")

        videoView.seekTo(seekMs)
        videoView.start()

        delay(1000) // Show for 1 second
        videoView.pause()

        delay(200) // Brief pause between letters
        currentTokenIndex++


    }

    // Reset Sequence when done playing all letters
    LaunchedEffect(currentTokenIndex,tokens) {
        if (currentTokenIndex >= tokens.size && tokens.isNotEmpty()) {
            delay(500)
            currentTokenIndex = 0
        }
    }

    LaunchedEffect(replay) {
        currentTokenIndex = 0
    }
}