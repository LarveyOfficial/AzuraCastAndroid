package com.larvey.azuracastplayer.api

import androidx.annotation.VisibleForTesting
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.classes.data.StationJSON
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's single gateway to the network. Wraps [AzuraCastApi] and maps every
 * call into a typed [ApiResult] so callers handle failures explicitly.
 *
 * Threading: safe to call from any dispatcher, **including the main thread**.
 * Retrofit suspend functions are non-blocking (they enqueue internally and
 * resume on the caller's dispatcher), so this class deliberately does NOT
 * switch to [kotlinx.coroutines.Dispatchers.IO]. Do not "add IO for safety" —
 * callers such as [com.larvey.azuracastplayer.classes.models.NowPlayingData]
 * rely on resuming on the main thread to mutate the Media3 player, which must
 * only be touched from its application thread.
 *
 * Logging: this class intentionally does not log. It returns typed failures;
 * callers log with the context only they have (station short code, host, …).
 */
@Singleton
class AzuraCastRepository @Inject constructor(
  private val api: AzuraCastApi
) {

  /** Fetches one station's static now-playing JSON (primary metadata source). */
  suspend fun getNowPlayingStatic(
    host: String,
    shortCode: String
  ): ApiResult<StationJSON> = safeApiCall {
    api.getNowPlayingStatic(
      nowPlayingStaticUrl(
        host,
        shortCode
      )
    )
  }

  /** Lists every station served by [host] (add-station search). */
  suspend fun listHostStations(host: String): ApiResult<List<StationJSON>> = safeApiCall {
    api.listNowPlaying(nowPlayingUrl(host))
  }

  /** Fetches the community discovery catalog. */
  suspend fun getDiscoveryCatalog(): ApiResult<DiscoveryJSON> = safeApiCall {
    api.getDiscoveryCatalog()
  }

  companion object {

    /**
     * URL builders are pure and kept visible for tests. [host] may include a
     * port (e.g. "radio.example.com:8080") and is always addressed over HTTPS,
     * matching the app-wide `fixHttps` convention.
     */
    @VisibleForTesting
    internal fun nowPlayingStaticUrl(host: String, shortCode: String): String =
      "https://$host/api/nowplaying/$shortCode"

    @VisibleForTesting
    internal fun nowPlayingUrl(host: String): String = "https://$host/api/nowplaying"
  }
}
