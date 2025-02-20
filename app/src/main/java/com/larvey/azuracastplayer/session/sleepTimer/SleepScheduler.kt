package com.larvey.azuracastplayer.session.sleepTimer

interface SleepScheduler {
  fun schedule(item: SleepItem)
  fun cancel(item: SleepItem)
}