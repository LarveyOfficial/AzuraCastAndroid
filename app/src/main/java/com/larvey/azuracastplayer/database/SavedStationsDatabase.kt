package com.larvey.azuracastplayer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.larvey.azuracastplayer.classes.SavedStation

@Database(
  entities = [SavedStation::class],
  version = 1
)
abstract class SavedStationsDatabase : RoomDatabase() {
  abstract val dao: SavedStationDao
}