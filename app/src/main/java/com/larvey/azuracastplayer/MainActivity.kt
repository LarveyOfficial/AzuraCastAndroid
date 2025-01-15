package com.larvey.azuracastplayer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
        val navController = rememberNavController();

        NavHost(
          navController = navController,
          startDestination = "radioList"
        ) {
          composable(
            "radioList"
          ) {
            RadioList(viewModel, navController)
          }
          composable("nowPlaying/{stationURL}",
            arguments = listOf(navArgument("stationURL") { type = NavType.StringType })
          ){
            Log.d("Test", it.arguments?.getString("stationURL") ?: "No URL")
            NowPlaying(it.arguments?.getString("stationURL"))
          }
        }
      }
    }
  }
}
