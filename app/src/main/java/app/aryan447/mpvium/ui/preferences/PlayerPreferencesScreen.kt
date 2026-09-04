package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.player.PlayerOrientation
import app.aryan447.mpvium.ui.player.controls.components.sheets.toFixed
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import org.koin.compose.koinInject

@Serializable
object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(id = R.string.pref_player))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          // General Section
          item {
            PreferenceSectionHeader(title = "General")
          }

          item {
            PreferenceCard {
              val orientation by preferences.orientation.collectAsState()
              ListPreference(
                value = orientation,
                onValueChange = preferences.orientation::set,
                values = PlayerOrientation.entries,
                valueToText = { AnnotatedString(context.getString(it.titleRes)) },
                title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
                summary = {
                  Text(
                    text = stringResource(id = orientation.titleRes),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
              HapticSwitchPreference(
                value = savePositionOnQuit,
                onValueChange = preferences.savePositionOnQuit::set,
                title = { Text(stringResource(R.string.pref_player_save_position_on_quit)) },
              )

              PreferenceDivider()

              val closeAfterEndOfVideo by preferences.closeAfterReachingEndOfVideo.collectAsState()
              HapticSwitchPreference(
                value = closeAfterEndOfVideo,
                onValueChange = preferences.closeAfterReachingEndOfVideo::set,
                title = { Text(stringResource(id = R.string.pref_player_close_after_eof)) },
              )

              PreferenceDivider()

              val autoplayNextVideo by preferences.autoplayNextVideo.collectAsState()
              HapticSwitchPreference(
                value = autoplayNextVideo,
                onValueChange = preferences.autoplayNextVideo::set,
                title = { Text(text = "Autoplay next video") },
                summary = {
                  Text(
                    text = if (autoplayNextVideo)
                      "Automatically play next video when current ends"
                    else
                      "Stay on current video when it ends",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val playlistMode by preferences.playlistMode.collectAsState()
              HapticSwitchPreference(
                value = playlistMode,
                onValueChange = preferences.playlistMode::set,
                title = { Text(text = "Enable next/previous navigation") },
                summary = {
                  Text(
                    text = if (playlistMode)
                      "Show next/previous buttons for all videos in folder"
                    else
                      "Play videos individually (select multiple for playlist)",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val rememberBrightness by preferences.rememberBrightness.collectAsState()
              HapticSwitchPreference(
                value = rememberBrightness,
                onValueChange = preferences.rememberBrightness::set,
                title = { Text(text = stringResource(R.string.pref_player_remember_brightness)) },
              )

              PreferenceDivider()

              val autoPiPOnNavigation by preferences.autoPiPOnNavigation.collectAsState()
              HapticSwitchPreference(
                value = autoPiPOnNavigation,
                onValueChange = preferences.autoPiPOnNavigation::set,
                title = { Text("Auto Picture-in-Picture") },
                summary = {
                  Text(
                    text = "Automatically enter PIP mode when pressing home or back",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val keepScreenOnWhenPaused by preferences.keepScreenOnWhenPaused.collectAsState()
              HapticSwitchPreference(
                value = keepScreenOnWhenPaused,
                onValueChange = preferences.keepScreenOnWhenPaused::set,
                title = { Text("Keep screen on when paused") },
                summary = {
                  Text(
                    text = if (keepScreenOnWhenPaused)
                      "Screen stays awake while video is paused"
                    else
                      "Screen can turn off while video is paused",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }
          // Seeking Section
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_seeking_title))
          }

          item {
            PreferenceCard {
              val showDoubleTapOvals by preferences.showDoubleTapOvals.collectAsState()
              HapticSwitchPreference(
                value = showDoubleTapOvals,
                onValueChange = preferences.showDoubleTapOvals::set,
                title = { Text(stringResource(R.string.show_splash_ovals_on_double_tap_to_seek)) },
              )

              PreferenceDivider()

              val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
              HapticSwitchPreference(
                value = showSeekTimeWhileSeeking,
                onValueChange = preferences.showSeekTimeWhileSeeking::set,
                title = { Text(stringResource(R.string.show_time_on_double_tap_to_seek)) },
              )

              PreferenceDivider()

              val usePreciseSeeking by preferences.usePreciseSeeking.collectAsState()
              HapticSwitchPreference(
                value = usePreciseSeeking,
                onValueChange = preferences.usePreciseSeeking::set,
                title = { Text(stringResource(R.string.pref_player_use_precise_seeking)) },
              )

              PreferenceDivider()

              val customSkipDuration by preferences.customSkipDuration.collectAsState()
              SliderPreference(
                value = customSkipDuration.toFloat(),
                onValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                title = { Text(stringResource(R.string.pref_player_custom_skip_duration_title)) },
                valueRange = 5f..180f,
                summary = {
                   val summaryText = stringResource(R.string.pref_player_custom_skip_duration_summary)
                   Text(
                     "$summaryText ($customSkipDuration s)",
                     color = MaterialTheme.colorScheme.outline,
                   )
                },
                onSliderValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                sliderValue = customSkipDuration.toFloat(),
              )

              PreferenceDivider()

              val skipIntroDuration by preferences.skipIntroDuration.collectAsState()
              SliderPreference(
                value = skipIntroDuration.toFloat(),
                onValueChange = { preferences.skipIntroDuration.set(it.roundToInt()) },
                title = { Text("Skip intro duration") },
                valueRange = 5f..300f,
                summary = { Text("Skip forward $skipIntroDuration s", color = MaterialTheme.colorScheme.outline) },
                onSliderValueChange = { preferences.skipIntroDuration.set(it.roundToInt()) },
                sliderValue = skipIntroDuration.toFloat(),
              )

              PreferenceDivider()

              val skipRecapDuration by preferences.skipRecapDuration.collectAsState()
              SliderPreference(
                value = skipRecapDuration.toFloat(),
                onValueChange = { preferences.skipRecapDuration.set(it.roundToInt()) },
                title = { Text("Skip recap duration") },
                valueRange = 5f..300f,
                summary = { Text("Skip forward $skipRecapDuration s", color = MaterialTheme.colorScheme.outline) },
                onSliderValueChange = { preferences.skipRecapDuration.set(it.roundToInt()) },
                sliderValue = skipRecapDuration.toFloat(),
              )
            }
          }
          // Gestures Section
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_gestures))
          }

          item {
            PreferenceCard {
              val brightnessGesture by preferences.brightnessGesture.collectAsState()
              HapticSwitchPreference(
                value = brightnessGesture,
                onValueChange = preferences.brightnessGesture::set,
                title = { Text(stringResource(R.string.pref_player_gestures_brightness)) },
              )

              PreferenceDivider()

              val volumeGesture by preferences.volumeGesture.collectAsState()
              HapticSwitchPreference(
                value = volumeGesture,
                onValueChange = preferences.volumeGesture::set,
                title = { Text(stringResource(R.string.pref_player_gestures_volume)) },
              )

              PreferenceDivider()

              val pinchToZoomGesture by preferences.pinchToZoomGesture.collectAsState()
              HapticSwitchPreference(
                value = pinchToZoomGesture,
                onValueChange = preferences.pinchToZoomGesture::set,
                title = { Text(stringResource(R.string.pref_player_gestures_pinch_to_zoom)) },
              )

              PreferenceDivider()

              val horizontalSwipeToSeek by preferences.horizontalSwipeToSeek.collectAsState()
              HapticSwitchPreference(
                value = horizontalSwipeToSeek,
                onValueChange = preferences.horizontalSwipeToSeek::set,
                title = { Text(stringResource(R.string.pref_player_gestures_horizontal_swipe_to_seek)) },
              )

              PreferenceDivider()

              val horizontalSwipeSensitivity by preferences.horizontalSwipeSensitivity.collectAsState()
              SliderPreference(
                value = horizontalSwipeSensitivity,
                onValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                title = { Text(stringResource(R.string.pref_player_gestures_horizontal_swipe_sensitivity)) },
                valueRange = 0.020f..0.1f,
                summary = {
                  val sensitivityPercent = (horizontalSwipeSensitivity * 1000).toInt()
                  Text(
                    "Current: ${sensitivityPercent}/100 (${if (sensitivityPercent < 30) "Low" else if (sensitivityPercent < 55) "Medium" else "High"})",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onSliderValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                sliderValue = horizontalSwipeSensitivity,
              )

              PreferenceDivider()

              val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
              SliderPreference(
                value = holdForMultipleSpeed,
                onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                title = { Text(stringResource(R.string.pref_player_gestures_hold_for_multiple_speed)) },
                valueRange = 0f..6f,
                summary = {
                  Text(
                    if (holdForMultipleSpeed == 0F) {
                      stringResource(R.string.generic_disabled)
                    } else {
                      "%.2fx".format(holdForMultipleSpeed)
                    },
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onSliderValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                sliderValue = holdForMultipleSpeed,
              )

              PreferenceDivider()

              val showDynamicSpeedOverlay by preferences.showDynamicSpeedOverlay.collectAsState()
              HapticSwitchPreference(
                value = showDynamicSpeedOverlay,
                onValueChange = preferences.showDynamicSpeedOverlay::set,
                title = { Text("Dynamic Speed Overlay") },
                summary = {
                  Text(
                    "Show advance overlay for speed control during long press and swipe",
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
              )
            }
          }
          // Controls Section
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_controls))
          }

          item {
            PreferenceCard {
              val allowGesturesInPanels by preferences.allowGesturesInPanels.collectAsState()
              HapticSwitchPreference(
                value = allowGesturesInPanels,
                onValueChange = preferences.allowGesturesInPanels::set,
                title = {
                  Text(
                    text = stringResource(id = R.string.pref_player_controls_allow_gestures_in_panels),
                  )
                },
              )

              PreferenceDivider()

              val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
              HapticSwitchPreference(
                value = swapVolumeAndBrightness,
                onValueChange = preferences.swapVolumeAndBrightness::set,
                title = { Text(stringResource(R.string.swap_the_volume_and_brightness_slider)) },
              )

              PreferenceDivider()

              val showLoadingCircle by preferences.showLoadingCircle.collectAsState()
              HapticSwitchPreference(
                value = showLoadingCircle,
                onValueChange = preferences.showLoadingCircle::set,
                title = { Text(stringResource(R.string.pref_player_controls_show_loading_circle)) },
              )
            }
          }
          // Display Section
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_display))
          }

          item {
            PreferenceCard {
              val showSystemStatusBar by preferences.showSystemStatusBar.collectAsState()
              HapticSwitchPreference(
                value = showSystemStatusBar,
                onValueChange = preferences.showSystemStatusBar::set,
                title = { Text(stringResource(R.string.pref_player_display_show_status_bar)) },
              )

              PreferenceDivider()

              val showSystemNavigationBar by preferences.showSystemNavigationBar.collectAsState()
              HapticSwitchPreference(
                value = showSystemNavigationBar,
                onValueChange = preferences.showSystemNavigationBar::set,
                title = { Text("Show navigation bar with controls") },
              )

              PreferenceDivider()

              val reduceMotion by preferences.reduceMotion.collectAsState()
              HapticSwitchPreference(
                value = reduceMotion,
                onValueChange = preferences.reduceMotion::set,
                title = { Text(stringResource(R.string.pref_player_display_reduce_player_animation)) },
              )
            }
          }
        }
      }
    }
  }
}
