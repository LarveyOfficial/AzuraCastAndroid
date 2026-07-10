package com.larvey.azuracastplayer.session

/**
 * Computes the playback position reported for a live radio stream.
 *
 * Live streams have no meaningful player-side position, so elapsed time is
 * derived from wall clock vs. the song's `played_at` timestamp (epoch
 * seconds), minus the raw stream position for dynamic (live-window) items.
 *
 * Extracted verbatim from the `ForwardingPlayer.getCurrentPosition` override
 * so the arithmetic is unit-testable.
 *
 * KNOWN QUIRK (preserved deliberately): the clock-skew guard — taken when the
 * device clock is *behind* `played_at` — computes `playedAtSec - playedAtSec`,
 * which is always zero, so it returns `0 - rawPositionMs` for dynamic items
 * rather than a plausible elapsed time. This matches the long-shipped
 * behavior; fixing it is a deliberate follow-up, not part of the refactor.
 */
fun computeLiveElapsedMs(
  nowMs: Long,
  playedAtSec: Long,
  isDynamic: Boolean,
  rawPositionMs: Long
): Long {
  if ((nowMs / 1000) < playedAtSec) {
    return (playedAtSec.minus(playedAtSec) * 1000) - if (isDynamic) rawPositionMs else 0
  }
  return ((nowMs / 1000).minus(playedAtSec) * 1000) - if (isDynamic) rawPositionMs else 0
}
