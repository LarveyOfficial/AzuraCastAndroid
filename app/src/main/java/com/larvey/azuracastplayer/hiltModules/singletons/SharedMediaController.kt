package com.larvey.azuracastplayer.hiltModules.singletons

import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedMediaControllerModule {

  @Provides
  @Singleton
  fun provideSharedMediaController(): SharedMediaController {
    return SharedMediaController()
  }
}