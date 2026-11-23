package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.preferences.CustomScriptButton

/**
 * Modern floating glass pill row for user-defined custom mpv / Lua script buttons.
 * Each button is styled as an independent frosted-glass pill chip matching Modern Player aesthetics.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomButtonsRow(
  buttons: List<CustomScriptButton>,
  onExecute: (String) -> Unit,
  resetControlsTimestamp: () -> Unit,
  modifier: Modifier = Modifier,
  surfaceColor: Color = Color(0x9916181D),
  borderStroke: BorderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
) {
  if (buttons.isEmpty()) return

  val haptic = LocalHapticFeedback.current

  Row(
    modifier = modifier
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    buttons.forEach { button ->
      val hasLongPress = button.longPressCode.isNotBlank()
      Surface(
        modifier = Modifier
          .height(36.dp)
          .clip(RoundedCornerShape(18.dp))
          .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White),
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
            } else null,
          ),
        shape = RoundedCornerShape(18.dp),
        color = surfaceColor,
        border = borderStroke,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
      ) {
        Box(
          modifier = Modifier.padding(horizontal = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = button.title,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp,
              letterSpacing = 0.2.sp,
              color = Color.White,
              shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 3f),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
