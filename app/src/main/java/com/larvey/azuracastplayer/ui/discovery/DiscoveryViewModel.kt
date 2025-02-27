package com.larvey.azuracastplayer.ui.discovery

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
  val discoveryJSON: MutableState<DiscoveryJSON?>,
  val nowPlayingData: NowPlayingData,
  val sharedMediaController: SharedMediaController
) : ViewModel() {

  fun setPlaybackSource(
    url: String, mountURI: String, shortCode: String
  ) {
    val parsedURI = Uri.parse(mountURI)
    nowPlayingData.setPlaybackSource(
      mountURI = parsedURI,
      url = url,
      shortCode = shortCode,
      mediaPlayer = sharedMediaController.mediaSession.value?.player
    )
  }
}