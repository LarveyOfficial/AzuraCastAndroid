package com.larvey.azuracastplayer.classes.models

import androidx.compose.runtime.mutableStateOf
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.larvey.azuracastplayer.state.PlayerState

/**
 * App-wide singleton bridging playback state between
 * [com.larvey.azuracastplayer.session.MusicPlayerService] and the UI.
 *
 * - [mediaSession] — set by the service in `onCreate`, cleared in `onDestroy`.
 *   ViewModels call `mediaSession.value?.player` to control playback directly.
 *   Note the service also keeps its own *local* `mediaSession` property; only
 *   the service writes this shared field.
 * - [isSleeping] — whether a sleep timer is armed. Toggled by the sleep-timer
 *   UI and reset by the player's `stop()` override.
 * - [playerState] — a Compose-observable snapshot of the client-side
 *   [androidx.media3.session.MediaController], created and disposed by
 *   `MainActivity`. Read by any ViewModel that needs reactive playback state.
 *
 * All fields are written on the main thread.
 */
class SharedMediaController {

  var mediaSession = mutableStateOf<MediaLibrarySession?>(null)

  var isSleeping = mutableStateOf(false)

  var playerState = mutableStateOf<PlayerState?>(null)


}