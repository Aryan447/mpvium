package app.aryan447.mpvium.ui.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.MediaLayoutMode
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor

/**
 * Shared tap-to-toggle icon button for switching between Grid and List layouts.
 * Matches the tonal icon-button style used across mpvium headers.
 */
@Composable
fun MediaLayoutToggleButton(
  layoutMode: MediaLayoutMode,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val isGrid = layoutMode == MediaLayoutMode.GRID
  FilledTonalIconButton(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onToggle()
    },
    modifier = modifier,
    colors = IconButtonDefaults.filledTonalIconButtonColors(
      containerColor = glassButtonContainerColor(MaterialTheme.colorScheme.secondaryContainer),
      contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSecondaryContainer),
    ),
  ) {
    Icon(
      imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
      contentDescription = if (isGrid) {
        stringResource(R.string.switch_to_list_view)
      } else {
        stringResource(R.string.switch_to_grid_view)
      },
    )
  }
}
