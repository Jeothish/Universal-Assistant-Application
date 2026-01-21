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
    isPlaying: Boolean,
    replayTrigger: Int
) {
    val context = LocalContext.current

    var currentTokenIndex by remember { mutableStateOf(0) }
    var pausedPositionMs by remember { mutableLongStateOf(0L) }
    var isLetterPlaying by remember { mutableStateOf(false) }

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
        }
    }

    if (tokens.isNotEmpty() && currentTokenIndex < tokens.size) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Current Letter: ${tokens[currentTokenIndex]}",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
            )
        }
    }

    //Go to start of sequence and replay
    LaunchedEffect(replayTrigger) {
        currentTokenIndex = 0
        pausedPositionMs = 0L
        exoPlayer.seekTo(0)
        exoPlayer.pause()
    }

    //Cleanup ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
