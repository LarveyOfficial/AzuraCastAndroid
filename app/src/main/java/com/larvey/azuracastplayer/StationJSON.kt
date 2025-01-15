package com.larvey.azuracastplayer
import com.google.gson.annotations.SerializedName

data class StationJSON(
  val station: Station,
  val listeners: Listeners,
  val live: Live,
  @SerializedName("now_playing")
  val nowPlaying: NowPlaying,
  @SerializedName("playing_next")
  val playingNext: PlayingNext,
  @SerializedName("song_history")
  val songHistory: List<SongHistory>,
  @SerializedName("is_online")
  val isOnline: Boolean,
  val cache: Any?,
)

data class Station(
  val id: Long,
  val name: String,
  val shortcode: String,
  val description: String,
  val frontend: String,
  val backend: String,
  val timezone: String,
  @SerializedName("listen_url")
  val listenUrl: String,
  val url: String,
  @SerializedName("public_player_url")
  val publicPlayerUrl: String,
  @SerializedName("playlist_pls_url")
  val playlistPlsUrl: String,
  @SerializedName("playlist_m3u_url")
  val playlistM3uUrl: String,
  @SerializedName("is_public")
  val isPublic: Boolean,
  val mounts: List<Mount>,
  val remotes: List<Any?>,
  @SerializedName("hls_enabled")
  val hlsEnabled: Boolean,
  @SerializedName("hls_is_default")
  val hlsIsDefault: Boolean,
  @SerializedName("hls_url")
  val hlsUrl: Any?,
  @SerializedName("hls_listeners")
  val hlsListeners: Long,
)

data class Mount(
  val id: Long,
  val name: String,
  val url: String,
  val bitrate: Long,
  val format: String,
  val listeners: Listeners,
  val path: String,
  @SerializedName("is_default")
  val isDefault: Boolean,
)

data class Listeners(
  val total: Long,
  val unique: Long,
  val current: Long,
)

data class Live(
  @SerializedName("is_live")
  val isLive: Boolean,
  @SerializedName("streamer_name")
  val streamerName: String,
  @SerializedName("broadcast_start")
  val broadcastStart: Any?,
  val art: Any?,
)

data class NowPlaying(
  @SerializedName("sh_id")
  val shId: Long,
  @SerializedName("played_at")
  val playedAt: Long,
  val duration: Long,
  val playlist: String,
  val streamer: String,
  @SerializedName("is_request")
  val isRequest: Boolean,
  val song: Song,
  val elapsed: Long,
  val remaining: Long,
)

data class Song(
  val id: String,
  val art: String,
  @SerializedName("custom_fields")
  val customFields: List<Any?>,
  val text: String,
  val artist: String,
  val title: String,
  val album: String,
  val genre: String,
  val isrc: String,
  val lyrics: String,
)

data class PlayingNext(
  @SerializedName("cued_at")
  val cuedAt: Long,
  @SerializedName("played_at")
  val playedAt: Long,
  val duration: Long,
  val playlist: String,
  @SerializedName("is_request")
  val isRequest: Boolean,
  val song: Song,
)

data class SongHistory(
  @SerializedName("sh_id")
  val shId: Long,
  @SerializedName("played_at")
  val playedAt: Long,
  val duration: Long,
  val playlist: String,
  val streamer: String,
  @SerializedName("is_request")
  val isRequest: Boolean,
  val song: Song,
)

