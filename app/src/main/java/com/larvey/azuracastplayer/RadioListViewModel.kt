package com.larvey.azuracastplayer

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.api.getStationHostData
import kotlinx.coroutines.launch

class RadioListViewModel : ViewModel() {
  val stationHostData = mutableStateMapOf<String, List<StationJSON>>()
  val stationData = mutableStateMapOf<String, StationJSON>()

  fun searchStationHost(url: String) {
    viewModelScope.launch{
      getStationHostData(
        stationHostData,
        url
      )
    }
  }

  fun getStationData(url: String, shortcode: String) {

  }

}