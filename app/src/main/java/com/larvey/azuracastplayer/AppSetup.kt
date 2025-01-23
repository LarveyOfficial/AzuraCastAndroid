package com.larvey.azuracastplayer

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.larvey.azuracastplayer.classes.NowPlayingData
import com.larvey.azuracastplayer.classes.SavedStationsDB
import com.larvey.azuracastplayer.database.SavedStationsDatabase

class AppSetup : Application() {

  val nowPlayingData = NowPlayingData()

  lateinit var db: SavedStationsDatabase

  lateinit var savedStationsDB: SavedStationsDB


  override fun onCreate() {
    super.onCreate()
    Log.d(
      "DEBUGBRO",
      "App Started"
    )
    db = Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
    savedStationsDB = SavedStationsDB(db.dao)
  }
}