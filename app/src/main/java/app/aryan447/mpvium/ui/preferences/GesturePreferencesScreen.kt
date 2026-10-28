package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.GesturePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.player.SingleActionGesture
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

/**
 * Registry key -> LazyColumn item index for the items that hold searchable rows.
 */
private val gestureAnchors: Map<String, Int> = mapOf(
  "gestures.page" to 0,
  "gestures.double_tap_seek_duration" to 1,
  "gestures.double_tap_seek_area" to 1,
  "gestures.double_tap_left" to 1,
  "gestures.double_tap_center" to 1,
  "gestures.double_tap_right" to 1,
  "gestures.single_tap_center" to 1,
  "gestures.media_previous" to 3,
  "gestures.media_play" to 3,
  "gestures.media_next" to 3,
)

@Serializable
object GesturePreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<GesturePreferences>()
    val context = LocalContext.current
    val useSingleTapForCenter by preferences.useSingleTapForCenter.collectAsState()
    val listState = rememberLazyListState()

    SettingsHighlightEffect(
      screen = GesturePreferencesScreen,
      listState = listState,
      anchors = gestureAnchors,
    )

    var showCustomSeekDialog by remember { mutableStateOf(false) }
    var customSeekValue by remember { mutableStateOf("") }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_gesture))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          state = listState,
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_gesture_double_tap_title), count = 6)
          }

          item {
            PreferenceCard {
          val doubleTapSeekDuration by preferences.doubleTapToSeekDuration.collectAsState()
          val predefinedValues = listOf(3, 5, 10, 15, 20, 25, 30)
          val isCustomValue = !predefinedValues.contains(doubleTapSeekDuration)

          ExpressiveListPreference(
            value = if (isCustomValue) -1 else doubleTapSeekDuration,
            onValueChange = { newValue ->
              if (newValue == -1) {
                customSeekValue = doubleTapSeekDuration.toString()
                showCustomSeekDialog = true
              } else {
                preferences.doubleTapToSeekDuration.set(newValue)
              }
            },
            values = predefinedValues + listOf(-1),
            modifier = Modifier.settingsHighlight("gestures.double_tap_seek_duration"),
            valueToText = { value ->
              if (value == -1) {
                AnnotatedString("Custom")
              } else {
                AnnotatedString("${value}s")
              }
            },
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.Timer) },
            summary = {
              Text(
                text = if (isCustomValue) {
                  "Custom (${doubleTapSeekDuration}s)"
                } else {
                  "${doubleTapSeekDuration}s"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          PreferenceDivider()

          if (showCustomSeekDialog) {
            AlertDialog(
              onDismissRequest = { showCustomSeekDialog = false },
              containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
              title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration)) },
              text = {
                Column {
                  Text(
                    text = "Enter custom seek duration in seconds (1-120)",
                    modifier = Modifier.padding(bottom = 8.dp),
                  )
                  OutlinedTextField(
                    value = customSeekValue,
                    onValueChange = { customSeekValue = it },
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                  )
                }
              },
              confirmButton = {
                TextButton(
                  onClick = {
                    val value = customSeekValue.toIntOrNull()
                    if (value != null && value in 1..120) {
                      preferences.doubleTapToSeekDuration.set(value)
                      showCustomSeekDialog = false
                    }
                  },
                ) {
                  Text(stringResource(R.string.generic_ok))
                }
              },
              dismissButton = {
                TextButton(onClick = { showCustomSeekDialog = false }) {
                  Text(stringResource(R.string.generic_cancel))
                }
              },
            )
          }

          val doubleTapSeekAreaWidth by preferences.doubleTapSeekAreaWidth.collectAsState()
          val seekAreaValues = listOf(20, 25, 30, 35, 40, 45)

          ExpressiveListPreference(
            value = doubleTapSeekAreaWidth,
            onValueChange = { preferences.doubleTapSeekAreaWidth.set(it) },
            values = seekAreaValues,
            modifier = Modifier.settingsHighlight("gestures.double_tap_seek_area"),
            valueToText = { AnnotatedString("${it}%") },
            title = { Text(text = "Double Tap Seek Area Width") },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.Tune) },
            summary = {
              Text(
                text = "Current: ${doubleTapSeekAreaWidth}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          PreferenceDivider()

          val leftDoubleTap by preferences.leftSingleActionGesture.collectAsState()
          ExpressiveListPreference(
            value = leftDoubleTap,
            onValueChange = { preferences.leftSingleActionGesture.set(it) },
            values = SingleActionGesture.entries,
            modifier = Modifier.settingsHighlight("gestures.double_tap_left"),
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_left_title)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.SwipeLeft) },
            summary = { Text(
              text = stringResource(leftDoubleTap.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          PreferenceDivider()

          val centerDoubleTap by preferences.centerSingleActionGesture.collectAsState()
          ExpressiveListPreference(
            value = centerDoubleTap,
            onValueChange = { preferences.centerSingleActionGesture.set(it) },
            modifier = Modifier.settingsHighlight("gestures.double_tap_center"),
            values =
              listOf(
                SingleActionGesture.None,
                SingleActionGesture.PlayPause,
                SingleActionGesture.Custom,
              ),
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.TouchApp) },
            title = {
              Text(
                text =
                  stringResource(
                    if (useSingleTapForCenter) R.string.pref_gesture_single_tap_center_title else R.string.pref_gesture_double_tap_center_title,
                  ),
              )
            },
            summary = { Text(
              text = stringResource(centerDoubleTap.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          PreferenceDivider()

          val rightDoubleTap by preferences.rightSingleActionGesture.collectAsState()
          ExpressiveListPreference(
            value = rightDoubleTap,
            onValueChange = { preferences.rightSingleActionGesture.set(it) },
            modifier = Modifier.settingsHighlight("gestures.double_tap_right"),
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_right_title)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.SwipeRight) },
            summary = { Text(
              text = stringResource(rightDoubleTap.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          PreferenceDivider()

          HapticSwitchPreference(
            value = useSingleTapForCenter,
            onValueChange = { preferences.useSingleTapForCenter.set(it) },
            highlightKey = "gestures.single_tap_center",
            icon = { PreferenceRowIcon(icon = Icons.Outlined.Gesture) },
            title = {
              Text(
                text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_title),
              )
            },
            summary = {
              Text(
                text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          FooterPreference(
            summary = {
              Text(
                text = stringResource(R.string.pref_gesture_double_tap_custom_info),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )
            }
          }

          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_gesture_media_title), count = 3)
          }

          item {
            PreferenceCard {
          val mediaPreviousGesture by preferences.mediaPreviousGesture.collectAsState()
          ExpressiveListPreference(
            value = mediaPreviousGesture,
            onValueChange = { preferences.mediaPreviousGesture.set(it) },
            modifier = Modifier.settingsHighlight("gestures.media_previous"),
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_previous)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.Replay10) },
            summary = { Text(
              text = stringResource(mediaPreviousGesture.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          PreferenceDivider()
          val mediaPlayGesture by preferences.mediaPlayGesture.collectAsState()
          ExpressiveListPreference(
            value = mediaPlayGesture,
            onValueChange = { preferences.mediaPlayGesture.set(it) },
            modifier = Modifier.settingsHighlight("gestures.media_play"),
            values =
              listOf(
                SingleActionGesture.None,
                SingleActionGesture.PlayPause,
                SingleActionGesture.Custom,
              ),
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_play)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.PlayArrow) },
            summary = { Text(
              text = stringResource(mediaPlayGesture.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          PreferenceDivider()
          val mediaNextGesture by preferences.mediaNextGesture.collectAsState()
          ExpressiveListPreference(
            value = mediaNextGesture,
            onValueChange = { preferences.mediaNextGesture.set(it) },
            modifier = Modifier.settingsHighlight("gestures.media_next"),
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_next)) },
            icon = { PreferenceRowIcon(icon = Icons.Outlined.Forward10) },
            summary = { Text(
              text = stringResource(mediaNextGesture.titleRes),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
          )

          FooterPreference(
            summary = {
              Text(
                text = stringResource(R.string.pref_gesture_media_custom_info),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )
            }
          }

          // View Section removed - "Tap thumbnail to select" moved to Appearance Preferences
        }
      }
    }
  }
}
