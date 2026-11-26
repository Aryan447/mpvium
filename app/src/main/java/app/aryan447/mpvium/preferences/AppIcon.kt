package app.aryan447.mpvium.preferences

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.aryan447.mpvium.R

enum class AppIcon(
  val key: String,
  @StringRes val titleRes: Int,
  @DrawableRes val previewRes: Int,
  val aliasClass: String,
) {
  PORTAL(
    key = "portal",
    titleRes = R.string.pref_app_icon_portal,
    previewRes = R.drawable.ic_launcher_portal_foreground,
    aliasClass = "app.aryan447.mpvium.MainActivityPortal",
  ),
  CLASSIC(
    key = "classic",
    titleRes = R.string.pref_app_icon_classic,
    previewRes = R.drawable.ic_launcher_classic_foreground,
    aliasClass = "app.aryan447.mpvium.MainActivityClassic",
  );

  companion object {
    fun fromKey(key: String): AppIcon =
      entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: PORTAL
  }
}

object AppIconManager {
  private const val TAG = "AppIconManager"

  fun sync(context: Context, targetIcon: AppIcon) {
    val pm = context.packageManager
    val portalComponent = ComponentName(context, AppIcon.PORTAL.aliasClass)
    val classicComponent = ComponentName(context, AppIcon.CLASSIC.aliasClass)

    val (enableComponent, disableComponent) = when (targetIcon) {
      AppIcon.PORTAL -> portalComponent to classicComponent
      AppIcon.CLASSIC -> classicComponent to portalComponent
    }

    try {
      val enableState = pm.getComponentEnabledSetting(enableComponent)
      val disableState = pm.getComponentEnabledSetting(disableComponent)

      if (enableState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        pm.setComponentEnabledSetting(
          enableComponent,
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
          PackageManager.DONT_KILL_APP,
        )
      }

      if (disableState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
        pm.setComponentEnabledSetting(
          disableComponent,
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP,
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync app icon alias", e)
    }
  }

  fun setAppIcon(context: Context, targetIcon: AppIcon) = sync(context, targetIcon)
}
