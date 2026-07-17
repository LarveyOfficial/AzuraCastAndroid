package com.larvey.azuracastplayer.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends a unique cache-busting query parameter — the current Unix time in **milliseconds** — to
 * every outgoing request on the API client, so a response can never be served from a URL-keyed cache
 * (CDN, reverse proxy, or the platform HTTP cache). AzuraCast's static now-playing JSON is an
 * aggressively cacheable file on disk; without this, a stale metadata snapshot can be replayed and
 * the app shows the wrong "now playing".
 *
 * Millis (not seconds) matters: several saved stations are polled every 30s and a playing station is
 * refreshed on every song change, so two requests can land inside the same second — a seconds value
 * would collide and defeat the purpose. The clock is injectable purely so the behavior can be tested.
 *
 * Static file servers ignore unknown query strings, so this only changes the cache key, never the
 * response. Only the API client carries this; Coil's separate image client is left cacheable on
 * purpose (album art should be reused, not re-fetched every poll).
 */
class CacheBustingInterceptor(
  private val now: () -> Long = System::currentTimeMillis
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val original = chain.request()
    val bustedUrl = original.url
      .newBuilder()
      .setQueryParameter(
        CACHE_BUST_PARAM,
        now().toString()
      )
      .build()
    return chain.proceed(
      original
        .newBuilder()
        .url(bustedUrl)
        .build()
    )
  }

  companion object {
    /** Query-parameter name for the cache-busting timestamp (`_ts`). */
    const val CACHE_BUST_PARAM = "_ts"
  }
}
