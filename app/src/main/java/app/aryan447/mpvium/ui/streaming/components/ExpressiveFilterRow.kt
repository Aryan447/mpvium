package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassToggleButtonColors

/**
 * Single-choice filter row built from connected Material 3 Expressive toggle
 * buttons. The selected item morphs into a full pill while the rest stay
 * joined, and the row scrolls when the labels do not fit.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ExpressiveFilterRow(
  options: List<T>,
  selected: T,
  labelOf: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val colors = filterToggleColors()
  val unselectedBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

  LazyRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    itemsIndexed(options, key = { _, option -> labelOf(option) }) { index, option ->
      val isSelected = option == selected
      ToggleButton(
        checked = isSelected,
        onCheckedChange = {
          if (!isSelected) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(option)
          }
        },
        shapes =
          when {
            options.size == 1 -> ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
          },
        colors = colors,
        border = if (isSelected) null else unselectedBorder,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Text(
          text = labelOf(option),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          maxLines = 1,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun filterToggleColors(): ToggleButtonColors {
  if (LocalGlass.current) return glassToggleButtonColors()
  val scheme = MaterialTheme.colorScheme
  return ToggleButtonDefaults.toggleButtonColors(
    containerColor = Color.Transparent,
    contentColor = scheme.onSurfaceVariant,
    checkedContainerColor = scheme.primary,
    checkedContentColor = scheme.onPrimary,
  )
}
