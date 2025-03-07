package com.larvey.azuracastplayer.classes.models

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import com.larvey.azuracastplayer.api.fetchStationData
import com.larvey.azuracastplayer.api.refreshMetadata
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.utils.fixHttps

class NowPlayingData {

  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()

  val staticData = mutableStateOf<StationJSON?>(null)

  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingMount = mutableStateOf("")

  fun setMediaMetadata(
    url: String, shortCode: String, mediaPlayer: Player?, reset: Boolean? = false
  ) {
    refreshMetadata(
      staticDataMap = staticDataMap,
      url = url,
      shortCode = shortCode,
      mountURI = nowPlayingMount.value,
      reset = reset == true,
      mediaPlayer = mediaPlayer,
      staticData = staticData
    )
  }

  fun setPlaybackSource(
    mountURI: Uri,
    url: String,
    shortCode: String,
    mediaPlayer: Player?
  ) {
    mediaPlayer?.stop()
    nowPlayingMount.value = mountURI.toString().fixHttps()
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
    fetchStationData(
      staticDataMap = staticDataMap,
      url = url,
      shortCode = shortCode
    )
  }
}