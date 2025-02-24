package com.larvey.azuracastplayer

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.db.SavedStationsDatabase
import com.larvey.azuracastplayer.db.settings.UserPreferences
import com.larvey.azuracastplayer.session.sleepTimer.AndroidAlarmScheduler
import com.larvey.azuracastplayer.session.sleepTimer.SleepItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

val Context.dataStore by preferencesDataStore("settings")

class AppSetup : Application() {

  val nowPlayingData = NowPlayingData()

  lateinit var db: SavedStationsDatabase

  lateinit var savedStationsDB: SavedStationsDB

  lateinit var savedStations: List<SavedStation>

  lateinit var userPreferences: UserPreferences

  private lateinit var mediaSession: MediaLibrarySession

  var sleepTimer = mutableStateOf(false)

  override fun onCreate() {
    super.onCreate()
    val scheduler = AndroidAlarmScheduler(this)
    SleepItem(LocalDateTime.now()).let(scheduler::cancel)

    //@formatter:off
    val MIGRATION_1_2 = object : Migration(1, 2) {
      //@formatter:on
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

    db = Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).addMigrations(MIGRATION_1_2).build()
    savedStationsDB = SavedStationsDB(db.dao)

    CoroutineScope(Dispatchers.IO).launch {
      var stations = savedStationsDB.getAllEntries()
      savedStations = stations
      for (item in stations) {
        nowPlayingData.getStationInformation(
          url = item.url,
          shortCode = item.shortcode
        )
      }
    }
    userPreferences = UserPreferences(dataStore)
  }

  fun setMediaSession(mediaSession: MediaLibrarySession?) {
    mediaSession?.let {
      this.mediaSession = mediaSession
    }
  }

  fun getMediaSession(): MediaLibrarySession? {
    return if (::mediaSession.isInitialized) {
      mediaSession
    } else null
  }
}