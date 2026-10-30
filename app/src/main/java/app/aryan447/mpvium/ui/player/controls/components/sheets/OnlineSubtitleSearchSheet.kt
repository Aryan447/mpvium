package app.aryan447.mpvium.ui.player.controls.components.sheets

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.components.PlayerSheet
import app.aryan447.mpvium.repository.wyzie.WyzieSubtitle
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.utils.media.MediaInfoParser
import app.aryan447.mpvium.utils.media.SubtitleReleaseParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class OnlineSubtitleItem {
  data class OnlineTrack(val subtitle: WyzieSubtitle) : OnlineSubtitleItem()
  data class Header(val title: String) : OnlineSubtitleItem()
  object Divider : OnlineSubtitleItem()
}

@Composable
fun OnlineSubtitleSearchSheet(
  onDismissRequest: () -> Unit,
  onDownloadOnline: (WyzieSubtitle) -> Unit,
  isSearching: Boolean = false,
  isDownloading: Boolean = false,
  searchResults: ImmutableList<WyzieSubtitle> = emptyList<WyzieSubtitle>().toImmutableList(),
  isOnlineSectionExpanded: Boolean = true,
  onToggleOnlineSection: () -> Unit = {},
  modifier: Modifier = Modifier,
  mediaTitle: String = "",
  // Autocomplete & Series Selection
  mediaSearchResults: ImmutableList<app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult> = emptyList<app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult>().toImmutableList(),
  isSearchingMedia: Boolean = false,
  onSearchMedia: (String) -> Unit = {},
  onSelectMedia: (app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult) -> Unit = {},
  selectedTvShow: app.aryan447.mpvium.repository.wyzie.WyzieTvShowDetails? = null,
  isFetchingTvDetails: Boolean = false,
  selectedSeason: app.aryan447.mpvium.repository.wyzie.WyzieSeason? = null,
  onSelectSeason: (app.aryan447.mpvium.repository.wyzie.WyzieSeason) -> Unit = {},
  seasonEpisodes: ImmutableList<app.aryan447.mpvium.repository.wyzie.WyzieEpisode> = emptyList<app.aryan447.mpvium.repository.wyzie.WyzieEpisode>().toImmutableList(),
  isFetchingEpisodes: Boolean = false,
  selectedEpisode: app.aryan447.mpvium.repository.wyzie.WyzieEpisode? = null,
  onSelectEpisode: (app.aryan447.mpvium.repository.wyzie.WyzieEpisode) -> Unit = {},
   onClearMediaSelection: () -> Unit = {}
) {
  // Two panes side by side on wide (landscape) screens: series/movie titles
  // on the left, downloadable subtitle results on the right. Narrow screens
  // keep the existing stacked layout below.
  val configuration = LocalConfiguration.current
  val twoPane = configuration.orientation == ORIENTATION_LANDSCAPE &&
    configuration.screenWidthDp >= 700

  val keyboardController = LocalSoftwareKeyboardController.current
  val mediaInfo = remember(mediaTitle) { MediaInfoParser.parse(mediaTitle) }
  var searchQuery by remember { mutableStateOf(mediaInfo.title) }

  // Build the detected info string for display
  val detectedInfo = remember(mediaInfo) {
    buildString {
      append(mediaInfo.title)
      if (mediaInfo.season != null || mediaInfo.episode != null) {
        append(" • ")
        if (mediaInfo.season != null) append("S${String.format("%02d", mediaInfo.season)}")
        if (mediaInfo.episode != null) append("E${String.format("%02d", mediaInfo.episode)}")
      }
      mediaInfo.year?.let { append(" ($it)") }
    }
  }

  // Auto-trigger search on open
  LaunchedEffect(mediaInfo) {
    if (mediaInfo.title.isNotBlank()) {
      onSearchMedia(mediaInfo.title)
    }
  }

  if (twoPane) {
    TwoPaneSubtitleSearch(
      detectedInfo = detectedInfo,
      searchQuery = searchQuery,
      onSearchQueryChange = { searchQuery = it },
      mediaInfoTitle = mediaInfo.title,
      isSearching = isSearching,
      isDownloading = isDownloading,
      isSearchingMedia = isSearchingMedia,
      searchResults = searchResults,
      isOnlineSectionExpanded = isOnlineSectionExpanded,
      onToggleOnlineSection = onToggleOnlineSection,
      onDownloadOnline = onDownloadOnline,
      mediaSearchResults = mediaSearchResults,
      selectedTvShow = selectedTvShow,
      isFetchingTvDetails = isFetchingTvDetails,
      selectedSeason = selectedSeason,
      onSelectSeason = onSelectSeason,
      seasonEpisodes = seasonEpisodes,
      isFetchingEpisodes = isFetchingEpisodes,
      selectedEpisode = selectedEpisode,
      onSelectEpisode = onSelectEpisode,
      onClearMediaSelection = onClearMediaSelection,
      onSearchMedia = onSearchMedia,
      onSelectMedia = onSelectMedia,
      onDismissRequest = onDismissRequest,
      modifier = modifier,
    )
    return
  }

  val onlineHeader = stringResource(R.string.player_sheets_online_results, searchResults.size)
  val items = remember(searchResults, isSearching, isOnlineSectionExpanded, onlineHeader) {
    val list = mutableListOf<OnlineSubtitleItem>()

    // Online Search Results section
    if (searchResults.isNotEmpty() || isSearching) {
        list.add(OnlineSubtitleItem.Header(onlineHeader))
        if (isOnlineSectionExpanded) {
            list.addAll(searchResults.map { OnlineSubtitleItem.OnlineTrack(it) })
        }
    }

    list.toImmutableList()
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      Column(
        modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
      ) {
        // Detected info chip
        DetectedInfoRow(detectedInfo)
        SubtitleSearchField(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          detectedTitle = mediaInfo.title,
          isBusy = isSearching || isDownloading || isSearchingMedia,
          onSubmit = { q ->
            searchQuery = q
            onSearchMedia(q)
          },
          onClear = {
            searchQuery = ""
            onClearMediaSelection()
          },
        )

        // Autocomplete Results
        if (mediaSearchResults.isNotEmpty()) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = MaterialTheme.spacing.medium)
              .heightIn(max = 200.dp),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
          ) {
            androidx.compose.foundation.lazy.LazyColumn {
              items(mediaSearchResults.size) { index ->
                val result = mediaSearchResults[index]
                TmdbResultRow(
                  result = result,
                  onClick = {
                    searchQuery = result.title
                    onSelectMedia(result)
                    keyboardController?.hide()
                  }
                )
                if (index < mediaSearchResults.size - 1) {
                  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
              }
            }
          }
        }

        // Series / Season / Episode Selection UI
        if (selectedTvShow != null) {
          SeriesDetailsSection(
            tvShow = selectedTvShow,
            isFetchingSeasons = isFetchingTvDetails,
            selectedSeason = selectedSeason,
            onSelectSeason = onSelectSeason,
            isFetchingEpisodes = isFetchingEpisodes,
            episodes = seasonEpisodes,
            selectedEpisode = selectedEpisode,
            onSelectEpisode = onSelectEpisode,
            onClose = onClearMediaSelection
          )
        }
      }
      if (isSearching) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium).height(2.dp),
          color = MaterialTheme.colorScheme.primary
        )
      }
    },
    track = { item ->
      when (item) {
        is OnlineSubtitleItem.OnlineTrack -> {
            WyzieSubtitleRow(
                subtitle = item.subtitle,
                onDownload = { onDownloadOnline(item.subtitle) },
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.extraSmall
                )
            )
        }
        is OnlineSubtitleItem.Header -> {
            val isOnlineHeader = item.title == onlineHeader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isOnlineHeader) Modifier.clickable { onToggleOnlineSection() } else Modifier)
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (isOnlineHeader) {
                    Icon(
                        imageVector = if (isOnlineSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        OnlineSubtitleItem.Divider -> {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
      }
    },
    modifier = modifier,
  )
}

