package com.larvey.azuracastplayer.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.db.settings.ThemeTypes

@Composable
fun ColorScheme.isDark(): Boolean {
  val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
  val themeType by settingsModel.themeType.collectAsState()

  val isDarkTheme = when (themeType) {
    ThemeTypes.SYSTEM -> isSystemInDarkTheme()
    ThemeTypes.LIGHT -> false
    ThemeTypes.DARK -> true
  }
  
  return isDarkTheme
}