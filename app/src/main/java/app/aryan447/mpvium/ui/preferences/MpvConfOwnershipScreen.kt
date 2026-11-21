package app.aryan447.mpvium.ui.preferences

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.MpvConfigOwnership
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ExpandableCard
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * Chooses which mpv option groups are owned by the user's mpv.conf instead of
 * the in-app settings. Groups collapse to a checkbox plus a count so the page
 * stays scannable; expanding reveals the individual options.
 */
@Serializable
object MpvConfOwnershipScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AdvancedPreferences>()
    val storedValues by preferences.mpvConfOverrides.collectAsState()
    val mpvConf by preferences.mpvConf.collectAsState()
    val mpvConfStorageUri by preferences.mpvConfStorageUri.collectAsState()
    val selectedOptions = remember(storedValues) { MpvConfigOwnership.resolveOptionNames(storedValues) }
    val hasMpvConfig =
      remember(mpvConf, mpvConfStorageUri) {
        mpvConfStorageUri.isNotBlank() || hasMeaningfulMpvConfig(mpvConf)
      }
    var expandedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val backStack = LocalBackStack.current

    fun apply(selection: Set<String>) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      preferences.mpvConfOverrides.set(selection)
    }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_mpv_conf_ownership_title))
      },
    ) { padding ->
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large),
      ) {
        item(key = "summary") {
          OwnershipSummaryCard(
            selectedCount = selectedOptions.size,
            hasMpvConfig = hasMpvConfig,
            onEditConfig = {
              backStack.add(ConfigEditorScreen(ConfigEditorScreen.ConfigType.MPV_CONF))
            },
            onReset = { apply(emptySet()) },
          )
        }

        item(key = "header") {
          PreferenceSectionHeader(
            title = stringResource(R.string.pref_mpv_conf_ownership_section),
          )
        }

        items(MpvConfigOwnership.entries, key = { it.preferenceKey }) { group ->
          OwnershipGroupCard(
            group = group,
            selectedOptions = selectedOptions,
            isExpanded = expandedGroup == group.preferenceKey,
            onExpand = { expanded -> expandedGroup = if (expanded) group.preferenceKey else null },
            onSelectionChange = ::apply,
          )
        }
      }
    }
  }
}

@Composable
private fun OwnershipSummaryCard(
  selectedCount: Int,
  hasMpvConfig: Boolean,
  onEditConfig: () -> Unit,
  onReset: () -> Unit,
) {
  PreferenceCard {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = MaterialTheme.spacing.small),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PreferenceRowIcon(icon = Icons.Outlined.Tune)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            if (selectedCount > 0) {
              pluralStringResource(
                R.plurals.pref_mpv_conf_ownership_active,
                selectedCount,
                selectedCount,
              )
            } else {
              stringResource(R.string.pref_mpv_conf_ownership_inactive)
            },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            if (hasMpvConfig) {
              stringResource(R.string.pref_mpv_conf_ownership_hint)
            } else {
              stringResource(R.string.pref_mpv_conf_ownership_empty_config_warning)
            },
          style = MaterialTheme.typography.bodySmall,
          color =
            if (hasMpvConfig) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              MaterialTheme.colorScheme.error
            },
        )
        if (selectedCount > 0) {
          Row {
            TextButton(
              onClick = onEditConfig,
              contentPadding = PaddingValues(0.dp),
            ) {
              Text(stringResource(R.string.pref_mpv_conf_ownership_edit_config))
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            TextButton(
              onClick = onReset,
              contentPadding = PaddingValues(0.dp),
            ) {
              Text(stringResource(R.string.pref_mpv_conf_ownership_reset))
            }
          }
        } else {
          TextButton(
            onClick = onEditConfig,
            contentPadding = PaddingValues(0.dp),
          ) {
            Text(stringResource(R.string.pref_mpv_conf_ownership_edit_config))
          }
        }
      }
    }
  }
}

