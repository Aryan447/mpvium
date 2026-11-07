package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.PlayerLayout
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.ui.player.controls.LocalPlayerButtonsClickEvent
import app.aryan447.mpvium.ui.player.controls.LocalPlayerColors
import app.aryan447.mpvium.ui.theme.spacing
import org.koin.compose.koinInject

private val cinemaGlyphShadow =
  Brush.radialGradient(
    0.0f to Color.Black.copy(alpha = 0.35f),
    0.7f to Color.Black.copy(alpha = 0.08f),
    1.0f to Color.Transparent,
  )

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color? = null,
  active: Boolean = false,
  hideBackground: Boolean? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isInspection = LocalInspectionMode.current
  val appearancePreferences = if (isInspection) null else koinInject<AppearancePreferences>()
  val rawHideBackground by appearancePreferences?.hidePlayerButtonsBackground?.collectAsState()
    ?: remember { androidx.compose.runtime.mutableStateOf(false) }
  val playerLayout by appearancePreferences?.playerLayout?.collectAsState()
    ?: remember { androidx.compose.runtime.mutableStateOf(PlayerLayout.GLASS_PILL) }
  val isCinema = playerLayout == PlayerLayout.CINEMA
  val effectiveHideBackground = hideBackground ?: (rawHideBackground || isCinema)
  val colors = LocalPlayerColors.current

  val clickEvent = LocalPlayerButtonsClickEvent.current
  val contentColor =
    color
      ?: when {
        active && effectiveHideBackground -> colors.activeOnVideoContent
        active -> colors.activeContent
        effectiveHideBackground -> colors.onVideoIconContent
        else -> colors.controlIconContent
      }
  Surface(
    modifier =
      modifier
        .clip(CircleShape)
        .combinedClickable(
          onClick = {
            clickEvent()
            onClick()
          },
          onLongClick = onLongClick,
          interactionSource = interactionSource,
          indication = ripple(),
        )
        .then(
          if (effectiveHideBackground) {
            Modifier.background(brush = cinemaGlyphShadow, shape = CircleShape)
          } else {
            Modifier
          },
        ),
    shape = CircleShape,
    color =
      when {
        effectiveHideBackground -> Color.Transparent
        active -> colors.activeFill
        else -> colors.controlFill
      },
    contentColor = contentColor,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = if (effectiveHideBackground) null else BorderStroke(1.dp, colors.controlBorder),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = contentColor,
        modifier =
          Modifier
            .padding(MaterialTheme.spacing.small)
            .size(20.dp),
      )
      if (active && effectiveHideBackground) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 3.dp)
            .size(4.dp)
            .background(contentColor, CircleShape)
        )
      }
    }
  }
}

@Composable
fun ControlsGroup(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  val spacing = MaterialTheme.spacing

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      androidx.compose.foundation.layout.Arrangement
        .spacedBy(spacing.extraSmall),
    content = content,
  )
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    onClick = {},
  )
}
