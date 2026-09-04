package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals

@Serializable
object PreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_preferences))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          // Search bar - full width, prominent placement
          item {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { backstack.add(SettingsSearchScreen) },
              shape = RoundedCornerShape(28.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              tonalElevation = 2.dp,
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = stringResource(R.string.settings_search_hint),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.outline,
                )
              }
            }
          }

          // UI & Appearance Section
          item {
            PreferenceSectionHeader(title = "UI & Appearance")
          }

          item {
            PreferenceCard {
              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_appearance_title),
                summary = stringResource(id = R.string.pref_appearance_summary),
                icon = Icons.Outlined.Palette,
                onClick = { backstack.add(AppearancePreferencesScreen) },
              )

              PreferenceDivider()

              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_layout_title),
                summary = stringResource(id = R.string.pref_layout_summary),
                icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                onClick = { backstack.add(PlayerControlsPreferencesScreen) },
              )
            }
          }

          // Playback & Controls Section
          item {
            PreferenceSectionHeader(title = "Playback & Controls")
          }

          item {
            PreferenceCard {
              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_player),
                summary = stringResource(id = R.string.pref_player_summary),
                icon = Icons.Outlined.PlayCircle,
                onClick = { backstack.add(PlayerPreferencesScreen) },
              )

              PreferenceDivider()

              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_gesture),
                summary = stringResource(id = R.string.pref_gesture_summary),
                icon = Icons.Outlined.Gesture,
                onClick = { backstack.add(GesturePreferencesScreen) },
              )
            }
          }

          // File Management Section
          item {
            PreferenceSectionHeader(title = "File Management")
          }

          item {
            PreferenceCard {
              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_folders_title),
                summary = stringResource(id = R.string.pref_folders_summary),
                icon = Icons.Outlined.Folder,
                onClick = { backstack.add(FoldersPreferencesScreen) },
              )
            }
          }

          // Media Settings Section
          item {
            PreferenceSectionHeader(title = "Media Settings")
          }

          item {
            PreferenceCard {
              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_decoder),
                summary = stringResource(id = R.string.pref_decoder_summary),
                icon = Icons.Outlined.Memory,
                onClick = { backstack.add(DecoderPreferencesScreen) },
              )

              PreferenceDivider()

              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_subtitles),
                summary = stringResource(id = R.string.pref_subtitles_summary),
                icon = Icons.Outlined.Subtitles,
                onClick = { backstack.add(SubtitlesPreferencesScreen) },
              )

              PreferenceDivider()

              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_audio),
                summary = stringResource(id = R.string.pref_audio_summary),
                icon = Icons.Outlined.Audiotrack,
                onClick = { backstack.add(AudioPreferencesScreen) },
              )
            }
          }

          // Advanced & About Section
          item {
            PreferenceSectionHeader(title = "Advanced & About")
          }

          item {
            PreferenceCard {
              SettingsPreferenceRow(
                title = stringResource(R.string.pref_advanced),
                summary = stringResource(id = R.string.pref_advanced_summary),
                icon = Icons.Outlined.Code,
                onClick = { backstack.add(AdvancedPreferencesScreen) },
              )

              PreferenceDivider()

              SettingsPreferenceRow(
                title = stringResource(id = R.string.pref_about_title),
                summary = stringResource(id = R.string.pref_about_summary),
                icon = Icons.Outlined.Info,
                onClick = { backstack.add(AboutScreen) },
              )
            }
          }
        }
      }
    }
  }
}
