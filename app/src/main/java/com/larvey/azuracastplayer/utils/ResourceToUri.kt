package com.larvey.azuracastplayer.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

fun resourceToUri(context: Context, resID: Int): Uri {
  return Uri.parse(
    ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
        context.resources.getResourcePackageName(resID) + '/' +
        context.resources.getResourceTypeName(resID) + '/' +
        context.resources.getResourceEntryName(resID)
  )
}