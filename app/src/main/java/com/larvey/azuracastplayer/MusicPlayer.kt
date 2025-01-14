package com.larvey.azuracastplayer

import android.content.ComponentName
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

@Composable
fun MusicPlayer() {
  val context = LocalContext.current
  lateinit var player: Player
  var songTitle by remember { mutableStateOf("")}
  val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
  val mediacontrollerFuture = MediaController.Builder(context, sessionToken).buildAsync()
  mediacontrollerFuture.addListener({
    player = mediacontrollerFuture.get()
    player.addListener(object : Player.Listener {
      override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        songTitle = mediaMetadata.title.toString()
      }
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
          player.seekToDefaultPosition()
        }
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
//              loadMediaItem(Uri.parse("https://radio.owencramer.dev/listen/test_radio/aac"))
        if (player.mediaItemCount == 0)  {
          loadMediaItem(Uri.parse("https://hear.moe/listen/muse/radio.mp3"))
        } else {
          player.play()
        }

      }) {
        Text("Play")
      }
      Button(onClick = {
        player.pause()
      }) {
        Text("Pause")
      }
      Button(onClick = {
        player.stop()
      }) {
        Text("Stop")
      }
    }

  }
}