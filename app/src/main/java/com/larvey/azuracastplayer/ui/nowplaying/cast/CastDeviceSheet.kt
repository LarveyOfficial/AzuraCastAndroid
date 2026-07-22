package com.larvey.azuracastplayer.ui.nowplaying.cast

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.mediarouter.media.MediaRouter
import com.larvey.azuracastplayer.classes.models.CastManager
import com.larvey.azuracastplayer.session.cast.CastConnectivity
import com.larvey.azuracastplayer.utils.RoundedStarShape
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * The Cast device sheet. Opened from the Now Playing top-bar cast button. Shows
 * nearby Cast devices, the connected device + volume + disconnect, plus Wi-Fi and
 * Bluetooth connectivity tiles, across a Controls / Devices tab pair. The tab pair
 * animates on switch (scale bounce + neighbour nudge, see [CastTab]) and the sheet
 * height animates between the two tabs' content sizes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDeviceSheet(
  castManager: CastManager,
  castConnectivity: CastConnectivity,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  val requiredPermissions = remember {
    buildList {
      // We only read already-paired / connected Bluetooth audio devices (needs
      // BLUETOOTH_CONNECT), and never scan for new ones — so no BLUETOOTH_SCAN.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
      }
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }
  }
  var missingPermissions by remember { mutableStateOf(missingPermissions(context, requiredPermissions)) }
  var requestedOnce by remember { mutableStateOf(false) }

  fun recheck() {
    missingPermissions = missingPermissions(context, requiredPermissions)
    if (missingPermissions.isEmpty()) {
      castConnectivity.refresh()
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { recheck() }

  LaunchedEffect(Unit) { recheck() }

  // Re-check when returning to the app (e.g. after granting from app settings).
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) recheck()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Keep active device discovery running for the whole time the sheet is open, so
  // other Cast devices stay visible even after connecting to one. Drop back to
  // passive discovery when the sheet closes.
  DisposableEffect(missingPermissions.isEmpty()) {
    if (missingPermissions.isEmpty()) {
      castManager.beginActiveDiscovery()
    }
    onDispose { castManager.endActiveDiscovery() }
  }

  fun animatedDismiss() {
    scope.launch { sheetState.hide() }.invokeOnCompletion {
      if (!sheetState.isVisible) onDismiss()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    shape = RoundedCornerShape(
      topStart = getRoundedCornerRadius(),
      topEnd = getRoundedCornerRadius()
    ),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 12.dp
  ) {
    if (missingPermissions.isNotEmpty()) {
      CastPermissionStep(
        onRequest = {
          requestedOnce = true
          permissionLauncher.launch(missingPermissions.toTypedArray())
        },
        // If a request already happened and permission is still missing, it may be
        // permanently denied — offer the app settings as an escape hatch.
        showOpenSettings = requestedOnce,
        onOpenSettings = {
          context.startActivity(
            Intent(
              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        }
      )
    } else {
      CastSheetBody(
        castManager = castManager,
        castConnectivity = castConnectivity,
        onDisconnect = {
          castManager.disconnect()
          animatedDismiss()
        }
      )
    }
  }
}

@Composable
private fun CastPermissionStep(
  onRequest: () -> Unit,
  showOpenSettings: Boolean,
  onOpenSettings: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Get ready to connect",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Allow nearby-device access so AzuraCast can find Cast devices on your network.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
      onClick = onRequest,
      shape = RoundedCornerShape(50),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Allow access")
    }
    if (showOpenSettings) {
      Text(
        text = "Still blocked? Enable nearby-device access in app settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Button(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Open app settings")
      }
    }
    Spacer(Modifier.height(8.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastSheetBody(
  castManager: CastManager,
  castConnectivity: CastConnectivity,
  onDisconnect: () -> Unit
) {
  val context = LocalContext.current

  val isCasting = castManager.isCasting.value
  val isConnecting = castManager.isConnecting.value
  val routes = castManager.castRoutes.value.filterNot { it.isDefault }
  val selectedRoute = castManager.selectedRoute.value?.takeUnless { it.isDefault }
  val routeVolume = castManager.routeVolume.value
  val isRefreshing = castManager.isRefreshingRoutes.value
  val btDevices = castConnectivity.bluetoothDevices.value

  val refresh: () -> Unit = {
    castManager.refreshRoutes()
    castConnectivity.refresh()
  }

  val pagerState = rememberPagerState(
    initialPage = if (isCasting || isConnecting) 0 else 1,
    pageCount = { 2 }
  )
  val scope = rememberCoroutineScope()

  // Bound the pager so a long device list can't push the sheet past the screen; the
  // sheet animates between the two tabs' content heights within this cap.
  val configuration = LocalConfiguration.current
  val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
  val maxPagerHeight = (
    configuration.screenHeightDp.dp -
      safeInsets.calculateTopPadding() -
      safeInsets.calculateBottomPadding() -
      212.dp
    ).coerceAtLeast(280.dp)

  // Skip the height animation for the first couple of frames so the sheet opens at its
  // natural size instead of growing from zero; animate every switch after that.
  var heightAnimationEnabled by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    withFrameNanos { }
    withFrameNanos { }
    heightAnimationEnabled = true
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .background(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = CircleShape
        )
    ) {
      Text(
        text = "Connect device",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
      )
    }
    Spacer(Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = maxPagerHeight)
        .animateContentSize(
          animationSpec = if (heightAnimationEnabled) {
            tween(durationMillis = 280, easing = FastOutSlowInEasing)
          } else {
            snap()
          },
          alignment = Alignment.TopCenter
        )
    ) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .wrapContentHeight()
          .fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) { page ->
        when (page) {
          0 -> CastControlsTab(
            isCasting = isCasting,
            isConnecting = isConnecting,
            deviceName = selectedRoute?.name,
            volume = routeVolume,
            volumeMax = (selectedRoute?.volumeMax ?: 20).coerceAtLeast(1),
            onVolumeChange = { castManager.setRouteVolume(it) },
            onDisconnect = onDisconnect,
            wifiConnected = castConnectivity.isWifiEnabled.value,
            wifiName = castConnectivity.wifiName.value,
            bluetoothConnected = castConnectivity.isBluetoothEnabled.value,
            onOpenWifi = { context.openSettings(Settings.ACTION_WIFI_SETTINGS) },
            onOpenBluetooth = { context.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
            onRefresh = refresh
          )
          1 -> CastDevicesTab(
            routes = routes,
            selectedRouteId = selectedRoute?.id,
            isCasting = isCasting,
            isRefreshing = isRefreshing,
            btDevices = btDevices,
            maxContentHeight = maxPagerHeight,
            onSelect = { route -> castManager.selectRoute(route) },
            onDisconnect = onDisconnect,
            onOpenBluetooth = { context.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
            onRefresh = refresh
          )
        }
      }
    }

    PrimaryTabRow(
      selectedTabIndex = pagerState.currentPage,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(5.dp),
      containerColor = Color.Transparent,
      divider = {},
      indicator = {}
    ) {
      CastTab(
        index = 0,
        selectedIndex = pagerState.currentPage,
        transformOrigin = TransformOrigin(0f, 0.5f),
        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Speaker,
            contentDescription = null,
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .size(18.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text("Controls", fontWeight = FontWeight.Bold)
        }
      }
      CastTab(
        index = 1,
        selectedIndex = pagerState.currentPage,
        transformOrigin = TransformOrigin(1f, 0.5f),
        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Devices,
            contentDescription = null,
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .size(18.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text("Devices", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun CastControlsTab(
  isCasting: Boolean,
  isConnecting: Boolean,
  deviceName: String?,
  volume: Int,
  volumeMax: Int,
  onVolumeChange: (Int) -> Unit,
  onDisconnect: () -> Unit,
  wifiConnected: Boolean,
  wifiName: String?,
  bluetoothConnected: Boolean,
  onOpenWifi: () -> Unit,
  onOpenBluetooth: () -> Unit,
  onRefresh: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Card(
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            if (isConnecting) {
              CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.Cast,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = deviceName ?: "This phone",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = when {
                isConnecting -> "Connecting…"
                isCasting -> "Casting"
                else -> "Local playback"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              maxLines = 1
            )
          }
        }

        if (isCasting || isConnecting) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Device volume",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            var sliderValue by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
            Slider(
              value = sliderValue.coerceIn(0f, volumeMax.toFloat()),
              onValueChange = {
                sliderValue = it
                onVolumeChange(it.toInt())
              },
              valueRange = 0f..volumeMax.toFloat()
            )
          }
          Button(
            onClick = onDisconnect,
            shape = RoundedCornerShape(50),
            enabled = !isConnecting,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
          ) {
            Text("Disconnect")
          }
        } else {
          Text(
            text = "Pick a device on the Devices tab to start casting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Connectivity",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = if (!wifiConnected && !bluetoothConnected) {
            "Turn on Wi-Fi or Bluetooth"
          } else {
            "Manage active radios and rescan"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      FilledRefreshButton(onClick = onRefresh)
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      QuickTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Wifi,
        label = if (wifiConnected) (wifiName ?: "Wi-Fi") else "Wi-Fi",
        subtitle = if (wifiConnected) "Connected" else "Off",
        active = wifiConnected,
        onClick = onOpenWifi
      )
      QuickTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Bluetooth,
        label = "Bluetooth",
        subtitle = if (bluetoothConnected) "On" else "Off",
        active = bluetoothConnected,
        onClick = onOpenBluetooth
      )
    }
    Spacer(Modifier.height(20.dp))
  }
}

@Composable
private fun QuickTile(
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  subtitle: String,
  active: Boolean,
  onClick: () -> Unit
) {
  val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
  val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  Row(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .clickable { onClick() }
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(bg),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
    Column {
      Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
  }
}

@Composable
private fun CastDevicesTab(
  routes: List<MediaRouter.RouteInfo>,
  selectedRouteId: String?,
  isCasting: Boolean,
  isRefreshing: Boolean,
  btDevices: List<com.larvey.azuracastplayer.session.cast.BluetoothAudioDevice>,
  maxContentHeight: androidx.compose.ui.unit.Dp,
  onSelect: (MediaRouter.RouteInfo) -> Unit,
  onDisconnect: () -> Unit,
  onOpenBluetooth: () -> Unit,
  onRefresh: () -> Unit
) {
  val hasDevices = routes.isNotEmpty() || btDevices.isNotEmpty()
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = maxContentHeight),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(key = "nearbyHeader") {
      DeviceSectionHeader(hasDevices = hasDevices, onRefresh = onRefresh)
    }
    if (isRefreshing) {
      item(key = "refreshIndicator") {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
        )
      }
    }
    if (!hasDevices) {
      item(key = "empty") {
        Text(
          text = if (isRefreshing) "Searching for devices…" else "No devices found",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp)
        )
      }
    }
    items(routes, key = { it.id }) { route ->
      val isSelected = route.id == selectedRouteId
      CastDeviceRow(
        name = route.name,
        description = route.description,
        isTv = route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_TV,
        isBluetooth = false,
        isSelected = isSelected && isCasting,
        isConnecting = isSelected && !isCasting,
        onClick = {
          if (isSelected && isCasting) onDisconnect() else onSelect(route)
        }
      )
    }
    items(btDevices, key = { "bt_" + it.address }) { device ->
      CastDeviceRow(
        name = device.name,
        description = if (device.isConnected) "Connected" else "Bluetooth",
        isTv = false,
        isBluetooth = true,
        isSelected = false,
        isConnecting = false,
        onClick = onOpenBluetooth
      )
    }
  }
}

@Composable
private fun DeviceSectionHeader(
  hasDevices: Boolean,
  onRefresh: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.padding(start = 4.dp, end = 4.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = "Nearby devices",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = if (hasDevices) "Tap to connect" else "No devices yet",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    FilledRefreshButton(onClick = onRefresh)
  }
}

/** Material3 filled icon button (tonal surface background) used by both refresh affordances. */
@Composable
private fun FilledRefreshButton(onClick: () -> Unit) {
  IconButton(
    onClick = onClick,
    colors = IconButtonDefaults.iconButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.onSurface
    ),
    modifier = Modifier.clip(RoundedCornerShape(16.dp))
  ) {
    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
  }
}

