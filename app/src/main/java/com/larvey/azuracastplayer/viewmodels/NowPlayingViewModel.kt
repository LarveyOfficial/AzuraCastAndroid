package com.larvey.azuracastplayer.viewmodels

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.larvey.azuracastplayer.classes.StationJSON
import com.larvey.azuracastplayer.api.updateSongData

@OptIn(UnstableApi::class)
class NowPlayingViewModel
  (private val mediaControllerFuture: ListenableFuture<MediaController>) : ViewModel() {

  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()

  lateinit var mediaPlayer: Player

  val staticData = mutableStateOf<StationJSON?>(null)

  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingURI = mutableStateOf("")

  var playerIsPlaying = mutableStateOf(false)



  init {
    mediaControllerFuture.addListener({
      mediaPlayer = object : ForwardingPlayer(mediaControllerFuture.get()) {
        override fun play() {
          Log.d("DEBUG", "SEEKING")
          super.seekToDefaultPosition()
          super.play()
        }
      }
      mediaPlayer.addListener(object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
          super.onMediaMetadataChanged(mediaMetadata)
          if (mediaMetadata.title != null) setMediaMetadata(url = nowPlayingURL.value, shortCode = nowPlayingShortCode.value)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          playerIsPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
          Log.d("BITCH", "FUCK ${error.toString()}")
          if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            mediaPlayer.seekToDefaultPosition()
            mediaPlayer.prepare()
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
      mediaPlayer = mediaPlayer,
      staticData = staticData
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