package com.larvey.azuracastplayer.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.api.getStationHostData
import com.larvey.azuracastplayer.classes.StationJSON
import kotlinx.coroutines.launch

class RadioSearchViewModel : ViewModel() {
  var stationHostData = mutableStateMapOf<String, List<StationJSON>>()

  fun searchStationHost(url: String) {
    viewModelScope.launch {
      getStationHostData(
        stationHostData,
        url
      )
    }
  }
}