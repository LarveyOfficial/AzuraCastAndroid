package com.larvey.azuracastplayer.viewmodels

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.larvey.azuracastplayer.classes.StationJSON
import com.larvey.azuracastplayer.api.updateSongData

class NowPlayingViewModel(private val mediaControllerFuture: ListenableFuture<MediaController>) : ViewModel() {
  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()
  lateinit var mediaPlayer: Player

  var songTitle = mutableStateOf("")
  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingURI = mutableStateOf("")

  init {
    mediaControllerFuture.addListener({
      mediaPlayer = mediaControllerFuture.get()
      mediaPlayer.addListener(object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
          super.onMediaMetadataChanged(mediaMetadata)
          songTitle.value = mediaMetadata.title.toString()
          if (mediaMetadata.title != null) setMediaMetadata(url = nowPlayingURL.value, shortCode = nowPlayingShortCode.value)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) {
            mediaPlayer.seekToDefaultPosition()
          }
        }
      })
    }, MoreExecutors.directExecutor())

  }

  fun setMediaMetadata(url: String, shortCode: String, reset: Boolean? = false) {
    updateSongData(
      staticDataMap = staticDataMap,
      url = url,
      shortCode = shortCode,
      uri = nowPlayingURI.value,
      reset = reset == true,
      mediaPlayer = mediaPlayer
    )
  }

  fun setPlaybackSource(uri: Uri, url: String, shortcode: String) {
    mediaPlayer.stop()
    nowPlayingURI.value = uri.toString()
    nowPlayingURL.value = url
    nowPlayingShortCode.value = shortcode

    setMediaMetadata(url, shortcode, true)

  }


}