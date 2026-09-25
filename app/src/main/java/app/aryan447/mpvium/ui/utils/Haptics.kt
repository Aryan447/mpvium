package app.aryan447.mpvium.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.aryan447.mpvium.ui.utils.rememberHapticFeedback

/**
 * Master switch for all in-app haptic feedback, backed by the
 * "Haptic feedback" preference. Provided at the activity roots
 * ([MainActivity][app.aryan447.mpvium.MainActivity] and the player
 * controls); defaults to on so previews stay truthful.
 */
val LocalHapticsEnabled: ProvidableCompositionLocal<Boolean> =
  compositionLocalOf { true }

/**
 * Drop-in replacement for [LocalHapticFeedback] that silently no-ops
 * while [LocalHapticsEnabled] is off. Use this everywhere instead of
 * reading [LocalHapticFeedback] directly.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
  val delegate = rememberHapticFeedback()
  val enabled = LocalHapticsEnabled.current
  return remember(delegate, enabled) {
    object : HapticFeedback {
      override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (enabled) delegate.performHapticFeedback(hapticFeedbackType)
      }
    }
  }
}
