package com.larvey.azuracastplayer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DataModel (
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,
  val nickname: String,
  val url: String
)

