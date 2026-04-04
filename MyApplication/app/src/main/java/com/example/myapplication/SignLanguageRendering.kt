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

/**
 * Video renderer that translates text to ASL
 *
 * @param tokens List of letters to be animated
 * @param isPlaying Boolean toggle to pause or resume the playback
 * @param currentTokenIndex The current position in the token list
 * @param replayTrigger Used to force a restart to the start of the video
 * @param onTokenChange Callback when a letter finishes playing
 */
@Composable
fun ASLRenderer(
    tokens: List<String>,
    isPlaying: Boolean,
    currentTokenIndex: Int,
    replayTrigger: Int,
    onTokenChange: (Int) -> Unit
) {

    val context = LocalContext.current

    //Load video file
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = RawResourceDataSource.buildRawResourceUri(
                R.raw.aslanimationfinal
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


    //Handles seek + play/pause when the slider is moved + playback buttons
    LaunchedEffect(isPlaying,currentTokenIndex) {
        if (currentTokenIndex >= tokens.size) return@LaunchedEffect
        val startMs = tokens.getOrNull(currentTokenIndex)?.firstOrNull()?.let { (it - 'A') * 1000L} ?: 0L
        exoPlayer.seekTo(startMs)
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    //Iterates through each letter and plays it corresponding animation
    LaunchedEffect(tokens,isPlaying,currentTokenIndex) {
        if (tokens.isEmpty() || currentTokenIndex >= tokens.size) return@LaunchedEffect

        var index = currentTokenIndex
        while (index < tokens.size) {
            val letter = tokens[index]

            //Find the start time of this letter in the video
            val startMs = (letter.first() - 'A') * 1000L

            var elapsed = 0L
            val letterDuration = 1000L //How long each letter is shown for
            val interval = 200L


            //Jump to the correct position in the video that corresponds to this letter
            exoPlayer.seekTo(startMs)

            //Plays the letter is small time segments to allow for responsive pause/resume
            while (elapsed < letterDuration) {
                if (isPlaying) {
                    exoPlayer.play()
                    delay(interval)
                    elapsed += interval
                } else {
                    //Pause while preserving the current letter being played
                    exoPlayer.pause()
                    delay(interval)
                }
            }

            //Stop playback at the end of the letter and move to the next letter
            exoPlayer.pause()
            index++
            onTokenChange(index)
            delay(200)  // Pause between letters
        }
    }


    //Go to start of sequence and replay
    LaunchedEffect(replayTrigger) {
        if(tokens.isEmpty()) return@LaunchedEffect
        val firstToken = tokens.firstOrNull() ?: return@LaunchedEffect
        val startMs = (firstToken.first() - 'A') * 1000L
        onTokenChange(0)
        exoPlayer.seekTo(startMs)
        exoPlayer.pause()

    }

    //Cleanup ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

/**
 * UI for displaying ASL translation
 *
 * @param returnToChat Callback to navigate back to chat
 */
@Composable
fun ASLOutputScreen(returnToChat: () -> Unit){
    val tokens = GlobalState.aslTokens.value
    var isPlaying by remember { mutableStateOf(true) }
    var currentTokenIndex by remember { mutableStateOf(0) }
    var replayTrigger by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(11.dp)
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
            onValueChangeFinished = {},
            valueRange = 0f..(tokens.size - 1).toFloat(),
            steps = maxOf(tokens.size - 2,0),
            modifier = Modifier.padding(6.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2196F3),
                activeTrackColor = Color(0xFFFF9800),
                inactiveTrackColor =  Color(0xFF6F6F6D)
            )

        )


        Row(modifier = Modifier.fillMaxWidth().padding(6.dp).height(IntrinsicSize.Min),Arrangement.spacedBy(6.dp)){


            Button(
                onClick = {isPlaying = !isPlaying },
                modifier = Modifier.weight(1f).aspectRatio(1.2f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isPlaying) Color(0xFFEA0A0A) else Color(0xFF0CE021),
                    contentColor = Color(0xFFFFFEFE)
                ),
                contentPadding = PaddingValues.Zero
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
                modifier = Modifier.weight(1f).aspectRatio(1.2f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color(0xFFFFFFFF),
                ),
                contentPadding = PaddingValues.Zero
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
                modifier = Modifier.weight(1f).aspectRatio(1.2f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    contentColor = Color(0xFFFFFFFF),
                ),
                contentPadding = PaddingValues.Zero
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

