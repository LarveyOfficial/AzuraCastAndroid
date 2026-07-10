package com.larvey.azuracastplayer.hiltModules

import com.larvey.azuracastplayer.api.AzuraCastApi
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
 * The client is deliberately default-configured: the previous per-call clients
 * used OkHttp defaults, and this refactor must not change network behavior. Do
 * not add timeouts or interceptors here without a deliberate follow-up change.
 *
 * The base URL points at the one fixed-host endpoint (the discovery catalog);
 * the per-station endpoints pass fully-qualified URLs via [retrofit2.http.Url]
 * (see [AzuraCastApi]).
 *
 * Note: Coil builds its own OkHttp client for image loading — unifying that is
 * intentionally out of scope for this module.
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
