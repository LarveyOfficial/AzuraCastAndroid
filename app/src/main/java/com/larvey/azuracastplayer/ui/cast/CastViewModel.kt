package com.larvey.azuracastplayer.ui.cast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CastDeviceUi(
    val id: String,
    val name: String,
    val description: String?,
    val isConnected: Boolean,
    val route: MediaRouter.RouteInfo
)

class CastViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRouter = MediaRouter.getInstance(application)
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    private val _deviceList = MutableStateFlow<List<CastDeviceUi>>(emptyList())
    val deviceList = _deviceList.asStateFlow()

    // Callback that listens for device updates
    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = updateRoutes()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = updateRoutes()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = updateRoutes()
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) = updateRoutes()
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) = updateRoutes()
    }

    // Call this when the sheet opens
    fun startDiscovery() {
        mediaRouter.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        updateRoutes()
    }

    // Call this when the sheet closes
    fun stopDiscovery() {
        mediaRouter.removeCallback(callback)
    }

    private fun updateRoutes() {
        val routes = mediaRouter.routes
        val uiList = routes
            .filter { !it.isDefault && it.matchesSelector(selector) } // Filter out phone speaker
            .map { route ->
                val castDevice = CastDevice.getFromBundle(route.extras)
                val isConnected = route.isSelected
                CastDeviceUi(
                    id = route.id,
                    name = route.name,
                    description = route.description,
                    isConnected = isConnected,
                    route = route
                )
            }
        _deviceList.value = uiList
    }

    fun selectRoute(route: MediaRouter.RouteInfo) {
        if (route.isSelected) {
            // Already connected? Disconnect (User clicked "Stop Casting")
            mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
        } else {
            mediaRouter.selectRoute(route)
        }
    }

    fun stopCasting() {
        mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
    }
}