/**
 * Wide (landscape) layout: series/movie titles on the left, downloadable
 * subtitle results on the right, so results are visible without scrolling
 * past the title matches.
 */
@Composable
private fun TwoPaneSubtitleSearch(
  detectedInfo: String,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  mediaInfoTitle: String,
  isSearching: Boolean,
  isDownloading: Boolean,
  isSearchingMedia: Boolean,
  searchResults: ImmutableList<WyzieSubtitle>,
  isOnlineSectionExpanded: Boolean,
  onToggleOnlineSection: () -> Unit,
  onDownloadOnline: (WyzieSubtitle) -> Unit,
  mediaSearchResults: ImmutableList<app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult>,
  selectedTvShow: app.aryan447.mpvium.repository.wyzie.WyzieTvShowDetails?,
  isFetchingTvDetails: Boolean,
  selectedSeason: app.aryan447.mpvium.repository.wyzie.WyzieSeason?,
  onSelectSeason: (app.aryan447.mpvium.repository.wyzie.WyzieSeason) -> Unit,
  seasonEpisodes: ImmutableList<app.aryan447.mpvium.repository.wyzie.WyzieEpisode>,
  isFetchingEpisodes: Boolean,
  selectedEpisode: app.aryan447.mpvium.repository.wyzie.WyzieEpisode?,
  onSelectEpisode: (app.aryan447.mpvium.repository.wyzie.WyzieEpisode) -> Unit,
  onClearMediaSelection: () -> Unit,
  onSearchMedia: (String) -> Unit,
  onSelectMedia: (app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val configuration = LocalConfiguration.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val paneMaxHeight = ((configuration.screenHeightDp * 0.55f).dp).coerceIn(240.dp, 520.dp)

  PlayerSheet(onDismissRequest, customMaxWidth = 920.dp) {
    Column(modifier) {
      DetectedInfoRow(detectedInfo)
      if (isSearching) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium).height(2.dp),
          color = MaterialTheme.colorScheme.primary
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = MaterialTheme.spacing.small,
            vertical = MaterialTheme.spacing.extraSmall
          ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        // LEFT: series/movie titles + season/episode pickers
        Column(
          modifier = Modifier
            .weight(0.42f)
            .heightIn(max = paneMaxHeight)
            .verticalScroll(rememberScrollState()),
        ) {
          SubtitleSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            detectedTitle = mediaInfoTitle,
            isBusy = isSearchingMedia || isDownloading,
            onSubmit = { q ->
              onSearchQueryChange(q)
              onSearchMedia(q)
            },
            onClear = {
              onSearchQueryChange("")
              onClearMediaSelection()
            },
          )
          if (mediaSearchResults.isNotEmpty()) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraSmall),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
              Column {
                mediaSearchResults.forEachIndexed { index, result ->
                  TmdbResultRow(
                    result = result,
                    onClick = {
                      onSearchQueryChange(result.title)
                      onSelectMedia(result)
                      keyboardController?.hide()
                    }
                  )
                  if (index < mediaSearchResults.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                  }
                }
              }
            }
          } else if (isSearchingMedia) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
          }
          if (selectedTvShow != null) {
            SeriesDetailsSection(
              tvShow = selectedTvShow,
              isFetchingSeasons = isFetchingTvDetails,
              selectedSeason = selectedSeason,
              onSelectSeason = onSelectSeason,
              isFetchingEpisodes = isFetchingEpisodes,
              episodes = seasonEpisodes,
              selectedEpisode = selectedEpisode,
              onSelectEpisode = onSelectEpisode,
              onClose = onClearMediaSelection
            )
          }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // RIGHT: subtitle results with download buttons
        Column(modifier = Modifier.weight(0.58f)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onToggleOnlineSection() }
              .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.extraSmall
              ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = stringResource(R.string.player_sheets_online_results, searchResults.size),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Icon(
              imageVector = if (isOnlineSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          if (isOnlineSectionExpanded) {
            when {
              searchResults.isNotEmpty() -> {
                LazyColumn(modifier = Modifier.heightIn(max = paneMaxHeight)) {
                  items(searchResults) { subtitle ->
                    WyzieSubtitleRow(
                      subtitle = subtitle,
                      onDownload = { onDownloadOnline(subtitle) },
                      modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.extraSmall
                      )
                    )
                  }
                }
              }
              isSearching || isDownloading -> {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = paneMaxHeight),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator()
                }
              }
              else -> {
                Text(
                  text = stringResource(R.string.player_sheets_pick_title_for_subtitles),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetectedInfoRow(
  detectedInfo: String,
  modifier: Modifier = Modifier,
) {
  if (detectedInfo.isBlank()) return
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium)
      .padding(bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Default.AutoFixHigh,
      contentDescription = null,
      modifier = Modifier.size(14.dp),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    )
    Spacer(Modifier.width(4.dp))
    Text(
      text = detectedInfo,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      maxLines = 1,
      modifier = Modifier.basicMarquee()
    )
  }
}

@Composable
private fun SubtitleSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  detectedTitle: String,
  isBusy: Boolean,
  onSubmit: (String) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
    placeholder = { Text(stringResource(R.string.pref_subtitles_search_online)) },
    leadingIcon = {
      IconButton(onClick = {
        onQueryChange(detectedTitle)
        onSubmit(detectedTitle)
      }) {
        Icon(Icons.Default.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary)
      }
    },
    trailingIcon = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (query.isNotEmpty()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, null)
          }
        }
        if (isBusy) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = {
          val q = if (query.isNotBlank()) query else detectedTitle
          onQueryChange(q)
          onSubmit(q)
          keyboardController?.hide()
        }) {
          Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
        }
      }
    },
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = {
      val q = if (query.isNotBlank()) query else detectedTitle
      onQueryChange(q)
      onSubmit(q)
      keyboardController?.hide()
    }),
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = Color.Transparent,
      unfocusedContainerColor = Color.Transparent,
      focusedIndicatorColor = MaterialTheme.colorScheme.primary,
      unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
  )
}

