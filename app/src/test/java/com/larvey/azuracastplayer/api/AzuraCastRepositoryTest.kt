package com.larvey.azuracastplayer.api

import com.google.common.truth.Truth.assertThat
import com.larvey.azuracastplayer.classes.data.StationJSON
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Exercises the full HTTP → [ApiResult] pipeline against a real local server:
 * request-path construction ([retrofit2.http.Url] methods), Gson parsing, and
 * every [safeApiCall] failure branch.
 *
 * The per-station repository methods always build `https://` URLs (matching
 * the app-wide convention), which a plaintext MockWebServer cannot terminate —
 * so these tests drive [AzuraCastApi] with the server's own URL. The
 * `https://` URL construction itself is locked separately by [StationUrlsTest].
 */
class AzuraCastRepositoryTest {

  private lateinit var server: MockWebServer
  private lateinit var api: AzuraCastApi
  private lateinit var repository: AzuraCastRepository

  private fun fixture(name: String): String =
    javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
      .bufferedReader()
      .use { it.readText() }

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    api = Retrofit.Builder()
      .baseUrl(server.url("/"))
      .client(OkHttpClient())
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(AzuraCastApi::class.java)
    repository = AzuraCastRepository(api)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `now-playing call hits the expected path and parses the body`() = runTest {
    server.enqueue(
      MockResponse().setResponseCode(200).setBody(fixture("nowplaying_static_full.json"))
    )

    val url = server.url("/api/nowplaying/test_radio").toString()
    val result = safeApiCall { api.getNowPlayingData(url) }

    val request = server.takeRequest()
    assertThat(request.path).isEqualTo("/api/nowplaying/test_radio")
    assertThat(result).isInstanceOf(ApiResult.Success::class.java)
    val station = (result as ApiResult.Success<StationJSON>).data
    assertThat(station.station.shortcode).isEqualTo("test_radio")
    assertThat(station.nowPlaying.song.title).isEqualTo("Test Song")
  }

  @Test
  fun `host station list call parses an array body`() = runTest {
    server.enqueue(
      MockResponse().setResponseCode(200).setBody(fixture("nowplaying_host_list.json"))
    )

    val url = server.url("/api/nowplaying").toString()
    val result = safeApiCall { api.listNowPlaying(url) }

    assertThat(server.takeRequest().path).isEqualTo("/api/nowplaying")
    assertThat(result).isInstanceOf(ApiResult.Success::class.java)
    val stations = (result as ApiResult.Success<List<StationJSON>>).data
    assertThat(stations).hasSize(2)
    assertThat(stations[0].station.shortcode).isEqualTo("first")
    assertThat(stations[1].station.mounts[0].format).isEqualTo("opus")
  }

  @Test
  fun `discovery catalog uses the relative path against the base url`() = runTest {
    server.enqueue(
      MockResponse().setResponseCode(200).setBody(fixture("discovery_catalog.json"))
    )

    val result = repository.getDiscoveryCatalog()

    assertThat(server.takeRequest().path).isEqualTo("/azuracastDiscovery.json")
    assertThat(result).isInstanceOf(ApiResult.Success::class.java)
    val catalog = (result as ApiResult.Success<*>).data
      as com.larvey.azuracastplayer.classes.data.DiscoveryJSON
    assertThat(catalog.featuredStations.stations).hasSize(1)
    assertThat(catalog.discoveryStations).hasSize(2)
  }

  @Test
  fun `404 maps to Http failure with the status code`() = runTest {
    server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

    val result = repository.getDiscoveryCatalog()

    assertThat(result).isInstanceOf(ApiResult.Failure.Http::class.java)
    assertThat((result as ApiResult.Failure.Http).code).isEqualTo(404)
  }

  @Test
  fun `500 maps to Http failure with the status code`() = runTest {
    server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

    val result = repository.getDiscoveryCatalog()

    assertThat(result).isInstanceOf(ApiResult.Failure.Http::class.java)
    assertThat((result as ApiResult.Failure.Http).code).isEqualTo(500)
  }

  @Test
  fun `connection drop maps to Network failure`() = runTest {
    server.enqueue(
      MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
    )

    val result = repository.getDiscoveryCatalog()

    assertThat(result).isInstanceOf(ApiResult.Failure.Network::class.java)
  }

  @Test
  fun `syntactically malformed body maps to Network failure`() = runTest {
    // Gson's MalformedJsonException extends IOException, so broken JSON
    // *syntax* lands in the Network branch — documenting actual semantics.
    server.enqueue(
      MockResponse().setResponseCode(200).setBody("this is not json {{{")
    )

    val result = repository.getDiscoveryCatalog()

    assertThat(result).isInstanceOf(ApiResult.Failure.Network::class.java)
  }

  @Test
  fun `type-mismatched body maps to Unexpected failure`() = runTest {
    // Valid JSON of the wrong shape throws JsonSyntaxException (a
    // RuntimeException), which maps to Unexpected.
    server.enqueue(
      MockResponse().setResponseCode(200).setBody("[1, 2, 3]")
    )

    val result = repository.getDiscoveryCatalog()

    assertThat(result).isInstanceOf(ApiResult.Failure.Unexpected::class.java)
  }
}
