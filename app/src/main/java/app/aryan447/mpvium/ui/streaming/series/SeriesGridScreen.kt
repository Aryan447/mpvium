package app.aryan447.mpvium.ui.streaming.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.glassSearchBarColors
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.browser.states.GridLoadingSkeleton
import app.aryan447.mpvium.ui.streaming.components.SeriesPosterCard
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.ui.utils.LocalDetailPaneBack
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * Quick-win sort + watch-state filter for the Shows grid.
 * Session-persisted via rememberSaveable; a DataStore-backed
 * version can follow the same shape in BrowserPreferences.
 */
enum class SeriesGridSort(val label: String) {
  Title("Title"),
  Year("Year"),
  Rating("Rating"),
  Episodes("Episodes"),
}

enum class SeriesWatchFilter(val label: String) {
  All("All"),
  Unwatched("Unwatched"),
  InProgress("In progress"),
  Completed("Completed"),
}

@Serializable
object SeriesGridScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val columns = when {
      isTablet -> if (isLandscape) 5 else 4
      isLandscape -> 4
      else -> 3
    }

    var seriesList by remember { mutableStateOf<List<LocalSeries>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    // Ordinals (not the enums themselves) so rotation restore stays Bundle-safe.
    var sortOrdinal by rememberSaveable { mutableIntStateOf(SeriesGridSort.Title.ordinal) }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var watchFilterOrdinal by rememberSaveable { mutableIntStateOf(SeriesWatchFilter.All.ordinal) }
    val sortType = SeriesGridSort.entries[sortOrdinal.coerceIn(SeriesGridSort.entries.indices)]
    val watchFilter = SeriesWatchFilter.entries[watchFilterOrdinal.coerceIn(SeriesWatchFilter.entries.indices)]
    var showSortMenu by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isRefreshing = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // Master-detail on wide screens when this grid is the tab root.
    // Pushed instances (e.g. from home See-all) stay single-pane.
    var selectedSeriesId by rememberSaveable { mutableStateOf<String?>(null) }
    val twoPane = configuration.screenWidthDp >= 840 && backstack.size == 1
    BackHandler(enabled = twoPane && selectedSeriesId != null) {
      selectedSeriesId = null
    }
    val atTop by remember {
      derivedStateOf {
        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
      }
    }

    LaunchedEffect(refreshKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        // Drop cached TMDB entries for shows with zero episodes left.
        // Remaining shows reuse their cache with no re-scrape.
        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentSeriesIds = detected.series.map { it.id }.toSet(),
          )
        }
        seriesList = detected.series
        isLoading = false

        // Background enrichment
        val enriched = detected.series.map { metadataRepository.enrichSeries(it) }
        seriesList = enriched
      }
    }

    // Refresh progress after exiting the player. The player persists its final
    // position in onStop/onDestroy, which can land after ON_RESUME, so also
    // refresh on the post-save notification to correct a stale read. Cached
    // enrichment only: no extra network calls.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      var resumedOnce = false
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          if (resumedOnce) {
            refreshKey++
          } else {
            resumedOnce = true
          }
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
      MediaLibraryEvents.changes.collect { refreshKey++ }
    }

    LaunchedEffect(isSearching) {
      if (isSearching) {
        kotlinx.coroutines.delay(100)
        runCatching { searchFocusRequester.requestFocus() }
      }
    }

    val filteredSeries = remember(seriesList, searchQuery, sortType, sortAscending, watchFilter) {
      val searched = if (searchQuery.isBlank()) {
        seriesList
      } else {
        seriesList.filter { it.title.lowercase().contains(searchQuery.lowercase()) }
      }
      val watchFiltered = when (watchFilter) {
        SeriesWatchFilter.All -> searched
        SeriesWatchFilter.Unwatched -> searched.filter { it.watchedEpisodesCount <= 0 }
        SeriesWatchFilter.InProgress -> searched.filter { it.watchedEpisodesCount > 0 && !it.isCompleted }
        SeriesWatchFilter.Completed -> searched.filter { it.isCompleted }
      }
      val sorted = when (sortType) {
        SeriesGridSort.Title -> watchFiltered.sortedBy { it.title.lowercase() }
        SeriesGridSort.Year -> watchFiltered.sortedWith(
          compareBy({ it.year.isNullOrBlank() }, { it.year }),
        )
        // Highest-rated first; the direction toggle below flips it.
        SeriesGridSort.Rating -> watchFiltered.sortedWith(
          compareBy({ it.rating == null || it.rating <= 0f }, { -(it.rating ?: 0f) }),
        )
        SeriesGridSort.Episodes -> watchFiltered.sortedByDescending { it.totalEpisodes }
      }
      if (sortAscending) sorted else sorted.reversed()
    }

    Scaffold(
      topBar = {
        val isGlass = LocalGlass.current
        val glassHaze = rememberGlassHazeState()
        val glassStyle = glassHazeStyle(isDark = isSystemInDarkTheme(), kind = GlassKind.Bar)
        if (isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search TV shows...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                  IconButton(onClick = {
                    isSearching = false
                    searchQuery = ""
                  }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                  }
                },
                modifier = Modifier.focusRequester(searchFocusRequester),
              )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = glassSearchBarColors(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
          ) {}
        } else {
          TopAppBar(
            title = {
              Text(
                text = "Shows",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
            },
            navigationIcon = {
              if (backstack.size > 1) {
                IconButton(
                  onClick = {
                    if (backstack.size > 1) {
                      backstack.removeLastOrNull()
                    }
                  }
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
              }
            },
            actions = {
              Box {
                IconButton(onClick = { showSortMenu = true }) {
                  Icon(Icons.Filled.Sort, contentDescription = "Sort shows")
                }
                DropdownMenu(
                  expanded = showSortMenu,
                  onDismissRequest = { showSortMenu = false },
                ) {
                  SeriesGridSort.entries.forEach { option ->
                    DropdownMenuItem(
                      text = { Text(option.label) },
                      trailingIcon = if (sortType == option) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                      } else {
                        null
                      },
                      onClick = {
                        sortOrdinal = option.ordinal
                        showSortMenu = false
                      },
                    )
                  }
                  DropdownMenuItem(
                    text = { Text(if (sortAscending) "Ascending" else "Descending") },
                    onClick = { sortAscending = !sortAscending },
                  )
                }
              }
              IconButton(onClick = { isSearching = true }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
              }
            },
            colors = if (isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
              else TopAppBarDefaults.topAppBarColors(),
            modifier = Modifier.glassChrome(
              state = glassHaze,
              style = glassStyle,
              shape = RoundedCornerShape(0.dp),
              enabled = isGlass,
            ),
          )
        }
      },
    ) { innerPadding ->
      if (twoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .weight(0.42f)
              .fillMaxHeight(),
          ) {
            SeriesGridContent(
              innerPadding = innerPadding,
              isLoading = isLoading,
              filteredSeries = filteredSeries,
              searchQuery = searchQuery,
              onClearSearch = { searchQuery = "" },
              onRescan = { refreshKey++ },
              watchFilter = watchFilter,
              onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
              onClearFilters = {
                searchQuery = ""
                watchFilterOrdinal = SeriesWatchFilter.All.ordinal
              },
              columns = columns,
              gridState = gridState,
              navigationBarHeight = navigationBarHeight,
              isRefreshing = isRefreshing,
              onRefresh = { refreshKey++ },
              refreshEnabled = atTop && !isLoading,
              onSeriesClick = { selectedSeriesId = it.id },
            )
          }
          VerticalDivider(modifier = Modifier.fillMaxHeight())
          Box(
            modifier = Modifier
              .weight(0.58f)
              .fillMaxHeight(),
          ) {
            SeriesDetailPane(
              selectedSeriesId = selectedSeriesId,
              seriesList = seriesList,
              onClearSelection = { selectedSeriesId = null },
            )
          }
        }
      } else {
        SeriesGridContent(
          innerPadding = innerPadding,
          isLoading = isLoading,
          filteredSeries = filteredSeries,
          searchQuery = searchQuery,
          onClearSearch = { searchQuery = "" },
          onRescan = { refreshKey++ },
          watchFilter = watchFilter,
          onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
          onClearFilters = {
            searchQuery = ""
            watchFilterOrdinal = SeriesWatchFilter.All.ordinal
          },
          columns = columns,
          gridState = gridState,
          navigationBarHeight = navigationBarHeight,
          isRefreshing = isRefreshing,
          onRefresh = { refreshKey++ },
          refreshEnabled = atTop && !isLoading,
          onSeriesClick = { backstack.add(SeriesDetailScreen(it.id)) },
        )
      }
    }
  }
}

