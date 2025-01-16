package com.larvey.azuracastplayer.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedStation::class], version = 1)
abstract class SavedStationsDatabase: RoomDatabase() {
  abstract val dao: SavedStationDao
}