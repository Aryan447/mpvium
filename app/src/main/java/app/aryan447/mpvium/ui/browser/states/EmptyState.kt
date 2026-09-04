package app.aryan447.mpvium.ui.browser.states

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
) {
  BrowserStateMessage(
    icon = icon,
    title = title,
    message = message,
    modifier = modifier,
  )
}
