package app.aryan447.mpvium.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Utility helpers for detecting device types, such as Android TV / Google TV.
 */
object DeviceUtils {
  private var isTvCached: Boolean? = null

  /**
   * Returns true if the app is running on an Android TV, Google TV, or Fire TV device.
   * Caches result to avoid repeated PackageManager queries.
   */
  fun isTv(context: Context): Boolean {
    isTvCached?.let { return it }

    val appContext = context.applicationContext ?: context
    val uiModeManager = appContext.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    val pm = appContext.packageManager
    val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val hasFireTv = pm.hasSystemFeature("amazon.hardware.fire_tv")
    val lacksTouchscreen = !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

    val result = isTvMode || hasLeanback || hasFireTv || lacksTouchscreen
    isTvCached = result
    return result
  }
}
