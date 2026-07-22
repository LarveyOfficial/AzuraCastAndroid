package com.larvey.azuracastplayer.session.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Bootstraps the Google Cast SDK. Declared in the manifest via the
 * `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME` meta-data
 * and instantiated reflectively by [com.google.android.gms.cast.framework.CastContext]
 * (hence the ProGuard keep rule).
 *
 * We target the stock Google **Default Media Receiver** — it needs no console
 * registration and plays any standard audio stream, which is all a radio station
 * needs.
 *
 * We deliberately do **not** set a `CastMediaOptions`. That tells the SDK not to
 * create its own media session or notification: the app keeps its single Media3
 * notification (driven by the local player, which stays running muted while
 * casting), and that notification's controls drive the receiver through
 * [CastManager]'s sync. Setting `CastMediaOptions` would spawn a competing cast
 * notification.
 */
class CastOptionsProvider : OptionsProvider {
  override fun getCastOptions(context: Context): CastOptions {
    return CastOptions.Builder()
      .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
      .build()
  }

  override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
