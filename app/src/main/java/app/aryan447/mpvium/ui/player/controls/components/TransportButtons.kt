package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.theme.controlColor
import app.aryan447.mpvium.ui.theme.spacing

private val transportShadow =
  Brush.radialGradient(
    0.0f to Color.Black.copy(alpha = 0.3f),
    0.7f to Color.Transparent,
    1.0f to Color.Transparent,
  )

/**
 * Shared circular transport button (prev/next): tonal glass surface with border,
 * or transparent with a soft shadow when button backgrounds are hidden.
 */
@Composable
fun PlayerTransportButton(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  size: Dp = 56.dp,
  hideBackground: Boolean = false,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .clickable(
          enabled = enabled,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
        )
        .then(
          if (hideBackground) {
            Modifier.background(brush = transportShadow, shape = CircleShape)
          } else {
            Modifier
          },
        ),
    shape = CircleShape,
    color =
      if (!hideBackground) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
      } else {
        Color.Transparent
      },
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      if (!hideBackground) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      } else {
        null
      },
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint =
        if (enabled) {
          if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
        } else {
          if (hideBackground) {
            controlColor.copy(alpha = 0.38f)
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
          }
        },
      modifier =
        Modifier
          .fillMaxSize()
          .padding(MaterialTheme.spacing.small),
    )
  }
}

/**
 * Shared glass pill container for top-bar player buttons with text content
 * (video title, active speed, decoder, active zoom).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerPillButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onLongClick: () -> Unit = {},
  hideBackground: Boolean = false,
  height: Dp = 40.dp,
  content: @Composable RowScope.() -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    shape = RoundedCornerShape(50),
    color =
      if (hideBackground) {
        Color.Transparent
      } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
      },
    contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      if (hideBackground) {
        null
      } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      },
    modifier =
      modifier
        .height(height)
        .clip(RoundedCornerShape(50))
        .combinedClickable(
          enabled = enabled,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
          onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
          },
        ),
    content = {
      Row(content = content)
    },
  )
}

/**
 * Hero play/pause button: larger circle with animated play<->pause vector,
 * styled like other transport buttons.
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun PlayerPlayPauseButton(
  paused: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 72.dp,
  hideBackground: Boolean = false,
) {
  val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
  val interaction = remember { MutableInteractionSource() }
  val haptic = LocalHapticFeedback.current

  Surface(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .clickable(
          interaction,
          ripple(),
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
        )
        .then(
          if (hideBackground) {
            Modifier.background(brush = transportShadow, shape = CircleShape)
          } else {
            Modifier
          },
        ),
    shape = CircleShape,
    color =
      if (!hideBackground) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
      } else {
        Color.Transparent
      },
    contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      if (!hideBackground) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      } else {
        null
      },
  ) {
    Image(
      painter = rememberAnimatedVectorPainter(icon, paused == false),
      modifier =
        Modifier
          .fillMaxSize()
          .padding(MaterialTheme.spacing.medium),
      contentDescription = null,
      colorFilter = ColorFilter.tint(LocalContentColor.current),
    )
  }
}
