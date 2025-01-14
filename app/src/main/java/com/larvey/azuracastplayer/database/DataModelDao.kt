package com.larvey.azuracastplayer.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow


@Dao
interface DataModelDao {

  @Upsert
  suspend fun upsertDataModel(dataModel: DataModel)

  @Query("SELECT * FROM datamodel")
  fun getAllEntries(): Flow<List<DataModel>>

}