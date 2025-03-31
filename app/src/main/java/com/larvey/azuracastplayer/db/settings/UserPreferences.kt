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
const val ANDROID_AUTO_LAYOUT = "android_auto_layout"
const val LEGACY_MEDIA_BACKGROUND = "legacy_media_background"

enum class ThemeTypes { SYSTEM, LIGHT, DARK }

enum class AndroidAutoLayouts { GRID, LIST }

class UserPreferences(private val dataStore: DataStore<Preferences>) {
  companion object {
    val isGridView = booleanPreferencesKey(IS_GRID_VIEW)
    val themeType = intPreferencesKey(THEME_TYPE)
    val isDynamicTheme = booleanPreferencesKey(IS_DYNAMIC_THEME)
    val androidAutoLayout = intPreferencesKey(ANDROID_AUTO_LAYOUT)
    val legacyMediaBackground = booleanPreferencesKey(LEGACY_MEDIA_BACKGROUND)
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

  val androidAutoLayoutFlow: Flow<AndroidAutoLayouts> = dataStore.data.map { preferences ->
    AndroidAutoLayouts.entries[preferences[androidAutoLayout] ?: 0]
  }

  val legacyMediaBackgroundFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[legacyMediaBackground] == true
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

  fun setAndroidAutoLayout(value: AndroidAutoLayouts, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[androidAutoLayout] = value.ordinal
      }
    }
  }

  fun setLegacyMediaBackground(value: Boolean, scope: CoroutineScope) {
    scope.launch {
      dataStore.edit { preferences ->
        preferences[legacyMediaBackground] = value
      }
    }
  }

}