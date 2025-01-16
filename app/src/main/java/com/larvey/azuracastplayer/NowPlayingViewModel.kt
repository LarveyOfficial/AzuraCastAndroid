package com.larvey.azuracastplayer

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.larvey.azuracastplayer.api.getStaticData

class NowPlayingViewModel(private val mediaControllerFuture: ListenableFuture<MediaController>) : ViewModel() {
  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()
  lateinit var mediaPlayer: Player

  var songTitle = mutableStateOf("")
  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")

  init {
    mediaControllerFuture.addListener({
      mediaPlayer = mediaControllerFuture.get()
      mediaPlayer.addListener(object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
          super.onMediaMetadataChanged(mediaMetadata)
          songTitle.value = mediaMetadata.title.toString()
          if (songTitle.value != "null") fetchStaticJSON(nowPlayingURL.value, nowPlayingShortCode.value)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) {
            mediaPlayer.seekToDefaultPosition()
          }
        }
      })
    }, MoreExecutors.directExecutor())
  }

  fun fetchStaticJSON(url: String, shortcode: String) {
    getStaticData(staticDataMap, url, shortcode)
  }

  fun setPlaybackSource(uri: Uri, url: String, shortcode: String) {
    nowPlayingURL.value = url
    nowPlayingShortCode.value = shortcode
    val mediaItem = MediaItem.Builder()
      .setMediaId("$uri") /* setMediaId and NOT setUri */
      .build()
    mediaPlayer.stop()
    mediaPlayer.setMediaItem(mediaItem)
    mediaPlayer.prepare()
    mediaPlayer.play()
  }


}