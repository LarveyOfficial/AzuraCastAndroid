package com.larvey.azuracastplayer.session.sleepTimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    val intent = Intent(
      context,
      AlarmReceiver::class.java
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
      if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC,
          item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
          PendingIntent.getBroadcast(
            context,
            item.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
        )
      }
    } else {
      alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC,
        item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
        PendingIntent.getBroadcast(
          context,
          item.hashCode(),
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      )
    }
  }

  override fun cancel(item: SleepItem) {
    val intent = Intent(
      context,
      AlarmReceiver::class.java
    )
    alarmManager.cancel(
      PendingIntent.getBroadcast(
        context,
        item.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    )
  }
}