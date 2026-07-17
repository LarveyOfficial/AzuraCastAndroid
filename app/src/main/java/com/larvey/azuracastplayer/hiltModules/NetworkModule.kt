package com.larvey.azuracastplayer.hiltModules

import com.larvey.azuracastplayer.api.AzuraCastApi
import com.larvey.azuracastplayer.api.CacheBustingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Provides the app's single OkHttp/Retrofit stack for API traffic.
 *
 * The client is otherwise default-configured (no timeouts) — keep it that way
 * unless a change is deliberate. The one interceptor it carries is
 * [CacheBustingInterceptor], which appends a per-request timestamp so stale
 * metadata can never be served from a URL-keyed cache.
 *
 * The base URL points at the one fixed-host endpoint (the discovery catalog);
 * the per-station endpoints pass fully-qualified URLs via [retrofit2.http.Url]
 * (see [AzuraCastApi]).
 *
 * Note: Coil builds its own OkHttp client for image loading — unifying that is
 * intentionally out of scope for this module, and album art is left cacheable
 * on purpose (only API metadata is cache-busted).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  private const val DISCOVERY_BASE_URL = "https://owcramer.github.io/AzuraCast-Discovery-API/"

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    // The pre-repository code built a fresh OkHttpClient per call, so OkHttp's
    // per-host dispatcher cap (default 5) never applied — N polled stations on
    // one host ran N parallel requests. Raise the per-host cap to the global
    // cap so consolidating onto one client doesn't queue same-host polls
    // (users commonly save >5 stations from a single AzuraCast host).
    val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }
    return OkHttpClient.Builder()
      .dispatcher(dispatcher)
      // Cache-bust every API request so metadata is always fresh (never a cached snapshot).
      .addInterceptor(CacheBustingInterceptor())
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(DISCOVERY_BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  @Provides
  @Singleton
  fun provideAzuraCastApi(retrofit: Retrofit): AzuraCastApi =
    retrofit.create(AzuraCastApi::class.java)
}
