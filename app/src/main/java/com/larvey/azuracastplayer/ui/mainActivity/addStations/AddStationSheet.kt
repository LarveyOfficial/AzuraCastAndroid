package com.larvey.azuracastplayer.ui.mainActivity.addStations

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.utils.conditional
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import com.larvey.azuracastplayer.utils.isDark
import kotlinx.coroutines.launch

data class AddableStation(
  val name: String,
  val shortcode: String,
  val defaultMount: String
)

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalComposeUiApi::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AddStationSheet(
  hideSheet: () -> Unit,
  addData: (stations: List<SavedStation>) -> Unit,
  currentStationCount: Int
) {

  val addStationViewModel: AddStationViewModel = viewModel()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var checkedStations by remember { mutableStateOf(emptyList<AddableStation>()) }

  var colorScheme = MaterialTheme.colorScheme

  var radioURL by remember { mutableStateOf("") }
  val formatedURL = remember { mutableStateOf("") }
  remember { mutableStateOf(false) }

  LaunchedEffect(addStationViewModel.isSearchInvalid.value) {
    if (addStationViewModel.isSearchInvalid.value) {
      formatedURL.value = ""
      radioURL = ""
    }
  }

  val dismiss = {
    scope.launch {
      sheetState.hide()
      addStationViewModel.stationHostData =
        mutableStateMapOf()
      addStationViewModel.isSearchInvalid.value = false
      hideSheet()
    }
  }

  ModalBottomSheet(
    onDismissRequest = {
      dismiss()
    },
    sheetState = sheetState,
    dragHandle = {},
    sheetGesturesEnabled = addStationViewModel.stationHostData.isEmpty(),
    modifier = Modifier
      .fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(getRoundedCornerRadius())
  ) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = { dismiss() }) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Add"
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
          title = {
            Text("Add AzuraCast Radio Stations!")
          }
        )
      },
      floatingActionButton = {
        AnimatedVisibility(
          visible = checkedStations.isNotEmpty(),
          exit = slideOutHorizontally(targetOffsetX = { fullWidth ->
            fullWidth * 2
          }),
          enter = slideInHorizontally(
            initialOffsetX = { fullWidth ->
              fullWidth * 2
            },
            animationSpec = spring(
              dampingRatio = DampingRatioLowBouncy,
              stiffness = StiffnessLow
            )
          )
        ) {
          FloatingActionButton(
            modifier = Modifier.padding(bottom = 16.dp),
            onClick = {
              var listOfStations = mutableListOf<SavedStation>()
              for ((index, item) in checkedStations.withIndex()) {
                listOfStations.add(
                  SavedStation(
                    item.name,
                    formatedURL.value.lowercase(),
                    item.shortcode,
                    item.defaultMount,
                    currentStationCount + index
                  )
                )
              }
              addData(listOfStations)
              dismiss()
            }
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = "Add"
            )
          }
        }
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
      ) {
        Text(
          "Type the URL of the Radio Station you'd like to add below to get started!",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(
            top = 16.dp,
            bottom = 4.dp
          )
        )
        OutlinedTextField(
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
          value = radioURL,
          onValueChange = {
            radioURL = it
          },
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Search,
          ),
          keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
            addStationViewModel.parseURL(
              formatedURL,
              radioURL
            )
          }),
          singleLine = true,
          label = {
            Text("Host URL")
          },
          isError = addStationViewModel.isSearchInvalid.value
        )
        TextButton(
          modifier = Modifier.align(Alignment.End),
          onClick = {
            focusManager.clearFocus()
            addStationViewModel.parseURL(
              formatedURL,
              radioURL
            )
          },
          enabled = radioURL.isNotBlank()
        ) {
          Text("Search")
        }
        AnimatedVisibility(addStationViewModel.stationHostData.isNotEmpty()) {
          Column {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AnimatedContent(
              targetState = addStationViewModel.stationHostData[formatedURL.value],
            ) { hostData ->
              if (hostData != null) {
                val mounts = hostData.filterNot { host ->
                  host.station.mounts.filterNot { mount ->
                    listOf(
                      "flac",
                      "opus",
                      "ogg"
                    ).contains(mount.format)
                  }.isEmpty()
                }
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 180.dp)) {
                  itemsIndexed(mounts) { _, item ->
                    val supportedMounts = item.station.mounts.filterNot { mount ->
                      listOf(
                        "flac",
                        "opus",
                        "ogg"
                      ).contains(mount.format)
                    }
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Box {
                        AsyncImage(
                          model = ImageRequest.Builder(LocalContext.current)
                            .data(item.nowPlaying.song.art.toString().fixHttps())
                            .crossfade(true)
                            .placeholderMemoryCacheKey(
                              item.nowPlaying.song.art.toString()
                                .fixHttps()
                            )
                            .placeholder(
                              if (MaterialTheme.colorScheme.isDark()) {
                                R.drawable.loading_image_dark
                              } else {
                                R.drawable.loading_image
                              }
                            )
                            .diskCacheKey(item.nowPlaying.song.art.toString().fixHttps())
                            .build(),
                          contentDescription = item.station.name,
                          contentScale = ContentScale.FillBounds,
                          error = if (MaterialTheme.colorScheme.isDark()) {
                            painterResource(R.drawable.image_loading_failed_dark)
                          } else {
                            painterResource(R.drawable.image_loading_failed)
                          },
                          modifier = Modifier
                            .size(174.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                              true,
                              onClick = {
                                if (mounts.isNotEmpty()) {
                                  checkedStations = if (checkedStations.contains(
                                      AddableStation(
                                        item.station.name,
                                        item.station.shortcode,
                                        supportedMounts[0].url.fixHttps()
                                      )
                                    )
                                  ) {
                                    checkedStations.minus(
                                      AddableStation(
                                        item.station.name,
                                        item.station.shortcode,
                                        supportedMounts[0].url.fixHttps()
                                      )
                                    )
                                  } else {
                                    checkedStations.plus(
                                      AddableStation(
                                        item.station.name,
                                        item.station.shortcode,
                                        supportedMounts[0].url.fixHttps()
                                      )
                                    )
                                  }
                                } else {
                                  Toast.makeText(
                                    context,
                                    "No compatible mounts available for this station",
                                    Toast.LENGTH_LONG
                                  ).show()
                                  addStationViewModel.stationHostData.clear()
                                  addStationViewModel.isSearchInvalid.value = true
                                }
                              })
                            .conditional(
                              checkedStations.contains(
                                AddableStation(
                                  item.station.name,
                                  item.station.shortcode,
                                  supportedMounts[0].url.fixHttps()
                                )
                              )
                            ) {
                              drawWithContent {
                                drawContent()
                                drawRect(
                                  Brush.radialGradient(
                                    listOf(
                                      Color.Transparent,
                                      colorScheme.primary
                                    ),
                                    radius = 500f
                                  )
                                )
                                drawCircle(
                                  colorScheme.primaryContainer,
                                  radius = 95f
                                )
                              }
                            }
                        )
                        if (checkedStations.contains(
                            AddableStation(
                              item.station.name,
                              item.station.shortcode,
                              supportedMounts[0].url.fixHttps()
                            )
                          )
                        ) {
                          Icon(
                            modifier = Modifier
                              .align(Alignment.Center)
                              .size(64.dp),
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimary
                          )
                        }
                      }
                      Column(
                        Modifier
                          .fillMaxWidth()
                          .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                        Text(
                          text = item.station.name,
                          style = MaterialTheme.typography.titleMedium,
                          maxLines = 1,
                          modifier = Modifier
                            .widthIn(max = 164.dp)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                          fontWeight = FontWeight.Bold
                        )
                        Row(
                          modifier = Modifier.widthIn(max = 164.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "Playing: ",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmallEmphasized
                          )
                          Text(
                            text = item.nowPlaying.song.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmallEmphasized,
                            modifier = Modifier
                              .basicMarquee(iterations = Int.MAX_VALUE)
                          )
                        }
                      }

                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

