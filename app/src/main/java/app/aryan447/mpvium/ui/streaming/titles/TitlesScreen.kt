package app.aryan447.mpvium.ui.streaming.titles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.MovieLibraryClassifier
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.preferences.MediaLayoutMode
import app.aryan447.mpvium.preferences.StreamingPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.components.MediaLayoutToggle
import app.aryan447.mpvium.ui.browser.dialogs.DeleteConfirmationDialog
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.preferences.FoldersPreferencesScreen
import app.aryan447.mpvium.ui.streaming.components.LibraryFilterBar
import app.aryan447.mpvium.ui.streaming.components.LibraryResultCount
import app.aryan447.mpvium.ui.streaming.components.MovieDurationFilter
import app.aryan447.mpvium.ui.streaming.components.MovieDurationFilterButton
import app.aryan447.mpvium.ui.streaming.components.MovieListRow
import app.aryan447.mpvium.ui.streaming.components.MoviePosterCard
import app.aryan447.mpvium.ui.streaming.components.SeriesListRow
import app.aryan447.mpvium.ui.streaming.components.SeriesPosterCard
import app.aryan447.mpvium.ui.streaming.components.SortOption
import app.aryan447.mpvium.ui.streaming.components.TitleWatchFilterButton
import app.aryan447.mpvium.ui.streaming.movies.MovieDetailScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesDetailScreen
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.glassSearchBarColors
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.ui.utils.LocalDetailPaneBack
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import app.aryan447.mpvium.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class TitlesScreen(
  val initialTypeFilter: TitlesTypeFilter? = null,
) : Screen {

  companion object {
    private val _pendingFilter = MutableStateFlow<TitlesTypeFilter?>(null)
    val pendingFilterFlow = _pendingFilter.asStateFlow()

    /**
     * Request preselection of a specific type filter (e.g. Movies from launcher shortcut 2).
     */
    fun preselectFilter(filter: TitlesTypeFilter) {
      _pendingFilter.value = filter
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val streamingPreferences = koinInject<StreamingPreferences>()
    val libraryIndexRepository = koinInject<LibraryIndexRepository>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val introSkipRepository = koinInject<IntroSkipRepository>()
    val foldersPreferences = koinInject<FoldersPreferences>()
    val browserPreferences = koinInject<BrowserPreferences>()

    val layoutTitles by browserPreferences.layoutTitles.collectAsState()
    val whitelistOnlyEnabled by foldersPreferences.whitelistOnlyEnabled.collectAsState()
    val whitelistedFolders by foldersPreferences.whitelistedFolders.collectAsState()
    val libraryFiltered = whitelistOnlyEnabled && whitelistedFolders.isNotEmpty()
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
    var movieList by remember { mutableStateOf<List<LocalMovie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var moviePendingDeletion by remember { mutableStateOf<LocalMovie?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isRefreshing = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    var lastLayoutMode by remember { mutableStateOf(layoutTitles) }
    LaunchedEffect(layoutTitles) {
      if (lastLayoutMode != layoutTitles) {
        if (layoutTitles == MediaLayoutMode.GRID) {
          gridState.scrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        } else {
          listState.scrollToItem(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }
        lastLayoutMode = layoutTitles
      }
    }

    // Persisted type filter (All | Shows | Movies)
    val persistedTypeFilter by streamingPreferences.titlesTypeFilter.collectAsState()
    var activeTypeFilter by rememberSaveable {
      mutableStateOf(initialTypeFilter ?: persistedTypeFilter)
    }

    // External preselect requests (e.g. launcher shortcut requestTab(2))
    LaunchedEffect(Unit) {
      pendingFilterFlow.collect { pending ->
        if (pending != null) {
          activeTypeFilter = pending
          streamingPreferences.titlesTypeFilter.set(pending)
          _pendingFilter.value = null
        }
      }
    }

    LaunchedEffect(activeTypeFilter) {
      if (streamingPreferences.titlesTypeFilter.get() != activeTypeFilter) {
        streamingPreferences.titlesTypeFilter.set(activeTypeFilter)
      }
    }

    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItemType by rememberSaveable { mutableStateOf<String?>(null) }
    val twoPane = configuration.screenWidthDp >= 840 && backstack.size == 1
    BackHandler(enabled = twoPane && selectedItemId != null) {
      selectedItemId = null
      selectedItemType = null
    }

    val isGrid = layoutTitles == MediaLayoutMode.GRID
    val atTop by remember(isGrid) {
      derivedStateOf {
        if (isGrid) {
          gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        } else {
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
      }
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
      withContext(Dispatchers.IO) {
        val detected = libraryIndexRepository.getLibrary(force = isRefreshing.value)
        val libraryMovies = detected.movies.filter { MovieLibraryClassifier.isMovieLibraryItem(it) }

        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentSeriesIds = detected.series.map { it.id }.toSet(),
            presentMovieKeys = detected.movies.map {
              StreamingMetadataRepository.movieKeyFor(it.title, it.year)
            }.toSet(),
          )
        }

        val cachedSeries = detected.series.map { metadataRepository.applyCachedSeries(it) }
        val cachedMovies = libraryMovies.map { metadataRepository.applyCachedMovie(it) }
        seriesList = cachedSeries
        movieList = cachedMovies
        isLoading = false

        // Background enrichment
        val enrichedSeries = detected.series.map { metadataRepository.enrichSeries(it) }
        val enrichedMovies = libraryMovies.map { metadataRepository.enrichMovie(it) }
        seriesList = enrichedSeries
        movieList = enrichedMovies
      }
    }

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

    var watchFilterOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val watchFilter = TitleWatchFilter.entries[watchFilterOrdinal.coerceIn(TitleWatchFilter.entries.indices)]
    val durationFilter by streamingPreferences.movieDurationFilter.collectAsState()
    val titlesSort by streamingPreferences.titlesSort.collectAsState()
    val titlesSortOptions = remember {
      TitlesSort.entries.map { SortOption(it, it.label) }
    }

    val allTitleItems = remember(seriesList, movieList) {
      val items = ArrayList<TitleItem>(seriesList.size + movieList.size)
      seriesList.forEach { items.add(TitleItem.Show(it)) }
      movieList.forEach { items.add(TitleItem.Movie(it)) }
      items
    }

    val filteredTitles = remember(
      allTitleItems,
      searchQuery,
      activeTypeFilter,
      watchFilter,
      durationFilter,
      titlesSort,
    ) {
      var result: List<TitleItem> = when (activeTypeFilter) {
        TitlesTypeFilter.ALL -> allTitleItems
        TitlesTypeFilter.SHOWS -> allTitleItems.filterIsInstance<TitleItem.Show>()
        TitlesTypeFilter.MOVIES -> allTitleItems.filterIsInstance<TitleItem.Movie>()
      }

      if (searchQuery.isNotBlank()) {
        val query = searchQuery.lowercase()
        result = result.filter { it.title.lowercase().contains(query) }
      }

      if (watchFilter != TitleWatchFilter.ALL) {
        result = result.filter { watchFilter.matches(it) }
      }

      if (durationFilter != MovieDurationFilter.ALL) {
        result = result.filter { durationFilter.matches(it) }
      }

      result.sortedByTitlesSort(titlesSort)
    }

    val totalCount = remember(activeTypeFilter, seriesList, movieList) {
      when (activeTypeFilter) {
        TitlesTypeFilter.ALL -> seriesList.size + movieList.size
        TitlesTypeFilter.SHOWS -> seriesList.size
        TitlesTypeFilter.MOVIES -> movieList.size
      }
    }

    Scaffold(
      containerColor = Color.Transparent,
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
                placeholder = { Text(stringResource(R.string.titles_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                  IconButton(onClick = {
                    isSearching = false
                    searchQuery = ""
                  }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                  }
                },
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
                text = stringResource(R.string.tab_titles),
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
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
              }
            },
            actions = {
              IconButton(onClick = { isSearching = true }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
              }
              MediaLayoutToggle(
                mode = layoutTitles,
                onToggle = {
                  val next = if (layoutTitles == MediaLayoutMode.GRID) MediaLayoutMode.LIST else MediaLayoutMode.GRID
                  browserPreferences.layoutTitles.set(next)
                },
                modifier = Modifier.padding(horizontal = 4.dp),
              )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
            TitlesGridContent(
              innerPadding = innerPadding,
              isLoading = isLoading,
              filteredTitles = filteredTitles,
              searchQuery = searchQuery,
              onClearSearch = { searchQuery = "" },
              columns = columns,
              gridState = gridState,
              listState = listState,
              layoutMode = layoutTitles,
              navigationBarHeight = navigationBarHeight,
              isRefreshing = isRefreshing,
              onRefresh = { refreshKey++ },
              refreshEnabled = atTop && !isLoading,
              typeFilter = activeTypeFilter,
              onTypeFilterChange = { activeTypeFilter = it },
              watchFilter = watchFilter,
              onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
              durationFilter = durationFilter,
              onDurationFilterChange = { streamingPreferences.movieDurationFilter.set(it) },
              titlesSort = titlesSort,
              titlesSortOptions = titlesSortOptions,
              onTitlesSortChange = { streamingPreferences.titlesSort.set(it) },
              totalTitlesCount = totalCount,
              libraryFiltered = libraryFiltered,
              onManageLibrarySources = { backstack.add(FoldersPreferencesScreen) },
              onItemClick = { item ->
                when (item) {
                  is TitleItem.Show -> {
                    selectedItemId = item.series.id
                    selectedItemType = "series"
                  }
                  is TitleItem.Movie -> {
                    selectedItemId = item.movie.video.id.toString()
                    selectedItemType = "movie"
                  }
                }
              },
              onMovieLongClick = { moviePendingDeletion = it },
            )
          }
          VerticalDivider(modifier = Modifier.fillMaxHeight())
          Box(
            modifier = Modifier
              .weight(0.58f)
              .fillMaxHeight(),
          ) {
            TitlesDetailPane(
              selectedItemId = selectedItemId,
              selectedItemType = selectedItemType,
              seriesList = seriesList,
              movieList = movieList,
              onClearSelection = {
                selectedItemId = null
                selectedItemType = null
              },
            )
          }
        }
      } else {
        TitlesGridContent(
          innerPadding = innerPadding,
          isLoading = isLoading,
          filteredTitles = filteredTitles,
          searchQuery = searchQuery,
          onClearSearch = { searchQuery = "" },
          columns = columns,
          gridState = gridState,
          listState = listState,
          layoutMode = layoutTitles,
          navigationBarHeight = navigationBarHeight,
          isRefreshing = isRefreshing,
          onRefresh = { refreshKey++ },
          refreshEnabled = atTop && !isLoading,
          typeFilter = activeTypeFilter,
          onTypeFilterChange = { activeTypeFilter = it },
          watchFilter = watchFilter,
          onWatchFilterChange = { watchFilterOrdinal = it.ordinal },
          durationFilter = durationFilter,
          onDurationFilterChange = { streamingPreferences.movieDurationFilter.set(it) },
          titlesSort = titlesSort,
          titlesSortOptions = titlesSortOptions,
          onTitlesSortChange = { streamingPreferences.titlesSort.set(it) },
          totalTitlesCount = totalCount,
          libraryFiltered = libraryFiltered,
          onManageLibrarySources = { backstack.add(FoldersPreferencesScreen) },
          onItemClick = { item ->
            when (item) {
              is TitleItem.Show -> backstack.add(SeriesDetailScreen(item.series.id))
              is TitleItem.Movie -> backstack.add(MovieDetailScreen(item.movie.video.id, item.movie.title))
            }
          },
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
            runCatching { metadataRepository.clearMovieMetadata(movie.title) }
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
private fun TitlesDetailPane(
  selectedItemId: String?,
  selectedItemType: String?,
  seriesList: List<LocalSeries>,
  movieList: List<LocalMovie>,
  onClearSelection: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    when {
      selectedItemType == "series" -> {
        val series = seriesList.find { it.id == selectedItemId }
        if (series == null) {
          TitlesDetailEmptyState()
        } else {
          key(series.id) {
            CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
              SeriesDetailScreen(series.id).Content()
            }
          }
        }
      }
      selectedItemType == "movie" -> {
        val movieId = selectedItemId?.toLongOrNull()
        val movie = movieList.find { it.video.id == movieId }
        if (movie == null) {
          TitlesDetailEmptyState()
        } else {
          key(movie.video.id) {
            CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
              MovieDetailScreen(movie.video.id, movie.title).Content()
            }
          }
        }
      }
      else -> {
        TitlesDetailEmptyState()
      }
    }
  }
}

@Composable
private fun TitlesDetailEmptyState() {
  EmptyState(
    icon = Icons.Filled.Movie,
    title = stringResource(R.string.titles_pane_select_title),
    message = stringResource(R.string.titles_pane_select_message),
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
private fun TitlesGridContent(
  innerPadding: PaddingValues,
  isLoading: Boolean,
  filteredTitles: List<TitleItem>,
  searchQuery: String,
  onClearSearch: () -> Unit,
  columns: Int,
  gridState: LazyGridState,
  listState: LazyListState,
  layoutMode: MediaLayoutMode,
  navigationBarHeight: Dp,
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  refreshEnabled: Boolean,
  typeFilter: TitlesTypeFilter,
  onTypeFilterChange: (TitlesTypeFilter) -> Unit,
  watchFilter: TitleWatchFilter,
  onWatchFilterChange: (TitleWatchFilter) -> Unit,
  durationFilter: MovieDurationFilter,
  onDurationFilterChange: (MovieDurationFilter) -> Unit,
  titlesSort: TitlesSort,
  titlesSortOptions: List<SortOption<TitlesSort>>,
  onTitlesSortChange: (TitlesSort) -> Unit,
  totalTitlesCount: Int,
  libraryFiltered: Boolean,
  onManageLibrarySources: () -> Unit,
  onItemClick: (TitleItem) -> Unit,
  onMovieLongClick: (LocalMovie) -> Unit,
) {
  if (isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  } else if (filteredTitles.isEmpty()) {
    val filteringByWatchState = watchFilter != TitleWatchFilter.ALL
    val filteringByDuration = durationFilter != MovieDurationFilter.ALL
    val hasFilter = filteringByWatchState || filteringByDuration

    val emptyIcon = when (typeFilter) {
      TitlesTypeFilter.SHOWS -> Icons.Filled.Tv
      TitlesTypeFilter.MOVIES, TitlesTypeFilter.ALL -> Icons.Filled.Movie
    }

    val emptyTitle = when {
      searchQuery.isNotBlank() -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> stringResource(R.string.titles_empty_search_no_shows_title)
        TitlesTypeFilter.MOVIES -> stringResource(R.string.titles_empty_search_no_movies_title)
        TitlesTypeFilter.ALL -> stringResource(R.string.titles_empty_search_no_titles_title)
      }
      hasFilter -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> {
          val watchLabel = if (watchFilter == TitleWatchFilter.WATCHED) "finished" else watchFilter.label.lowercase()
          "No $watchLabel TV shows"
        }
        TitlesTypeFilter.MOVIES -> stringResource(R.string.titles_empty_filter_no_movies)
        TitlesTypeFilter.ALL -> stringResource(R.string.titles_empty_filter_no_titles)
      }
      else -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> stringResource(R.string.titles_empty_no_shows_title)
        TitlesTypeFilter.MOVIES -> stringResource(R.string.titles_empty_no_movies_title)
        TitlesTypeFilter.ALL -> stringResource(R.string.titles_empty_no_titles_title)
      }
    }

    val emptyMessage = when {
      searchQuery.isNotBlank() -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> stringResource(R.string.titles_empty_search_no_shows_message, searchQuery)
        TitlesTypeFilter.MOVIES -> stringResource(R.string.titles_empty_search_no_movies_message, searchQuery)
        TitlesTypeFilter.ALL -> stringResource(R.string.titles_empty_search_no_titles_message, searchQuery)
      }
      hasFilter -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> stringResource(R.string.titles_empty_filter_watch_state_message)
        else -> stringResource(R.string.titles_empty_filter_message)
      }
      libraryFiltered -> stringResource(R.string.library_sources_banner_message)
      else -> when (typeFilter) {
        TitlesTypeFilter.SHOWS -> stringResource(R.string.titles_empty_no_shows_message)
        TitlesTypeFilter.MOVIES -> stringResource(R.string.titles_empty_no_movies_message)
        TitlesTypeFilter.ALL -> stringResource(R.string.titles_empty_no_titles_message)
      }
    }

    EmptyState(
      icon = emptyIcon,
      title = emptyTitle,
      message = emptyMessage,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      actionLabel = when {
        searchQuery.isNotBlank() -> stringResource(R.string.clear_search)
        hasFilter -> stringResource(R.string.titles_empty_action_show_all)
        libraryFiltered -> stringResource(R.string.library_sources_empty_action)
        else -> null
      },
      onAction = when {
        searchQuery.isNotBlank() -> onClearSearch
        hasFilter -> ({
          onWatchFilterChange(TitleWatchFilter.ALL)
          onDurationFilterChange(MovieDurationFilter.ALL)
        })
        libraryFiltered -> onManageLibrarySources
        else -> null
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
      if (layoutMode == MediaLayoutMode.GRID) {
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
          item(span = { GridItemSpan(columns) }) {
            TitlesFilterControls(
              typeFilter = typeFilter,
              onTypeFilterChange = onTypeFilterChange,
              watchFilter = watchFilter,
              onWatchFilterChange = onWatchFilterChange,
              durationFilter = durationFilter,
              onDurationFilterChange = onDurationFilterChange,
              titlesSort = titlesSort,
              titlesSortOptions = titlesSortOptions,
              onTitlesSortChange = onTitlesSortChange,
              shownCount = filteredTitles.size,
              totalCount = totalTitlesCount,
            )
          }
          items(filteredTitles, key = { it.id }) { item ->
            when (item) {
              is TitleItem.Show -> SeriesPosterCard(
                series = item.series,
                onClick = { onItemClick(item) },
              )
              is TitleItem.Movie -> MoviePosterCard(
                movie = item.movie,
                onClick = { onItemClick(item) },
                onLongClick = { onMovieLongClick(item.movie) },
              )
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            bottom = navigationBarHeight + 24.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            TitlesFilterControls(
              typeFilter = typeFilter,
              onTypeFilterChange = onTypeFilterChange,
              watchFilter = watchFilter,
              onWatchFilterChange = onWatchFilterChange,
              durationFilter = durationFilter,
              onDurationFilterChange = onDurationFilterChange,
              titlesSort = titlesSort,
              titlesSortOptions = titlesSortOptions,
              onTitlesSortChange = onTitlesSortChange,
              shownCount = filteredTitles.size,
              totalCount = totalTitlesCount,
            )
          }
          items(filteredTitles, key = { it.id }) { item ->
            when (item) {
              is TitleItem.Show -> SeriesListRow(
                series = item.series,
                onClick = { onItemClick(item) },
              )
              is TitleItem.Movie -> MovieListRow(
                movie = item.movie,
                onClick = { onItemClick(item) },
                onLongClick = { onMovieLongClick(item.movie) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TitlesFilterControls(
  typeFilter: TitlesTypeFilter,
  onTypeFilterChange: (TitlesTypeFilter) -> Unit,
  watchFilter: TitleWatchFilter,
  onWatchFilterChange: (TitleWatchFilter) -> Unit,
  durationFilter: MovieDurationFilter,
  onDurationFilterChange: (MovieDurationFilter) -> Unit,
  titlesSort: TitlesSort,
  titlesSortOptions: List<SortOption<TitlesSort>>,
  onTitlesSortChange: (TitlesSort) -> Unit,
  shownCount: Int,
  totalCount: Int,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // Single row: All | Shows | Movies chips, followed by Watch filter button, Duration filter button, and Sort button
    LibraryFilterBar(
      options = TitlesTypeFilter.entries,
      selected = typeFilter,
      labelOf = { it.label },
      onSelect = onTypeFilterChange,
      sorts = titlesSortOptions,
      selectedSort = titlesSort,
      onSortSelect = onTitlesSortChange,
      modifier = Modifier.padding(horizontal = 4.dp),
      actions = {
        TitleWatchFilterButton(
          selected = watchFilter,
          typeFilter = typeFilter,
          onSelect = onWatchFilterChange,
        )
        MovieDurationFilterButton(
          selected = durationFilter,
          onSelect = onDurationFilterChange,
        )
      },
    )

    // Result count
    LibraryResultCount(
      shown = shownCount,
      total = totalCount,
      filterType = typeFilter,
    )
  }
}
