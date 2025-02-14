package com.larvey.azuracastplayer

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.db.SavedStationsDatabase
import com.larvey.azuracastplayer.db.settings.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore("settings")

class AppSetup : Application() {

  val nowPlayingData = NowPlayingData()

  lateinit var db: SavedStationsDatabase

  lateinit var savedStationsDB: SavedStationsDB

  lateinit var savedStations: List<SavedStation>

  lateinit var userPreferences: UserPreferences

  override fun onCreate() {
    super.onCreate()
    db = Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
    savedStationsDB = SavedStationsDB(db.dao)

    CoroutineScope(Dispatchers.IO).launch {
      var stations = savedStationsDB.getAllEntries()
      savedStations = stations
      for (item in stations) {
        nowPlayingData.getStationInformation(
          url = item.url,
          shortCode = item.shortcode
        )
      }
    }
    userPreferences = UserPreferences(dataStore)

  }
}