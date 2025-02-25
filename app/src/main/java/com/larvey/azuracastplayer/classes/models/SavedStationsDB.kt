package com.larvey.azuracastplayer.classes.models

import androidx.compose.runtime.mutableStateOf
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.db.SavedStationDao
import kotlinx.coroutines.coroutineScope

class SavedStationsDB(private val dao: SavedStationDao) {

  suspend fun saveStation(
    newStation: SavedStation,
  ) {
    coroutineScope {
      dao.insertStation(newStation)
    }
  }

  val savedStations = mutableStateOf<List<SavedStation>?>(null)

  fun getAllEntries(): List<SavedStation> {
    return dao.getAllEntries()
  }

  suspend fun removeStation(station: SavedStation) = coroutineScope {
    dao.deleteStation(station)
  }

  suspend fun updateStation(station: SavedStation) = coroutineScope {
    dao.updateStation(station)
  }

}