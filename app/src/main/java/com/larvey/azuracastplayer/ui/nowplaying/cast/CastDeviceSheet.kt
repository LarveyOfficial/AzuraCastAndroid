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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.mediarouter.media.MediaRouter
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.models.CastManager
import com.larvey.azuracastplayer.session.cast.CastConnectivity
import com.larvey.azuracastplayer.utils.RoundedStarShape
import com.larvey.azuracastplayer.utils.albumColors
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import kotlinx.coroutines.launch

/**
 * The Cast device sheet. Opened from the Now Playing top-bar cast button. Shows
 * nearby Cast devices, the connected device + volume + disconnect, plus Wi-Fi and
 * Bluetooth connectivity tiles, across a Controls / Devices tab pair. Themed from
 * the current station's palette via [albumColors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDeviceSheet(
  castManager: CastManager,
  castConnectivity: CastConnectivity,
  palette: Palette?,
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
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
        palette = palette,
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

@Composable
private fun CastSheetBody(
  castManager: CastManager,
  castConnectivity: CastConnectivity,
  palette: Palette?,
  onDisconnect: () -> Unit
) {
  val context = LocalContext.current
  val colors = albumColors(palette)

  val isCasting = castManager.isCasting.value
  val isConnecting = castManager.isConnecting.value
  val routes = castManager.castRoutes.value.filterNot { it.isDefault }
  val selectedRoute = castManager.selectedRoute.value?.takeUnless { it.isDefault }
  val routeVolume = castManager.routeVolume.value
  val isRefreshing = castManager.isRefreshingRoutes.value
  val btDevices = castConnectivity.bluetoothDevices.value

  val pagerState = rememberPagerState(
    initialPage = if (isCasting || isConnecting) 0 else 1,
    pageCount = { 2 }
  )
  val scope = rememberCoroutineScope()

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Connect device",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
      modifier = Modifier.padding(horizontal = 24.dp)
    )
    Spacer(Modifier.height(8.dp))

    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxWidth()
    ) { page ->
      when (page) {
        0 -> CastControlsTab(
          colors = colors,
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
          onRefresh = {
            castManager.refreshRoutes()
            castConnectivity.refresh()
          }
        )
        1 -> CastDevicesTab(
          colors = colors,
          routes = routes,
          selectedRouteId = selectedRoute?.id,
          isCasting = isCasting,
          isRefreshing = isRefreshing,
          btDevices = btDevices,
          onSelect = { route -> castManager.selectRoute(route) },
          onDisconnect = onDisconnect,
          onOpenBluetooth = { context.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
        )
      }
    }

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 12.dp)
    ) {
      SegmentedButton(
        selected = pagerState.currentPage == 0,
        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        icon = {
          Icon(Icons.Rounded.Speaker, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        label = { Text("Controls") }
      )
      SegmentedButton(
        selected = pagerState.currentPage == 1,
        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        icon = {
          Icon(Icons.Rounded.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        label = { Text("Devices") }
      )
    }
  }
}

@Composable
private fun CastControlsTab(
  colors: com.larvey.azuracastplayer.utils.AlbumColors,
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
      Text(
        text = "Connectivity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      IconButton(onClick = onRefresh) {
        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
      }
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
  colors: com.larvey.azuracastplayer.utils.AlbumColors,
  routes: List<MediaRouter.RouteInfo>,
  selectedRouteId: String?,
  isCasting: Boolean,
  isRefreshing: Boolean,
  btDevices: List<com.larvey.azuracastplayer.session.cast.BluetoothAudioDevice>,
  onSelect: (MediaRouter.RouteInfo) -> Unit,
  onDisconnect: () -> Unit,
  onOpenBluetooth: () -> Unit
) {
  val context = LocalContext.current
  Column(modifier = Modifier.fillMaxWidth()) {
    if (isRefreshing) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .clip(RoundedCornerShape(50))
      )
      Spacer(Modifier.height(8.dp))
    }
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 360.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (routes.isEmpty() && btDevices.isEmpty()) {
        item {
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
