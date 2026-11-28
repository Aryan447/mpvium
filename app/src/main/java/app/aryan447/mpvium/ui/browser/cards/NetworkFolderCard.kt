package app.aryan447.mpvium.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.domain.network.NetworkFile
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import app.aryan447.mpvium.ui.utils.tvFocusHighlight
import org.koin.compose.koinInject

@Composable
fun NetworkFolderCard(
  file: NetworkFile,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  val cardInteraction = remember { MutableInteractionSource() }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .tvFocusHighlight(
          shape = RoundedCornerShape(12.dp),
          interactionSource = cardInteraction,
          scale = 1.02f,
        )
        .combinedClickable(
          interactionSource = cardInteraction,
          indication = ripple(),
          onClick = onClick,
          onLongClick = onLongClick,
        ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else Color.Transparent,
          )
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.Folder,
          contentDescription = "Folder",
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          file.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
