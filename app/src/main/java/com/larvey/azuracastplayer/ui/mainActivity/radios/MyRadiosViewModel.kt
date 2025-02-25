package com.larvey.azuracastplayer.ui.mainActivity.radios

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRadiosViewModel @Inject constructor(private val nowPlayingData: NowPlayingData) : ViewModel() {
  fun refreshList(
    savedRadioList: List<SavedStation>,
    isRefreshing: MutableState<Boolean>
  ) {
    viewModelScope.launch {
      for (item in savedRadioList) {
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