package app.aryan447.mpvium.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import androidx.compose.foundation.isSystemInDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle

/**
 * Material 3 Floating Button Bar for file/folder operations
 * Icon-only buttons in a floating pill-shaped surface
 *
 * Live blur (Step 5e): pass the screen's shared [hazeState]/[hazeStyle] (with
 * content marked via `glassBackdrop`) for true backdrop blur. When null
 * (default), the bar falls back to its private state = translucent frost,
 * so non-wired screens keep their existing look.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowserBottomBar(
  isSelectionMode: Boolean,
  onCopyClick: () -> Unit,
  onMoveClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onAddToPlaylistClick: () -> Unit,
  modifier: Modifier = Modifier,
  showCopy: Boolean = true,
  showMove: Boolean = true,
  showRename: Boolean = true,
  showDelete: Boolean = true,
  showAddToPlaylist: Boolean = true,
  hazeState: HazeState? = null,
  hazeStyle: HazeStyle? = null,
) {
  AnimatedVisibility(
    visible = isSelectionMode,
    modifier = modifier,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    val isGlass = LocalGlass.current
    // Always remember the fallback so hooks stay unconditional; the shared
    // screen state (when provided) takes precedence. Blur is removed: the
    // style below only supplies the frost fill, never a blur lens.
    val fallbackHaze = rememberGlassHazeState()
    val fallbackStyle = glassHazeStyle(
      isDark = isSystemInDarkTheme(),
      kind = GlassKind.Bar,
    )
    val effectiveHaze = hazeState ?: fallbackHaze
    val effectiveStyle = hazeStyle ?: fallbackStyle
    Surface(
      modifier = Modifier
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .glassChrome(
          state = effectiveHaze,
          style = effectiveStyle,
          shape = RoundedCornerShape(32.dp),
          enabled = isGlass,
          rim = true,
        ),
      shape = RoundedCornerShape(32.dp),
      color = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 3.dp,
      shadowElevation = 8.dp
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalIconButton(
          onClick = onCopyClick,
          enabled = showCopy,
          modifier = Modifier.size(50.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = glassButtonContainerColor(
              MaterialTheme.colorScheme.secondaryContainer,
            ),
            contentColor = glassButtonContentColor(
              MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          )
        ) {
          Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "Copy",
            modifier = Modifier.size(24.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onMoveClick,
          enabled = showMove,
          modifier = Modifier.size(50.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = glassButtonContainerColor(
              MaterialTheme.colorScheme.secondaryContainer,
            ),
            contentColor = glassButtonContentColor(
              MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          )
        ) {
          Icon(
            Icons.AutoMirrored.Filled.DriveFileMove,
            contentDescription = "Move",
            modifier = Modifier.size(24.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onRenameClick,
          enabled = showRename,
          modifier = Modifier.size(50.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = glassButtonContainerColor(
              MaterialTheme.colorScheme.secondaryContainer,
            ),
            contentColor = glassButtonContentColor(
              MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          )
        ) {
          Icon(
            Icons.Filled.DriveFileRenameOutline,
            contentDescription = "Rename",
            modifier = Modifier.size(24.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onAddToPlaylistClick,
          enabled = showAddToPlaylist,
          modifier = Modifier.size(50.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = glassButtonContainerColor(
              MaterialTheme.colorScheme.secondaryContainer,
            ),
            contentColor = glassButtonContentColor(
              MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          )
        ) {
          Icon(
            Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = "Add to Playlist",
            modifier = Modifier.size(24.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onDeleteClick,
          enabled = showDelete,
          modifier = Modifier.size(50.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            // Danger signal survives glass: error hue at glass alpha.
            containerColor = if (isGlass) {
              MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            } else {
              MaterialTheme.colorScheme.errorContainer
            },
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          )
        ) {
          Icon(
            Icons.Filled.Delete,
            contentDescription = "Delete",
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}
