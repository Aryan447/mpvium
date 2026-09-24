package app.aryan447.mpvium.ui.streaming.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
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
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.browser.states.GridLoadingSkeleton
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
import app.aryan447.mpvium.ui.streaming.components.MoviePosterCard
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.ui.utils.LocalDetailPaneBack
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import app.aryan447.mpvium.utils.media.MediaUtils
import app.aryan447.mpvium.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * Quick-win sort + watch-state filter for the Movies grid.
 * Session-persisted via rememberSaveable; a DataStore-backed
 * version can follow the same shape in BrowserPreferences.
 */
enum class MovieGridSort(val label: String) {
  Title("Title"),
  Year("Year"),
  Rating("Rating"),
}

enum class MovieWatchFilter(val label: String) {
  All("All"),
  Unwatched("Unwatched"),
  InProgress("In progress"),
  Watched("Watched"),
}

@Serializable
object MoviesGridScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val introSkipRepository = koinInject<IntroSkipRepository>()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val columns = when {
      isTablet -> if (isLandscape) 5 else 4
      isLandscape -> 4
      else -> 3
    }

    var movieList by remember { mutableStateOf<List<LocalMovie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    // Ordinals (not the enums themselves) so rotation restore stays Bundle-safe.
    var sortOrdinal by rememberSaveable { mutableIntStateOf(MovieGridSort.Title.ordinal) }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var watchFilterOrdinal by rememberSaveable { mutableIntStateOf(MovieWatchFilter.All.ordinal) }
    val sortType = MovieGridSort.entries[sortOrdinal.coerceIn(MovieGridSort.entries.indices)]
    val watchFilter = MovieWatchFilter.entries[watchFilterOrdinal.coerceIn(MovieWatchFilter.entries.indices)]
    var showSortMenu by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var moviePendingDeletion by remember { mutableStateOf<LocalMovie?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isRefreshing = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // Master-detail on wide screens when this grid is the tab root.
    // Pushed instances (e.g. from home See-all) stay single-pane.
    var selectedMovieId by rememberSaveable { mutableStateOf<Long?>(null) }
    val twoPane = configuration.screenWidthDp >= 840 && backstack.size == 1
    BackHandler(enabled = twoPane && selectedMovieId != null) {
      selectedMovieId = null
    }
    val atTop by remember {
      derivedStateOf {
        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
      }
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        // Drop cached TMDB entries for deleted movies.
        // Remaining movies reuse their cache with no re-scrape.
        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentMovieTitles = detected.movies.map { it.title }.toSet(),
          )
        }
        movieList = detected.movies
        isLoading = false

        // Background enrichment
        val enriched = detected.movies.map { metadataRepository.enrichMovie(it) }
        movieList = enriched
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

    val filteredMovies = remember(movieList, searchQuery, sortType, sortAscending, watchFilter) {
      val searched = if (searchQuery.isBlank()) {
        movieList
      } else {
        movieList.filter { it.title.lowercase().contains(searchQuery.lowercase()) }
      }
      val watchFiltered = when (watchFilter) {
        MovieWatchFilter.All -> searched
        MovieWatchFilter.Unwatched -> searched.filter { it.progressPercentage <= 0f && !it.isWatched }
        MovieWatchFilter.InProgress -> searched.filter { it.progressPercentage > 0f && !it.isWatched }
        MovieWatchFilter.Watched -> searched.filter { it.isWatched }
      }
      val sorted = when (sortType) {
        MovieGridSort.Title -> watchFiltered.sortedBy { it.title.lowercase() }
        MovieGridSort.Year -> watchFiltered.sortedWith(
          compareBy({ it.year.isNullOrBlank() }, { it.year }),
        )
        // Highest-rated first; the direction toggle below flips it.
        MovieGridSort.Rating -> watchFiltered.sortedWith(
          compareBy({ it.rating == null || it.rating <= 0f }, { -(it.rating ?: 0f) }),
        )
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
                placeholder = { Text("Search movies...") },
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
                text = "Movies",
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
                  Icon(Icons.Filled.Sort, contentDescription = "Sort movies")
                }
                DropdownMenu(
                  expanded = showSortMenu,
                  onDismissRequest = { showSortMenu = false },
                ) {
                  MovieGridSort.entries.forEach { option ->
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
            MoviesGridContent(
              innerPadding = innerPadding,
              isLoading = isLoading,
              filteredMovies = filteredMovies,
              searchQuery = searchQuery,
              onClearSearch = { searchQuery = "" },
              onRescan = { refreshKey++ },
              watchFilter = watchFilter,
              onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
              onClearFilters = {
                searchQuery = ""
                watchFilterOrdinal = MovieWatchFilter.All.ordinal
              },
              columns = columns,
              gridState = gridState,
              navigationBarHeight = navigationBarHeight,
              isRefreshing = isRefreshing,
              onRefresh = { refreshKey++ },
              refreshEnabled = atTop && !isLoading,
              onMovieClick = { selectedMovieId = it.video.id },
              onMovieLongClick = { moviePendingDeletion = it },
            )
          }
          VerticalDivider(modifier = Modifier.fillMaxHeight())
          Box(
            modifier = Modifier
              .weight(0.58f)
              .fillMaxHeight(),
          ) {
            MovieDetailPane(
              selectedMovieId = selectedMovieId,
              movieList = movieList,
              onClearSelection = { selectedMovieId = null },
            )
          }
        }
      } else {
        MoviesGridContent(
          innerPadding = innerPadding,
          isLoading = isLoading,
          filteredMovies = filteredMovies,
          searchQuery = searchQuery,
          onClearSearch = { searchQuery = "" },
          onRescan = { refreshKey++ },
          watchFilter = watchFilter,
          onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
          onClearFilters = {
            searchQuery = ""
            watchFilterOrdinal = MovieWatchFilter.All.ordinal
          },
          columns = columns,
          gridState = gridState,
          navigationBarHeight = navigationBarHeight,
          isRefreshing = isRefreshing,
          onRefresh = { refreshKey++ },
          refreshEnabled = atTop && !isLoading,
          onMovieClick = { backstack.add(MovieDetailScreen(it.video.id, it.title)) },
          onMovieLongClick = { moviePendingDeletion = it },
        )
      }
    }

    moviePendingDeletion?.let { movie ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { moviePendingDeletion = null },
        onConfirm = {
          coroutineScope.launch {
            PermissionUtils.StorageOps.deleteVideos(context, listOf(movie.video))
            // Movie file is gone: drop its cached TMDB entry immediately.
            runCatching { metadataRepository.clearMovieMetadata(movie.title) }
            // Movie file is gone: drop its cached intro/recap windows too.
            runCatching { introSkipRepository.evictAll(listOf(movie.video.displayName, movie.title)) }
            movieList = movieList.filterNot { it.video.id == movie.video.id }
          }
        },
        itemType = "movie",
        itemCount = 1,
        itemNames = listOf(movie.title),
      )
    }
  }
}

