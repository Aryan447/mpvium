package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.CustomScriptButton
import app.aryan447.mpvium.ui.player.controls.LocalPlayerColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomButtonsRow(
  buttons: List<CustomScriptButton>,
  onExecute: (String) -> Unit,
  resetControlsTimestamp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (buttons.isEmpty()) return

  val haptic = LocalHapticFeedback.current
  val colors = LocalPlayerColors.current

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    buttons.forEach { button ->
      val hasLongPress = button.longPressCode.isNotBlank()
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .combinedClickable(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              resetControlsTimestamp()
              onExecute(button.code)
            },
            onLongClick = if (hasLongPress) {
              {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resetControlsTimestamp()
                onExecute(button.longPressCode)
              }
            } else {
              { /* No-op to prevent single-click triggering on long press */ }
            },
          ),
        shape = RoundedCornerShape(50),
        color = colors.chipFill,
        contentColor = colors.chipContent,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, colors.controlBorder),
      ) {
        Text(
          text = button.title,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
      }
    }
  }
}
