package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import app.aryan447.mpvium.ui.utils.LocalBackStack
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
              isVertical = true,
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
              isVertical = true,
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
                icon = { PreferenceRowIcon(icon = Icons.Outlined.VisibilityOff) },
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
                icon = { PreferenceRowIcon(icon = Icons.Outlined.Timer) },
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
   * Custom composable for the category header with an Edit button.
   */
  @Composable
  private fun PreferenceCategoryWithEditButton(
    title: String,
    onClick: () -> Unit,
  ) {
    Row(
      modifier =
        Modifier
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
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            CircleShape,
          ),
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

  private fun sliderStyleHint(style: SeekbarStyle): String =
    when (style) {
      SeekbarStyle.Standard -> "Classic thin track"
      SeekbarStyle.Wavy -> "Playful squiggle"
      SeekbarStyle.Thick -> "Bold full-height bar"
    }

  /**
   * Static miniature of the real player renderers at a fixed 60% progress.
   * Mirrors Seekbar.kt (StandardSeekbar/SquigglySeekbar) and
   * VerticalSliders.kt (Standard/Thick/WavyVerticalBar): same track widths,
   * thumb shapes, wave parameters and primary colors, frozen with no
   * animation or gesture handling. Horizontal for the seekbar, vertical
   * for volume/brightness which are vertical in the player.
   */
  @Composable
  private fun SliderStylePreview(style: SeekbarStyle, isVertical: Boolean = false) {
    if (isVertical) {
      Box(
        modifier = Modifier.size(width = 64.dp, height = 64.dp),
        contentAlignment = Alignment.Center,
      ) {
        VerticalMiniPreview(style = style)
      }
    } else {
      Box(
        modifier = Modifier.size(width = 64.dp, height = 28.dp),
        contentAlignment = Alignment.Center,
      ) {
        HorizontalMiniPreview(style = style)
      }
    }
  }

  @Composable
  private fun HorizontalMiniPreview(style: SeekbarStyle) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.size(width = 64.dp, height = 28.dp)) {
      val progress = 0.6f
      val totalWidth = size.width
      val progressPx = totalWidth * progress
      val centerY = size.height / 2f
      val played = scheme.primary
      val unplayed = scheme.primary.copy(alpha = 0.3f)
      // Wavy unplayed uses 77/255 like SquigglySeekbar.
      val wavyUnplayed = scheme.primary.copy(alpha = 77f / 255f)

      when (style) {
        SeekbarStyle.Standard -> {
          val trackHeight = 4.dp.toPx()
          val outerRadius = trackHeight / 2f
          val top = centerY - trackHeight / 2f
          fun hSegment(startX: Float, endX: Float, leftRound: Float, rightRound: Float, color: androidx.compose.ui.graphics.Color) {
            if (endX - startX < 0.5f) return
            val path = Path()
            path.addRoundRect(
              RoundRect(
                left = startX,
                top = top,
                right = endX,
                bottom = top + trackHeight,
                topLeftCornerRadius = CornerRadius(leftRound),
                bottomLeftCornerRadius = CornerRadius(leftRound),
                topRightCornerRadius = CornerRadius(rightRound),
                bottomRightCornerRadius = CornerRadius(rightRound),
              ),
            )
            drawPath(path, color)
          }
          // Standard: square inner corners, rounded outer ends.
          hSegment(0f, progressPx, outerRadius, 0f, played)
          hSegment(progressPx, totalWidth, 0f, outerRadius, unplayed)
          drawCircle(
            color = played,
            radius = 7.dp.toPx(),
            center = Offset(progressPx.coerceIn(0f, totalWidth), centerY),
          )
        }
        SeekbarStyle.Thick -> {
          val trackHeight = 16.dp.toPx()
          val outerRadius = trackHeight / 2f
          val gapHalf = 7.dp.toPx()
          val top = centerY - trackHeight / 2f
          fun hSegment(startX: Float, endX: Float, color: androidx.compose.ui.graphics.Color) {
            if (endX - startX < 0.5f) return
            val path = Path()
            path.addRoundRect(
              RoundRect(
                left = startX,
                top = top,
                right = endX,
                bottom = top + trackHeight,
                topLeftCornerRadius = CornerRadius(outerRadius),
                bottomLeftCornerRadius = CornerRadius(outerRadius),
                topRightCornerRadius = CornerRadius(outerRadius),
                bottomRightCornerRadius = CornerRadius(outerRadius),
              ),
            )
            drawPath(path, color)
          }
          // Thick: split track with a 14dp thumb gap, inner corners fully rounded.
          hSegment(0f, progressPx - gapHalf, played)
          hSegment(progressPx + gapHalf, totalWidth, unplayed)
          val thumbWidth = 6.dp.toPx()
          val thumbHeight = 16.dp.toPx()
          val thumbRadius = 3.dp.toPx()
          drawRoundRect(
            color = played,
            topLeft = Offset(progressPx - thumbWidth / 2f, centerY - thumbHeight / 2f),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbRadius),
          )
        }
        SeekbarStyle.Wavy -> {
          val strokeWidth = 5.dp.toPx()
          val waveLength = 80f
          val lineAmplitude = 6f
          val transitionPeriods = 1.5f
          val waveProgressPx = totalWidth * progress
          fun amplitudeAt(x: Float, sign: Float): Float {
            val length = transitionPeriods * waveLength
            val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
            return sign * lineAmplitude * coeff
          }
          val path = Path()
          val waveStart = -waveLength / 2f
          path.moveTo(waveStart, centerY)
          var currentX = waveStart
          var waveSign = 1f
          var currentAmp = amplitudeAt(currentX, waveSign)
          val dist = waveLength / 2f
          while (currentX < totalWidth) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2f
            val nextAmp = amplitudeAt(nextX, waveSign)
            path.cubicTo(midX, centerY + currentAmp, midX, centerY + nextAmp, nextX, centerY + nextAmp)
            currentAmp = nextAmp
            currentX = nextX
          }
          val waveStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          val clipTop = lineAmplitude + strokeWidth
          clipRect(left = 0f, top = centerY - clipTop, right = progressPx, bottom = centerY + clipTop) {
            drawPath(path, played, style = waveStyle)
          }
          clipRect(left = progressPx, top = centerY - clipTop, right = totalWidth, bottom = centerY + clipTop) {
            drawPath(path, wavyUnplayed, style = waveStyle)
          }
          // Vertical bar thumb like SquigglySeekbar.
          val barHalfHeight = lineAmplitude + strokeWidth
          drawLine(
            color = played,
            start = Offset(progressPx, centerY - barHalfHeight),
            end = Offset(progressPx, centerY + barHalfHeight),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }
    }
  }

  @Composable
  private fun VerticalMiniPreview(style: SeekbarStyle) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.size(width = 24.dp, height = 56.dp)) {
      val fraction = 0.6f
      val played = scheme.primary
      val unplayed = scheme.primary.copy(alpha = 0.3f)
      val wavyUnplayed = scheme.primary.copy(alpha = 77f / 255f)
      val centerX = size.width / 2f
      val levelY = (size.height * (1f - fraction)).coerceIn(0f, size.height)

      when (style) {
        SeekbarStyle.Standard -> {
          val trackWidth = 4.dp.toPx()
          drawRoundRect(
            color = unplayed,
            topLeft = Offset(centerX - trackWidth / 2f, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = CornerRadius(trackWidth / 2f),
          )
          val playedHeight = size.height - levelY
          if (playedHeight > 0.5f) {
            drawRoundRect(
              color = played,
              topLeft = Offset(centerX - trackWidth / 2f, levelY),
              size = Size(trackWidth, playedHeight),
              cornerRadius = CornerRadius(trackWidth / 2f),
            )
          }
          drawCircle(
            color = played,
            radius = 7.dp.toPx(),
            center = Offset(centerX, levelY.coerceIn(0f, size.height)),
          )
        }
        SeekbarStyle.Thick -> {
          val trackWidth = 16.dp.toPx()
          val thumbHeight = 6.dp.toPx()
          val gapHalf = 7.dp.toPx()
          val outerRadius = trackWidth / 2f
          val innerRadius = 3.dp.toPx()
          fun vSegment(topY: Float, bottomY: Float, color: androidx.compose.ui.graphics.Color, topRadius: Float, bottomRadius: Float) {
            if (bottomY - topY < 0.5f) return
            val path = Path()
            path.addRoundRect(
              RoundRect(
                left = centerX - trackWidth / 2f,
                top = topY,
                right = centerX + trackWidth / 2f,
                bottom = bottomY,
                topLeftCornerRadius = CornerRadius(topRadius),
                bottomLeftCornerRadius = CornerRadius(bottomRadius),
                topRightCornerRadius = CornerRadius(topRadius),
                bottomRightCornerRadius = CornerRadius(bottomRadius),
              ),
            )
            drawPath(path, color)
          }
          vSegment(0f, levelY - gapHalf, unplayed, outerRadius, innerRadius)
          vSegment(levelY + gapHalf, size.height, played, innerRadius, outerRadius)
          vSegment(levelY - thumbHeight / 2f, levelY + thumbHeight / 2f, played, innerRadius, innerRadius)
        }
        SeekbarStyle.Wavy -> {
          val strokeWidth = 5.dp.toPx()
          val waveLength = 80f
          val amplitude = 6f
          val path = Path()
          val yStart = size.height + waveLength / 2f
          val yEnd = -waveLength / 2f
          path.moveTo(centerX, yStart)
          var currentY = yStart
          var waveSign = 1f
          var currentAmp = waveSign * amplitude
          val dist = waveLength / 2f
          while (currentY > yEnd) {
            waveSign = -waveSign
            val nextY = currentY - dist
            val midY = currentY - dist / 2f
            val nextAmp = waveSign * amplitude
            path.cubicTo(centerX + currentAmp, midY, centerX + nextAmp, midY, centerX + nextAmp, nextY)
            currentAmp = nextAmp
            currentY = nextY
          }
          val waveStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          clipRect(left = 0f, top = -strokeWidth, right = size.width, bottom = levelY) {
            drawPath(path, wavyUnplayed, style = waveStyle)
          }
          clipRect(left = 0f, top = levelY, right = size.width, bottom = size.height + strokeWidth) {
            drawPath(path, played, style = waveStyle)
          }
          val barHalf = amplitude + strokeWidth
          drawLine(
            color = played,
            start = Offset(centerX - barHalf, levelY),
            end = Offset(centerX + barHalf, levelY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
          )
        }
      }
    }
  }

  /**
   * Radio group for picking a [SeekbarStyle], shared by the seekbar,
   * volume slider and brightness slider sections.
   */
  @Composable
  private fun SliderStylePicker(
    selected: SeekbarStyle,
    onSelect: (SeekbarStyle) -> Unit,
    isVertical: Boolean = false,
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
          SliderStylePreview(style = style, isVertical = isVertical)
          Spacer(modifier = Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = style.name,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = sliderStyleHint(style),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
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
