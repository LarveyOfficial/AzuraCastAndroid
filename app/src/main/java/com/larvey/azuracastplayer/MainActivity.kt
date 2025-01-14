package com.larvey.azuracastplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.larvey.azuracastplayer.database.DataModelDatabase
import com.larvey.azuracastplayer.database.DataModelViewModel
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme

class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = DataModelDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }
  private val viewModel by viewModels<DataModelViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return DataModelViewModel(db.dao) as T
        }
      }
    }
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AzuraCastPlayerTheme {
        RadioList(viewModel)
      }
    }
  }
}
