package com.larvey.azuracastplayer.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SavedStationsViewModel (private val dao: SavedStationDao): ViewModel() {
  fun addData(name: String, shortcode: String, url: String) {
    viewModelScope.launch {
      val newEntry = SavedStation(name = name, shortcode = shortcode, url = url)
      dao.insertStation(newEntry)
    }
  }

  fun getAllEntries(): Flow<List<SavedStation>> = dao.getAllEntries()
}