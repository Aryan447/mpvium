package app.aryan447.mpvium.ui.browser.states

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
) {
  BrowserStateMessage(
    icon = icon,
    title = title,
    message = message,
    modifier = modifier,
    bottomContent = {
      if (actionLabel != null && onAction != null) {
        Spacer(
          modifier = Modifier.height(20.dp),
        )
        androidx.compose.material3.FilledTonalButton(onClick = onAction) {
          androidx.compose.material3.Text(text = actionLabel)
        }
      }
    },
  )
}
