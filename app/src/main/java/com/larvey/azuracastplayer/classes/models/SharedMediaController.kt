package com.larvey.azuracastplayer.classes.models

import androidx.compose.runtime.mutableStateOf
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.larvey.azuracastplayer.state.PlayerState

class SharedMediaController {

  var mediaSession = mutableStateOf<MediaLibrarySession?>(null)

  var isSleeping = mutableStateOf(false)

  var playerState = mutableStateOf<PlayerState?>(null)


}