@Composable
private fun MoviesGridContent(
  innerPadding: PaddingValues,
  isLoading: Boolean,
  filteredMovies: List<LocalMovie>,
  searchQuery: String,
  onClearSearch: () -> Unit,
  onRescan: () -> Unit,
  watchFilter: MovieWatchFilter,
  onWatchFilterChange: (MovieWatchFilter) -> Unit,
  onClearFilters: () -> Unit,
  columns: Int,
  gridState: LazyGridState,
  navigationBarHeight: Dp,
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  refreshEnabled: Boolean,
  onMovieClick: (LocalMovie) -> Unit,
  onMovieLongClick: (LocalMovie) -> Unit,
) {
  if (isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      GridLoadingSkeleton(columns = columns, navigationBarHeight = 0.dp)
    }
  } else if (filteredMovies.isEmpty()) {
    val filtering = searchQuery.isNotBlank() || watchFilter != MovieWatchFilter.All
    EmptyState(
      icon = Icons.Filled.Movie,
      title = if (searchQuery.isNotBlank()) "No movies found" else if (watchFilter != MovieWatchFilter.All) "Nothing here yet" else "No movies detected",
      message = if (searchQuery.isNotBlank()) "No movies match '$searchQuery'" else if (watchFilter != MovieWatchFilter.All) "No movies match the ${watchFilter.label.lowercase()} filter" else "Add movie files to your device storage to see them here",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      actionLabel = if (searchQuery.isNotBlank()) "Clear search" else if (watchFilter != MovieWatchFilter.All) "Show all" else "Rescan library",
      onAction = if (searchQuery.isNotBlank()) {
        onClearSearch
      } else if (watchFilter != MovieWatchFilter.All) {
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
            items(MovieWatchFilter.entries) { filter ->
              FilterChip(
                selected = watchFilter == filter,
                onClick = { onWatchFilterChange(filter) },
                label = { Text(filter.label) },
              )
            }
          }
        }
        items(filteredMovies, key = { "grid_movie_${it.video.id}" }) { movie ->
          MoviePosterCard(
            movie = movie,
            onClick = { onMovieClick(movie) },
            onLongClick = { onMovieLongClick(movie) },
            cardWidth = 180.dp,
            highlightQuery = searchQuery,
          )
        }
      }
    }
  }
}

@Composable
private fun MovieDetailPane(
  selectedMovieId: Long?,
  movieList: List<LocalMovie>,
  onClearSelection: () -> Unit,
) {
  val selected = movieList.find { it.video.id == selectedMovieId }
  Box(modifier = Modifier.fillMaxSize()) {
    if (selected == null) {
      EmptyState(
        icon = Icons.Filled.Movie,
        title = "Select a movie",
        message = "Pick a movie to see details, actions and matches here",
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      key(selected.video.id) {
        CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
          MovieDetailScreen(selected.video.id, selected.title).Content()
        }
      }
    }
  }
}
