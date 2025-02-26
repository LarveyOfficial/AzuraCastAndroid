package com.larvey.azuracastplayer.ui.nowplaying

import androidx.lifecycle.ViewModel
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
  val nowPlayingData: NowPlayingData,
  val savedStationsDB: SavedStationsDB,
  val sharedMediaController: SharedMediaController
) : ViewModel()