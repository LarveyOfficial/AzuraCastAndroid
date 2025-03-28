package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.utils.conditional
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(
  ExperimentalMaterial3Api::class,
)
@Composable
fun MyRadios(
  innerPadding: PaddingValues,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit,
  editingList: MutableState<Boolean>,
  confirmEdit: MutableState<Boolean>,
  editAllStations: (List<SavedStation>) -> Unit,
  lazyListState: LazyListState,
  lazyGridState: LazyGridState
) {
  val view = LocalView.current

  val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
  val radioListMode by settingsModel.gridView.collectAsState()

  val myRadiosViewModel: MyRadiosViewModel = viewModel()

  if (!myRadiosViewModel.savedStationsDB.savedStations.value.isNullOrEmpty()) {

    var list by remember { mutableStateOf(myRadiosViewModel.savedStationsDB.savedStations.value) }

    LaunchedEffect(myRadiosViewModel.savedStationsDB.savedStations.value) {
      Log.d(
        "DEBUG",
        "Updating List"
      )
      list = myRadiosViewModel.savedStationsDB.savedStations.value
    }

    //region reOrderableLists
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
      list = list?.toMutableList()?.apply {
        add(
          to.index,
          removeAt(from.index)
        )
      }

      list = list?.toMutableList()?.apply {
        for ((index, item) in this.withIndex()) {
          this[index] = SavedStation(
            item.name,
            item.url,
            item.shortcode,
            item.defaultMount,
            index
          )
        }
      }

      ViewCompat.performHapticFeedback(
        view,
        HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
      )

    }

    BackHandler(editingList.value) {
      editingList.value = false
      confirmEdit.value = false
      list = myRadiosViewModel.savedStationsDB.savedStations.value
    }

    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
      list = list!!.toMutableList().apply {
        add(
          to.index,
          removeAt(from.index)
        )
      }

      list = list!!.toMutableList().apply {
        for ((index, item) in this.withIndex()) {
          this[index] = SavedStation(
            item.name,
            item.url,
            item.shortcode,
            item.defaultMount,
            index
          )
        }
      }

      ViewCompat.performHapticFeedback(
        view,
        HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
      )
    }

    when (editingList.value && confirmEdit.value) {
      true -> {
        editingList.value = false
        confirmEdit.value = false
        editAllStations(list!!)
      }

      false -> {}
    }
    //endregion

    val animatedPadding by animateDpAsState(
      targetValue = innerPadding.calculateBottomPadding() + 1.dp,
      animationSpec = tween(durationMillis = 100)
    )
    val isRefreshing = remember { mutableStateOf(false) }

    PullToRefreshBox(
      onRefresh = {
        isRefreshing.value = true
        myRadiosViewModel.refreshList(
          isRefreshing
        )
      },
      isRefreshing = isRefreshing.value,
      modifier = Modifier
        .padding(
          top = innerPadding.calculateTopPadding(),
          bottom = 0.dp
        )
    ) {
      AnimatedContent(radioListMode) { targetState ->
        if (targetState == false) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            state = lazyListState
          ) {
            itemsIndexed(
              list!!,
              key = { _, item -> item.shortcode }
            ) { index, item ->
              ReorderableItem(
                reorderableLazyListState,
                key = item.shortcode,
                modifier = Modifier.conditional(index == 0) {
                  padding(top = 4.dp)
                }
              ) {
                StationListEntry(
                  scope = this,
                  station = item,
                  setPlaybackSource = myRadiosViewModel::setPlaybackSource,
                  staticDataMap = myRadiosViewModel.nowPlayingData.staticDataMap,
                  deleteRadio = deleteRadio,
                  editRadio = editRadio,
                  editingList = editingList
                )

              }
              Spacer(Modifier.height(8.dp))
            }
            item {
              Spacer(modifier = Modifier.size(animatedPadding))
            }
          }
        } else {
          val spanLines = remember { mutableIntStateOf(1) }
          LazyVerticalGrid(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            columns = GridCells.Adaptive(minSize = 180.dp),
            state = lazyGridState
          ) {
            itemsIndexed(
              list!!,
              key = { _, item -> item.shortcode },
              span = { _, _ ->
                spanLines.intValue = maxLineSpan
                GridItemSpan(1)
              }) { index, item ->
              ReorderableItem(
                reorderableLazyGridState,
                key = item.shortcode,
                modifier = Modifier.conditional(index < spanLines.intValue) {
                  padding(top = 4.dp)
                },
              ) {
                StationGridEntry(
                  scope = this,
                  station = item,
                  setPlaybackSource = myRadiosViewModel::setPlaybackSource,
                  staticDataMap = myRadiosViewModel.nowPlayingData.staticDataMap,
                  deleteRadio = deleteRadio,
                  editRadio = editRadio,
                  editingList = editingList
                )
              }
            }
            item(span = {
              GridItemSpan(maxLineSpan)
            }) {
              Spacer(modifier = Modifier.height(animatedPadding))
            }
          }
        }
      }
    }
  } else {
    Box(modifier = Modifier.fillMaxSize())
  }
}

