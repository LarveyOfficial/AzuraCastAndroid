package com.larvey.azuracastplayer.api

import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.classes.data.StationJSON
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * The single Retrofit interface for every network call the app makes.
 *
 * AzuraCast is self-hosted, so the two station endpoints have no fixed host —
 * the user can add stations from any server. Those methods therefore take a
 * fully-qualified [@Url][Url] built by [AzuraCastRepository] instead of a
 * relative path. The one endpoint with a fixed host (the community discovery
 * catalog) stays declaratively relative to the Retrofit base URL.
 *
 * Endpoint reference:
 * https://www.azuracast.com/docs/developers/now-playing-data/
 */
interface AzuraCastApi {

  /**
   * The community discovery catalog (a static JSON published on GitHub Pages,
   * not part of AzuraCast itself). Relative to the Retrofit base URL.
   */
  @GET("azuracastDiscovery.json")
  suspend fun getDiscoveryCatalog(): Response<DiscoveryJSON>

  /**
   * A single station's now-playing metadata — the app's primary metadata
   * source. The per-station URL is built by [AzuraCastRepository].
   */
  @GET
  suspend fun getNowPlayingData(@Url url: String): Response<StationJSON>

  /**
   * Every station on a host, with live now-playing data. Used only by the
   * add-station flow to enumerate what a user-entered host serves.
   */
  @GET
  suspend fun listNowPlaying(@Url url: String): Response<List<StationJSON>>
}
