package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*

import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun ASLRenderer(
    tokens: List<String>,
    onReturn: () -> Unit
) {

    val context = LocalContext.current
    var currentTokenIndex by remember { mutableStateOf(0) }
    var pausedPositionMs by remember { mutableLongStateOf(0L) }
    var isLetterPlaying by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var replayTrigger by remember { mutableStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = RawResourceDataSource.buildRawResourceUri(
                R.raw.aslalpbabettwocameratestingphoneres2
            )
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    // Add ExoPlayer to Compose UI
    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    //Pause and Play
    LaunchedEffect(isPlaying) {
        if (!isLetterPlaying) {
            if (isPlaying) {
                exoPlayer.seekTo(pausedPositionMs)
                exoPlayer.play()
            } else {
                pausedPositionMs = exoPlayer.currentPosition
                exoPlayer.pause()
            }
        }
    }

    //ASL sequence playback
    LaunchedEffect(tokens, currentTokenIndex, isPlaying) {
        if (tokens.isEmpty() || currentTokenIndex >= tokens.size) return@LaunchedEffect

        while (currentTokenIndex < tokens.size) {
            val letter = tokens[currentTokenIndex]
            val startMs = (letter.first() - 'A') * 1000L
            pausedPositionMs = startMs

            var elapsed = 0L
            val letterDuration = 1000L
            val interval = 200L
            isLetterPlaying = true

            exoPlayer.seekTo(startMs)

            while (elapsed < letterDuration) {
                if (isPlaying) {
                    exoPlayer.play()
                    delay(interval)
                    elapsed += interval
                } else {
                    exoPlayer.pause()
                    pausedPositionMs = exoPlayer.currentPosition
                    delay(interval)
                }
            }

            exoPlayer.pause()
            pausedPositionMs = exoPlayer.currentPosition
            currentTokenIndex++
            isLetterPlaying = false
            delay(200)  // Pause between letters
        }
    }

    //Reset Sequence when playback is over
    LaunchedEffect(currentTokenIndex, tokens) {
        if (currentTokenIndex >= tokens.size && tokens.isNotEmpty()) {
            delay(500)
            currentTokenIndex = 0
            pausedPositionMs = 0L
            isPlaying = false
        }
    }


    //Go to start of sequence and replay
    LaunchedEffect(replayTrigger) {
        currentTokenIndex = 0
        pausedPositionMs = 0L
        exoPlayer.seekTo(0)
        exoPlayer.pause()
        isPlaying = true
    }

    Box(modifier = Modifier.fillMaxSize()){
        if(tokens.isNotEmpty() && currentTokenIndex < tokens.size){
            Text(
                text = "Current Letter: ${tokens[currentTokenIndex]}",
                color =  Color(0xFFFFFFFF),
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            Button(
                onClick = { isPlaying = !isPlaying },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isPlaying) Color.Red else Color.Green,
                    contentColor = Color.White
                )
            )
            { Text(if (isPlaying) "Pause" else "Resume") }

            Button(
                onClick = {onReturn()},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                )
            )
            { Text("Return") }

            Button(
                onClick = {
                    isPlaying = true
                    replayTrigger++ },

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Magenta,
                    contentColor = Color.White
                )
            )
            { Text("Replay") }
        }

    }


    //Cleanup ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
