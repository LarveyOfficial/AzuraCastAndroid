package com.larvey.azuracastplayer

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RadioListViewModel : ViewModel() {
  val stationData = mutableStateMapOf<String, StationJSON>()

  fun getData(url: String) {
    viewModelScope.launch{
      getNowPlaying(stationData, url)
    }
  }

}