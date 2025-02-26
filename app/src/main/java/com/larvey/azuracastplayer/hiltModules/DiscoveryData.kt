package com.larvey.azuracastplayer.hiltModules

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.larvey.azuracastplayer.api.fetchDiscoveryJSON
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object DiscoveryJSONModule {
  @Provides
  @ViewModelScoped
  fun provideDiscoveryJSON(): MutableState<DiscoveryJSON?> {
    val discoveryJSON: MutableState<DiscoveryJSON?> = mutableStateOf(null)
    fetchDiscoveryJSON(discoveryJSON)
    return discoveryJSON
  }
}