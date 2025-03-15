package com.larvey.azuracastplayer.ui.mainActivity.addStations

import android.util.Patterns
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.api.findHostsStations
import com.larvey.azuracastplayer.classes.data.StationJSON
import kotlinx.coroutines.launch
import java.net.URL

class AddStationViewModel : ViewModel() {
  var stationHostData = mutableStateMapOf<String, List<StationJSON>>()

  var isSearchInvalid = mutableStateOf(false)

  fun searchStationHost(url: String) {
    viewModelScope.launch {
      findHostsStations(
        stationHostData,
        url,
        isSearchInvalid
      )
    }
  }

  fun parseURL(
    formatedURL: MutableState<String>,
    radioURL: String
  ) {
    isSearchInvalid.value = false
    formatedURL.value =
      if (radioURL.startsWith("http://") || radioURL.startsWith("https://")) {
        radioURL
      } else {
        "https://$radioURL"
      }
    try {
      formatedURL.value = URL(formatedURL.value).host
      if (Patterns.WEB_URL.matcher(formatedURL.value).matches()) {
        searchStationHost(formatedURL.value)
      } else {
        isSearchInvalid.value = true
      }
    } catch (e: Exception) {
      isSearchInvalid.value = true
    }
  }

}