package com.larvey.azuracastplayer.session.sleepTimer

import java.time.LocalDateTime

/** The wall-clock time at which the sleep timer should stop playback. */
data class SleepItem(
  val time: LocalDateTime
)