package app.aryan447.mpvium.ui.preferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference

/**
 * A single-choice preference that opens an expressive picker sheet instead of
 * the default list dialog. Options share one connected surface, and the
 * selected row animates its container color while the radio indicator springs
 * in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExpressiveListPreference(
  value: T,
  onValueChange: (T) -> Unit,
  values: List<T>,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: @Composable (() -> Unit)? = null,
  summary: @Composable (() -> Unit)? = null,
  valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
) {
  var showPicker by rememberSaveable { mutableStateOf(false) }

  Preference(
    title = title,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    summary = summary,
    onClick = { showPicker = true },
  )

  if (showPicker) {
    ExpressiveChoiceSheet(
      title = title,
      values = values,
      value = value,
      valueToText = valueToText,
      onValueChange = onValueChange,
      onDismiss = { showPicker = false },
    )
  }
}

/**
 * The picker sheet. Rows are grouped into one rounded surface so the list
 * reads as a connected choice group; tapping a row updates the preference and
 * dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ExpressiveChoiceSheet(
  title: @Composable () -> Unit,
  values: List<T>,
  value: T,
  valueToText: @Composable (T) -> AnnotatedString,
  onValueChange: (T) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      ProvideTextStyle(
        value = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
          title()
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(SettingsCardShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .selectableGroup(),
      ) {
        values.forEachIndexed { index, option ->
          ExpressiveChoiceRow(
            selected = option == value,
            text = valueToText(option),
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onValueChange(option)
              scope.launch {
                sheetState.hide()
                onDismiss()
              }
            },
          )
          if (index < values.lastIndex) {
            HorizontalDivider(
              modifier = Modifier.padding(start = 20.dp),
              thickness = 0.75.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveChoiceRow(
  selected: Boolean,
  text: AnnotatedString,
  onClick: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val containerColor by animateColorAsState(
    targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    label = "choiceRowContainer",
  )
  val contentColor by animateColorAsState(
    targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    label = "choiceRowContent",
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(containerColor)
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      )
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
      color = contentColor,
      modifier = Modifier.weight(1f),
    )
    RadioButton(
      selected = selected,
      onClick = null,
      colors = RadioButtonDefaults.colors(
        selectedColor = scheme.onSecondaryContainer,
        unselectedColor = scheme.onSurfaceVariant,
      ),
    )
  }
}
