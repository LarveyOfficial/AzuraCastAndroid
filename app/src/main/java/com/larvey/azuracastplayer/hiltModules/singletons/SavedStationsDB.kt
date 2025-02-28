package com.larvey.azuracastplayer.hiltModules.singletons

import android.content.Context
import androidx.room.Room
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.db.SavedStationsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SavedStationsDBModule {

  @Provides
  @Singleton
  fun provideSavedStationsDB(
    @ApplicationContext applicationContext: Context
  ): SavedStationsDB {
    val db = Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).fallbackToDestructiveMigration().fallbackToDestructiveMigrationOnDowngrade().build()
    return SavedStationsDB(db.dao)
  }
}