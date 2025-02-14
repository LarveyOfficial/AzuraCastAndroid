package com.larvey.azuracastplayer.db.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {

  val gridView: StateFlow<Boolean?> = userPreferences.isGridViewFlow.map { it }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = null
  )

  fun toggleGridView() {
    gridView.value?.let {
      userPreferences.setGridView(
        !it,
        viewModelScope
      )
    }
  }

  object SettingsModelProvider {
    val Factory = viewModelFactory {
      initializer {
        SettingsViewModel(
          appViewModelProvider().userPreferences
        )
      }
    }
  }

}