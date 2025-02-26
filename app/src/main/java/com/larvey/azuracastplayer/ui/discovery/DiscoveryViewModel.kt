package com.larvey.azuracastplayer.ui.discovery

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
  val discoveryJSON: MutableState<DiscoveryJSON?>
) : ViewModel()