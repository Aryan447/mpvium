package app.aryan447.mpvium.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassToggleButtonColors

/**
 * Single-choice row built from connected Material 3 Expressive toggle
 * buttons. The selected item morphs into a full pill while the rest stay
 * joined, and the row scrolls when the labels do not fit.
 *
 * [equalWidth] shares one equal width across every option instead, so the
 * group can span a card edge to edge and line up with adjacent content. Use it
 * for short, fixed sets (two to four short labels); keep the default scrolling
 * behaviour for long or dynamic option lists.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ExpressiveChoiceRow(
  options: List<T>,
  selected: T,
  labelOf: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  equalWidth: Boolean = false,
) {
  val colors = expressiveChoiceColors()

  if (equalWidth) {
    Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      options.forEachIndexed { index, option ->
        ChoiceToggle(
          label = labelOf(option),
          isSelected = option == selected,
          index = index,
          count = options.size,
          centered = true,
          colors = colors,
          onSelect = { onSelect(option) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  } else {
    LazyRow(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      itemsIndexed(options, key = { _, option -> labelOf(option) }) { index, option ->
        ChoiceToggle(
          label = labelOf(option),
          isSelected = option == selected,
          index = index,
          count = options.size,
          centered = false,
          colors = colors,
          onSelect = { onSelect(option) },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceToggle(
  label: String,
  isSelected: Boolean,
  index: Int,
  count: Int,
  centered: Boolean,
  colors: ToggleButtonColors,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  ToggleButton(
    checked = isSelected,
    onCheckedChange = {
      if (!isSelected) {
        // SegmentTick is the platform haptic for segment selection: a light
        // tick. LongPress (used before) reads as a heavy buzz and is suppressed
        // on One UI in several vibration configurations.
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onSelect()
      }
    },
    modifier = modifier,
    shapes =
      when {
        count == 1 -> ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
      },
    colors = colors,
    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      textAlign = if (centered) TextAlign.Center else null,
      modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun expressiveChoiceColors(): ToggleButtonColors {
  if (LocalGlass.current) return glassToggleButtonColors()
  val scheme = MaterialTheme.colorScheme
  return ToggleButtonDefaults.colors(
    containerColor = Color.Transparent,
    contentColor = scheme.onSurfaceVariant,
    checkedContainerColor = scheme.primary,
    checkedContentColor = scheme.onPrimary,
  )
}
