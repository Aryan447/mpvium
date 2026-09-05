package app.aryan447.mpvium.ui.streaming.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.domain.streaming.model.StreamingCategory
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.browser.videolist.VideoListScreen
import app.aryan447.mpvium.ui.preferences.PreferencesScreen
import app.aryan447.mpvium.ui.streaming.components.ContinueWatchingRow
import app.aryan447.mpvium.ui.streaming.components.MoviePosterCard
import app.aryan447.mpvium.ui.streaming.components.SeriesPosterCard
import app.aryan447.mpvium.ui.streaming.components.StreamingHeroBanner
import app.aryan447.mpvium.ui.streaming.movies.MovieDetailScreen
import app.aryan447.mpvium.ui.streaming.movies.MoviesGridScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesDetailScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesGridScreen
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.utils.media.MediaUtils
import kotlinx.serialization.Serializable

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val isRefreshing = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
      topBar = {
        if (state.isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = state.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search series, movies, videos...") },
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
              IconButton(onClick = { viewModel.refreshMetadata() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh details")
              }
              IconButton(onClick = { viewModel.setSearching(true) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
              }
              IconButton(onClick = { backstack.add(PreferencesScreen) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
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
                  text = "Scanning library & series...",
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
                message = "No series or movies match '${state.searchQuery}'",
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
                      text = "Series (${filteredSeries.size})",
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
              message = "Add video files to your device storage to browse them here",
              modifier = Modifier.fillMaxSize(),
            )
          }

          else -> {
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

              // Featured Hero Spotlight Banner (shown in ALL or SERIES mode)
              if (state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.SERIES) {
                state.heroSeries?.let { hero ->
                  item {
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

              // Continue Watching Row
              if ((state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.CONTINUE_WATCHING)
                && state.continueWatching.isNotEmpty()
              ) {
                item {
                  ContinueWatchingRow(
                    items = state.continueWatching,
                    onItemClick = { item ->
                      MediaUtils.playFile(item.video, context, "continue_watching")
                    },
                  )
                  Spacer(modifier = Modifier.height(24.dp))
                }
              }

              // TV Series Section
              if ((state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.SERIES)
                && state.series.isNotEmpty()
              ) {
                item {
                  SectionHeader(
                    title = "TV Series",
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

              // Movies Section
              if ((state.selectedCategory == StreamingCategory.ALL || state.selectedCategory == StreamingCategory.MOVIES)
                && state.movies.isNotEmpty()
              ) {
                item {
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

              // Storage Folders Section (Quick Access)
              if (state.selectedCategory == StreamingCategory.ALL && state.folders.isNotEmpty()) {
                item {
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
      }
    }
  }
}

@Composable
private fun CategoryChipsRow(
  selectedCategory: StreamingCategory,
  onCategorySelect: (StreamingCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(StreamingCategory.values()) { category ->
      val selected = category == selectedCategory
      FilterChip(
        selected = selected,
        onClick = {
          if (!selected) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onCategorySelect(category)
        },
        label = {
          Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = RoundedCornerShape(20.dp),
      )
    }
  }
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
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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

@Composable
private fun themeBadgeLabel(): String? {
  return when (LocalAppTheme.current) {
    AppTheme.Cinema -> "CINEMA"
    AppTheme.NoirCinema -> "NOIR"
    else -> null
  }
}
