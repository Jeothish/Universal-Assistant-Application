package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip

import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ASLRenderer(
    tokens: List<String>,
    isPlaying: Boolean,
    currentTokenIndex: Int,
    replayTrigger: Int,
    onTokenChange: (Int) -> Unit
) {

    val context = LocalContext.current
    var pausedPositionMs by remember { mutableLongStateOf(0L) }
    var isLetterPlaying by remember { mutableStateOf(false) }


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = RawResourceDataSource.buildRawResourceUri(
                R.raw.aslanimationcentered
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
    LaunchedEffect(isPlaying,currentTokenIndex) {
        if (currentTokenIndex >= tokens.size) return@LaunchedEffect
        val startMs = tokens.getOrNull(currentTokenIndex)?.firstOrNull()?.let { (it - 'A') * 1000L} ?: 0L
        pausedPositionMs = startMs
        exoPlayer.seekTo(startMs)
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    //ASL sequence playback
    LaunchedEffect(tokens,isPlaying,currentTokenIndex) {
        if (tokens.isEmpty() || currentTokenIndex >= tokens.size) return@LaunchedEffect

        var index = currentTokenIndex
        while (index < tokens.size) {
            val letter = tokens[index]
            val startMs = (letter.first() - 'A') * 1000L
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
            index++
            onTokenChange(index)
            isLetterPlaying = false
            delay(200)  // Pause between letters
        }




    }


    //Go to start of sequence and replay
    LaunchedEffect(replayTrigger) {
        if(tokens.isEmpty()) return@LaunchedEffect
        val firstToken = tokens.firstOrNull() ?: return@LaunchedEffect
        val startMs = (firstToken.first() - 'A') * 1000L
        onTokenChange(0)
        pausedPositionMs =  startMs
        exoPlayer.seekTo(startMs)
        exoPlayer.pause()
        //isPlaying = true
    }

    //Cleanup ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Composable
fun ASLOutputScreen(returnToChat: () -> Unit){
    val tokens = GlobalState.aslTokens.value
    var isPlaying by remember { mutableStateOf(true) }
    var currentTokenIndex by remember { mutableStateOf(0) }
    var replayTrigger by remember { mutableStateOf(0) }


    //ASLRenderer(tokens = GlobalState.aslTokens.value,onReturn = {GlobalState.hideResponse.value = false})
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(
            modifier = Modifier.fillMaxWidth().padding(11.dp)
                .height(650.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF000000))
                .border(4.dp, Color(0xFFE7B212), RoundedCornerShape(24.dp)),

            contentAlignment = Alignment.Center


        ){
            ASLRenderer(tokens = tokens,isPlaying = isPlaying, currentTokenIndex = currentTokenIndex, replayTrigger = replayTrigger,onTokenChange = {currentTokenIndex = it})

            Text(
                text = tokens.getOrNull(currentTokenIndex) ?: "",
                fontSize = 140.sp,
                textAlign = TextAlign.End,
                color = Color(0xFFFF9800),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            )
        }

        Slider(
            value = currentTokenIndex.toFloat(),
            onValueChange = {currentTokenIndex = it.roundToInt()},
            onValueChangeFinished = {replayTrigger++},
            valueRange = 0f..(tokens.size - 1).toFloat(),
            steps = maxOf(tokens.size - 2,0),
            modifier = Modifier.padding(6.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2196F3),
                activeTrackColor = Color(0xFFFF9800),
                inactiveTrackColor =  Color(0xFF6F6F6D)
            )

        )


        Row(modifier = Modifier.fillMaxWidth().padding(6.dp),Arrangement.spacedBy(6.dp)){


            Button(
                onClick = {isPlaying = !isPlaying },
                modifier = Modifier.weight(1f).height(95.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isPlaying) Color(0xFFEA0A0A) else Color(0xFF0CE021),
                    contentColor = Color(0xFFFFFEFE)
                )
            )

            {
                Row(modifier = Modifier.padding()) {

                    Icon(
                        imageVector = if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)

                    )

                    Text(
                        text = (if (isPlaying) "Pause" else "Resume"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )


                }
            }

            Button(
                onClick = {isPlaying = true;replayTrigger++},
                modifier = Modifier.weight(1f).height(95.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color(0xFFFFFFFF),
                )
            )

            {
                Row(modifier = Modifier.padding()) {

                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)

                    )

                    Text(
                        text = "Replay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {returnToChat()},
                modifier = Modifier.weight(1f).height(95.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    contentColor = Color(0xFFFFFFFF),
                )
            )

            {
                Row(modifier = Modifier.padding()) {

                    Icon(
                        imageVector = Icons.Default.KeyboardReturn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)

                    )

                    Text(
                        text = "Return",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

        }

    }
}

