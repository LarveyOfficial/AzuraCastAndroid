package com.larvey.azuracastplayer.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.larvey.azuracastplayer.classes.SavedStation
import kotlinx.coroutines.flow.Flow


@Dao
interface SavedStationDao {

  @Upsert
  suspend fun insertStation(savedStation: SavedStation)

  @Query("SELECT * FROM savedstation")
  fun getAllEntries(): Flow<List<SavedStation>>

}