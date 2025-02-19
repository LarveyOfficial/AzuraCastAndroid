package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MyRadios(
  savedRadioList: List<SavedStation>?,
  innerPadding: PaddingValues,
  setPlaybackSource: (String, String, String) -> Unit,
  staticDataMap: SnapshotStateMap<Pair<String, String>, StationJSON>?,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit,
  radioListMode: Boolean,
  editingList: MutableState<Boolean>,
  confirmEdit: MutableState<Boolean>,
  editAllStations: (List<SavedStation>) -> Unit,
  lazyListState: LazyListState,
  lazyGridState: LazyGridState
) {
  if (savedRadioList?.isNotEmpty() == true) {

    val view = LocalView.current

    val myRadiosViewModel: MyRadiosViewModel = viewModel()

    var list by remember { mutableStateOf(savedRadioList) }

    LaunchedEffect(savedRadioList.size) {
      Log.d(
        "DEBUG",
        "Updating List"
      )
      list = savedRadioList
    }

    //region reOrderableLists
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
      list = list.toMutableList().apply {
        add(
          to.index,
          removeAt(from.index)
        )
      }

      list = list.toMutableList().apply {
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
      list = savedRadioList
    }

    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
      list = list.toMutableList().apply {
        add(
          to.index,
          removeAt(from.index)
        )
      }

      list = list.toMutableList().apply {
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
        editAllStations(list)
      }

      false -> {}
    }
    //endregion

    val animatedPadding by animateDpAsState(
      targetValue = if (innerPadding.calculateBottomPadding() - WindowInsets.navigationBars.asPaddingValues()
          .calculateBottomPadding() == 0.dp
      ) WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding() else innerPadding.calculateBottomPadding(),
      animationSpec = tween(durationMillis = 100)
    )

    var isRefreshing = remember { mutableStateOf(false) }

    PullToRefreshBox(
      onRefresh = {
        isRefreshing.value = true
        myRadiosViewModel.refreshList(
          savedRadioList,
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
        if (!targetState) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            state = lazyListState
          ) {
            items(
              list,
              key = { it.shortcode }) { item ->
              ReorderableItem(
                reorderableLazyListState,
                key = item.shortcode
              ) {
                StationListEntry(
                  scope = this,
                  interactionSource = remember { MutableInteractionSource() },
                  station = item,
                  setPlaybackSource = setPlaybackSource,
                  staticDataMap = staticDataMap,
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

          LazyVerticalGrid(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            columns = GridCells.Adaptive(minSize = 164.dp),
            state = lazyGridState
          ) {
            items(
              list,
              key = { it.shortcode }) { item ->
              ReorderableItem(
                reorderableLazyGridState,
                key = item.shortcode
              ) {
                StationGridEntry(
                  scope = this,
                  station = item,
                  setPlaybackSource = setPlaybackSource,
                  staticDataMap = staticDataMap,
                  deleteRadio = deleteRadio,
                  editRadio = editRadio,
                  editingList = editingList
                )
              }
            }
            item {
              Spacer(modifier = Modifier.size(animatedPadding))
            }
          }
        }
      }
    }
  }
}

