package app.aryan447.mpvium.ui.browser.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.utils.media.ManualWatchStatus

/**
 * "Mark as" dialog mirroring MX Player: manually set the watch status of the
 * selected video(s) to last played, new, finished, or clear the status.
 */
@Composable
fun MarkAsDialog(
  isOpen: Boolean,
  selectedCount: Int,
  onDismiss: () -> Unit,
  onSelect: (ManualWatchStatus) -> Unit,
) {
  if (!isOpen) return

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.mark_as_title, selectedCount),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MarkAsOption(
          icon = Icons.Filled.History,
          title = stringResource(R.string.mark_as_last_played),
          summary = stringResource(R.string.mark_as_last_played_summary),
          onClick = { onSelect(ManualWatchStatus.LAST_PLAYED) },
        )
        MarkAsOption(
          icon = Icons.Filled.Star,
          title = stringResource(R.string.mark_as_new),
          summary = stringResource(R.string.mark_as_new_summary),
          onClick = { onSelect(ManualWatchStatus.NEW) },
        )
        MarkAsOption(
          icon = Icons.Filled.CheckCircle,
          title = stringResource(R.string.mark_as_finished),
          summary = stringResource(R.string.mark_as_finished_summary),
          onClick = { onSelect(ManualWatchStatus.FINISHED) },
        )
        MarkAsOption(
          icon = Icons.Filled.RemoveCircle,
          title = stringResource(R.string.mark_as_none),
          summary = stringResource(R.string.mark_as_none_summary),
          onClick = { onSelect(ManualWatchStatus.CLEARED) },
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.generic_cancel))
      }
    },
  )
}

@Composable
private fun MarkAsOption(
  icon: ImageVector,
  title: String,
  summary: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.secondary,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
