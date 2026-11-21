package app.aryan447.mpvium.ui.utils

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * State manager for text field keyboard interaction on Android TV.
 *
 * Prevents the on-screen keyboard from automatically popping up when D-pad focus
 * merely passes over a text field. The keyboard is only shown when the user explicitly
 * presses Enter / D-pad Center or clicks on the focused field.
 *
 * On mobile/tablets ([isTv] = false), this acts as a no-op, preserving standard behavior.
 */
@Stable
class TvTextFieldState(
  val isTv: Boolean,
  private val keyboardController: SoftwareKeyboardController?,
) {
  var isEditing by mutableStateOf(!isTv)
    internal set

  val readOnly: Boolean
    get() = isTv && !isEditing

  fun onFocusChanged(focusState: FocusState) {
    if (isTv && !focusState.isFocused) {
      isEditing = false
    }
  }

  fun startEditing() {
    if (isTv) {
      isEditing = true
      keyboardController?.show()
    }
  }

  fun stopEditing() {
    if (isTv) {
      isEditing = false
      keyboardController?.hide()
    }
  }

  fun handleKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
    if (!isTv) return false
    val isCenterKey = keyEvent.key == Key.DirectionCenter ||
      keyEvent.key == Key.Enter ||
      keyEvent.key == Key.NumPadEnter

    if (isCenterKey) {
      if (!isEditing && keyEvent.type == KeyEventType.KeyUp) {
        startEditing()
        return true
      }
    }
    return false
  }
}

/**
 * Creates and remembers a [TvTextFieldState] scoped to the current platform.
 */
@Composable
fun rememberTvTextFieldState(
  isTv: Boolean = LocalIsTv.current,
): TvTextFieldState {
  val keyboardController = LocalSoftwareKeyboardController.current
  val state = remember(isTv, keyboardController) {
    TvTextFieldState(isTv, keyboardController)
  }

  if (isTv && state.isEditing) {
    BackHandler {
      state.stopEditing()
    }
  }

  return state
}

/**
 * Convenience modifier that attaches focus, click, and D-pad key handling
 * from a [TvTextFieldState] to a text field component.
 */
fun Modifier.tvTextFieldControl(
  state: TvTextFieldState,
  interactionSource: MutableInteractionSource? = null,
): Modifier {
  if (!state.isTv) return this
  return this
    .onFocusChanged { state.onFocusChanged(it) }
    .onPreviewKeyEvent { state.handleKeyEvent(it) }
    .clickable(
      interactionSource = interactionSource,
      indication = null,
      onClick = { state.startEditing() },
    )
}
