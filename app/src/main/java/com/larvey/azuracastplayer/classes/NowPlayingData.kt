package com.larvey.azuracastplayer.classes

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import com.larvey.azuracastplayer.api.initialStationData
import com.larvey.azuracastplayer.api.updateSongData

class NowPlayingData {

  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()

  val staticData = mutableStateOf<StationJSON?>(null)

  val testingStuff = mutableStateOf("Testing")

  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingURI = mutableStateOf("")


  fun setMediaMetadata(
    url: String, shortCode: String, mediaPlayer: Player?, reset: Boolean? = false
  ) {
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