package app.aryan447.mpvium.ui.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.launch

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

  var isFocused by mutableStateOf(false)
    internal set

  val readOnly: Boolean
    get() = isTv && !isEditing

  fun onFocusChanged(focusState: FocusState) {
    isFocused = focusState.isFocused
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
      if (!isEditing) {
        if (keyEvent.type == KeyEventType.KeyDown) {
          return true
        } else if (keyEvent.type == KeyEventType.KeyUp) {
          startEditing()
          return true
        }
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

  // When editing mode starts on TV, ensure the software keyboard opens once
  // the text field recomposes with readOnly = false.
  LaunchedEffect(state.isEditing) {
    if (isTv && state.isEditing) {
      keyboardController?.show()
    }
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
 *
 * Avoids attaching [Modifier.clickable] which creates an outer focus target
 * that swallows D-pad navigation and suppresses standard text field focus rendering.
 */
fun Modifier.tvTextFieldControl(
  state: TvTextFieldState,
  interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
  if (!state.isTv) return@composed this

  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val coroutineScope = rememberCoroutineScope()

  this
    .bringIntoViewRequester(bringIntoViewRequester)
    .onFocusChanged { focusState ->
      state.onFocusChanged(focusState)
      if (focusState.isFocused) {
        coroutineScope.launch {
          bringIntoViewRequester.bringIntoView()
        }
      }
    }
    .onPreviewKeyEvent { state.handleKeyEvent(it) }
    .pointerInput(state) {
      detectTapGestures {
        state.startEditing()
      }
    }
}
