package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals

@Serializable
object PreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val sections = settingsSections()
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
          // Search entry - tappable field that opens full search
          item {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { backstack.add(SettingsSearchScreen) },
              shape = RoundedCornerShape(20.dp),
              color = glassSheetContainerColor(MaterialTheme.colorScheme.surfaceContainerLow),
              tonalElevation = 0.dp,
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
                  maxLines = 1,
                )
              }
            }
          }

          sections.forEach { section ->
            item(key = "header_${section.title}") {
              PreferenceSectionHeader(title = section.title)
            }

            item(key = "card_${section.title}") {
              PreferenceCard {
                section.rows.forEachIndexed { index, row ->
                  if (index > 0) PreferenceDivider()
                  SettingsPreferenceRow(
                    title = row.title,
                    summary = row.summary,
                    icon = row.icon,
                    onClick = { backstack.add(row.screen) },
                  )
                }
              }
            }
          }

          // Bottom breathing room above the navigation bar
          item {
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    }
  }
}

private data class SettingsSectionRow(
  val title: String,
  val summary: String,
  val icon: ImageVector,
  val screen: Screen,
)

private data class SettingsSection(
  val title: String,
  val rows: List<SettingsSectionRow>,
)

@Composable
private fun settingsSections(): List<SettingsSection> = listOf(
  SettingsSection(
    title = "UI & Appearance",
    rows = listOf(
      SettingsSectionRow(
        stringResource(id = R.string.pref_appearance_title),
        stringResource(id = R.string.pref_appearance_summary),
        Icons.Outlined.Palette,
        AppearancePreferencesScreen,
      ),
      SettingsSectionRow(
        stringResource(id = R.string.pref_layout_title),
        stringResource(id = R.string.pref_layout_summary),
        Icons.AutoMirrored.Outlined.ViewQuilt,
        PlayerControlsPreferencesScreen,
      ),
    ),
  ),
  SettingsSection(
    title = "Playback & Controls",
    rows = listOf(
      SettingsSectionRow(
        stringResource(id = R.string.pref_player),
        stringResource(id = R.string.pref_player_summary),
        Icons.Outlined.PlayCircle,
        PlayerPreferencesScreen,
      ),
      SettingsSectionRow(
        stringResource(id = R.string.pref_gesture),
        stringResource(id = R.string.pref_gesture_summary),
        Icons.Outlined.Gesture,
        GesturePreferencesScreen,
      ),
    ),
  ),
  SettingsSection(
    title = "File Management",
    rows = listOf(
      SettingsSectionRow(
        stringResource(id = R.string.pref_folders_title),
        stringResource(id = R.string.pref_folders_summary),
        Icons.Outlined.Folder,
        FoldersPreferencesScreen,
      ),
    ),
  ),
  SettingsSection(
    title = "Media Settings",
    rows = listOf(
      SettingsSectionRow(
        stringResource(id = R.string.pref_decoder),
        stringResource(id = R.string.pref_decoder_summary),
        Icons.Outlined.Memory,
        DecoderPreferencesScreen,
      ),
      SettingsSectionRow(
        stringResource(id = R.string.pref_subtitles),
        stringResource(id = R.string.pref_subtitles_summary),
        Icons.Outlined.Subtitles,
        SubtitlesPreferencesScreen,
      ),
      SettingsSectionRow(
        stringResource(id = R.string.pref_audio),
        stringResource(id = R.string.pref_audio_summary),
        Icons.Outlined.Audiotrack,
        AudioPreferencesScreen,
      ),
    ),
  ),
  SettingsSection(
    title = "Advanced & About",
    rows = listOf(
      SettingsSectionRow(
        stringResource(R.string.pref_advanced),
        stringResource(id = R.string.pref_advanced_summary),
        Icons.Outlined.Code,
        AdvancedPreferencesScreen,
      ),
      SettingsSectionRow(
        stringResource(id = R.string.pref_about_title),
        stringResource(id = R.string.pref_about_summary),
        Icons.Outlined.Info,
        AboutScreen,
      ),
    ),
  ),
)
