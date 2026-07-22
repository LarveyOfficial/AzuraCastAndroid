package com.larvey.azuracastplayer.session.cast

import com.google.android.gms.cast.MediaStatus

/**
 * Projects a Cast [MediaStatus] into "should the (muted) local player be
 * playing?" for the receiver → app half of the two-way sync in [CastManager].
 *
 * Returns:
 * - `true`  → the receiver is playing/buffering; mirror local to playing.
 * - `false` → the receiver is paused; mirror local to paused.
 * - `null`  → indeterminate (unknown/idle/loading); leave the local player as-is
 *             so a transient state doesn't fight the app or a pending reload.
 *
 * Live radio has no meaningful seek/finish, so we keep this intentionally small:
 * BUFFERING counts as playing (it's trying to), and IDLE/UNKNOWN/LOADING are
 * left indeterminate rather than forcing a pause.
 */
internal object CastRemotePlaybackState {
  fun shouldLocalBePlaying(mediaStatus: MediaStatus?): Boolean? {
    return when (mediaStatus?.playerState) {
      MediaStatus.PLAYER_STATE_PLAYING,
      MediaStatus.PLAYER_STATE_BUFFERING -> true
      MediaStatus.PLAYER_STATE_PAUSED -> false
      else -> null
    }
  }
}
