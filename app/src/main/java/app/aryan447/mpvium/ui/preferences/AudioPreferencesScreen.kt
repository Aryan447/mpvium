package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AudioChannels
import app.aryan447.mpvium.preferences.AudioPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.koinInject

@Serializable
object AudioPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val preferences = koinInject<AudioPreferences>()

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_audio))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_audio))
          }

          item {
            PreferenceCard {
          val preferredLanguages by preferences.preferredLanguages.collectAsState()
          TextFieldPreference(
            value = preferredLanguages,
            onValueChange = { preferences.preferredLanguages.set(it) },
            textToValue = { it },
            title = { Text(stringResource(R.string.pref_preferred_languages)) },
            summary = {
                if (preferredLanguages.isNotBlank()) {
                  Text(
                    preferredLanguages,
                    color = MaterialTheme.colorScheme.outline,
                  )
                } else {
                  Text(
                    stringResource(R.string.not_set_video_default),
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
              },
            textField = { value, onValueChange, _ ->
              Column {
                Text(stringResource(R.string.pref_audio_preferred_language))
                TextField(
                  value,
                  onValueChange,
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            },
          )

          PreferenceDivider()
          val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
          HapticSwitchPreference(
            value = audioPitchCorrection,
            onValueChange = { preferences.audioPitchCorrection.set(it) },
            title = { Text(stringResource(R.string.pref_audio_pitch_correction_title)) },
            summary = {
              Text(
                stringResource(R.string.pref_audio_pitch_correction_summary),
                color = MaterialTheme.colorScheme.outline,
              )
            },
          )

          PreferenceDivider()
          val volumeNormalization by preferences.volumeNormalization.collectAsState()
          HapticSwitchPreference(
            value = volumeNormalization,
            onValueChange = { preferences.volumeNormalization.set(it) },
            title = { Text(stringResource(R.string.pref_audio_volume_normalization_title)) },
            summary = {
              Text(
                stringResource(R.string.pref_audio_volume_normalization_summary),
                color = MaterialTheme.colorScheme.outline,
              )
            },
          )

          PreferenceDivider()
          val automaticBackgroundPlayback by preferences.automaticBackgroundPlayback.collectAsState()
          HapticSwitchPreference(
            value = automaticBackgroundPlayback,
            onValueChange = { preferences.automaticBackgroundPlayback.set(it) },
            title = { Text(stringResource(R.string.background_playback_title)) },
          )

          PreferenceDivider()
          val audioChannel by preferences.audioChannels.collectAsState()
          ListPreference(
            value = audioChannel,
            onValueChange = { preferences.audioChannels.set(it) },
            values = AudioChannels.entries,
            valueToText = { AnnotatedString(context.getString(it.title)) },
            title = { Text(text = stringResource(id = R.string.pref_audio_channels)) },
            summary = {
              Text(
                text = context.getString(audioChannel.title),
                color = MaterialTheme.colorScheme.outline,
              )
            },
          )

          PreferenceDivider()
          val volumeBoostCap by preferences.volumeBoostCap.collectAsState()
          SliderPreference(
            value = volumeBoostCap.toFloat(),
            onValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
            title = { Text(stringResource(R.string.pref_audio_volume_boost_cap)) },
            valueRange = 0f..200f,
            summary = {
              Text(
                if (volumeBoostCap == 0) {
                  stringResource(R.string.generic_disabled)
                } else {
                  volumeBoostCap.toString()
                },
                color = MaterialTheme.colorScheme.outline,
              )
            },
            onSliderValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
            sliderValue = volumeBoostCap.toFloat(),
          )

          PreferenceDivider()
          val volumeStep by preferences.volumeStep.collectAsState()
          SliderPreference(
            value = volumeStep.toFloat(),
            onValueChange = { preferences.volumeStep.set(it.toInt()) },
            title = { Text(stringResource(R.string.pref_audio_volume_step_title)) },
            valueRange = 1f..5f,
            valueSteps = 3,
            summary = {
              Text(
                stringResource(R.string.pref_audio_volume_step_summary, volumeStep),
                color = MaterialTheme.colorScheme.outline,
              )
            },
            onSliderValueChange = { preferences.volumeStep.set(it.toInt()) },
            sliderValue = volumeStep.toFloat(),
          )

          PreferenceDivider()
          val volumeMinLimit by preferences.volumeMinLimit.collectAsState()
          SliderPreference(
            value = volumeMinLimit.toFloat(),
            onValueChange = { preferences.volumeMinLimit.set(it.toInt()) },
            title = { Text(stringResource(R.string.pref_audio_volume_min_limit_title)) },
            valueRange = 0f..100f,
            summary = {
              Text(
                stringResource(R.string.pref_audio_volume_min_limit_summary, volumeMinLimit),
                color = MaterialTheme.colorScheme.outline,
              )
            },
            onSliderValueChange = { preferences.volumeMinLimit.set(it.toInt()) },
            sliderValue = volumeMinLimit.toFloat(),
          )

          PreferenceDivider()
          val volumeMaxLimit by preferences.volumeMaxLimit.collectAsState()
          SliderPreference(
            value = volumeMaxLimit.toFloat(),
            onValueChange = { preferences.volumeMaxLimit.set(it.toInt()) },
            title = { Text(stringResource(R.string.pref_audio_volume_max_limit_title)) },
            valueRange = 0f..100f,
            summary = {
              Text(
                stringResource(R.string.pref_audio_volume_max_limit_summary, volumeMaxLimit),
                color = MaterialTheme.colorScheme.outline,
              )
            },
            onSliderValueChange = { preferences.volumeMaxLimit.set(it.toInt()) },
            sliderValue = volumeMaxLimit.toFloat(),
          )
            }
          }
        }
      }
    }
  }
}
