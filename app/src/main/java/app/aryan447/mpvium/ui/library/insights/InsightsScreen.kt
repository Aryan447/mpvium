package app.aryan447.mpvium.ui.library.insights

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.LocalNavigationBarHeight
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
object InsightsScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val navigationBarHeight = LocalNavigationBarHeight.current

    val viewModel: InsightsViewModel = viewModel(
      factory = InsightsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Insights",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            if (backstack.size > 1) {
              IconButton(onClick = { backstack.removeLastOrNull() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            }
          },
          actions = {
            IconButton(onClick = { viewModel.refresh() }) {
              Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
          },
        )
      },
    ) { innerPadding ->
      if (state.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = navigationBarHeight + 24.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            SectionLabel("Collection")
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatCard(
                value = "${state.totalVideos}",
                label = "Videos",
                modifier = Modifier.weight(1f),
              )
              StatCard(
                value = formatBytes(state.storageBytes),
                label = "Storage",
                modifier = Modifier.weight(1f),
              )
            }
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatCard(
                value = "${state.movieCount}",
                label = "Movies",
                modifier = Modifier.weight(1f),
              )
              StatCard(
                value = "${state.seriesCount}",
                label = "Series · ${state.episodeCount} eps",
                modifier = Modifier.weight(1f),
              )
            }
          }

          item {
            SectionLabel("Watch progress")
          }
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                LinearProgressIndicator(
                  progress = { state.completionFraction },
                  modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  ProgressLegend(
                    value = "${state.watchedCount}",
                    label = "Watched",
                  )
                  ProgressLegend(
                    value = "${state.inProgressCount}",
                    label = "In progress",
                  )
                  ProgressLegend(
                    value = "${state.unwatchedCount}",
                    label = "Unwatched",
                  )
                }
              }
            }
          }

          item {
            SectionLabel("Library")
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatCard(
                value = formatRuntime(state.runtimeMs),
                label = "Runtime",
                modifier = Modifier.weight(1f),
              )
              StatCard(
                value = "${state.embeddedSubsCount}",
                label = "With subtitles",
                modifier = Modifier.weight(1f),
              )
            }
          }

          item {
            SectionLabel("This week")
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatCard(
                value = "${state.weekPlays}",
                label = if (state.weekPlays == 1) "Play" else "Plays",
                modifier = Modifier.weight(1f),
              )
              StatCard(
                value = "${state.weekMinutes}m",
                label = "Watched",
                modifier = Modifier.weight(1f),
              )
            }
          }

          if (state.topTitles.isNotEmpty()) {
            item {
              SectionLabel("Most rewatched")
            }
            items(state.topTitles) { top ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = top.title,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f),
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (top.plays == 1) "1 play" else "${top.plays} plays",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    modifier = Modifier.padding(top = 4.dp),
  )
}

@Composable
private fun StatCard(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun ProgressLegend(
  value: String,
  label: String,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private fun formatBytes(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  var value = bytes.toDouble()
  var unit = 0
  while (value >= 1024 && unit < units.lastIndex) {
    value /= 1024
    unit++
  }
  return if (unit == 0) {
    "${value.roundToInt()} ${units[unit]}"
  } else {
    "%.1f %s".format(value, units[unit])
  }
}

private fun formatRuntime(millis: Long): String {
  if (millis <= 0) return "0m"
  val totalMinutes = millis / 60_000L
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
