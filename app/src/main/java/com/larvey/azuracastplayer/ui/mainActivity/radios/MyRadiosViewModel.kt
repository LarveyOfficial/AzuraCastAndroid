package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.larvey.azuracastplayer.AppSetup
import com.larvey.azuracastplayer.classes.data.SavedStation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyRadiosViewModel(app: Application) : AndroidViewModel(app) {
  val nowPlayingData = (app as AppSetup).nowPlayingData


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