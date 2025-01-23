package com.larvey.azuracastplayer.classes

import com.larvey.azuracastplayer.database.SavedStationDao
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class SavedStationsDB(private val dao: SavedStationDao) {

  suspend fun saveStation(name: String, shortcode: String, url: String) = coroutineScope {
    val newEntry = SavedStation(
      name = name,
      shortcode = shortcode,
      url = url
    )
    dao.insertStation(newEntry)
  }

  fun getAllEntries(): Flow<List<SavedStation>> {
    return dao.getAllEntries()
  }

  suspend fun removeStation(station: SavedStation) = coroutineScope {
    dao.deleteStation(station)
  }

  suspend fun updateStation(station: SavedStation) = coroutineScope {
    dao.updateStation(station)
  }

}