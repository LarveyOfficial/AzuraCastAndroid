package com.larvey.azuracastplayer.session.sleepTimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import java.time.ZoneId

class AndroidAlarmScheduler(
  private val context: Context
) : SleepScheduler {

  private val alarmManager = context.getSystemService(AlarmManager::class.java)

  @RequiresPermission(
    value = "android.permission.SCHEDULE_EXACT_ALARM",
    conditional = false
  )
  override fun schedule(item: SleepItem) {

    val intent = Intent("com.larvey.azuracastplayer.session.MusicPlayerService.SLEEP").apply {
      `package` = "com.larvey.azuracastplayer"
    }
    Log.d(
      "DEBUG",
      "Scheduled for ${
        item.time.atZone(ZoneId.systemDefault())
          .toEpochSecond() * 1000
      }, item: ${item.hashCode()}"
    )
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
      PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
      )
    )
  }

  override fun cancel(item: SleepItem) {
    val intent = Intent("com.larvey.azuracastplayer.session.MusicPlayerService.SLEEP").apply {
      `package` = "com.larvey.azuracastplayer"
    }
    Log.d(
      "DEBUG",
      "Canceled Sleep Timer, item: ${item.hashCode()}"
    )
    if (Build.VERSION.SDK_INT >= 34) {
      alarmManager.cancelAll()
    } else {
      alarmManager.cancel(
        PendingIntent.getBroadcast(
          context,
          0,
          intent,
          PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )
      )
    }
  }
}