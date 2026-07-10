package com.larvey.azuracastplayer.hiltModules.singletons

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.larvey.azuracastplayer.api.ApiResult
import com.larvey.azuracastplayer.api.AzuraCastRepository
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.hiltModules.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

private const val TAG = "DiscoveryJSONModule"

/**
 * Provides the discovery catalog as an app-wide `MutableState<DiscoveryJSON?>`.
 *
 * The fetch-at-provision side effect is deliberate: kicking the request off
 * while the DI graph is built warms the catalog before the Discovery screen
 * (or Android Auto's Discover folder) first reads it. Every consumer
 * null-checks the state, so a slow or failed fetch simply leaves discovery
 * empty rather than crashing.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscoveryJSONModule {

  @Provides
  @Singleton
  fun provideDiscoveryJSON(
    repository: AzuraCastRepository,
    @ApplicationScope applicationScope: CoroutineScope
  ): MutableState<DiscoveryJSON?> {
    val discoveryJSON: MutableState<DiscoveryJSON?> = mutableStateOf(null)
    applicationScope.launch {
      when (val result = repository.getDiscoveryCatalog()) {
        is ApiResult.Success -> discoveryJSON.value = result.data

        is ApiResult.Failure -> Log.e(
          TAG,
          "Failed to fetch the discovery catalog: $result"
        )
      }
    }
    return discoveryJSON
  }
}
