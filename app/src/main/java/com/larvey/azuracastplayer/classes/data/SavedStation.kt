package com.larvey.azuracastplayer.classes.data

import androidx.room.Entity
import androidx.room.Index

/**
 * A station the user saved, keyed by (shortcode, url) with a user-controlled
 * [position] for manual ordering. [defaultMount] is the stream URL that plays
 * when the station is selected. Persisted in Room (destructive migration —
 * schema bumps wipe saved stations).
 */
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