@Composable
fun WyzieSubtitleRow(
    subtitle: WyzieSubtitle,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val release = remember(subtitle.displayName) {
        SubtitleReleaseParser.parse(subtitle.displayName)
    }
    val title = remember(release, subtitle.displayName) {
        val base = release.title.ifBlank { subtitle.displayName }
        release.year?.let { "$base ($it)" } ?: base
    }
    val quality = remember(release) { release.qualityTags.joinToString(" · ") }
    val details = remember(subtitle) {
        buildList {
            add(subtitle.displayLanguage)
            if (subtitle.isHearingImpaired) add("HI")
            subtitle.source?.takeIf { it.isNotBlank() }?.let(::add)
            subtitle.format?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        }.distinct().joinToString(" · ")
    }
    val languageCode = remember(subtitle.language, subtitle.displayLanguage) {
        subtitle.language?.take(2)?.uppercase()?.takeIf { it.length == 2 }
            ?: subtitle.displayLanguage.take(2).uppercase()
    }

    Surface(
        modifier = modifier.fillMaxWidth().clickable { onDownload() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = languageCode,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (quality.isNotBlank()) {
                    Text(
                        text = quality,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.player_sheets_subtitle_download),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TmdbResultRow(
    result: app.aryan447.mpvium.repository.wyzie.WyzieTmdbResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${result.mediaType.uppercase()} ${result.releaseYear ?: ""}".trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeriesDetailsSection(
    tvShow: app.aryan447.mpvium.repository.wyzie.WyzieTvShowDetails,
    isFetchingSeasons: Boolean,
    selectedSeason: app.aryan447.mpvium.repository.wyzie.WyzieSeason?,
    onSelectSeason: (app.aryan447.mpvium.repository.wyzie.WyzieSeason) -> Unit,
    isFetchingEpisodes: Boolean,
    episodes: ImmutableList<app.aryan447.mpvium.repository.wyzie.WyzieEpisode>,
    selectedEpisode: app.aryan447.mpvium.repository.wyzie.WyzieEpisode?,
    onSelectEpisode: (app.aryan447.mpvium.repository.wyzie.WyzieEpisode) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(bottom = MaterialTheme.spacing.small),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tvShow.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Season Dropdown
                val seasonDropdownExpanded = remember { mutableStateOf(false) }
                Box {
                  FilledTonalButton(
                      onClick = { seasonDropdownExpanded.value = true },
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                      modifier = Modifier
                        .height(38.dp)
                        .glassSheen(RoundedCornerShape(8.dp), LocalGlass.current),
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = glassButtonContainerColor(
                          MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        contentColor = glassButtonContentColor(
                          MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                      ),
                  ) {
                      Text(
                          text = selectedSeason?.let { "S${it.season_number}" } ?: "Season",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1
                      )
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                  }
                  DropdownMenu(
                      expanded = seasonDropdownExpanded.value,
                      onDismissRequest = { seasonDropdownExpanded.value = false },
                      modifier = Modifier.heightIn(max = 300.dp),
                      shape = RoundedCornerShape(12.dp),
                      containerColor = glassMenuContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh)
                  ) {
                      tvShow.seasons.forEach { season ->
                          DropdownMenuItem(
                              text = {
                                Text(
                                  "Season ${season.season_number}",
                                  style = MaterialTheme.typography.bodyLarge
                                )
                              },
                              onClick = {
                                  onSelectSeason(season)
                                  seasonDropdownExpanded.value = false
                              }
                          )
                      }
                  }
                }

                // Episode Dropdown
                val episodeDropdownExpanded = remember { mutableStateOf(false) }
                Box {
                  FilledTonalButton(
                      onClick = { episodeDropdownExpanded.value = true },
                      enabled = selectedSeason != null && !isFetchingEpisodes,
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                      modifier = Modifier
                        .height(38.dp)
                        .glassSheen(RoundedCornerShape(8.dp), LocalGlass.current),
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = glassButtonContainerColor(
                          MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        contentColor = glassButtonContentColor(
                          MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                      ),
                  ) {
                      if (isFetchingEpisodes) {
                          CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                          )
                          Spacer(Modifier.width(6.dp))
                      }
                      Text(
                          text = selectedEpisode?.let { "E${it.episode_number}" } ?: "Ep",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1
                      )
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                  }
                  DropdownMenu(
                      expanded = episodeDropdownExpanded.value,
                      onDismissRequest = { episodeDropdownExpanded.value = false },
                      modifier = Modifier.heightIn(max = 300.dp).widthIn(min = 200.dp),
                      shape = RoundedCornerShape(12.dp),
                      containerColor = glassMenuContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh)
                  ) {
                      episodes.forEach { episode ->
                          DropdownMenuItem(
                              text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                  Text(
                                    "Ep ${episode.episode_number}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                  )
                                  episode.name?.let {
                                    Text(
                                      it,
                                      style = MaterialTheme.typography.bodySmall,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      maxLines = 1,
                                      modifier = Modifier.basicMarquee()
                                    )
                                  }
                                }
                              },
                              onClick = {
                                  onSelectEpisode(episode)
                                  episodeDropdownExpanded.value = false
                              }
                          )
                      }
                  }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
