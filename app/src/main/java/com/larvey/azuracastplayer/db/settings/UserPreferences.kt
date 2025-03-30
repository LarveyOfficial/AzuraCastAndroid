package com.larvey.azuracastplayer.db.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val IS_GRID_VIEW = "is_grid_view"
const val THEME_TYPE = "theme_type"
const val IS_DYNAMIC_THEME = "is_dynamic_theme"

enum class ThemeTypes { SYSTEM, LIGHT, DARK }

class UserPreferences(private val dataStore: DataStore<Preferences>) {
  companion object {
    val isGridView = booleanPreferencesKey(IS_GRID_VIEW)
    val themeType = intPreferencesKey(THEME_TYPE)
    val isDynamicTheme = booleanPreferencesKey(IS_DYNAMIC_THEME)
  }

  val isGridViewFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[isGridView] != false
  }

  val isDynamicThemeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[isDynamicTheme] != false
  }

  val themeTypeFlow: Flow<ThemeTypes> = dataStore.data.map { preferences ->
    ThemeTypes.entries[preferences[themeType] ?: 0]
  }

  fun setGridView(value: Boolean, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[isGridView] = value
      }
    }
  }

  fun setDynamicTheme(value: Boolean, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[isDynamicTheme] = value
      }
    }
  }

  fun setThemeType(value: ThemeTypes, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[themeType] = value.ordinal
      }
    }
  }

}