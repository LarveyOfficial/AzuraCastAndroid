package com.larvey.azuracastplayer.ui.mainActivity

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.larvey.azuracastplayer.AppSetup
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.weightedRandomColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivityViewModel(app: Application) : AndroidViewModel(app) {

  val nowPlayingData = (app as AppSetup).nowPlayingData
  val savedStationsDB = (app as AppSetup).savedStationsDB


  var palette = mutableStateOf<Palette?>(null)
  var colorList = mutableStateOf<List<Color>>(List(9) { Color.Gray })

  var fetchData = mutableStateOf(true)

  var savedRadioList = mutableStateListOf<SavedStation>()

  fun periodicUpdate(seconds: Long) {
    viewModelScope.launch {
      while (savedRadioList != emptyList<SavedStation>()) {
        Log.d(
          "DEBUG",
          "Waiting $seconds seconds to fetch data"
        )
        updateRadioList()
        delay(seconds * 1000)
      }
    }
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

  fun updatePalette(playerState: PlayerState?) {
    viewModelScope.launch {
      if (playerState?.mediaMetadata?.artworkUri != null) {
        this.async(Dispatchers.IO) {
          Glide.with(getApplication<Application>().applicationContext).asBitmap().load(
            playerState.mediaMetadata.artworkUri.toString()
          ).submit().get().let { bitmap ->
            palette.value = Palette.from(bitmap).maximumColorCount(32).generate()
          }
        }
      } else {
        palette.value = null
      }
    }
  }

  fun updateColorList(defaultColor: Color) {
    var defaultHSL = floatArrayOf(
      0f,
      0f,
      0f
    )
    colorToHSL(
      defaultColor.toArgb(),
      defaultHSL
    )
    val maxLuminance = 0.45f
    val maxSaturation = 0.75f

    var vibrantSwatch = palette.value?.vibrantSwatch?.hsl
      ?: palette.value?.dominantSwatch?.hsl ?: defaultHSL

    var mutedSwatch = palette.value?.mutedSwatch?.hsl
      ?: palette.value?.dominantSwatch?.hsl
      ?: defaultHSL

    var lightMutedSwatch = palette.value?.lightMutedSwatch?.hsl
      ?: defaultHSL

    var lightVibrantSwatch = palette.value?.lightVibrantSwatch?.hsl
      ?: defaultHSL

    var darkVibrantSwatch = palette.value?.darkVibrantSwatch?.hsl ?: defaultHSL

    if (vibrantSwatch[2] > maxLuminance) {
      vibrantSwatch[2] = maxLuminance
    }
    if (vibrantSwatch[1] > maxSaturation) {
      vibrantSwatch[1] = maxSaturation
    }

    if (mutedSwatch[2] > maxLuminance) {
      mutedSwatch[2] = maxLuminance
    }
    if (mutedSwatch[1] > maxSaturation) {
      mutedSwatch[1] = maxSaturation
    }

    if (lightMutedSwatch[2] > maxLuminance) {
      lightMutedSwatch[2] = maxLuminance
    }
    if (lightMutedSwatch[1] > maxSaturation) {
      lightMutedSwatch[1] = maxSaturation
    }

    if (darkVibrantSwatch[2] > maxLuminance) {
      darkVibrantSwatch[2] = maxLuminance
    }
    if (darkVibrantSwatch[1] > maxSaturation) {
      darkVibrantSwatch[1] = maxSaturation
    }

    if (lightVibrantSwatch[2] > maxLuminance) {
      lightVibrantSwatch[2] = maxLuminance
    }
    if (lightVibrantSwatch[1] > maxSaturation) {
      lightVibrantSwatch[1] = maxSaturation
    }

    val paletteColors = listOf(
      Color(
        HSLToColor(vibrantSwatch)
      ),
      Color(
        HSLToColor(mutedSwatch)
      ),
      Color(
        HSLToColor(lightMutedSwatch)
      ),
      Color(
        HSLToColor(darkVibrantSwatch)
      ),
      Color(
        HSLToColor(lightVibrantSwatch)
      )
    )

    colorList.value = weightedRandomColors(
      paletteColors,
      9
    )
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