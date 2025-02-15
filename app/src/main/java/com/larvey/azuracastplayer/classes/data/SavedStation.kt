package com.larvey.azuracastplayer.classes.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
  primaryKeys = ["shortcode", "url"],
  indices = [Index(
    value = ["position"]
  )]
)
data class SavedStation(
  val name: String,
  val url: String,
  val shortcode: String,
  val defaultMount: String,
  val position: Int,
)

