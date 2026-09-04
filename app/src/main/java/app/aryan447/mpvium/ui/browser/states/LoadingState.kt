package app.aryan447.mpvium.ui.browser.states

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(
  icon: ImageVector = Icons.Filled.FolderOpen,
  title: String = "Scanning for videos...",
  message: String = "Please wait while we search your device",
  modifier: Modifier = Modifier,
) {
  BrowserStateMessage(
    icon = icon,
    title = title,
    message = message,
    modifier = modifier,
    bottomContent = {
      Spacer(modifier = Modifier.height(24.dp))
      LinearProgressIndicator(
        modifier =
          Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      )
    },
  )
}
