package com.larvey.azuracastplayer.ui.mainActivity

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import com.larvey.azuracastplayer.AppSetup
import com.larvey.azuracastplayer.classes.data.SavedStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel(app: Application) : AndroidViewModel(app) {

  val nowPlayingData = (app as AppSetup).nowPlayingData
  val savedStationsDB = (app as AppSetup).savedStationsDB

  var fetchData = mutableStateOf(true)

  var savedRadioList = mutableStateListOf<SavedStation>()

  fun updateCurrentMetadata(mediaController: MediaController?) {
    nowPlayingData.setMediaMetadata(
      nowPlayingData.nowPlayingURL.value,
      nowPlayingData.nowPlayingShortCode.value,
      mediaController
    )
  }

  fun updateRadioList() {
    if (fetchData.value) {
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

  fun setPlaybackSource(url: String, shortCode: String, mediaController: MediaController?) {
    nowPlayingData.staticDataMap[Pair(
      url,
      shortCode
    )]?.station?.mounts?.get(0)?.url?.let {
      val uri = Uri.parse(it)
      nowPlayingData.setPlaybackSource(
        uri = uri,
        url = url,
        shortCode = shortCode,
        mediaPlayer = mediaController
      )
    }
  }

  fun deleteStation(station: SavedStation) {
    viewModelScope.launch {
      savedStationsDB.removeStation(station)
      getStationList()
    }
  }

  fun addStation(stations: List<SavedStation>) {
    viewModelScope.launch {
      for (item in stations) {
        savedStationsDB.saveStation(
          item.name,
          item.shortcode,
          item.url,
          item.defaultMount
        )
      }
      getStationList()
    }
  }

  fun getStationList(updateMetadata: Boolean? = true) {
    CoroutineScope(Dispatchers.IO).launch {
      savedRadioList.clear()
      savedRadioList.addAll(savedStationsDB.getAllEntries())
      if (updateMetadata == true) {
        updateRadioList()
      }

    }
  }

  fun resumeActivity() {
    fetchData.value = true
    updateRadioList()
  }

  fun pauseActivity() {
    fetchData.value = false
  }

}