/**
 * A single pill tab in the Controls / Devices switch. On selection change it does a brief
 * scale bounce (1 → 1.05 → 1) and nudges its direct neighbour aside (±12px), then settles —
 * so switching tabs feels springy rather than instant. The first composition does not animate.
 */
@Composable
private fun CastTab(
  index: Int,
  selectedIndex: Int,
  onClick: () -> Unit,
  transformOrigin: TransformOrigin,
  content: @Composable () -> Unit
) {
  val haptics = LocalHapticFeedback.current
  val isSelected = index == selectedIndex

  val selectedColor = MaterialTheme.colorScheme.primary
  val unselectedColor = MaterialTheme.colorScheme.surface
  val onSelectedColor = MaterialTheme.colorScheme.onPrimary
  val onUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)

  val animationSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
  val scale = remember { Animatable(1f) }
  val offsetX = remember { Animatable(0f) }
  var hasAnimatedSelectionChange by remember { mutableStateOf(false) }

  val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) selectedColor else unselectedColor,
    animationSpec = tween(durationMillis = 200),
    label = "castTabBackground"
  )
  val contentColor by animateColorAsState(
    targetValue = if (isSelected) onSelectedColor else onUnselectedColor,
    animationSpec = tween(durationMillis = 200),
    label = "castTabContent"
  )

  // Animate only on actual selection changes, not on first composition.
  LaunchedEffect(selectedIndex) {
    if (!hasAnimatedSelectionChange) {
      hasAnimatedSelectionChange = true
      scale.snapTo(1f)
      offsetX.snapTo(0f)
      return@LaunchedEffect
    }
    if (isSelected) {
      launch {
        scale.animateTo(1.05f, animationSpec = animationSpec)
        scale.animateTo(1f, animationSpec = animationSpec)
      }
      offsetX.snapTo(0f)
    } else {
      scale.snapTo(1f)
      val distance = index - selectedIndex
      if (abs(distance) == 1) {
        val direction = if (distance > 0) 1 else -1
        launch {
          offsetX.animateTo(12f * direction, animationSpec = animationSpec)
          offsetX.animateTo(0f, animationSpec = animationSpec)
        }
      } else {
        offsetX.snapTo(0f)
      }
    }
  }

  Tab(
    modifier = Modifier
      .padding(all = 5.dp)
      .graphicsLayer {
        scaleX = scale.value
        translationX = offsetX.value
        this.transformOrigin = transformOrigin
      }
      .clip(CircleShape)
      .background(
        color = backgroundColor,
        shape = RoundedCornerShape(50)
      ),
    selected = isSelected,
    text = content,
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    selectedContentColor = contentColor,
    unselectedContentColor = contentColor
  )
}

