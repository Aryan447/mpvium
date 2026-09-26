package app.aryan447.mpvium.ui.preferences

// import androidx.compose.material.icons.outlined.VideoLabel // No longer needed here
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.SeekbarStyle
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.preferences.components.PlayerButtonChip
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

// Enum to identify which region we are editing
@Serializable
enum class ControlRegion {
  TOP_RIGHT,
  BOTTOM_RIGHT,
  BOTTOM_LEFT,
  PORTRAIT_BOTTOM,
}

@Serializable
object PlayerControlsPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val appearancePrefs = koinInject<AppearancePreferences>()
    val playerPrefs = koinInject<PlayerPreferences>()

    // Get the current state for all four regions
    val topRState by appearancePrefs.topRightControls.collectAsState()
    val bottomRState by appearancePrefs.bottomRightControls.collectAsState()
    val bottomLState by appearancePrefs.bottomLeftControls.collectAsState()
    val portraitBottomState by appearancePrefs.portraitBottomControls.collectAsState()

    val topRightButtons = remember(topRState) {
      appearancePrefs.parseButtons(topRState, mutableSetOf())
    }

    val bottomRightButtons = remember(bottomRState) {
      appearancePrefs.parseButtons(bottomRState, mutableSetOf())
    }

    val bottomLeftButtons = remember(bottomLState) {
      appearancePrefs.parseButtons(bottomLState, mutableSetOf())
    }

    val portraitBottomButtons = remember(portraitBottomState) {
      appearancePrefs.parseButtons(portraitBottomState, mutableSetOf())
    }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(id = R.string.pref_layout_title))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          // Landscape Controls Section
          item {
            PreferenceSectionHeader(title = "Landscape Controls", count = 3)
          }

          item {
            PreferenceCard {
              PreferenceCategoryWithEditButton(
                title = stringResource(id = R.string.pref_layout_top_right_controls),
                onClick = {
                  backstack.add(ControlLayoutEditorScreen(ControlRegion.TOP_RIGHT))
                },
              )
              PreferenceIconSummary(buttons = topRightButtons)

              PreferenceDivider()

              PreferenceCategoryWithEditButton(
                title = stringResource(id = R.string.pref_layout_bottom_right_controls),
                onClick = {
                  backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_RIGHT))
                },
              )
              PreferenceIconSummary(buttons = bottomRightButtons)

              PreferenceDivider()

              PreferenceCategoryWithEditButton(
                title = stringResource(id = R.string.pref_layout_bottom_left_controls),
                onClick = {
                  backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_LEFT))
                },
              )
              PreferenceIconSummary(buttons = bottomLeftButtons)
            }
          }

          // Portrait Controls Section
          item {
            PreferenceSectionHeader(title = "Portrait Controls", count = 1)
          }

          item {
            PreferenceCard {


              PreferenceCategoryWithEditButton(
                title = stringResource(id = R.string.pref_layout_portrait_bottom_controls),
                onClick = {
                  backstack.add(ControlLayoutEditorScreen(ControlRegion.PORTRAIT_BOTTOM))
                },
              )
              PreferenceIconSummary(buttons = portraitBottomButtons)
            }
          }

          // Seekbar Section
          item {
            PreferenceSectionHeader(title = "Seekbar Style", count = SeekbarStyle.entries.size)
          }

          item {
            val seekbarStyle by appearancePrefs.seekbarStyle.collectAsState()
            SliderStylePicker(
              selected = seekbarStyle,
              onSelect = { appearancePrefs.seekbarStyle.set(it) },
            )
          }

          // Volume Slider Style Section
          item {
            PreferenceSectionHeader(title = "Volume Slider Style", count = SeekbarStyle.entries.size)
          }

          item {
            val volumeSliderStyle by appearancePrefs.volumeSliderStyle.collectAsState()
            SliderStylePicker(
              selected = volumeSliderStyle,
              onSelect = { appearancePrefs.volumeSliderStyle.set(it) },
            )
          }

          // Brightness Slider Style Section
          item {
            PreferenceSectionHeader(title = "Brightness Slider Style", count = SeekbarStyle.entries.size)
          }

          item {
            val brightnessSliderStyle by appearancePrefs.brightnessSliderStyle.collectAsState()
            SliderStylePicker(
              selected = brightnessSliderStyle,
              onSelect = { appearancePrefs.brightnessSliderStyle.set(it) },
            )
          }

          // Appearance Section
          item {
            PreferenceSectionHeader(title = "Appearance", count = 2)
          }

          item {
            val hidePlayerButtonsBackground by appearancePrefs.hidePlayerButtonsBackground.collectAsState()
            val playerTimeToDisappear by playerPrefs.playerTimeToDisappear.collectAsState()
            val predefinedTimeValues = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
            val isCustomTimeValue = !predefinedTimeValues.contains(playerTimeToDisappear)

            var showCustomTimeDialog by remember { mutableStateOf(false) }
            var customTimeValue by remember { mutableStateOf("") }

            PreferenceCard {
              HapticSwitchPreference(
                value = hidePlayerButtonsBackground,
                onValueChange = { appearancePrefs.hidePlayerButtonsBackground.set(it) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.VisibilityOff) },
                title = {
                  Text(
                    text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_title),
                  )
                },
                summary = {
                  Text(
                    text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_summary),
                  )
                },
              )

              PreferenceDivider()

              ListPreference(
                value = if (isCustomTimeValue) -1 else playerTimeToDisappear,
                onValueChange = { newValue ->
                  if (newValue == -1) {
                    customTimeValue = playerTimeToDisappear.toString()
                    showCustomTimeDialog = true
                  } else {
                    playerPrefs.playerTimeToDisappear.set(newValue)
                  }
                },
                values = predefinedTimeValues + listOf(-1),
                valueToText = { value ->
                  if (value == -1) {
                    AnnotatedString("Custom")
                  } else {
                    AnnotatedString("$value ms")
                  }
                },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Timer) },
                title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
                summary = {
                  Text(
                    text = if (isCustomTimeValue) {
                      "Custom ($playerTimeToDisappear ms)"
                    } else {
                      "$playerTimeToDisappear ms"
                    },
                  )
                },
              )
            }

            if (showCustomTimeDialog) {
              AlertDialog(
                onDismissRequest = { showCustomTimeDialog = false },
                containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
                title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
                text = {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .verticalScroll(rememberScrollState()),
                  ) {
                    Text(
                      text = "Enter custom hide time in milliseconds",
                      modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                      value = customTimeValue,
                      onValueChange = { customTimeValue = it },
                      label = { Text("Milliseconds") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.fillMaxWidth(),
                      singleLine = true,
                    )
                  }
                },
                confirmButton = {
                  TextButton(
                    onClick = {
                      val value = customTimeValue.toIntOrNull()
                      if (value != null && value in 100..1000000000000) {
                        playerPrefs.playerTimeToDisappear.set(value)
                        showCustomTimeDialog = false
                      }
                    },
                  ) {
                    Text(stringResource(R.string.generic_ok))
                  }
                },
                dismissButton = {
                  TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text(stringResource(R.string.generic_cancel))
                  }
                },
              )
            }
          }
        }
      }
    }
  }

  /**
   * Category row with a circular edit affordance; the whole row opens
   * the layout editor, matching SettingsPreferenceRow.
   */
  @Composable
  private fun PreferenceCategoryWithEditButton(
    title: String,
    onClick: () -> Unit,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "Edit $title",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }

  /**
   * Radio group for picking a [SeekbarStyle], shared by the seekbar,
   * volume slider and brightness slider sections. Each row shows a live
   * mini track preview so the style reads at a glance.
   */
  @Composable
  private fun SliderStylePicker(
    selected: SeekbarStyle,
    onSelect: (SeekbarStyle) -> Unit,
  ) {
    PreferenceCard {
      SeekbarStyle.entries.forEachIndexed { index, style ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(style) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SliderStylePreview(style = style)
          Spacer(modifier = Modifier.size(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = style.label,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
            )
            Text(
              text = sliderStyleHint(style),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
          RadioButton(
            selected = selected == style,
            onClick = null,
          )
        }
        if (index < SeekbarStyle.entries.size - 1) {
          PreferenceDivider()
        }
      }
    }
  }

  private fun sliderStyleHint(style: SeekbarStyle): String = when (style) {
    SeekbarStyle.Standard -> "Classic thin track"
    SeekbarStyle.Wavy -> "Playful squiggle"
    SeekbarStyle.Thick -> "Bold full-height bar"
    SeekbarStyle.Slim -> "Extra-thin minimal"
    SeekbarStyle.NeonGlow -> "Glowing halo track"
    SeekbarStyle.Segmented -> "Ticked intervals"
    SeekbarStyle.RetroBlocky -> "Chunky square blocks"
  }

  /**
   * Miniature non-interactive preview of a [SeekbarStyle] track.
   */
  @Composable
  private fun SliderStylePreview(style: SeekbarStyle) {
    val primary = MaterialTheme.colorScheme.primary
    val trackHeight = when (style) {
      SeekbarStyle.Slim -> 2.dp
      SeekbarStyle.Standard, SeekbarStyle.Wavy -> 4.dp
      SeekbarStyle.NeonGlow -> 6.dp
      SeekbarStyle.Segmented -> 8.dp
      SeekbarStyle.RetroBlocky -> 12.dp
      SeekbarStyle.Thick -> 16.dp
    }
    val square = style == SeekbarStyle.RetroBlocky
    val shape = if (square) RoundedCornerShape(0.dp) else CircleShape
    Box(
      modifier = Modifier.size(width = 64.dp, height = 28.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (style == SeekbarStyle.NeonGlow) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight + 8.dp)
            .background(primary.copy(alpha = 0.15f), CircleShape),
        )
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(trackHeight)
          .background(primary.copy(alpha = 0.3f), shape),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(trackHeight)
            .background(primary, shape)
            .align(Alignment.CenterStart),
        )
      }
      if (style == SeekbarStyle.Segmented) {
        Box(
          modifier = Modifier
            .size(width = 2.dp, height = trackHeight)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        )
      }
      val thumbSize = if (style == SeekbarStyle.Slim) 6.dp else 8.dp
      Box(
        modifier = Modifier
          .size(thumbSize)
          .background(primary, if (square) RoundedCornerShape(2.dp) else CircleShape)
          .align(Alignment.Center),
      )
    }
  }

  /**
   * Custom composable to show a row of icons for the summary.
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  private fun PreferenceIconSummary(buttons: List<PlayerButton>) {
    FlowRow(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp), // Increased spacing
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
      if (buttons.isEmpty()) {
        Text(
          "None", // TODO: strings
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        buttons.forEach { button ->
          // Use the chip in "preview mode" (no badge, enabled=true but no onClick)
          PlayerButtonChip(
            button = button,
            enabled = true,
            onClick = null,
            badgeIcon = null,
            badgeColor = null
          )
        }
      }
    }
  }
}
