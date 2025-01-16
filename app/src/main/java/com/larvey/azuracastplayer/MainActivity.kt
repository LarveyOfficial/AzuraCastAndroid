package com.larvey.azuracastplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.larvey.azuracastplayer.database.SavedStationsDatabase
import com.larvey.azuracastplayer.database.SavedStationsViewModel
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }
  private val savedStationsViewModel by viewModels<SavedStationsViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SavedStationsViewModel(db.dao) as T
        }
      }
    }
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AzuraCastPlayerTheme {
        HomePage(savedStationsViewModel)
      }
    }
  }
}
