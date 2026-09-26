package app.aryan447.mpvium.ui.preferences

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassCardColors
import app.aryan447.mpvium.ui.theme.glassRimStroke
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals

private data class DashboardEntry(
  val icon: ImageVector,
  @StringRes val titleRes: Int,
  @StringRes val summaryRes: Int,
  val screen: Screen,
  val container: @Composable () -> Color,
  val content: @Composable () -> Color,
)

private data class DashboardSection(
  val title: String,
  val entries: List<DashboardEntry>,
)

@Serializable
object PreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val results by remember(query) {
      derivedStateOf {
        SearchablePreferences.search(query) { resId -> context.getString(resId) }
      }
    }
    val isSearching = query.isNotBlank()

    val sections = rememberDashboardSections()
    val totalDestinations = remember(sections) { sections.sumOf { it.entries.size } }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_preferences))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          DashboardSearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )

          if (isSearching) {
            DashboardSearchResults(
              query = query,
              results = results,
              onOpen = { backstack.add(it.screen) },
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
            ) {
              item {
                DashboardHero(
                  destinations = totalDestinations,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
              }

              sections.forEach { section ->
                item {
                  PreferenceSectionHeader(
                    title = section.title,
                    count = section.entries.size,
                  )
                }
                item {
                  DashboardGrid(
                    entries = section.entries,
                    onOpen = { backstack.add(it.screen) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                  )
                }
              }

              item {
                Spacer(modifier = Modifier.height(20.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun rememberDashboardSections(): List<DashboardSection> {
  val scheme = MaterialTheme.colorScheme
  return remember(scheme) {
    listOf(
      DashboardSection(
        title = "Personalize",
        entries = listOf(
          DashboardEntry(
            icon = Icons.Outlined.Palette,
            titleRes = R.string.pref_appearance_title,
            summaryRes = R.string.pref_appearance_summary,
            screen = AppearancePreferencesScreen,
            container = { scheme.primaryContainer },
            content = { scheme.onPrimaryContainer },
          ),
          DashboardEntry(
            icon = Icons.AutoMirrored.Outlined.ViewQuilt,
            titleRes = R.string.pref_layout_title,
            summaryRes = R.string.pref_layout_summary,
            screen = PlayerControlsPreferencesScreen,
            container = { scheme.secondaryContainer },
            content = { scheme.onSecondaryContainer },
          ),
        ),
      ),
      DashboardSection(
        title = "Playback",
        entries = listOf(
          DashboardEntry(
            icon = Icons.Outlined.PlayCircle,
            titleRes = R.string.pref_player,
            summaryRes = R.string.pref_player_summary,
            screen = PlayerPreferencesScreen,
            container = { scheme.tertiaryContainer },
            content = { scheme.onTertiaryContainer },
          ),
          DashboardEntry(
            icon = Icons.Outlined.Gesture,
            titleRes = R.string.pref_gesture,
            summaryRes = R.string.pref_gesture_summary,
            screen = GesturePreferencesScreen,
            container = { scheme.primaryContainer },
            content = { scheme.onPrimaryContainer },
          ),
        ),
      ),
      DashboardSection(
        title = "Library & Media",
        entries = listOf(
          DashboardEntry(
            icon = Icons.Outlined.Folder,
            titleRes = R.string.pref_folders_title,
            summaryRes = R.string.pref_folders_summary,
            screen = FoldersPreferencesScreen,
            container = { scheme.secondaryContainer },
            content = { scheme.onSecondaryContainer },
          ),
          DashboardEntry(
            icon = Icons.Outlined.Memory,
            titleRes = R.string.pref_decoder,
            summaryRes = R.string.pref_decoder_summary,
            screen = DecoderPreferencesScreen,
            container = { scheme.tertiaryContainer },
            content = { scheme.onTertiaryContainer },
          ),
          DashboardEntry(
            icon = Icons.Outlined.Subtitles,
            titleRes = R.string.pref_subtitles,
            summaryRes = R.string.pref_subtitles_summary,
            screen = SubtitlesPreferencesScreen,
            container = { scheme.primaryContainer },
            content = { scheme.onPrimaryContainer },
          ),
          DashboardEntry(
            icon = Icons.Outlined.Audiotrack,
            titleRes = R.string.pref_audio,
            summaryRes = R.string.pref_audio_summary,
            screen = AudioPreferencesScreen,
            container = { scheme.secondaryContainer },
            content = { scheme.onSecondaryContainer },
          ),
        ),
      ),
      DashboardSection(
        title = "System",
        entries = listOf(
          DashboardEntry(
            icon = Icons.Outlined.Code,
            titleRes = R.string.pref_advanced,
            summaryRes = R.string.pref_advanced_summary,
            screen = AdvancedPreferencesScreen,
            container = { scheme.tertiaryContainer },
            content = { scheme.onTertiaryContainer },
          ),
          DashboardEntry(
            icon = Icons.Outlined.Info,
            titleRes = R.string.pref_about_title,
            summaryRes = R.string.pref_about_summary,
            screen = AboutScreen,
            container = { scheme.surfaceContainerHighest },
            content = { scheme.onSurface },
          ),
        ),
      ),
    )
  }
}

/**
 * Premium hero: layered primary→tertiary gradient, decorative translucent
 * orbs, glass icon medallion, and a destination-count pill.
 */
@Composable
private fun DashboardHero(
  destinations: Int,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val isGlass = LocalGlass.current
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  Card(
    modifier = modifier
      .fillMaxWidth()
      .glassSheen(SettingsCardShape, isGlass),
    shape = SettingsCardShape,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    border = if (isGlass) glassRimStroke(dark) else BorderStroke(
      1.dp,
      scheme.outlineVariant.copy(alpha = 0.4f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              scheme.primaryContainer,
              scheme.tertiaryContainer,
              scheme.secondaryContainer,
            ),
          ),
        ),
    ) {
      // Decorative orbs for depth.
      Box(
        modifier = Modifier
          .size(180.dp)
          .offset(x = 120.dp, y = (-70).dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.14f))
          .align(Alignment.TopEnd),
      )
      Box(
        modifier = Modifier
          .size(120.dp)
          .offset(x = 60.dp, y = 40.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.10f))
          .align(Alignment.TopEnd),
      )
      // Top gloss.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.Transparent,
              ),
            ),
          )
          .align(Alignment.TopCenter),
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
          )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.pref_preferences),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = scheme.primary.copy(alpha = 0.9f),
            ) {
              Text(
                text = "$destinations destinations",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Search to jump in",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = scheme.onPrimaryContainer.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var focused by remember { mutableStateOf(false) }
  val scheme = MaterialTheme.colorScheme
  TextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { focused = it.isFocused }
      .border(
        width = 1.5.dp,
        color = if (focused || query.isNotEmpty()) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(28.dp),
      ),
    placeholder = {
      Text(
        text = stringResource(R.string.settings_search_hint),
        color = scheme.onSurfaceVariant,
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = null,
        tint = if (focused || query.isNotEmpty()) scheme.primary else scheme.onSurfaceVariant,
      )
    },
    trailingIcon = {
      AnimatedVisibility(
        visible = query.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(
            imageVector = Icons.Outlined.Clear,
            contentDescription = "Clear search",
            tint = scheme.onSurfaceVariant,
          )
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(28.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = scheme.surfaceContainerHighest,
      unfocusedContainerColor = scheme.surfaceContainerHigh,
      disabledContainerColor = scheme.surfaceContainerHigh,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      disabledIndicatorColor = Color.Transparent,
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
  )
}

@Composable
private fun DashboardGrid(
  entries: List<DashboardEntry>,
  onOpen: (DashboardEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val columns = if (maxWidth > 600.dp) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      entries.chunked(columns).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          row.forEach { entry ->
            DashboardTile(
              entry = entry,
              onClick = { onOpen(entry) },
              modifier = Modifier.weight(1f),
            )
          }
          // Balance an incomplete last row so tiles keep equal width.
          repeat(columns - row.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

/**
 * Premium tile: hairline border, gradient icon medallion, chevron
 * affordance, and a springy press-scale for tactile feedback.
 */
@Composable
private fun DashboardTile(
  entry: DashboardEntry,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scheme = MaterialTheme.colorScheme
  val isGlass = LocalGlass.current
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.965f else 1f,
    animationSpec = spring(stiffness = 420f, dampingRatio = 0.72f),
    label = "tilePress",
  )
  Card(
    modifier = modifier
      .heightIn(min = 168.dp)
      .graphicsLayer(scaleX = scale, scaleY = scale)
      .glassSheen(SettingsTileShape, isGlass)
      .clip(SettingsTileShape)
      .clickable(
        interactionSource = interaction,
        indication = null,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
      ),
    shape = SettingsTileShape,
    colors = if (isGlass) glassCardColors() else CardDefaults.cardColors(
      containerColor = scheme.surfaceContainer,
    ),
    border = if (isGlass) glassRimStroke(dark) else BorderStroke(
      1.dp,
      scheme.outlineVariant.copy(alpha = 0.45f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
    ) {
      Row(verticalAlignment = Alignment.Top) {
        PreferenceIconBox(
          icon = entry.icon,
          container = entry.container(),
          content = entry.content(),
          boxSize = 52.dp,
          iconSize = 28.dp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(entry.titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = stringResource(entry.summaryRes),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun DashboardSearchResults(
  query: String,
  results: List<SearchablePreference>,
  onOpen: (SearchablePreference) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (results.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.settings_search_no_results),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "“$query”",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    return
  }

  LazyColumn(modifier = modifier) {
    item {
      Text(
        text = "${results.size} result${if (results.size == 1) "" else "s"}",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
    }
    items(
      items = results,
      key = { "${it.titleRes}_${it.category}_${it.screen}" },
    ) { preference ->
      SettingsSearchResultRow(
        preference = preference,
        onClick = { onOpen(preference) },
      )
    }
    item {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
