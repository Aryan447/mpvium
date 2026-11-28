package app.aryan447.mpvium.ui.streaming.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassFrostColor
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.glassRimColor
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.glassSearchBarColors
import androidx.compose.material3.Surface
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.preferences.HomeSection
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.streaming.model.StreamingCategory
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.dialogs.MarkAsDialog
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.browser.videolist.VideoListScreen
import app.aryan447.mpvium.ui.preferences.PreferencesScreen
import app.aryan447.mpvium.ui.audio.AudioPlayerLauncher
import app.aryan447.mpvium.ui.streaming.components.ContinueListeningRow
import app.aryan447.mpvium.ui.streaming.components.ContinueWatchingRow
import app.aryan447.mpvium.ui.streaming.components.MoviePosterCard
import app.aryan447.mpvium.ui.streaming.components.SeriesPosterCard
import app.aryan447.mpvium.ui.streaming.components.SpeedBingeCard
import app.aryan447.mpvium.ui.streaming.components.SpeedBingeItem
import app.aryan447.mpvium.ui.streaming.components.StreamingHeroBanner
import app.aryan447.mpvium.domain.streaming.MovieLibraryClassifier
import app.aryan447.mpvium.domain.streaming.MovieLibraryKind
import app.aryan447.mpvium.ui.streaming.movies.MovieDetailScreen
import app.aryan447.mpvium.ui.streaming.movies.MoviesGridScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesDetailScreen
import app.aryan447.mpvium.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.aryan447.mpvium.ui.preferences.FoldersPreferencesScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesGridScreen
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.utils.media.MediaUtils
import app.aryan447.mpvium.utils.media.VideoWatchStatusOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object StreamingHomeScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val navigationBarHeight = LocalNavigationBarHeight.current

    val viewModel: StreamingHomeViewModel = viewModel(
      factory = StreamingHomeViewModel.factory(context.applicationContext as android.app.Application)
    )
    val state by viewModel.uiState.collectAsState()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val foldersPreferences = koinInject<FoldersPreferences>()
    val showSpeedBinge by appearancePreferences.showSpeedBinge.collectAsState()
    val showFeaturedHero by appearancePreferences.showFeaturedHero.collectAsState()
    val homeSectionOrder by appearancePreferences.homeSectionOrder.collectAsState()
    val showContinueWatching by appearancePreferences.showContinueWatching.collectAsState()
    val showContinueListening by appearancePreferences.showContinueListening.collectAsState()
    val showUpNext by appearancePreferences.showUpNext.collectAsState()
    val showRecentlyAdded by appearancePreferences.showRecentlyAdded.collectAsState()
    val showShowsSection by appearancePreferences.showShowsSection.collectAsState()
    val showMoviesSection by appearancePreferences.showMoviesSection.collectAsState()
    val showFoldersSection by appearancePreferences.showFoldersSection.collectAsState()
    val homeSections = remember(homeSectionOrder) { HomeSection.parseOrder(homeSectionOrder) }
    // Empty-state hint (issue #87): when whitelist-only leaves no results,
    // offer a one-tap route to Settings → Folders.
    val whitelistOnlyEnabled by foldersPreferences.whitelistOnlyEnabled.collectAsState()
    val whitelistedFolders by foldersPreferences.whitelistedFolders.collectAsState()
    val libraryFiltered = whitelistOnlyEnabled && whitelistedFolders.isNotEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isRefreshing = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var markAsTarget by remember { mutableStateOf<ContinueWatchingItem?>(null) }

    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = state.isSearching) {
      viewModel.setSearching(false)
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        val isGlass = LocalGlass.current
        val glassHaze = rememberGlassHazeState()
        val glassStyle = glassHazeStyle(isDark = isSystemInDarkTheme(), kind = GlassKind.Bar)
        if (state.isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = state.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search TV shows, movies, videos...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                  IconButton(onClick = { viewModel.setSearching(false) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
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
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "mpvium",
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Black,
                      letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  val themeBadge = themeBadgeLabel()
                  if (themeBadge != null) {
                    Surface(
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = themeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
                DynamicGreetingText()
              }
            },
            actions = {
              val dark = isSystemInDarkTheme()
              val primary = MaterialTheme.colorScheme.primary
              val iconGlass = Modifier
                .clip(CircleShape)
                .background(glassFrostColor(isDark = dark, kind = GlassKind.Chip))
                .border(1.dp, glassRimColor(dark), CircleShape)
                .glassSheen(CircleShape, true)

              var refreshFocused by remember { mutableStateOf(false) }
              IconButton(
                onClick = { viewModel.refreshMetadata() },
                modifier = (if (isGlass) iconGlass else Modifier)
                  .onFocusChanged { refreshFocused = it.isFocused || it.hasFocus }
                  .clip(CircleShape)
                  .then(
                    if (refreshFocused) {
                      Modifier
                        .background(primary.copy(alpha = 0.2f), CircleShape)
                        .border(BorderStroke(2.dp, primary), CircleShape)
                    } else Modifier
                  ),
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                  contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSurface),
                ),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh details")
              }

              var searchFocused by remember { mutableStateOf(false) }
              IconButton(
                onClick = { viewModel.setSearching(true) },
                modifier = (if (isGlass) iconGlass else Modifier)
                  .onFocusChanged { searchFocused = it.isFocused || it.hasFocus }
                  .clip(CircleShape)
                  .then(
                    if (searchFocused) {
                      Modifier
                        .background(primary.copy(alpha = 0.2f), CircleShape)
                        .border(BorderStroke(2.dp, primary), CircleShape)
                    } else Modifier
                  ),
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                  contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSurface),
                ),
              ) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
              }

              var settingsFocused by remember { mutableStateOf(false) }
              IconButton(
                onClick = { backstack.add(PreferencesScreen) },
                modifier = (if (isGlass) iconGlass else Modifier)
                  .onFocusChanged { settingsFocused = it.isFocused || it.hasFocus }
                  .clip(CircleShape)
                  .then(
                    if (settingsFocused) {
                      Modifier
                        .background(primary.copy(alpha = 0.2f), CircleShape)
                        .border(BorderStroke(2.dp, primary), CircleShape)
                    } else Modifier
                  ),
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                  contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSurface),
                ),
              ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
            ),
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
      PullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        listState = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        when {
          state.isLoading && state.series.isEmpty() && state.movies.isEmpty() -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                   text = "Scanning library & TV shows...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          state.isSearching && state.searchQuery.isNotBlank() -> {
            // Search Results
            val query = state.searchQuery.lowercase()
            val filteredSeries = state.series.filter { it.title.lowercase().contains(query) }
            val filteredMovies = state.movies.filter { it.title.lowercase().contains(query) }

            if (filteredSeries.isEmpty() && filteredMovies.isEmpty()) {
              EmptyState(
                icon = Icons.Filled.Search,
                title = "No results found",
                message = "No TV shows or movies match '${state.searchQuery}'",
                modifier = Modifier.fillMaxSize(),
                actionLabel = "Clear search",
                onAction = {
                  viewModel.setSearchQuery("")
                  viewModel.setSearching(false)
                },
              )
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = navigationBarHeight + 20.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                if (filteredSeries.isNotEmpty()) {
                  item {
                    Text(
                      text = "Shows (${filteredSeries.size})",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(bottom = 8.dp),
                    )
                  }
                  items(filteredSeries) { series ->
                    SeriesPosterCard(
                      series = series,
                      onClick = { backstack.add(SeriesDetailScreen(series.id)) },
                      cardWidth = 160.dp,
                    )
                  }
                }

                if (filteredMovies.isNotEmpty()) {
                  item {
                    Text(
                      text = "Movies (${filteredMovies.size})",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(vertical = 8.dp),
                    )
                  }
                  items(filteredMovies) { movie ->
                    MoviePosterCard(
                      movie = movie,
                      onClick = { MediaUtils.playFile(movie.video, context, "movie_play") },
                      cardWidth = 160.dp,
                    )
                  }
                }
              }
            }
          }

          state.series.isEmpty() && state.movies.isEmpty() && !state.isLoading -> {
            EmptyState(
              icon = Icons.Filled.Movie,
              title = "No videos detected",
              message = if (libraryFiltered) {
                context.getString(R.string.library_sources_banner_message)
              } else {
                "Add video files to your device storage to browse them here"
              },
              modifier = Modifier.fillMaxSize(),
              actionLabel = if (libraryFiltered) {
                context.getString(R.string.library_sources_empty_action)
              } else {
                null
              },
              onAction = if (libraryFiltered) {
                { backstack.add(FoldersPreferencesScreen) }
              } else {
                null
              },
            )
          }

          else -> {
            // Hoisted out of the LazyListScope: remember() is @Composable and
            // cannot be called inside LazyColumn's item DSL.
            val upNext = remember(state.series) { buildUpNext(state.series) }
            val recentlyAdded = remember(state.series, state.movies) {
              buildRecentlyAdded(state.series, state.movies)
            }
            val speedBingeItems = remember(
              state.continueWatching,
              state.series,
              state.movies,
              state.selectedCategory,
            ) {
              buildSpeedBingeItems(
                continueWatching = state.continueWatching,
                seriesList = state.series,
                movieList = state.movies,
                category = state.selectedCategory,
              )
            }
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val isTablet = configuration.smallestScreenWidthDp >= 600
            val screenWidthDp = configuration.screenWidthDp

            val speedBingeColumns = when {
              isTablet && isLandscape -> if (screenWidthDp >= 900) 6 else 5
              isTablet && !isLandscape -> if (screenWidthDp >= 720) 5 else 4
              isLandscape -> 5
              else -> 3
            }
            val speedBingeRows = when {
              !isTablet && isLandscape -> 1
              else -> 2
            }
            val speedBingeItemsPerPage = speedBingeColumns * speedBingeRows

            val speedBingePageCount = remember(speedBingeItems, speedBingeItemsPerPage) {
              if (speedBingeItems.isEmpty()) 0 else (speedBingeItems.size + speedBingeItemsPerPage - 1) / speedBingeItemsPerPage
            }
            val speedBingePagerState = rememberPagerState(pageCount = { speedBingePageCount })
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = navigationBarHeight + 32.dp),
            ) {
              // Category Filter Chips
              item {
                CategoryChipsRow(
                  selectedCategory = state.selectedCategory,
                  onCategorySelect = { viewModel.setCategory(it) },
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
              }

              // Home sections in user order (Appearance > Home). A hidden
              // section stays hidden everywhere, including category filters.
              homeSections.forEach { section ->
                when (section) {
                  HomeSection.SPEED_BINGE -> {
                    if (showSpeedBinge && speedBingeItems.isNotEmpty()) {
                      item(key = "speed_binge_header") {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = "Speed Binge",
                            style = MaterialTheme.typography.titleMedium.copy(
                              fontWeight = FontWeight.Bold,
                              letterSpacing = (-0.3).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                          ) {
                            Text(
                              text = "${speedBingeItems.size}",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                              ),
                              color = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                          }
                        }
                      }

                      item(key = "speed_binge_pager") {
                        HorizontalPager(
                          state = speedBingePagerState,
                          modifier = Modifier.fillMaxWidth(),
                        ) { pageIndex ->
                          val startIndex = pageIndex * speedBingeItemsPerPage
                          val pageItems = speedBingeItems.subList(
                            startIndex,
                            minOf(startIndex + speedBingeItemsPerPage, speedBingeItems.size),
                          )
                          val rows = pageItems.chunked(speedBingeColumns)
                          Column(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                          ) {
                            rows.forEach { rowItems ->
                              Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                              ) {
                                rowItems.forEach { bingeItem ->
                                  SpeedBingeCard(
                                    item = bingeItem,
                                    onClick = {
                                      when (bingeItem) {
                                        is SpeedBingeItem.LastPlayed -> {
                                          MediaUtils.playFile(bingeItem.continueItem.video, context, "speed_binge_resume")
                                        }
                                        is SpeedBingeItem.SeriesItem -> {
                                          backstack.add(SeriesDetailScreen(bingeItem.series.id))
                                        }
                                        is SpeedBingeItem.MovieItem -> {
                                          MediaUtils.playFile(bingeItem.movie.video, context, "speed_binge_movie")
                                        }
                                      }
                                    },
                                    onLongClick = {
                                      when (bingeItem) {
                                        is SpeedBingeItem.LastPlayed -> {
                                          markAsTarget = bingeItem.continueItem
                                        }
                                        is SpeedBingeItem.SeriesItem -> {
                                          backstack.add(SeriesDetailScreen(bingeItem.series.id))
                                        }
                                        is SpeedBingeItem.MovieItem -> {
                                          backstack.add(MovieDetailScreen(bingeItem.movie.video.id, bingeItem.movie.title))
                                        }
                                      }
                                    },
                                    modifier = Modifier.weight(1f),
                                  )
                                }
                                repeat(speedBingeColumns - rowItems.size) {
                                  Spacer(modifier = Modifier.weight(1f))
                                }
                              }
                            }
                          }
                        }
                      }

                      if (speedBingePageCount > 1) {
                        item(key = "speed_binge_dots") {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            repeat(minOf(speedBingePageCount, 7)) { index ->
                              val selected = index == speedBingePagerState.currentPage
                              val dotWidth by animateDpAsState(
                                targetValue = if (selected) 16.dp else 6.dp,
                                label = "speed_dot_w",
                              )
                              val dotColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                              } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                              }
                              Box(
                                modifier = Modifier
                                  .padding(horizontal = 3.dp)
                                  .height(6.dp)
                                  .width(dotWidth)
                                  .clip(CircleShape)
                                  .background(dotColor),
                              )
                            }
                          }
                        }
                      }

                      item(key = "speed_binge_spacer") {
                        Spacer(modifier = Modifier.height(14.dp))
                      }
                    }
                  }

                  HomeSection.FEATURED -> {
                    // Featured Hero Spotlight Banner (shown in ALL or SERIES mode)
                    if (showFeaturedHero && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.SERIES)) {
                      state.heroSeries?.let { hero ->
                        item(key = "home_featured") {
                          StreamingHeroBanner(
                            series = hero,
                            onPlayClick = {
                              val videoToPlay = hero.nextEpisodeToWatch?.video
                                ?: hero.seasons.values.firstOrNull()?.firstOrNull()?.video
                              if (videoToPlay != null) {
                                MediaUtils.playFile(videoToPlay, context, "hero_play")
                              }
                            },
                            onDetailsClick = {
                              backstack.add(SeriesDetailScreen(hero.id))
                            },
                          )
                          Spacer(modifier = Modifier.height(16.dp))
                        }
                      }
                    }
                  }

                  HomeSection.CONTINUE_WATCHING -> {
                    // Continue Watching Row
                    if (showContinueWatching && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.CONTINUE_WATCHING)
                      && state.continueWatching.isNotEmpty()
                    ) {
                      item(key = "home_continue_watching") {
                        ContinueWatchingRow(
                          items = state.continueWatching,
                          onItemClick = { item ->
                            MediaUtils.playFile(item.video, context, "continue_watching")
                          },
                          onSeeAllClick = { backstack.add(RecentlyPlayedScreen) },
                          onItemRemove = { item -> viewModel.removeFromContinueWatching(item) },
                          onMarkAsClick = { item -> markAsTarget = item },
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                      }
                    }
                  }

                  HomeSection.CONTINUE_LISTENING -> {
                    // Continue Listening Row (Audiobooks & Discourses)
                    if (showContinueListening && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.CONTINUE_WATCHING)
                      && state.continueListening.isNotEmpty()
                    ) {
                      item(key = "home_continue_listening") {
                        ContinueListeningRow(
                          items = state.continueListening,
                          onItemClick = { item ->
                            AudioPlayerLauncher.playContinueListening(
                              context = context,
                              item = item,
                            )
                          },
                          onItemRemove = { item ->
                            viewModel.removeFromContinueListening(item)
                          },
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                      }
                    }
                  }

                  HomeSection.UP_NEXT -> {
                    // Up Next: next unwatched episode per show, in-progress shows first.
                    // One tap resumes the episode; artwork/progress reuse the cards.
                    if (showUpNext && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.SERIES)) {
                      if (upNext.isNotEmpty()) {
                        item(key = "home_up_next") {
                          ContinueWatchingRow(
                            items = upNext,
                            title = "Up Next",
                            onItemClick = { item ->
                              MediaUtils.playFile(item.video, context, "up_next")
                            },
                            onMarkAsClick = { item -> markAsTarget = item },
                          )
                          Spacer(modifier = Modifier.height(24.dp))
                        }
                      }
                    }
                  }

                  HomeSection.RECENTLY_ADDED -> {
                    // Recently Added: newest files across shows and movies by date added.
                    if (showRecentlyAdded && state.selectedCategory == StreamingCategory.ALL) {
                      if (recentlyAdded.isNotEmpty()) {
                        item(key = "home_recently_added") {
                          ContinueWatchingRow(
                            items = recentlyAdded,
                            title = "Recently Added",
                            onItemClick = { item ->
                              MediaUtils.playFile(item.video, context, "recently_added")
                            },
                            onMarkAsClick = { item -> markAsTarget = item },
                          )
                          Spacer(modifier = Modifier.height(24.dp))
                        }
                      }
                    }
                  }

                  HomeSection.SHOWS -> {
                    // TV Series Section (shown when enabled and Speed Binge is off in ALL mode, or in SERIES tab)
                    if (showShowsSection && (!showSpeedBinge || state.selectedCategory == StreamingCategory.SERIES)
                      && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.SERIES)
                      && state.series.isNotEmpty()
                    ) {
                      item(key = "home_shows") {
                        SectionHeader(
                          title = "Shows",
                          badge = "${state.series.size}",
                          onSeeAllClick = { backstack.add(SeriesGridScreen) },
                        )
                        LazyRow(
                          contentPadding = PaddingValues(horizontal = 16.dp),
                          horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                          items(state.series, key = { "series_${it.id}" }) { series ->
                            SeriesPosterCard(
                              series = series,
                              onClick = { backstack.add(SeriesDetailScreen(series.id)) },
                            )
                          }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                      }
                    }
                  }

                  HomeSection.MOVIES -> {
                    // Movies Section (shown when enabled and Speed Binge is off in ALL mode, or in MOVIES tab)
                    if (showMoviesSection && (!showSpeedBinge || state.selectedCategory == StreamingCategory.MOVIES)
                      && (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.MOVIES)
                      && state.movies.isNotEmpty()
                    ) {
                      item(key = "home_movies") {
                        SectionHeader(
                          title = "Movies & Videos",
                          badge = "${state.movies.size}",
                          onSeeAllClick = { backstack.add(MoviesGridScreen) },
                        )
                        LazyRow(
                          contentPadding = PaddingValues(horizontal = 16.dp),
                          horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                          items(state.movies, key = { "movie_${it.video.id}" }) { movie ->
                            MoviePosterCard(
                              movie = movie,
                              onClick = { backstack.add(MovieDetailScreen(movie.video.id, movie.title)) },
                            )
                          }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                      }
                    }
                  }

                  HomeSection.FOLDERS -> {
                    // Storage Folders Section (Quick Access)
                    if (showFoldersSection && state.selectedCategory == StreamingCategory.ALL && state.folders.isNotEmpty()) {
                      item(key = "home_folders") {
                        SectionHeader(
                          title = "Browse Folders",
                          badge = "${state.folders.size}",
                          onSeeAllClick = null,
                        )
                        LazyRow(
                          contentPadding = PaddingValues(horizontal = 16.dp),
                          horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                          items(state.folders, key = { "folder_${it.bucketId}" }) { folder ->
                            FolderQuickCard(
                              folder = folder,
                              onClick = {
                                backstack.add(VideoListScreen(folder.bucketId, folder.name))
                              },
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              val anySectionVisible = showSpeedBinge || showFeaturedHero || showContinueWatching ||
                showContinueListening || showUpNext || showRecentlyAdded || showShowsSection ||
                showMoviesSection || showFoldersSection

              if (!anySectionVisible) {
                item(key = "home_all_sections_hidden") {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.GridView,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = "All Home sections are hidden",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Turn on Speed Binge or customize sections in Appearance settings",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { backstack.add(app.aryan447.mpvium.ui.preferences.AppearancePreferencesScreen) }) {
                      Text("Customize Home")
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Manual watch status (issue #94): long-press a card -> Mark as… ->
      // Finished clears it from Continue Watching and advances Up Next.
      // MediaLibraryEvents from VideoWatchStatusOps triggers a rescan.
      MarkAsDialog(
        isOpen = markAsTarget != null,
        selectedCount = 1,
        onDismiss = { markAsTarget = null },
        onSelect = { status ->
          val target = markAsTarget
          markAsTarget = null
          if (target != null) {
            coroutineScope.launch(Dispatchers.IO) {
              val applied = VideoWatchStatusOps.markVideo(target.video, status)
              withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                  context,
                  context.getString(
                    if (applied) app.aryan447.mpvium.R.string.mark_as_updated
                    else app.aryan447.mpvium.R.string.mark_as_failed,
                  ),
                  android.widget.Toast.LENGTH_SHORT,
                ).show()
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun CategoryChipsRow(
  selectedCategory: StreamingCategory,
  onCategorySelect: (StreamingCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  ExpressiveChoiceRow(
    options = StreamingCategory.entries,
    selected = selectedCategory,
    labelOf = { it.displayName },
    onSelect = onCategorySelect,
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
private fun SectionHeader(
  title: String,
  badge: String? = null,
  onSeeAllClick: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.3).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (badge != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = CircleShape,
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }
    }

    if (onSeeAllClick != null) {
      TextButton(onClick = onSeeAllClick) {
        Text("See all", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(2.dp))
        Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
      }
    }
  }
}

@Composable
private fun FolderQuickCard(
  folder: VideoFolder,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    modifier = Modifier
      .width(160.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }),
    color = glassSheetContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Filled.Folder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${folder.videoCount} videos",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DynamicGreetingText(
  modifier: Modifier = Modifier,
) {
  val greetings = remember {
    listOf(
      "What's on the binge today? 🍿",
      "Feeling bored? Watch something cool!",
      "Time for a movie night! ✨",
      "Discover your next obsession...",
      "Ready for another episode?",
      "Unwind with your favorite shows!",
      "Lights, camera, action! 🎬",
      "Grab some popcorn and relax!"
    )
  }

  var currentIndex by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    while (isActive) {
      delay(4000)
      currentIndex = (currentIndex + 1) % greetings.size
    }
  }

  AnimatedContent(
    targetState = greetings[currentIndex],
    transitionSpec = {
      (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { height -> height / 2 })
        .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { height -> -height / 2 })
    },
    label = "greeting_animation",
    modifier = modifier,
  ) { greeting ->
    Text(
      text = greeting,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Next unwatched episode per show. In-progress shows (a partially watched
 * last episode) sort first so resuming is one tap away.
 */
private fun buildUpNext(series: List<LocalSeries>): List<ContinueWatchingItem> =
  series
    .filter { it.nextEpisodeToWatch != null }
    .sortedWith(
      compareByDescending<LocalSeries> {
        it.lastWatchedEpisode != null && it.lastWatchedEpisode.isWatched == false
      }.thenBy { it.title.lowercase() },
    )
    .take(10)
    .map { show ->
      val episode = show.nextEpisodeToWatch!!
      ContinueWatchingItem(
        video = episode.video,
        title = show.title,
        subtitle = "${episode.formattedEpisodeTag} • ${episode.displayTitle}",
        posterUrl = show.posterUrl,
        backdropUrl = episode.stillUrl ?: show.backdropUrl,
        playbackPositionMs = episode.playbackPositionMs,
        totalDurationMs = episode.video.duration,
        progressPercentage = episode.progressPercentage,
        lastPlayedTimestamp = episode.video.dateAdded,
        isSeries = true,
        seriesId = show.id,
      )
    }

/**
 * Newest files across episodes and movies by file date-added.
 */
private fun buildRecentlyAdded(
  series: List<LocalSeries>,
  movies: List<LocalMovie>,
): List<ContinueWatchingItem> {
  val episodes = series.flatMap { show ->
    show.seasons.values.flatten().map { episode ->
      ContinueWatchingItem(
        video = episode.video,
        title = show.title,
        subtitle = "${episode.formattedEpisodeTag} • ${episode.displayTitle}",
        posterUrl = show.posterUrl,
        backdropUrl = episode.stillUrl ?: show.backdropUrl,
        playbackPositionMs = episode.playbackPositionMs,
        totalDurationMs = episode.video.duration,
        progressPercentage = episode.progressPercentage,
        lastPlayedTimestamp = episode.video.dateAdded,
        isSeries = true,
        seriesId = show.id,
      )
    }
  }
  val movieItems = movies.map { movie ->
    ContinueWatchingItem(
      video = movie.video,
      title = movie.title,
      subtitle = "Movie" + (movie.year?.let { " • $it" } ?: ""),
      posterUrl = movie.posterUrl,
      backdropUrl = movie.backdropUrl,
      playbackPositionMs = movie.playbackPositionMs,
      totalDurationMs = movie.video.duration,
      progressPercentage = movie.progressPercentage,
      lastPlayedTimestamp = movie.video.dateAdded,
      isSeries = false,
    )
  }
  return (episodes + movieItems)
    .sortedByDescending { it.lastPlayedTimestamp }
    .take(12)
}

/**
 * Builds items for the Speed Binge grid dial:
 * Slot 1: The most recently played item (Last Played) with direct resume.
 * Remaining slots: All available shows and movies, each with its progress bar,
 * sorted with in-progress titles first.
 * Only actual movies and TV shows are included (matching Titles tab classifier).
 */
private fun buildSpeedBingeItems(
  continueWatching: List<ContinueWatchingItem>,
  seriesList: List<LocalSeries>,
  movieList: List<LocalMovie>,
  category: StreamingCategory,
): List<SpeedBingeItem> {
  // Only actual movies (matching Titles tab filter, excludes short clips/recordings)
  val libraryMovies = movieList.filter { MovieLibraryClassifier.isMovieLibraryItem(it) }

  // Only valid continue watching (series or actual movie)
  val validContinueWatching = continueWatching.filter { item ->
    item.isSeries || MovieLibraryClassifier.classify(
      fileName = item.video.displayName,
      folderPath = item.video.path.substringBeforeLast('/', ""),
      durationMs = item.video.duration,
      width = item.video.width,
      height = item.video.height,
    ) == MovieLibraryKind.MOVIE
  }
  val lastPlayed = validContinueWatching.firstOrNull()

  val result = mutableListOf<SpeedBingeItem>()

  when (category) {
    StreamingCategory.ALL -> {
      if (lastPlayed != null) {
        result.add(SpeedBingeItem.LastPlayed(lastPlayed))
      }
      val remainingSeries = seriesList
        .filterNot { lastPlayed != null && lastPlayed.isSeries && it.id == lastPlayed.seriesId }
        .map { SpeedBingeItem.SeriesItem(it) }

      val remainingMovies = libraryMovies
        .filterNot { lastPlayed != null && !lastPlayed.isSeries && it.video.path == lastPlayed.video.path }
        .map { SpeedBingeItem.MovieItem(it) }

      val combinedRemaining = (remainingSeries + remainingMovies).sortedWith(
        compareByDescending<SpeedBingeItem> { it.progressPercentage > 0f && it.progressPercentage < 1f }
          .thenBy { it.title.lowercase(java.util.Locale.getDefault()) }
      )
      result.addAll(combinedRemaining)
    }

    StreamingCategory.SERIES -> {
      if (lastPlayed != null && lastPlayed.isSeries) {
        result.add(SpeedBingeItem.LastPlayed(lastPlayed))
      }
      val remainingSeries = seriesList
        .filterNot { lastPlayed != null && lastPlayed.isSeries && it.id == lastPlayed.seriesId }
        .map { SpeedBingeItem.SeriesItem(it) }
        .sortedWith(
          compareByDescending<SpeedBingeItem> { it.progressPercentage > 0f && it.progressPercentage < 1f }
            .thenBy { it.title.lowercase(java.util.Locale.getDefault()) }
        )
      result.addAll(remainingSeries)
    }

    StreamingCategory.MOVIES -> {
      if (lastPlayed != null && !lastPlayed.isSeries) {
        result.add(SpeedBingeItem.LastPlayed(lastPlayed))
      }
      val remainingMovies = libraryMovies
        .filterNot { lastPlayed != null && !lastPlayed.isSeries && it.video.path == lastPlayed.video.path }
        .map { SpeedBingeItem.MovieItem(it) }
        .sortedWith(
          compareByDescending<SpeedBingeItem> { it.progressPercentage > 0f && it.progressPercentage < 1f }
            .thenBy { it.title.lowercase(java.util.Locale.getDefault()) }
        )
      result.addAll(remainingMovies)
    }

    StreamingCategory.CONTINUE_WATCHING -> {
      validContinueWatching.forEach { item ->
        result.add(SpeedBingeItem.LastPlayed(item))
      }
    }
  }

  return result
}

@Composable
private fun themeBadgeLabel(): String? {
  return when (LocalAppTheme.current) {
    AppTheme.Cinema -> "CINEMA"
    AppTheme.NoirCinema -> "NOIR"
    else -> null
  }
}
