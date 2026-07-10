package com.larvey.azuracastplayer.ui.mainActivity.addStations

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.api.ApiResult
import com.larvey.azuracastplayer.api.AzuraCastRepository
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.utils.normalizeToHttpsScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddStationViewModel"

/**
 * Backs the add-station sheet: parses/validates the host the user typed, then
 * lists every station that host serves so the user can pick which to save.
 */
@HiltViewModel
class AddStationViewModel @Inject constructor(
  private val repository: AzuraCastRepository
) : ViewModel() {

  /** Stations served by each searched host, keyed by host. */
  var stationHostData = mutableStateMapOf<String, List<StationJSON>>()

  /** True when the last search input failed validation or the host was unreachable. */
  var isSearchInvalid = mutableStateOf(false)

  fun searchStationHost(url: String) {
    viewModelScope.launch {
      when (val result = repository.listHostStations(url)) {
        is ApiResult.Success -> {
          // An empty station list is a silent no-op (matching pre-repository
          // behavior): the search UI simply keeps waiting.
          if (result.data.isNotEmpty()) {
            stationHostData[url] = result.data
          }
        }

        is ApiResult.Failure.Http -> {
          // Pre-repository behavior: non-2xx (and empty-body) responses were
          // silently ignored rather than flagged — preserved here.
          Log.e(
            TAG,
            "Station search for $url returned HTTP ${result.code}"
          )
        }

        is ApiResult.Failure.Network, is ApiResult.Failure.Unexpected -> {
          Log.e(
            TAG,
            "Station search for $url failed: $result"
          )
          isSearchInvalid.value = true
          stationHostData.clear()
        }
      }
    }
  }

  /**
   * Normalizes the raw user input into a bare host (adding https://, keeping
   * an explicit port), validates it, and kicks off the host search.
   */
  fun parseURL(
    formatedURL: MutableState<String>,
    radioURL: String
  ) {
    isSearchInvalid.value = false
    formatedURL.value = normalizeToHttpsScheme(radioURL)
    try {
      if (formatedURL.value.toUri().port.toString() != "-1") {
        formatedURL.value = "${formatedURL.value.toUri().host.toString()}:${formatedURL.value.toUri().port}"
      } else {
        formatedURL.value = formatedURL.value.toUri().host.toString()
      }

      if (Patterns.WEB_URL.matcher(formatedURL.value).matches()) {
        searchStationHost(formatedURL.value)
      } else {
        isSearchInvalid.value = true
      }
    } catch (e: Exception) {
      Log.e(
        TAG,
        "Failed to parse station URL input",
        e
      )
      isSearchInvalid.value = true
    }
  }

}
