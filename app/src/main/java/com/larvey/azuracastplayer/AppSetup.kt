package com.larvey.azuracastplayer

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.db.settings.UserPreferences
import com.larvey.azuracastplayer.session.sleepTimer.AndroidAlarmScheduler
import com.larvey.azuracastplayer.session.sleepTimer.SleepItem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

val Context.dataStore by preferencesDataStore("settings")

@HiltAndroidApp
class AppSetup : Application() {
  lateinit var userPreferences: UserPreferences

  override fun onCreate() {
    super.onCreate()
    val scheduler = AndroidAlarmScheduler(this)
    SleepItem(LocalDateTime.now()).let(scheduler::cancel)
    getInitialStations()
    userPreferences = UserPreferences(dataStore)
  }

  private fun getInitialStations() {
    val savedStationEntryPoint = EntryPointAccessors.fromApplication(
      this,
      SavedStationsDBEntryPoint::class.java
    )
    val nowPlayingDataEntryPoint = EntryPointAccessors.fromApplication(
      this,
      NowPlayingDataEntryPoint::class.java
    )
    val savedStationsDB = savedStationEntryPoint.savedStationsDB
    val nowPlayingData = nowPlayingDataEntryPoint.nowPlayingData
    CoroutineScope(Dispatchers.IO).launch {
      val stations = savedStationsDB.getAllEntries()
      for (item in stations) {
        nowPlayingData.getStationInformation(
          url = item.url,
          shortCode = item.shortcode
        )
      }
    }
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface SavedStationsDBEntryPoint {
    val savedStationsDB: SavedStationsDB
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface NowPlayingDataEntryPoint {
    val nowPlayingData: NowPlayingData
  }
}