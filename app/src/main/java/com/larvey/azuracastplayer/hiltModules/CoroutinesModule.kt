package com.larvey.azuracastplayer.hiltModules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the app-lifetime [CoroutineScope] used by singletons (such as
 * [com.larvey.azuracastplayer.classes.models.NowPlayingData]) that need to
 * launch work outliving any single screen.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

  /**
   * An app-lifetime scope on [Dispatchers.Main.immediate].
   *
   * Main-immediate is a deliberate, load-bearing choice: it reproduces the
   * thread affinity of the Retrofit callback executor this scope replaced
   * (Retrofit's `enqueue` callbacks run on Android's main thread). Everything
   * launched here — Media3 player mutations, Compose snapshot-state writes —
   * must happen on the main thread. [SupervisorJob] keeps one failed fetch
   * from cancelling the whole scope.
   */
  @Provides
  @Singleton
  @ApplicationScope
  fun provideApplicationScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
