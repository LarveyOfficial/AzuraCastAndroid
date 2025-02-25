package com.larvey.azuracastplayer.hiltModules.singletons

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val MIGRATION_1_2 = object : Migration(
      1,
      2
    ) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE savedstation ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
          """
            CREATE TABLE temp_savedstation AS SELECT * FROM savedstation;
        """
        )
        database.execSQL(
          """
            DROP TABLE savedstation;
        """
        )
        database.execSQL(
          """
            CREATE TABLE savedstation (
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                shortcode TEXT NOT NULL,
                defaultMount TEXT NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (shortcode, url)
            );
        """
        )
        database.execSQL(
          """
            CREATE INDEX index_savedstation_position ON savedstation (position);
        """
        )
        database.execSQL(
          """
            INSERT INTO savedstation (name, url, shortcode, defaultMount, position)
            SELECT name, url, shortcode, defaultMount, ROW_NUMBER() OVER (ORDER BY shortcode, url) - 1
            FROM temp_savedstation;
        """
        )
        database.execSQL(
          """
            DROP TABLE temp_savedstation;
        """
        )
      }

    }
    val db = Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).addMigrations(MIGRATION_1_2).build()
    return SavedStationsDB(db.dao)
  }
}