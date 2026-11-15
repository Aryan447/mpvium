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
import app.aryan447.mpvium.ui.player.controls.ModernChrome
import app.aryan447.mpvium.ui.player.controls.LocalPlayerButtonsClickEvent
import app.aryan447.mpvium.ui.player.controls.LocalPlayerColors
import app.aryan447.mpvium.ui.player.controls.LocalPlayerLayout
import app.aryan447.mpvium.ui.theme.spacing
import org.koin.compose.koinInject

private val modernGlyphShadow =
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
  val playerButtonsBackground by appearancePreferences?.playerButtonsBackground?.collectAsState()
    ?: remember { androidx.compose.runtime.mutableStateOf(false) }
  val playerLayout = LocalPlayerLayout.current
  val isModern = playerLayout == PlayerLayout.MODERN
  val effectiveHideBackground = hideBackground ?: (!playerButtonsBackground || isModern)
  val colors = LocalPlayerColors.current

  val clickEvent = LocalPlayerButtonsClickEvent.current
  val contentColor =
    color
      ?: when {
        isModern && active -> colors.accent
        isModern -> ModernChrome.glyph
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
          indication = ripple(bounded = false, radius = 24.dp),
        )
        .then(
          if (effectiveHideBackground || isModern) {
            Modifier.background(brush = modernGlyphShadow, shape = CircleShape)
          } else {
            Modifier
          },
        ),
    shape = CircleShape,
    color =
      when {
        // Modern controls are bare glyphs; only the active dot marks state.
        isModern -> Color.Transparent
        effectiveHideBackground -> Color.Transparent
        active -> colors.activeFill
        else -> colors.controlFill
      },
    contentColor = contentColor,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      when {
        isModern -> null
        effectiveHideBackground -> null
        else -> BorderStroke(1.dp, colors.controlBorder)
      },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = contentColor,
        modifier =
          Modifier
            .padding(MaterialTheme.spacing.small)
            .size(if (isModern) 22.dp else 20.dp),
      )
      if (active && effectiveHideBackground) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 3.dp)
            .size(if (isModern) 5.dp else 4.dp)
            .background(
              if (isModern) MaterialTheme.colorScheme.primary else contentColor,
              CircleShape,
            )
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
  val isModern = LocalPlayerLayout.current == PlayerLayout.MODERN

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      androidx.compose.foundation.layout.Arrangement
        .spacedBy(if (isModern) spacing.smaller else spacing.extraSmall),
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
