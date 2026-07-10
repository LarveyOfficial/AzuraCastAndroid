package com.larvey.azuracastplayer.classes.data

import com.google.gson.annotations.SerializedName

/**
 * AzuraCast's now-playing payload, as served by both
 * `/api/nowplaying_static/{shortCode}.json` and `/api/nowplaying`.
 * Schema: https://www.azuracast.com/docs/developers/now-playing-data/
 *
 * Gson notes: snake_case fields are mapped via [SerializedName]; `played_at`
 * is epoch seconds; `duration`/`elapsed`/`remaining` are typed [Number]
 * because AzuraCast may serve them fractional (Gson materializes them as its
 * internal lazily-parsed number — use the numeric conversions). Do not rename
 * or move these classes without updating the ProGuard keep rules.
 */
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

