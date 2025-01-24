package com.larvey.azuracastplayer.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.larvey.azuracastplayer.classes.data.SavedStation

@Database(
  entities = [SavedStation::class],
  version = 1
)
abstract class SavedStationsDatabase : RoomDatabase() {
  abstract val dao: SavedStationDao
}