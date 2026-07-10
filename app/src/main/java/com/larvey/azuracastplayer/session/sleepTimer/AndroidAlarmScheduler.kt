package com.larvey.azuracastplayer.session.sleepTimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import java.time.ZoneId

private const val TAG = "AndroidAlarmScheduler"

/** [SleepScheduler] backed by [AlarmManager]; fires the service's SLEEP broadcast. */
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
    Log.i(
      TAG,
      "Sleep timer scheduled for ${item.time}"
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
    Log.i(
      TAG,
      "Sleep timer cancelled"
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