@Composable
private fun OwnershipGroupCard(
  group: MpvConfigOwnership,
  selectedOptions: Set<String>,
  isExpanded: Boolean,
  onExpand: (Boolean) -> Unit,
  onSelectionChange: (Set<String>) -> Unit,
) {
  val selectedCount = group.optionNames.count(selectedOptions::contains)
  val toggleState =
    when (selectedCount) {
      0 -> ToggleableState.Off
      group.optionNames.size -> ToggleableState.On
      else -> ToggleableState.Indeterminate
    }

  ExpandableCard(
    isExpanded = isExpanded,
    onExpand = onExpand,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    shape = SettingsCardShape,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (selectedCount > 0) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
          } else {
            MaterialTheme.colorScheme.surfaceContainer
          },
      ),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TriStateCheckbox(
          state = toggleState,
          onClick = {
            onSelectionChange(
              if (toggleState == ToggleableState.On) {
                selectedOptions - group.optionNames
              } else {
                selectedOptions + group.optionNames
              },
            )
          },
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
        Column {
          Text(
            text = stringResource(group.titleRes()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              stringResource(
                R.string.pref_mpv_conf_ownership_option_count,
                selectedCount,
                group.optionNames.size,
              ),
            style = MaterialTheme.typography.labelSmall,
            color =
              if (selectedCount > 0) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.outline
              },
          )
        }
      }
    },
    content = {
      Column {
        Text(
          text = stringResource(group.summaryRes()),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = MaterialTheme.spacing.smaller),
        )
        group.optionNames.sorted().forEach { optionName ->
          val checked = optionName in selectedOptions
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .toggleable(
                  value = checked,
                  role = Role.Checkbox,
                  onValueChange = {
                    onSelectionChange(
                      if (checked) selectedOptions - optionName else selectedOptions + optionName,
                    )
                  },
                )
                .padding(vertical = MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.smaller))
            Text(
              text = optionName,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    },
  )
}

private fun hasMeaningfulMpvConfig(content: String): Boolean =
  content.lineSequence().any { line ->
    val value = line.trim()
    value.isNotEmpty() && !value.startsWith("#") && !value.startsWith(";")
  }

@StringRes
private fun MpvConfigOwnership.titleRes(): Int =
  when (this) {
    MpvConfigOwnership.RENDERER -> R.string.pref_mpv_conf_ownership_renderer
    MpvConfigOwnership.DECODER -> R.string.pref_mpv_conf_ownership_decoder
    MpvConfigOwnership.SHADERS -> R.string.pref_mpv_conf_ownership_shaders
    MpvConfigOwnership.VIDEO_FILTERS -> R.string.pref_mpv_conf_ownership_video_filters
    MpvConfigOwnership.VIDEO_GEOMETRY -> R.string.pref_mpv_conf_ownership_video_geometry
    MpvConfigOwnership.AUDIO_OUTPUT -> R.string.pref_mpv_conf_ownership_audio_output
    MpvConfigOwnership.AUDIO_FILTERS -> R.string.pref_mpv_conf_ownership_audio_filters
    MpvConfigOwnership.SUBTITLE_LOADING -> R.string.pref_mpv_conf_ownership_subtitle_loading
    MpvConfigOwnership.SUBTITLE_STYLE -> R.string.pref_mpv_conf_ownership_subtitle_style
    MpvConfigOwnership.PLAYBACK_TIMING -> R.string.pref_mpv_conf_ownership_playback_timing
    MpvConfigOwnership.NETWORK_BUFFERING -> R.string.pref_mpv_conf_ownership_network_buffering
  }

@StringRes
private fun MpvConfigOwnership.summaryRes(): Int =
  when (this) {
    MpvConfigOwnership.RENDERER -> R.string.pref_mpv_conf_ownership_renderer_summary
    MpvConfigOwnership.DECODER -> R.string.pref_mpv_conf_ownership_decoder_summary
    MpvConfigOwnership.SHADERS -> R.string.pref_mpv_conf_ownership_shaders_summary
    MpvConfigOwnership.VIDEO_FILTERS -> R.string.pref_mpv_conf_ownership_video_filters_summary
    MpvConfigOwnership.VIDEO_GEOMETRY -> R.string.pref_mpv_conf_ownership_video_geometry_summary
    MpvConfigOwnership.AUDIO_OUTPUT -> R.string.pref_mpv_conf_ownership_audio_output_summary
    MpvConfigOwnership.AUDIO_FILTERS -> R.string.pref_mpv_conf_ownership_audio_filters_summary
    MpvConfigOwnership.SUBTITLE_LOADING -> R.string.pref_mpv_conf_ownership_subtitle_loading_summary
    MpvConfigOwnership.SUBTITLE_STYLE -> R.string.pref_mpv_conf_ownership_subtitle_style_summary
    MpvConfigOwnership.PLAYBACK_TIMING -> R.string.pref_mpv_conf_ownership_playback_timing_summary
    MpvConfigOwnership.NETWORK_BUFFERING -> R.string.pref_mpv_conf_ownership_network_buffering_summary
  }
