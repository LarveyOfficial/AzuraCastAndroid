package com.larvey.azuracastplayer.classes.models

import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.db.SavedStationDao
import kotlinx.coroutines.coroutineScope

class SavedStationsDB(private val dao: SavedStationDao) {

  suspend fun saveStation(name: String, shortcode: String, url: String, defaultMount: String) =
    coroutineScope {
      val newEntry = SavedStation(
        name = name,
        shortcode = shortcode,
        url = url,
        defaultMount = defaultMount
      )
      dao.insertStation(newEntry)
    }

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