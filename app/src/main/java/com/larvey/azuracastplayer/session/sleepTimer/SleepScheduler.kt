package com.larvey.azuracastplayer.session.sleepTimer

/** Schedules/cancels the sleep-timer alarm that stops playback. */
interface SleepScheduler {
  fun schedule(item: SleepItem)
  fun cancel(item: SleepItem)
}