@Composable
private fun CastDeviceRow(
  name: String,
  description: CharSequence?,
  isTv: Boolean,
  isBluetooth: Boolean,
  isSelected: Boolean,
  isConnecting: Boolean,
  onClick: () -> Unit
) {
  val container = when {
    isSelected -> MaterialTheme.colorScheme.primaryContainer
    isBluetooth -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
  }
  val onContainer = when {
    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
    isBluetooth -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onSurface
  }
  val icon = when {
    isBluetooth -> Icons.Rounded.Bluetooth
    isTv -> Icons.Rounded.Tv
    else -> Icons.Rounded.Speaker
  }

  // Slow rotation on the active device's scalloped icon backdrop.
  val rotation by rememberInfiniteTransition(label = "castRowRotation").animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "castRowRotationValue"
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(CircleShape)
      .background(container)
      .clickable { onClick() }
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Box(
      modifier = Modifier.size(48.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .graphicsLayer { if (isSelected) rotationZ = rotation }
          .background(
            color = onContainer.copy(alpha = 0.12f),
            shape = if (isSelected) RoundedStarShape(sides = 8, curve = 0.10) else CircleShape
          )
      )
      if (isConnecting) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = onContainer)
      } else {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(24.dp))
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = onContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      val status = when {
        isSelected -> "Connected"
        isConnecting -> "Connecting…"
        else -> description?.toString() ?: "Available"
      }
      Text(status, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.8f), maxLines = 1)
    }
  }
}

private fun missingPermissions(context: Context, permissions: List<String>): List<String> =
  permissions.filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
  }

private fun Context.openSettings(action: String) {
  runCatching {
    startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
}
