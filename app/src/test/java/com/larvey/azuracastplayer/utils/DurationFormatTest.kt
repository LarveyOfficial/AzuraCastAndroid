package com.larvey.azuracastplayer.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Locks the Now Playing timestamp formatting (extracted from ProgressBar). */
class DurationFormatTest {

  @Test
  fun `formats minutes and seconds`() {
    assertThat(
      formatPlaybackTimestamp(
        3.minutes + 25.seconds,
        forceHours = false
      )
    ).isEqualTo("03:25")
  }

  @Test
  fun `formats zero`() {
    assertThat(
      formatPlaybackTimestamp(
        0.seconds,
        forceHours = false
      )
    ).isEqualTo("00:00")
  }

  @Test
  fun `switches to hour format past one hour`() {
    assertThat(
      formatPlaybackTimestamp(
        1.hours + 2.minutes + 3.seconds,
        forceHours = false
      )
    ).isEqualTo("01:02:03")
  }

  @Test
  fun `forceHours pads a short value to the hour format`() {
    // Used so the position label matches the duration label's width when the
    // other side of the bar is in hours.
    assertThat(
      formatPlaybackTimestamp(
        5.minutes,
        forceHours = true
      )
    ).isEqualTo("00:05:00")
  }

  @Test
  fun `hours clamp at 99`() {
    assertThat(
      formatPlaybackTimestamp(
        250.hours,
        forceHours = false
      )
    ).isEqualTo("99:00:00")
  }

  @Test
  fun `sub-second components truncate`() {
    assertThat(
      formatPlaybackTimestamp(
        1500.milliseconds,
        forceHours = false
      )
    ).isEqualTo("00:01")
  }
}
