package app.aryan447.mpvium.ui.browser.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.database.repository.CustomSeriesRepository
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import app.aryan447.mpvium.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun AddToSeriesDialog(
  isOpen: Boolean,
  videos: List<Video>,
  onDismiss: () -> Unit,
  onCreateNewSeries: () -> Unit,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val context = LocalContext.current
  val customSeriesRepository = koinInject<CustomSeriesRepository>()
  val seriesListFromDb by customSeriesRepository.observeAllSeries().collectAsState(initial = emptyList())
  val seriesList = remember(seriesListFromDb) {
    seriesListFromDb.sortedBy { it.title.lowercase() }
  }
  val coroutineScope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier,
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
    title = {
      Text(
        text = stringResource(R.string.custom_series_choose_series),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = if (videos.size == 1) {
            "Add \"${videos.first().displayName}\" to a custom TV series"
          } else {
            "Add ${videos.size} videos to a custom TV series"
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Create new series button
        OutlinedButton(
          onClick = {
            onDismiss()
            onCreateNewSeries()
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.custom_series_create_new),
            fontWeight = FontWeight.Medium,
          )
        }

        if (seriesList.isNotEmpty()) {
          Text(
            text = "Existing custom series",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(seriesList, key = { it.id }) { series ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    coroutineScope.launch {
                      withContext(Dispatchers.IO) {
                        customSeriesRepository.addVideosToSeries(series.id, videos)
                        MediaLibraryEvents.notifyChanged()
                      }
                      val msg = context.getString(R.string.custom_series_added_to, series.title)
                      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                      onSuccess()
                      onDismiss()
                    }
                  },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = series.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (!series.year.isNullOrBlank()) {
                      Text(
                        text = series.year,
                        style = MaterialTheme.typography.bodySmall,
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
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.generic_cancel))
      }
    },
  )
}
