package app.aryan447.mpvium.ui.browser.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
) {
  BrowserStateMessage(
    icon = icon,
    title = title,
    message = message,
    modifier = modifier,
    bottomContent = {
      if ((actionLabel != null && onAction != null) ||
        (secondaryActionLabel != null && onSecondaryAction != null)
      ) {
        Spacer(
          modifier = Modifier.height(20.dp),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
          if (secondaryActionLabel != null && onSecondaryAction != null) {
            androidx.compose.material3.OutlinedButton(onClick = onSecondaryAction) {
              androidx.compose.material3.Text(text = secondaryActionLabel)
            }
          }
          if (actionLabel != null && onAction != null) {
            androidx.compose.material3.FilledTonalButton(onClick = onAction) {
              androidx.compose.material3.Text(text = actionLabel)
            }
          }
        }
      }
    },
  )
}
