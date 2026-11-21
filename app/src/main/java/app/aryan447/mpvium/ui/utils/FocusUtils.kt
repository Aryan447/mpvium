package app.aryan447.mpvium.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier to provide TV / D-Pad friendly visual focus indication (scale + border ring)
 * adhering to mpvium design tokens and motion specs.
 */
fun Modifier.tvFocusHighlight(
  shape: Shape,
  interactionSource: MutableInteractionSource,
  scale: Float = 1.05f,
  borderWidth: Dp = 2.dp,
  borderColor: Color? = null,
  enabled: Boolean = true,
): Modifier = composed {
  if (!enabled) return@composed this

  val isFocused by interactionSource.collectIsFocusedAsState()
  val animatedScale by animateFloatAsState(
    targetValue = if (isFocused) scale else 1f,
    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    label = "tv_focus_scale",
  )

  val focusBorderColor = borderColor ?: MaterialTheme.colorScheme.primary

  this
    .graphicsLayer {
      scaleX = animatedScale
      scaleY = animatedScale
    }
    .then(
      if (isFocused) {
        Modifier.border(
          border = BorderStroke(borderWidth, focusBorderColor),
          shape = shape,
        )
      } else {
        Modifier
      },
    )
}
