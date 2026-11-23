package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterStart,
  ) {
    Surface(
      modifier = Modifier.height(38.dp),
      shape = RoundedCornerShape(19.dp),
      color = surfaceColor,
      border = borderStroke,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
    ) {
      Row(
        modifier = Modifier
          .fillMaxHeight()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        buttons.forEachIndexed { index, button ->
          val hasLongPress = button.longPressCode.isNotBlank()
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .clip(RoundedCornerShape(15.dp))
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
                } else {
                  { /* Consume long press so it does not trigger single click */ }
                },
              )
              .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = button.title,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  letterSpacing = 0.2.sp,
                  color = Color.White,
                  shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (hasLongPress) {
                Box(
                  modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                )
              }
            }
          }

          if (index < buttons.lastIndex) {
            Box(
              modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Color.White.copy(alpha = 0.16f)),
            )
          }
        }
      }
    }
  }
}
