package app.aryan447.mpvium.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.controlColor
import app.aryan447.mpvium.ui.theme.spacing
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun IntroSkipChip(
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  Surface(
    onClick = onClick,
    modifier = modifier.clip(RoundedCornerShape(50)),
    shape = RoundedCornerShape(50),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 4.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.smaller),
    ) {
      Icon(
        Icons.Default.FastForward,
        contentDescription = null,
        tint = controlColor,
        modifier = Modifier.padding(end = MaterialTheme.spacing.extraSmall),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun IntroSkipChipPreview() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    IntroSkipChip(
      label = "Skip Intro",
    )
  }
}
