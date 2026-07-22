package com.larvey.azuracastplayer.hiltModules.singletons

import android.content.Context
import com.larvey.azuracastplayer.session.cast.CastConnectivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CastConnectivityModule {

  @Provides
  @Singleton
  fun provideCastConnectivity(
    @ApplicationContext context: Context
  ): CastConnectivity {
    return CastConnectivity(context)
  }
}
