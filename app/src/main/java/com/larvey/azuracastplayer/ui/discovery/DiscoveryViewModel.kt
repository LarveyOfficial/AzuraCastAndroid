package com.larvey.azuracastplayer.ui.discovery

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaLibraryService
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.classes.data.DiscoveryStation
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
  val discoveryJSON: MutableState<DiscoveryJSON?>,
  val nowPlayingData: NowPlayingData,
  val savedStationsDB: SavedStationsDB,
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

  fun addStation(station: DiscoveryStation) {
    viewModelScope.launch {
      savedStationsDB.saveStation(
        SavedStation(
          name = station.friendlyName,
          url = URL(station.publicPlayerUrl).host,
          shortcode = station.shortCode,
          defaultMount = station.preferredMount,
          position = savedStationsDB.savedStations.value?.size?.plus(1) ?: 1
        )
      )
      getStationList()
    }
  }

  private fun getStationList(updateMetadata: Boolean? = true) {
    CoroutineScope(Dispatchers.IO).launch {
      savedStationsDB.savedStations.value = savedStationsDB.getAllEntries()
      notifySessionStationsUpdated()
      if (updateMetadata == true) {
        updateRadioList()
      }
    }
  }

  private fun notifySessionStationsUpdated() {
    sharedMediaController.mediaSession.value.let { session ->
      session?.notifyChildrenChanged(
        "Stations",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
      session?.notifyChildrenChanged(
        "/",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
    }
  }

  private fun updateRadioList() {
    viewModelScope.launch {
      savedStationsDB.savedStations.value?.let { savedRadioList ->
        for (item in savedRadioList) {
          Log.d(
            "DEBUG",
            "Fetching Data for ${item.name}"
          )
          nowPlayingData.getStationInformation(
            item.url,
            item.shortcode
          )
        }
      }
    }
  }

}