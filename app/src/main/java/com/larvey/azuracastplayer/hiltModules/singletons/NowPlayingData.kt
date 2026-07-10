package com.larvey.azuracastplayer.hiltModules.singletons

import com.larvey.azuracastplayer.api.AzuraCastRepository
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.hiltModules.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/** Provides the [NowPlayingData] state-bus singleton. */
@Module
@InstallIn(SingletonComponent::class)
object NowPlayingDataModule {

  @Provides
  @Singleton
  fun provideNowPlayingData(
    repository: AzuraCastRepository,
    @ApplicationScope applicationScope: CoroutineScope
  ): NowPlayingData {
    return NowPlayingData(
      repository = repository,
      applicationScope = applicationScope
    )
  }
}
