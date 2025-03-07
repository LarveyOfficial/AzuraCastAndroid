package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRadiosViewModel @Inject constructor(
  val nowPlayingData: NowPlayingData,
  val savedStationsDB: SavedStationsDB,
  val sharedMediaController: SharedMediaController
) : ViewModel() {
  fun refreshList(
    isRefreshing: MutableState<Boolean>
  ) {
    viewModelScope.launch {
      if (!savedStationsDB.savedStations.value.isNullOrEmpty()) {
        for (item in savedStationsDB.savedStations.value!!) {
          nowPlayingData.getStationInformation(
            item.url,
            item.shortcode
          )
        }
        delay(500) // It too fast sometimes
        isRefreshing.value = false
      }
    }
  }

  fun setPlaybackSource(
    url: String, mountURI: String, shortCode: String
  ) {
    val parsedURI = mountURI.toUri()
    Log.d(
      "DEBUG-PLAYBACK",
      parsedURI.toString()
    )
    nowPlayingData.setPlaybackSource(
      mountURI = parsedURI,
      url = url,
      shortCode = shortCode,
      mediaPlayer = sharedMediaController.mediaSession.value?.player
    )
  }

}