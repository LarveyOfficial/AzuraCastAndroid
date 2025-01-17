package com.larvey.azuracastplayer.classes

import androidx.room.Entity

@Entity(primaryKeys = ["name","shortcode"])
data class SavedStation (
  val name: String,
  val shortcode: String,
  val url: String
)

