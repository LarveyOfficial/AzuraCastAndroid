package com.larvey.azuracastplayer.hiltModules.singletons

import android.content.Context
import com.larvey.azuracastplayer.classes.models.CastManager
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import com.larvey.azuracastplayer.hiltModules.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CastManagerModule {

  @Provides
  @Singleton
  fun provideCastManager(
    @ApplicationContext context: Context,
    @ApplicationScope applicationScope: CoroutineScope,
    sharedMediaController: SharedMediaController,
    nowPlayingData: NowPlayingData
  ): CastManager {
    return CastManager(
      context = context,
      applicationScope = applicationScope,
      sharedMediaController = sharedMediaController,
      nowPlayingData = nowPlayingData
    )
  }
}
