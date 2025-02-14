package com.larvey.azuracastplayer.db.settings

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.larvey.azuracastplayer.AppSetup

fun CreationExtras.appViewModelProvider(): AppSetup =
  (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppSetup)