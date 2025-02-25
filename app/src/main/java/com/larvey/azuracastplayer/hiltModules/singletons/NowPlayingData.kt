package com.larvey.azuracastplayer.hiltModules.singletons

import com.larvey.azuracastplayer.classes.models.NowPlayingData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NowPlayingDataModule {

  @Provides
  @Singleton
  fun provideNowPlayingData(): NowPlayingData {
    return NowPlayingData()
  }
}