package com.larvey.azuracastplayer.classes.models

import androidx.compose.runtime.mutableStateOf
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.db.SavedStationDao
import kotlinx.coroutines.coroutineScope

/**
 * Room-backed store of the user's saved stations, plus [savedStations] — a
 * Compose-observable in-memory mirror of the table.
 *
 * The mirror is NOT kept in sync automatically: after any write
 * ([saveStation]/[removeStation]/[updateStation]) the caller must re-read via
 * [getAllEntries] and assign `savedStations.value` (see
 * `MainActivityViewModel.getStationList`). Android Auto's whole browse/play
 * path reads the mirror, so a stale mirror means stale Auto content.
 */
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