package com.larvey.azuracastplayer.ui.mainActivity

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaLibraryService
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.weightedRandomColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
  val nowPlayingData: NowPlayingData,
  val savedStationsDB: SavedStationsDB,
  val sharedMediaController: SharedMediaController,
  private val application: Application
) : ViewModel() {

  var palette = mutableStateOf<Palette?>(null)
  var colorList = mutableStateOf(List(9) { Color.Gray })

  private var fetchData = mutableStateOf(true)

  init {
    viewModelScope.launch {
      while (!savedStationsDB.savedStations.value.isNullOrEmpty()) {
        Log.d(
          "DEBUG",
          "Waiting 30 seconds to fetch data"
        )
        updateRadioList()
        delay(30 * 1000)
      }
    }
    viewModelScope.launch {
      getStationList(false)
    }
  }

  private fun updateRadioList() {
    if (fetchData.value) {
      viewModelScope.launch {
        savedStationsDB.savedStations.value?.let { savedRadioList ->
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
    }
  }

  fun updatePalette(defaultColor: Color) {
    viewModelScope.launch {
      if (sharedMediaController.playerState.value?.mediaMetadata?.artworkUri != null) {
        this.async(Dispatchers.IO) {
          try {
            val loader = ImageLoader(application as Context)
            val request = ImageRequest.Builder(application as Context)
              .allowHardware(false)
              .placeholderMemoryCacheKey(
                sharedMediaController.playerState.value?.mediaMetadata?.artworkUri.toString()
                  .fixHttps()
              )
              .diskCacheKey(
                sharedMediaController.playerState.value?.mediaMetadata?.artworkUri.toString()
                  .fixHttps()
              )
              .data(
                sharedMediaController.playerState.value?.mediaMetadata?.artworkUri.toString()
                  .fixHttps()
              ).build()
            loader.execute(request).let {
              if (it is SuccessResult) {
                palette.value = Palette.from(it.image.toBitmap())
                  .maximumColorCount(32)
                  .generate()
                updateColorList(defaultColor)
              }
            }

          } catch (e: Exception) {
          }
        }
      } else {
        palette.value = null
      }
    }
  }

  private fun updateColorList(defaultColor: Color) {
    val defaultHSL = floatArrayOf(
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

    val vibrantSwatch = palette.value?.vibrantSwatch?.hsl
      ?: palette.value?.dominantSwatch?.hsl ?: defaultHSL

    val mutedSwatch = palette.value?.mutedSwatch?.hsl
      ?: palette.value?.dominantSwatch?.hsl
      ?: defaultHSL

    val lightMutedSwatch = palette.value?.lightMutedSwatch?.hsl
      ?: defaultHSL

    val lightVibrantSwatch = palette.value?.lightVibrantSwatch?.hsl
      ?: defaultHSL

    val darkVibrantSwatch = palette.value?.darkVibrantSwatch?.hsl ?: defaultHSL

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

  private fun getStationList(updateMetadata: Boolean? = true) {
    CoroutineScope(Dispatchers.IO).launch {
      savedStationsDB.savedStations.value = savedStationsDB.getAllEntries()
      notifySessionStationsUpdated()
      if (updateMetadata == true) {
        updateRadioList()
      }
    }
  }

  private fun notifySessionStationsUpdated() {
    sharedMediaController.mediaSession.value.let { session ->
      session?.notifyChildrenChanged(
        "Stations",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
      session?.notifyChildrenChanged(
        "/",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
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