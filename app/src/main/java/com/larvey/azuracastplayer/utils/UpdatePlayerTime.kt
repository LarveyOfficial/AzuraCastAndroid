package com.larvey.azuracastplayer.utils

import androidx.media3.common.util.UnstableApi
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
suspend fun updateTime(
  isVisible: Boolean,
  updateProgress: (Float, Long) -> Unit,
  playerState: PlayerState?,
  nowPlaying: NowPlaying?
) {
  while (isVisible) {
    if (playerState?.isPlaying == true) {
      var currentPosition: Number = 0f
      currentPosition = if (playerState.player.isCurrentMediaItemDynamic) {
        ((System.currentTimeMillis() / 1000).minus(nowPlaying?.playedAt!!) * 1000) - playerState.player.currentPosition
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