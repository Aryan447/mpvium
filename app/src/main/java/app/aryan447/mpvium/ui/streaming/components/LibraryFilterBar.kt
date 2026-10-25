package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor

/**
 * Generic option type for [LibraryFilterBar] dropdown sorts.
 */
data class SortOption<T>(
  val value: T,
  val label: String,
)

/**
 * Shared filter-chip row + sort dropdown used by the Shows and Movies grids.
 * Read-only: callers apply [options]/[selected] and [sorts]/[selectedSort]
 * to their already-loaded lists.
 */
@Composable
fun <F, S> LibraryFilterBar(
  options: List<F>,
  selected: F,
  labelOf: (F) -> String,
  onSelect: (F) -> Unit,
  sorts: List<SortOption<S>>,
  selectedSort: S,
  onSortSelect: (S) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var sortExpanded by remember { mutableStateOf(false) }
  val activeSortLabel = sorts.firstOrNull { it.value == selectedSort }?.label

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ExpressiveChoiceRow(
      options = options,
      selected = selected,
      labelOf = labelOf,
      onSelect = onSelect,
      modifier = Modifier.weight(1f),
    )

    Box {
      FilledTonalIconButton(
        onClick = { sortExpanded = true },
        modifier = Modifier.padding(start = 8.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = glassButtonContainerColor(MaterialTheme.colorScheme.secondaryContainer),
          contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSecondaryContainer),
        ),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Sort,
          contentDescription = "Sort${activeSortLabel?.let { ": $it" } ?: ""}",
        )
      }
      DropdownMenu(
        expanded = sortExpanded,
        onDismissRequest = { sortExpanded = false },
        containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
      ) {
        sorts.forEach { sort ->
          val isSelected = sort.value == selectedSort
          DropdownMenuItem(
            text = { Text(sort.label) },
            leadingIcon = if (isSelected) {
              { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
              null
            },
            onClick = {
              sortExpanded = false
              if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onSortSelect(sort.value)
            },
          )
        }
      }
    }
  }
}

/**
 * Watch-state filters for the Shows grid.
 */
enum class SeriesWatchFilter(val label: String) {
  ALL("All"),
  IN_PROGRESS("In progress"),
  UNWATCHED("Unwatched"),
  FINISHED("Finished"),
}

/**
 * Watch-state filters for the Movies grid.
 */
enum class MovieWatchFilter(val label: String) {
  ALL("All"),
  UNWATCHED("Unwatched"),
  STARTED("Started"),
  WATCHED("Watched"),
}

/**
 * Result-count line shown above Shows/Movies grids when filters narrow the list.
 */
@Composable
fun LibraryResultCount(
  shown: Int,
  total: Int,
  modifier: Modifier = Modifier,
) {
  Text(
    text = if (shown == total) "$total titles" else "$shown of $total titles",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.padding(horizontal = 4.dp),
  )
}
