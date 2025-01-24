package com.larvey.azuracastplayer.ui.mainActivity.addStations

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.api.findHostsStations
import com.larvey.azuracastplayer.classes.data.StationJSON
import kotlinx.coroutines.launch

class RadioSearchViewModel : ViewModel() {
  var stationHostData = mutableStateMapOf<String, List<StationJSON>>()

  fun searchStationHost(url: String) {
    viewModelScope.launch {
      findHostsStations(
        stationHostData,
        url
      )
    }
  }
}