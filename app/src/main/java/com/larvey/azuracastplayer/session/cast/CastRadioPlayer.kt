package com.larvey.azuracastplayer.session.cast

import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.larvey.azuracastplayer.utils.fixHttps
import java.util.Locale

/**
 * The one static piece of metadata a radio station shows on the Cast receiver.
 * The receiver deliberately never sees the per-song metadata (see [CastManager]):
 * [title] is the station name, [artist] is the stream's format + bitrate, and
 * [artUrl] is the station art.
 */
data class CastStationInfo(
  val streamUrl: String,
  val title: String,
  val artist: String,
  val artUrl: String?
)

/**
 * Thin wrapper over a Cast session's [RemoteMediaClient] that plays a single
 * **live** radio stream. Unlike a normal media caster, it loads the stream as
 * [MediaInfo.STREAM_TYPE_LIVE] with **no duration**, so the receiver renders it
 * with no progress bar or timestamp, and it carries only the fixed station
 * metadata — a song change never touches the receiver; only a station change
 * triggers a fresh [loadStation].
 *
 * All [RemoteMediaClient] calls must run on the main thread (the caller,
 * [CastManager], already lives on the app's main-immediate scope).
 */
class CastRadioPlayer(castSession: CastSession) {

  private val remoteMediaClient: RemoteMediaClient? = castSession.remoteMediaClient

  val hasClient: Boolean get() = remoteMediaClient != null

  /** True if the receiver is currently playing or buffering our stream. */
  val isPlaying: Boolean get() = remoteMediaClient?.isPlaying == true

  fun loadStation(info: CastStationInfo) {
    val client = remoteMediaClient ?: return

    val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
      putString(MediaMetadata.KEY_TITLE, info.title)
      putString(MediaMetadata.KEY_ARTIST, info.artist)
      // [info.artUrl] is a phone-downscaled `data:` image URI (or null) prepared by
      // CastManager, so the receiver never fetches an oversized remote image.
      info.artUrl?.takeIf { it.isNotBlank() }?.let {
        addImage(WebImage(Uri.parse(it)))
      }
    }

    val streamUrl = info.streamUrl.fixHttps()
    val mediaInfo = MediaInfo.Builder(streamUrl)
      .setStreamType(MediaInfo.STREAM_TYPE_LIVE) // live → receiver shows no progress/timestamp
      .setContentType(contentTypeFor(streamUrl))
      .setMetadata(metadata)
      // No setStreamDuration: live streams have unknown duration.
      .build()

    client.load(
      MediaLoadRequestData.Builder()
        .setMediaInfo(mediaInfo)
        .setAutoplay(true)
        .build()
    )
  }

  fun play() {
    remoteMediaClient?.play()
  }

  fun pause() {
    remoteMediaClient?.pause()
  }

  fun stop() {
    remoteMediaClient?.stop()
  }

  companion object {
    /**
     * Best-effort MIME for the receiver based on the stream URL extension. The
     * Default Media Receiver plays MP3/AAC/OGG progressive streams and HLS
     * (HLS additionally requires CORS headers on the station host).
     */
    private fun contentTypeFor(streamUrl: String): String {
      val path = streamUrl.substringBefore('?').lowercase(Locale.ROOT)
      return when {
        path.endsWith(".m3u8") -> "application/x-mpegurl"
        path.endsWith(".aac") || path.endsWith(".aacp") -> "audio/aac"
        path.endsWith(".ogg") || path.endsWith(".opus") -> "audio/ogg"
        else -> "audio/mpeg" // mp3 and the common default for AzuraCast mounts
      }
    }
  }
}
