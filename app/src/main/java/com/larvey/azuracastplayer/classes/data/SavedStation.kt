package com.larvey.azuracastplayer.classes.data

import androidx.room.Entity

@Entity(primaryKeys = ["name", "url"])
data class SavedStation(
  val name: String,
  val url: String,
  val shortcode: String,
  val defaultMount: String
)

