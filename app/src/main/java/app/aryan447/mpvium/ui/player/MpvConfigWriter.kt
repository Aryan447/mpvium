package app.aryan447.mpvium.ui.player

import app.aryan447.mpvium.preferences.MpvConfigOwnershipPolicy
import `is`.xyz.mpv.MPVLib

/**
 * Writes player options through the mpv.conf ownership policy.
 *
 * A selected ownership group means the user's mpv.conf is the source of truth,
 * so mpvium must not overwrite those options when a file starts. Everything
 * outside the ownership groups (track selection, pause, position, volume,
 * lifecycle infrastructure) passes through unchanged, and so do adjustments
 * the user makes in the player afterwards.
 */
object MpvConfigWriter {
  fun setOptionString(name: String, value: String) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setOptionString(name, value)
  }

  fun setPropertyString(name: String, value: String) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyString(name, value)
  }

  fun setPropertyInt(name: String, value: Int) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyInt(name, value)
  }

  fun setPropertyFloat(name: String, value: Float) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyFloat(name, value)
  }

  fun setPropertyDouble(name: String, value: Double) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyDouble(name, value)
  }

  fun setPropertyBoolean(name: String, value: Boolean) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyBoolean(name, value)
  }

  fun setPropertyLong(name: String, value: Long) {
    if (MpvConfigOwnershipPolicy.isOwnedByMpvConf(name)) return
    MPVLib.setPropertyLong(name, value)
  }

  fun command(vararg args: String) {
    if (MpvConfigOwnershipPolicy.shouldSuppress(args)) return
    MPVLib.command(*args)
  }
}
