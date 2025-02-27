package com.larvey.azuracastplayer.classes.data

import com.google.gson.annotations.SerializedName

data class StationJSON(
  val station: Station,
  val listeners: Listeners,
  @SerializedName("now_playing")
  val nowPlaying: NowPlaying,
  @SerializedName("playing_next")
  val playingNext: PlayingNext?,
  @SerializedName("song_history")
  val songHistory: List<SongHistory>,
  @SerializedName("is_online")
  val isOnline: Boolean
)

data class Station(
  val id: Long,
  val name: String,
  val shortcode: String,
  val description: String,
  val mounts: List<Mount>,
  @SerializedName("hls_enabled")
  val hlsEnabled: Boolean,
  @SerializedName("hls_url")
  val hlsUrl: String?
)

data class Mount(
  val id: Long,
  val name: String,
  val url: String,
  val bitrate: Long?,
  val format: String?,
  val path: String,
  @SerializedName("is_default")
  val isDefault: Boolean
)

data class Listeners(
  val total: Long,
  val unique: Long,
  val current: Long
)

data class NowPlaying(
  @SerializedName("played_at")
  val playedAt: Long,
  val duration: Number,
  val song: Song,
  val elapsed: Number,
  val remaining: Number
)

data class Song(
  val id: String,
  val art: String,
  val text: String,
  val artist: String,
  val title: String,
  val album: String,
  val genre: String,
  val isrc: String?
)

data class PlayingNext(
  val duration: Number,
  val song: Song,
)

data class SongHistory(
  @SerializedName("played_at")
  val playedAt: Long,
  val duration: Number,
  val song: Song,
)

