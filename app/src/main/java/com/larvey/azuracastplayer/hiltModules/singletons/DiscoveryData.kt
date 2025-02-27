package com.larvey.azuracastplayer.hiltModules.singletons

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.larvey.azuracastplayer.api.fetchDiscoveryJSON
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryJSONModule {

  @Provides
  @Singleton
  fun provideDiscoveryJSON(): MutableState<DiscoveryJSON?> {
    val discoveryJSON: MutableState<DiscoveryJSON?> = mutableStateOf(null)
    fetchDiscoveryJSON(discoveryJSON)
    return discoveryJSON
  }
}