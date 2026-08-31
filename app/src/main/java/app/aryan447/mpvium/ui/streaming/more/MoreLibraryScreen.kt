package app.aryan447.mpvium.ui.streaming.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.browser.networkstreaming.NetworkStreamingScreen
import app.aryan447.mpvium.ui.browser.playlist.PlaylistScreen
import app.aryan447.mpvium.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.aryan447.mpvium.ui.preferences.PreferencesScreen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable

@Serializable
object MoreLibraryScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val navigationBarHeight = LocalNavigationBarHeight.current

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Library",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
        )
      },
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 12.dp,
          bottom = navigationBarHeight + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          LibraryItemCard(
            title = "Playback History",
            subtitle = "Recently played videos and timestamps",
            icon = Icons.Filled.History,
            onClick = { backstack.add(RecentlyPlayedScreen) },
          )
        }

        item {
          LibraryItemCard(
            title = "Playlists",
            subtitle = "Custom playlists and M3U streams",
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            onClick = { backstack.add(PlaylistScreen) },
          )
        }

        item {
          LibraryItemCard(
            title = "Network Streaming",
            subtitle = "SMB, FTP, WebDAV connections and streams",
            icon = Icons.Filled.Language,
            onClick = { backstack.add(NetworkStreamingScreen) },
          )
        }

        item {
          Spacer(modifier = Modifier.height(8.dp))
          LibraryItemCard(
            title = "Settings & Preferences",
            subtitle = "Appearance, decoder, audio, gestures, and subtitles",
            icon = Icons.Filled.Settings,
            onClick = { backstack.add(PreferencesScreen) },
          )
        }
      }
    }
  }
}

@Composable
private fun LibraryItemCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(14.dp),
      )
    }
  }
}
