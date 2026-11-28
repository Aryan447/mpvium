package app.aryan447.mpvium.ui.utils

import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvKeyboardHelperTest {

  private class TestFocusState(override val isFocused: Boolean) : FocusState {
    override val hasFocus: Boolean get() = isFocused
    override val isCaptured: Boolean get() = false
  }

  @Test
  fun testMobileBehaviorPreserved() {
    val state = TvTextFieldState(isTv = false, keyboardController = null)

    // Mobile: starts in editing mode, readOnly is false
    assertTrue(state.isEditing)
    assertFalse(state.readOnly)

    // Key events are not intercepted on mobile
    val downEvent = KeyEvent(NativeKeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_CENTER))
    assertFalse(state.handleKeyEvent(downEvent))

    // Focus change does not force editing off on mobile
    state.onFocusChanged(TestFocusState(false))
    assertTrue(state.isEditing)
    assertFalse(state.readOnly)
  }

  @Test
  fun testTvFocusAndKeyboardActivation() {
    val state = TvTextFieldState(isTv = true, keyboardController = null)

    // TV: starts not editing, readOnly is true (prevents automatic keyboard popup on focus)
    assertFalse(state.isEditing)
    assertTrue(state.readOnly)

    // D-Pad Center KeyDown is consumed so it doesn't trigger parent clicks or leak
    val centerDown = KeyEvent(NativeKeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_CENTER))
    assertTrue(state.handleKeyEvent(centerDown))
    assertFalse(state.isEditing)
    assertTrue(state.readOnly)

    // D-Pad Center KeyUp explicitly activates editing mode
    val centerUp = KeyEvent(NativeKeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_CENTER))
    assertTrue(state.handleKeyEvent(centerUp))
    assertTrue(state.isEditing)
    assertFalse(state.readOnly)

    // Once editing, normal typing keys (e.g. Enter or characters) are not consumed by the helper
    assertFalse(state.handleKeyEvent(centerDown))

    // Stopping editing returns to readOnly state
    state.stopEditing()
    assertFalse(state.isEditing)
    assertTrue(state.readOnly)
  }

  @Test
  fun testTvFocusChangeResetsEditing() {
    val state = TvTextFieldState(isTv = true, keyboardController = null)
    state.startEditing()
    assertTrue(state.isEditing)

    // Moving focus away resets editing mode so subsequent fields don't inherit active editing
    state.onFocusChanged(TestFocusState(false))
    assertFalse(state.isEditing)
    assertTrue(state.readOnly)
  }
}
