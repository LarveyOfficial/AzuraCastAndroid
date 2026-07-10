package com.larvey.azuracastplayer.utils

import java.util.Locale
import kotlin.time.Duration

/**
 * Formats a playback timestamp as `MM:SS`, or `HH:MM:SS` when [value] has an
 * hour component or [forceHours] is set (so the position and duration labels
 * switch to the hour format together). Hours are clamped to 99 to keep the
 * label width bounded for very long live-stream sessions.
 */
fun formatPlaybackTimestamp(value: Duration, forceHours: Boolean): String {
  return value.toComponents { hours, minutes, seconds, _ ->
    if (hours > 0 || forceHours) {
      var maxHours = hours
      if (hours > 99) {
        maxHours = 99
      }
      String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        maxHours,
        minutes,
        seconds
      )
    } else {
      String.format(
        Locale.getDefault(),
        "%02d:%02d",
        minutes,
        seconds
      )
    }
  }
}
