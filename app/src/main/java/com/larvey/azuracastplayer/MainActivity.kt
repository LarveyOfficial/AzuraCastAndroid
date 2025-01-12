package com.larvey.azuracastplayer


import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme

class MainActivity : ComponentActivity() {
  private lateinit var player: Player
  @OptIn(UnstableApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AzuraCastPlayerTheme {
        val context = LocalContext.current
        var songTitle by remember { mutableStateOf("")}
        val sessionToken = SessionToken(applicationContext, ComponentName(this, MusicPlayerService::class.java))
        val mediacontrollerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediacontrollerFuture.addListener({
          player = mediacontrollerFuture.get()
          player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
              super.onMediaMetadataChanged(mediaMetadata)
              songTitle = mediaMetadata.title.toString()
            }
          })


        }, MoreExecutors.directExecutor())
        fun loadMediaItem(uri: Uri) {
          val newItem = MediaItem.Builder()
            .setMediaId("$uri") /* setMediaId and NOT setUri */
            .build()

          /* Load it into our activity's MediaController */
          player.setMediaItem(newItem)
          player.prepare()
          player.play()
        }

        DisposableEffect(Unit) {
          onDispose {
            player.release()
          }
        }





        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Column(modifier = Modifier.padding(innerPadding)) {
            Text(text = songTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
              loadMediaItem(Uri.parse("https://radio.owencramer.dev/listen/test_radio/aac"))
            }) {
              Text("Play")
            }
            Button(onClick = {
              player.stop()
              songTitle = ""
            }) {
              Text("Stop")
            }
          }

        }
      }
    }
  }
}
