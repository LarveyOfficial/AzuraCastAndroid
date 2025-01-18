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
import com.larvey.azuracastplayer.api.initialStationData
import com.larvey.azuracastplayer.classes.StationJSON
import com.larvey.azuracastplayer.api.updateSongData

@OptIn(UnstableApi::class)
class NowPlayingViewModel : ViewModel() {

  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()


  val staticData = mutableStateOf<StationJSON?>(null)

  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingURI = mutableStateOf("")


  fun setMediaMetadata(url: String, shortCode: String, mediaPlayer: Player?, reset: Boolean? = false) {
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

  fun setPlaybackSource(uri: Uri, url: String, shortCode: String, mediaPlayer: Player?) {
    mediaPlayer?.stop()
    nowPlayingURI.value = uri.toString()
    nowPlayingURL.value = url
    nowPlayingShortCode.value = shortCode

    setMediaMetadata(
      url = url,
      shortCode = shortCode,
      mediaPlayer = mediaPlayer,
      reset = true
    )
  }

  fun getStationInformation(url: String, shortCode: String) {
    initialStationData(
      staticDataMap = staticDataMap,
      url = url,
      shortCode = shortCode
    )
  }


}