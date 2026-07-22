package com.larvey.azuracastplayer.session.cast

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

/** A Bluetooth audio (A2DP) device shown in the cast sheet. */
data class BluetoothAudioDevice(
  val name: String,
  val address: String,
  val isConnected: Boolean
)

/**
 * Lightweight Wi-Fi + Bluetooth state for the cast device sheet's connectivity
 * tiles and Bluetooth device list. All reads are best-effort and permission-safe
 * (wrapped in try/catch) — the sheet gates the relevant runtime permissions
 * before showing devices, but this must never crash if they're missing.
 *
 * SSID is intentionally not read on Android 12+ (it would imply location access);
 * we show a generic "Wi-Fi Connected" label there instead.
 */
class CastConnectivity(private val context: Context) {

  val isWifiEnabled = mutableStateOf(false)
  val wifiName = mutableStateOf<String?>(null)
  val isBluetoothEnabled = mutableStateOf(false)
  val bluetoothDevices = mutableStateOf<List<BluetoothAudioDevice>>(emptyList())

  private val wifiManager: WifiManager? =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

  private val bluetoothAdapter: BluetoothAdapter? =
    (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

  private var a2dpProxy: BluetoothA2dp? = null

  init {
    // Cache the A2DP profile proxy so we can report which bonded audio devices
    // are actually connected.
    runCatching {
      bluetoothAdapter?.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {
          override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
              a2dpProxy = proxy as? BluetoothA2dp
              refresh()
            }
          }

          override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
          }
        },
        BluetoothProfile.A2DP
      )
    }
  }

  /** Recompute all state. Called when the sheet opens and on its refresh button. */
  fun refresh() {
    isWifiEnabled.value = wifiManager?.isWifiEnabled == true
    wifiName.value = resolveWifiName()
    isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    bluetoothDevices.value = resolveBluetoothDevices()
  }

  private fun resolveWifiName(): String? {
    if (wifiManager?.isWifiEnabled != true) return null
    // Reading the SSID requires location on ≤ Android 11; on 12+ we avoid it.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
      @Suppress("DEPRECATION")
      val ssid = runCatching { wifiManager?.connectionInfo?.ssid }.getOrNull()
      if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
        return ssid.trim('"')
      }
    }
    return "Wi-Fi Connected"
  }

  // Every Bluetooth read below is guarded by an explicit BLUETOOTH_CONNECT check
  // and wrapped in runCatching; lint can't trace the helper, so suppress here.
  @SuppressLint("MissingPermission")
  private fun resolveBluetoothDevices(): List<BluetoothAudioDevice> {
    val adapter = bluetoothAdapter ?: return emptyList()
    if (!adapter.isEnabled) return emptyList()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    ) {
      return emptyList()
    }
    val connected = runCatching { a2dpProxy?.connectedDevices?.map { it.address }?.toSet() }
      .getOrNull() ?: emptySet()
    return runCatching {
      adapter.bondedDevices
        .filter { it.isAudioDevice() }
        .map { device ->
          BluetoothAudioDevice(
            name = device.safeName() ?: device.address,
            address = device.address,
            isConnected = device.address in connected
          )
        }
    }.getOrDefault(emptyList())
  }

  @SuppressLint("MissingPermission")
  private fun BluetoothDevice.isAudioDevice(): Boolean {
    val major = runCatching { bluetoothClass?.majorDeviceClass }.getOrNull()
    return major == BluetoothClass.Device.Major.AUDIO_VIDEO
  }

  @SuppressLint("MissingPermission")
  private fun BluetoothDevice.safeName(): String? =
    runCatching { name }.getOrNull()?.takeIf { it.isNotBlank() }

  private fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
