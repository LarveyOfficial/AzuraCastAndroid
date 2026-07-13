package com.larvey.azuracastplayer.utils

import androidx.media3.common.util.UnstableApi
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.delay

/**
 * 1-second polling loop advancing the Now Playing progress UI (the mini-player
 * and the Now Playing bar both drive their progress from this).
 *
 * [nowPlaying] is a **provider**, read fresh on every tick — this is load
 * bearing. The loop lives in a `LaunchedEffect` keyed on the lifecycle owner, so
 * it is NOT restarted when the station advances to a new song. Capturing the
 * `NowPlaying` snapshot by value froze `played_at` at the first song, so live
 * (dynamic/HLS) streams never reset and the bar pegged at 100% forever; reading
 * it live lets the derived elapsed time roll over each song. See the
 * `ForwardingPlayer.getCurrentPosition` override in MusicPlayerService for the
 * matching wall-clock derivation.
 */
@androidx.annotation.OptIn(UnstableApi::class)
suspend fun updateTime(
  isVisible: Boolean,
  updateProgress: (Float, Long) -> Unit,
  playerState: PlayerState?,
  nowPlaying: () -> NowPlaying?
) {
  while (isVisible) {
    if (playerState?.isPlaying == true) {
      val nowPlayingSnapshot = nowPlaying()
      var currentPosition: Number = 0f
      currentPosition = if (playerState.player.isCurrentMediaItemDynamic) {
        ((System.currentTimeMillis() / 1000).minus(nowPlayingSnapshot?.playedAt!!) * 1000) - playerState.player.currentPosition
      } else {
        playerState.player.currentPosition
      }

      updateProgress(
        currentPosition.toFloat() / (if (playerState.player.mediaMetadata.durationMs == 0L) 1 else playerState.player.mediaMetadata.durationMs
          ?: 1).toFloat(),
        currentPosition.toLong()
      )
    }
    delay(1000)
  }
}
