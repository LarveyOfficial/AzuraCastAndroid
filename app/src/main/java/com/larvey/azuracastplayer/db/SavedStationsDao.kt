package com.larvey.azuracastplayer.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.larvey.azuracastplayer.classes.data.SavedStation


@Dao
interface SavedStationDao {

  @Upsert
  suspend fun insertStation(savedStation: SavedStation)

  @Query("SELECT * FROM savedstation ORDER BY position")
  fun getAllEntries(): List<SavedStation>

  @Delete
  suspend fun deleteStation(savedStation: SavedStation)

  @Update
  suspend fun updateStation(savedStation: SavedStation)


}