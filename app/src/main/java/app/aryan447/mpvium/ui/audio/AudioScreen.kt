package app.aryan447.mpvium.ui.audio

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.MediaLayoutMode
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.presentation.components.pullrefresh.PullRefreshBox
import app.aryan447.mpvium.ui.audio.components.AlbumCard
import app.aryan447.mpvium.ui.audio.components.AlbumRow
import app.aryan447.mpvium.ui.audio.components.ArtistRow
import app.aryan447.mpvium.ui.audio.components.AudiobookCard
import app.aryan447.mpvium.ui.audio.components.AudiobookRow
import app.aryan447.mpvium.ui.audio.components.DiscourseCard
import app.aryan447.mpvium.ui.audio.components.DiscourseRow
import app.aryan447.mpvium.ui.audio.components.TrackRow
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.components.MediaLayoutToggle
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.browser.states.LoadingState
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object AudioScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val haptic = LocalHapticFeedback.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val layoutAudio by browserPreferences.layoutAudio.collectAsState()
    val navBarHeight = LocalNavigationBarHeight.current

    val vm: AudioViewModel = viewModel(
      factory = AudioViewModel.factory(context.applicationContext as Application)
    )

    val filter by vm.filter.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val albums by vm.filteredAlbums.collectAsState()
    val audiobooks by vm.filteredAudiobooks.collectAsState()
    val discourses by vm.filteredDiscourses.collectAsState()
    val tracks by vm.filteredTracks.collectAsState()
    val artists by vm.filteredArtists.collectAsState()
    val libraryData by vm.libraryData.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }

    BackHandler(enabled = isSearching) {
      vm.setSearching(false)
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        if (isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = vm::setSearchQuery,
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.audio_search_placeholder)) },
                leadingIcon = {
                  IconButton(onClick = { vm.setSearching(false) }) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = stringResource(R.string.back),
                    )
                  }
                },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vm.setSearchQuery("") }) {
                      Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                      )
                    }
                  }
                },
              )
            },
            expanded = true,
            onExpandedChange = { if (!it) vm.setSearching(false) },
            modifier = Modifier.fillMaxWidth(),
            colors = SearchBarDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
          ) {
            // Search results displayed in the body below
          }
        } else {
          TopAppBar(
            title = {
              Text(
                text = stringResource(R.string.tab_audio),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
            },
            actions = {
              IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.setSearching(true)
              }) {
                Icon(
                  imageVector = Icons.Filled.Search,
                  contentDescription = stringResource(R.string.search),
                )
              }

              if (filter == AudioTabFilter.ALBUMS || filter == AudioTabFilter.AUDIOBOOKS || filter == AudioTabFilter.DISCOURSES) {
                MediaLayoutToggle(
                  mode = layoutAudio,
                  onToggle = {
                    val next = if (layoutAudio == MediaLayoutMode.GRID) {
                      MediaLayoutMode.LIST
                    } else {
                      MediaLayoutMode.GRID
                    }
                    browserPreferences.layoutAudio.set(next)
                  },
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
            ),
          )
        }
      },
    ) { padding ->
      PullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.refresh() },
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Browsing Tabs: Albums / All tracks / Artists
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
          ) {
            val albumsLabel = stringResource(R.string.audio_filter_albums)
            val audiobooksLabel = stringResource(R.string.audio_filter_audiobooks)
            val discoursesLabel = stringResource(R.string.audio_filter_discourses)
            val tracksLabel = stringResource(R.string.audio_filter_tracks)
            val artistsLabel = stringResource(R.string.audio_filter_artists)

            ExpressiveChoiceRow(
              options = AudioTabFilter.entries,
              selected = filter,
              onSelect = { vm.setFilter(it) },
              labelOf = {
                when (it) {
                  AudioTabFilter.ALBUMS -> albumsLabel
                  AudioTabFilter.AUDIOBOOKS -> audiobooksLabel
                  AudioTabFilter.DISCOURSES -> discoursesLabel
                  AudioTabFilter.TRACKS -> tracksLabel
                  AudioTabFilter.ARTISTS -> artistsLabel
                }
              },
              modifier = Modifier.fillMaxWidth(),
            )
          }

          if (isLoading && libraryData.tracks.isEmpty()) {
            LoadingState(modifier = Modifier.fillMaxSize())
          } else if (libraryData.tracks.isEmpty()) {
            EmptyState(
              icon = Icons.Filled.Audiotrack,
              title = stringResource(R.string.audio_empty_no_audio_title),
              message = stringResource(R.string.audio_empty_no_audio_message),
              modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navBarHeight),
            )
          } else {
            val bottomPadding = navBarHeight + 16.dp

            when (filter) {
              AudioTabFilter.ALBUMS -> {
                if (albums.isEmpty()) {
                  EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.audio_empty_search_title),
                    message = stringResource(R.string.audio_empty_search_message, searchQuery),
                    modifier = Modifier.fillMaxSize(),
                  )
                } else if (layoutAudio == MediaLayoutMode.GRID) {
                  LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(albums, key = { it.id }) { album ->
                      AlbumCard(
                        album = album,
                        onClick = {
                          backstack.add(AlbumDetailScreen(album.id, album.title))
                        },
                      )
                    }
                  }
                } else {
                  LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(albums, key = { it.id }) { album ->
                      AlbumRow(
                        album = album,
                        onClick = {
                          backstack.add(AlbumDetailScreen(album.id, album.title))
                        },
                      )
                    }
                  }
                }
              }

              AudioTabFilter.AUDIOBOOKS -> {
                if (audiobooks.isEmpty()) {
                  EmptyState(
                    icon = if (searchQuery.isNotEmpty()) Icons.Filled.Search else Icons.Filled.Audiotrack,
                    title = if (searchQuery.isNotEmpty()) {
                      stringResource(R.string.audio_empty_search_title)
                    } else {
                      stringResource(R.string.audiobooks_empty_title)
                    },
                    message = if (searchQuery.isNotEmpty()) {
                      stringResource(R.string.audio_empty_search_message, searchQuery)
                    } else {
                      stringResource(R.string.audiobooks_empty_message)
                    },
                    modifier = Modifier.fillMaxSize(),
                  )
                } else if (layoutAudio == MediaLayoutMode.GRID) {
                  LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(audiobooks, key = { it.id }) { book ->
                      AudiobookCard(
                        audiobook = book,
                        onClick = {
                          backstack.add(AudiobookDetailScreen(book.id, book.title))
                        },
                      )
                    }
                  }
                } else {
                  LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(audiobooks, key = { it.id }) { book ->
                      AudiobookRow(
                        audiobook = book,
                        onClick = {
                          backstack.add(AudiobookDetailScreen(book.id, book.title))
                        },
                      )
                    }
                  }
                }
              }

              AudioTabFilter.DISCOURSES -> {
                if (discourses.isEmpty()) {
                  EmptyState(
                    icon = if (searchQuery.isNotEmpty()) Icons.Filled.Search else Icons.Filled.Audiotrack,
                    title = if (searchQuery.isNotEmpty()) {
                      stringResource(R.string.audio_empty_search_title)
                    } else {
                      stringResource(R.string.discourses_empty_title)
                    },
                    message = if (searchQuery.isNotEmpty()) {
                      stringResource(R.string.audio_empty_search_message, searchQuery)
                    } else {
                      stringResource(R.string.discourses_empty_message)
                    },
                    modifier = Modifier.fillMaxSize(),
                  )
                } else if (layoutAudio == MediaLayoutMode.GRID) {
                  LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(discourses, key = { it.id }) { discourse ->
                      DiscourseCard(
                        discourse = discourse,
                        onClick = {
                          backstack.add(DiscourseDetailScreen(discourse.id, discourse.collection))
                        },
                      )
                    }
                  }
                } else {
                  LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(discourses, key = { it.id }) { discourse ->
                      DiscourseRow(
                        discourse = discourse,
                        onClick = {
                          backstack.add(DiscourseDetailScreen(discourse.id, discourse.collection))
                        },
                      )
                    }
                  }
                }
              }

              AudioTabFilter.TRACKS -> {
                if (tracks.isEmpty()) {
                  EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.audio_empty_search_title),
                    message = stringResource(R.string.audio_empty_search_message, searchQuery),
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(tracks, key = { it.id }) { track ->
                      TrackRow(
                        track = track,
                        onClick = {
                          AudioPlayerLauncher.playTrack(context, track, tracks)
                        },
                      )
                    }
                  }
                }
              }

              AudioTabFilter.ARTISTS -> {
                if (artists.isEmpty()) {
                  EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.audio_empty_search_title),
                    message = stringResource(R.string.audio_empty_search_message, searchQuery),
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    items(artists, key = { it.id }) { artist ->
                      ArtistRow(
                        artist = artist,
                        onClick = {
                          if (artist.albums.size == 1) {
                            val album = artist.albums.first()
                            backstack.add(AlbumDetailScreen(album.id, album.title))
                          } else {
                            // Filter by artist name
                            vm.setSearchQuery(artist.name)
                            vm.setFilter(AudioTabFilter.ALBUMS)
                          }
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
