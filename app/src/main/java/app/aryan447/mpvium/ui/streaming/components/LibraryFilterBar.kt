package app.aryan447.mpvium.ui.streaming.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.ui.streaming.titles.TitleWatchFilter
import app.aryan447.mpvium.ui.streaming.titles.TitlesTypeFilter
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
 * Shared filter-chip row + trailing icon actions used by the Shows and Movies
 * grids. Read-only: callers apply [options]/[selected] and [sorts]/[selectedSort]
 * to their already-loaded lists. [actions] render between the chips and the sort
 * button, e.g. [MovieDurationFilterButton].
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
  actions: @Composable () -> Unit = {},
) {
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

    actions()

    SortMenuButton(
      sorts = sorts,
      selectedSort = selectedSort,
      onSortSelect = onSortSelect,
    )
  }
}

@Composable
private fun <S> SortMenuButton(
  sorts: List<SortOption<S>>,
  selectedSort: S,
  onSortSelect: (S) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  var sortExpanded by remember { mutableStateOf(false) }
  val activeSortLabel = sorts.firstOrNull { it.value == selectedSort }?.label

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
 * Duration filters for the Movies grid, shown as a tonal icon button next to
 * the sort control instead of a second chip row.
 */
enum class MovieDurationFilter(
  @StringRes val labelRes: Int,
  val minDurationMs: Long = 0L,
) {
  ALL(R.string.movie_duration_filter_all, 0L),
  AT_LEAST_30M(R.string.movie_duration_filter_30m, 30 * 60 * 1000L),
  AT_LEAST_1H(R.string.movie_duration_filter_1h, 60 * 60 * 1000L),
  AT_LEAST_1H30M(R.string.movie_duration_filter_1h30m, 90 * 60 * 1000L);

  fun matches(durationMs: Long): Boolean = durationMs >= minDurationMs
}

/**
 * Compact duration filter: clock icon button that opens the length thresholds
 * in a dropdown, mirroring [SortMenuButton]. Filled container while a threshold
 * is active so the current filter is visible without opening the menu.
 */
@Composable
fun MovieDurationFilterButton(
  selected: MovieDurationFilter,
  onSelect: (MovieDurationFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var expanded by remember { mutableStateOf(false) }
  val isFiltering = selected != MovieDurationFilter.ALL
  val selectedLabel = stringResource(selected.labelRes)

  Box {
    FilledTonalIconButton(
      onClick = { expanded = true },
      modifier = modifier.padding(start = 8.dp),
      colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = glassButtonContainerColor(
          if (isFiltering) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.secondaryContainer
          },
        ),
        contentColor = glassButtonContentColor(
          if (isFiltering) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSecondaryContainer
          },
        ),
      ),
    ) {
      Icon(
        imageVector = Icons.Filled.Schedule,
        // Tint the icon with primary while filtering: the filled container above
        // collapses to frost under Glass, where only the tint still reads.
        tint = if (isFiltering) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        contentDescription = stringResource(
          R.string.movie_duration_filter_content_description,
          selectedLabel,
        ),
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
    ) {
      MovieDurationFilter.entries.forEach { filter ->
        val isSelected = filter == selected
        DropdownMenuItem(
          text = { Text(stringResource(filter.labelRes)) },
          leadingIcon = if (isSelected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
          } else {
            null
          },
          onClick = {
            expanded = false
            if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(filter)
          },
        )
      }
    }
  }
}

/**
 * Compact watch-state filter: eye icon button that opens the watch states
 * in a dropdown, mirroring [SortMenuButton] and [MovieDurationFilterButton].
 * Filled container while a non-ALL state is selected.
 */
@Composable
fun TitleWatchFilterButton(
  selected: TitleWatchFilter,
  typeFilter: TitlesTypeFilter,
  onSelect: (TitleWatchFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var expanded by remember { mutableStateOf(false) }
  val isFiltering = selected != TitleWatchFilter.ALL
  val selectedLabel = if (typeFilter == TitlesTypeFilter.SHOWS && selected == TitleWatchFilter.WATCHED) {
    "Finished"
  } else {
    selected.label
  }

  Box {
    FilledTonalIconButton(
      onClick = { expanded = true },
      modifier = modifier.padding(start = 8.dp),
      colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = glassButtonContainerColor(
          if (isFiltering) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.secondaryContainer
          },
        ),
        contentColor = glassButtonContentColor(
          if (isFiltering) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSecondaryContainer
          },
        ),
      ),
    ) {
      Icon(
        imageVector = Icons.Filled.Visibility,
        tint = if (isFiltering) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        contentDescription = stringResource(
          R.string.title_watch_filter_content_description,
          selectedLabel,
        ),
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
    ) {
      TitleWatchFilter.entries.forEach { filter ->
        val isSelected = filter == selected
        val label = if (typeFilter == TitlesTypeFilter.SHOWS && filter == TitleWatchFilter.WATCHED) {
          "Finished"
        } else {
          filter.label
        }
        DropdownMenuItem(
          text = { Text(label) },
          leadingIcon = if (isSelected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
          } else {
            null
          },
          onClick = {
            expanded = false
            if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(filter)
          },
        )
      }
    }
  }
}

/**
 * Result-count line shown above Shows/Movies grids when filters narrow the list.
 */
@Composable
fun LibraryResultCount(
  shown: Int,
  total: Int,
  filterType: TitlesTypeFilter = TitlesTypeFilter.ALL,
  modifier: Modifier = Modifier,
) {
  val noun = when (filterType) {
    TitlesTypeFilter.ALL -> if (total == 1) "title" else "titles"
    TitlesTypeFilter.SHOWS -> if (total == 1) "show" else "shows"
    TitlesTypeFilter.MOVIES -> if (total == 1) "movie" else "movies"
  }
  Text(
    text = if (shown == total) "$total $noun" else "$shown of $total $noun",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.padding(horizontal = 4.dp),
  )
}
