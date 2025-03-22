package com.larvey.azuracastplayer.utils

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import com.google.android.renderscript.Toolkit

/**
 * A [Transformation] that applies a Gaussian blur^2 to an image.
 *
 * @param radius The radius of the blur.
 */
class BlurTransformation(
  private val radius: Int,
) : Transformation() {

  @Suppress("NullableToStringCall")
  override val cacheKey = "${BlurTransformation::class.java.name}-$radius"

  override suspend fun transform(input: Bitmap, size: Size): Bitmap {

    return Toolkit.blur(
      Toolkit.blur(
        input,
        radius
      ),
      radius
    )
  }
}