@Composable
private fun SeriesGridContent(
  innerPadding: PaddingValues,
  isLoading: Boolean,
  filteredSeries: List<LocalSeries>,
  searchQuery: String,
  onClearSearch: () -> Unit,
  onRescan: () -> Unit,
  watchFilter: SeriesWatchFilter,
  onWatchFilterChange: (SeriesWatchFilter) -> Unit,
  onClearFilters: () -> Unit,
  columns: Int,
  gridState: LazyGridState,
  navigationBarHeight: Dp,
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  refreshEnabled: Boolean,
  onSeriesClick: (LocalSeries) -> Unit,
) {
  if (isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      GridLoadingSkeleton(columns = columns, navigationBarHeight = 0.dp)
    }
  } else if (filteredSeries.isEmpty()) {
    EmptyState(
      icon = Icons.Filled.Tv,
      title = if (searchQuery.isNotBlank()) "No TV shows found" else if (watchFilter != SeriesWatchFilter.All) "Nothing here yet" else "No TV shows detected",
      message = if (searchQuery.isNotBlank()) "No TV shows match '$searchQuery'" else if (watchFilter != SeriesWatchFilter.All) "No shows match the ${watchFilter.label.lowercase()} filter" else "Add TV shows with S01E01 or season folders to see them here",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      actionLabel = if (searchQuery.isNotBlank()) "Clear search" else if (watchFilter != SeriesWatchFilter.All) "Show all" else "Rescan library",
      onAction = if (searchQuery.isNotBlank()) {
        onClearSearch
      } else if (watchFilter != SeriesWatchFilter.All) {
        onClearFilters
      } else {
        onRescan
      },
    )
  } else {
    PullRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      enabled = refreshEnabled,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 12.dp,
          end = 12.dp,
          top = 8.dp,
          bottom = navigationBarHeight + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp),
          ) {
            items(SeriesWatchFilter.entries) { filter ->
              FilterChip(
                selected = watchFilter == filter,
                onClick = { onWatchFilterChange(filter) },
                label = { Text(filter.label) },
              )
            }
          }
        }
        items(filteredSeries, key = { "grid_series_${it.id}" }) { series ->
          SeriesPosterCard(
            series = series,
            onClick = { onSeriesClick(series) },
            cardWidth = 180.dp,
            highlightQuery = searchQuery,
          )
        }
      }
    }
  }
}

@Composable
private fun SeriesDetailPane(
  selectedSeriesId: String?,
  seriesList: List<LocalSeries>,
  onClearSelection: () -> Unit,
) {
  val selected = seriesList.find { it.id == selectedSeriesId }
  Box(modifier = Modifier.fillMaxSize()) {
    if (selected == null) {
      EmptyState(
        icon = Icons.Filled.Tv,
        title = "Select a TV show",
        message = "Pick a show to see seasons, episodes and actions here",
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      key(selected.id) {
        CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
          SeriesDetailScreen(selected.id).Content()
        }
      }
    }
  }
}
