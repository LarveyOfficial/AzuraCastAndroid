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

  fun setPlaybackSource(
    url: String, uri: String, shortCode: String, mediaController: MediaController?
  ) {
    val uri = Uri.parse(uri)
    nowPlayingData.setPlaybackSource(
      uri = uri,
      url = url,
      shortCode = shortCode,
      mediaPlayer = mediaController
    )
  }

  fun deleteStation(station: SavedStation) {
    viewModelScope.launch {
      savedStationsDB.removeStation(station)
      getStationList()
    }
  }

  fun editStation(newStation: SavedStation) {
    viewModelScope.launch {
      savedStationsDB.updateStation(newStation)
      getStationList()
    }
  }

  fun editAllStations(newStations: List<SavedStation>) {
    viewModelScope.launch {
      for (item in newStations) {
        savedStationsDB.updateStation(item)
      }
      getStationList()
    }
  }

  fun addStation(stations: List<SavedStation>) {
    viewModelScope.launch {
      for (item in stations) {
        savedStationsDB.saveStation(
          SavedStation(
            item.name,
            item.url,
            item.shortcode,
            item.defaultMount,
            item.position
          )
        )
      }
      getStationList()
    }
  }

  fun getStationList(updateMetadata: Boolean? = true) {
    CoroutineScope(Dispatchers.IO).launch {
      savedRadioList.clear()
      savedRadioList.addAll(savedStationsDB.getAllEntries())
      (getApplication() as AppSetup).savedStations = savedRadioList
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