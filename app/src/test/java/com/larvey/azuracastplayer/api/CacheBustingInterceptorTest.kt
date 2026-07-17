package com.larvey.azuracastplayer.api

import com.google.common.truth.Truth.assertThat
import com.larvey.azuracastplayer.api.CacheBustingInterceptor.Companion.CACHE_BUST_PARAM
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Locks the cache-busting behavior applied to every API request: a fresh timestamp query parameter
 * is appended, existing parameters survive, and the value changes per request (so nothing is served
 * from a URL-keyed cache).
 */
class CacheBustingInterceptorTest {

  private lateinit var server: MockWebServer

  private fun clientAt(vararg times: Long): OkHttpClient {
    val queue = ArrayDeque(times.toList())
    return OkHttpClient.Builder()
      .addInterceptor(CacheBustingInterceptor(now = { queue.removeFirst() }))
      .build()
  }

  private fun OkHttpClient.get(url: String) =
    newCall(Request.Builder().url(url).build()).execute().close()

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `appends the current epoch millis as a cache-bust query param`() {
    server.enqueue(MockResponse().setResponseCode(200))

    clientAt(1_700_000_000_000L).get(server.url("/api/nowplaying_static/test_radio.json").toString())

    val request = server.takeRequest()
    assertThat(request.requestUrl!!.queryParameter(CACHE_BUST_PARAM)).isEqualTo("1700000000000")
    assertThat(request.path).startsWith("/api/nowplaying_static/test_radio.json?")
  }

  @Test
  fun `preserves any existing query parameters`() {
    server.enqueue(MockResponse().setResponseCode(200))

    clientAt(42L).get(server.url("/api/nowplaying?foo=bar").toString())

    val url = server.takeRequest().requestUrl!!
    assertThat(url.queryParameter("foo")).isEqualTo("bar")
    assertThat(url.queryParameter(CACHE_BUST_PARAM)).isEqualTo("42")
  }

  @Test
  fun `each request gets a fresh timestamp`() {
    server.enqueue(MockResponse().setResponseCode(200))
    server.enqueue(MockResponse().setResponseCode(200))

    val client = clientAt(100L, 200L)
    client.get(server.url("/a").toString())
    client.get(server.url("/b").toString())

    assertThat(server.takeRequest().requestUrl!!.queryParameter(CACHE_BUST_PARAM)).isEqualTo("100")
    assertThat(server.takeRequest().requestUrl!!.queryParameter(CACHE_BUST_PARAM)).isEqualTo("200")
  }
}
