package com.larvey.azuracastplayer.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DataModelViewModel (private val dao: DataModelDao): ViewModel() {
  fun addData(nickname: String, url: String) {
    viewModelScope.launch {
      val newEntry = DataModel(nickname = nickname, url = url)
      dao.upsertDataModel(newEntry)
    }
  }

  fun getAllEntries(): Flow<List<DataModel>> = dao.getAllEntries()
}