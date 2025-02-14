package com.larvey.azuracastplayer.db.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val IS_GRID_VIEW = "is_grid_view"

class UserPreferences(private val dataStore: DataStore<Preferences>) {
  companion object {
    val isGridView = booleanPreferencesKey(IS_GRID_VIEW)
  }

  val isGridViewFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[isGridView] != false
  }

  fun setGridView(value: Boolean, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[isGridView] = value
      }
    }
  }

}