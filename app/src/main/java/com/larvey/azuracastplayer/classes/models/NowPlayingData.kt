package com.larvey.azuracastplayer.classes.models

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.larvey.azuracastplayer.api.ApiResult
import com.larvey.azuracastplayer.api.AzuraCastRepository
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.utils.fixHttps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "NowPlayingData"

/**
 * App-wide singleton holding the "what is playing right now" state bus.
 *
 * The Compose-observable fields below are read and written from both the UI
 * (Activity/ViewModels) and [com.larvey.azuracastplayer.session.MusicPlayerService];
 * they are the app's core wiring:
 * - [staticDataMap] — cache of every known station's static now-playing JSON,
 *   keyed by (host, shortCode). Written here; read by the station list UI and
 *   discovery details.
 * - [staticData] — the currently-playing station's entry (see the note in
 *   [applyNowPlayingMetadata] about its one-poll lag). Read by the service's
 *   live-position math and the Now Playing UI.
 * - [nowPlayingURL]/[nowPlayingShortCode]/[nowPlayingMount] — the tuple
 *   identifying the active station. Written by [setPlaybackSource] and by the
 *   service (stop/onAddMediaItems/onDestroy).
 *
 * Threading: [applicationScope] runs on `Dispatchers.Main.immediate`, so all
 * state writes and all [Player] mutations happen on the main thread — the same
 * affinity the pre-repository Retrofit callbacks had. Public methods are safe
 * to call from any thread.
 */
class NowPlayingData(
  private val repository: AzuraCastRepository,
  private val applicationScope: CoroutineScope
) {

  val staticDataMap = mutableStateMapOf<Pair<String, String>, StationJSON>()

  val staticData = mutableStateOf<StationJSON?>(null)

  var nowPlayingURL = mutableStateOf("")
  var nowPlayingShortCode = mutableStateOf("")
  var nowPlayingMount = mutableStateOf("")

  /**
   * Refreshes the active station's metadata and pushes it into [mediaPlayer].
   * With [reset] the player is also (re)prepared and started — this is the
   * "start playing a new station" path.
   */
  fun setMediaMetadata(
    url: String, shortCode: String, mediaPlayer: Player?, reset: Boolean? = false
  ) {
    // Capture the mount at call time (not at response time) — this matches
    // the pre-repository behavior where the mount was passed as a parameter
    // before the request was enqueued.
    val mountUri = nowPlayingMount.value
    applicationScope.launch {
      when (val result = repository.getNowPlayingStatic(
        url,
        shortCode
      )) {
        is ApiResult.Success -> applyNowPlayingMetadata(
          data = result.data,
          url = url,
          shortCode = shortCode,
          mountUri = mountUri,
          mediaPlayer = mediaPlayer,
          reset = reset == true
        )

        is ApiResult.Failure -> Log.e(
          TAG,
          "Failed to refresh metadata for $shortCode@$url: $result"
        )
      }
    }
  }

  /** Switches playback to [mountURI] and starts playing. */
  fun setPlaybackSource(
    mountURI: Uri,
    url: String,
    shortCode: String,
    mediaPlayer: Player?
  ) {
    mediaPlayer?.stop()
    nowPlayingMount.value = mountURI.toString().fixHttps()
    nowPlayingURL.value = url
    nowPlayingShortCode.value = shortCode

    setMediaMetadata(
      url = url,
      shortCode = shortCode,
      mediaPlayer = mediaPlayer,
      reset = true
    )
  }

  /** Fetches a station's metadata into [staticDataMap] (list/grid display). */
  fun getStationInformation(url: String, shortCode: String) {
    applicationScope.launch {
      when (val result = repository.getNowPlayingStatic(
        url,
        shortCode
      )) {
        is ApiResult.Success -> staticDataMap[Pair(
          url,
          shortCode
        )] = result.data

        is ApiResult.Failure -> Log.e(
          TAG,
          "Failed to fetch station data for $shortCode@$url: $result"
        )
      }
    }
  }

  /**
   * Applies freshly-fetched station [data] to the state bus and the player.
   * Runs on the main thread ([applicationScope]) — required by Media3.
   */
  @OptIn(UnstableApi::class)
  private fun applyNowPlayingMetadata(
    data: StationJSON,
    url: String,
    shortCode: String,
    mountUri: String,
    mediaPlayer: Player?,
    reset: Boolean
  ) {
    // NOTE: Map.put() returns the PREVIOUS value, so staticData intentionally
    // lags one refresh behind staticDataMap. The service's live-position math
    // and the progress UI were built against this timing — do not "fix" this
    // to `= data` without re-verifying playback position behavior.
    staticData.value = staticDataMap.put(
      Pair(
        url,
        shortCode
      ),
      data
    )

    val metaData = MediaMetadata.Builder()
      .setMediaType(MEDIA_TYPE_MUSIC) // Hint for session consumers (e.g. Android Auto content styling).
      .setDisplayTitle(data.nowPlaying.song.title)
      .setSubtitle(data.nowPlaying.song.artist) // Android Auto renders the subtitle as the artist line.
      .setArtist(data.nowPlaying.song.artist)
      .setAlbumTitle(data.nowPlaying.song.album)
      .setAlbumArtist(data.nowPlaying.song.artist)
      .setDescription(data.nowPlaying.song.album) // Android Auto renders the description as the album line.
      .setGenre(data.nowPlaying.song.genre)
      .setArtworkUri(data.nowPlaying.song.art.fixHttps().toUri())
      .setDurationMs(data.nowPlaying.duration.toLong() * 1000) // AzuraCast reports seconds; Media3 expects milliseconds.
      .build()

    val newMedia = MediaItem.Builder()
      .setMediaId(mountUri)
      .setMediaMetadata(metaData)
      .build()

    val mediaItems = listOf(newMedia)

    val updatedMediaItems =
      mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()

    mediaPlayer?.replaceMediaItem(
      0,
      updatedMediaItems[0]
    )

    if (reset) {
      mediaPlayer?.prepare()
      mediaPlayer?.play()
    }
  }
}
