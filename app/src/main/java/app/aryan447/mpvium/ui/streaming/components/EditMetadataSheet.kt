package app.aryan447.mpvium.ui.streaming.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMetadataSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  currentTitle: String,
  currentYear: String?,
  currentPosterUrl: String?,
  fallbackVideo: Video? = null,
  isSeries: Boolean = false,
  onSave: (title: String, year: String?, posterUri: Uri?, removePoster: Boolean) -> Unit,
  onResetToAutomatic: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember(isOpen, currentTitle) { mutableStateOf(currentTitle) }
  var year by remember(isOpen, currentYear) { mutableStateOf(currentYear ?: "") }
  var selectedPosterUri by remember(isOpen) { mutableStateOf<Uri?>(null) }
  var removePoster by remember(isOpen) { mutableStateOf(false) }
  var previewBitmap by remember(isOpen) { mutableStateOf<Bitmap?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      selectedPosterUri = uri
      removePoster = false
    }
  }

  LaunchedEffect(selectedPosterUri) {
    val uri = selectedPosterUri
    if (uri != null) {
      val bitmap = withContext(Dispatchers.IO) {
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
          }
        }.getOrNull()
      }
      previewBitmap = bitmap
    } else {
      previewBitmap = null
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
        .imePadding()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.metadata_edit_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Poster preview and edit row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .width(100.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          when {
            previewBitmap != null -> {
              Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.metadata_field_poster),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
            !removePoster && !currentPosterUrl.isNullOrBlank() -> {
              StreamingImage(
                url = currentPosterUrl,
                fallbackVideo = fallbackVideo,
                isSeries = isSeries,
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.metadata_field_poster),
                modifier = Modifier.fillMaxSize(),
              )
            }
            else -> {
              Icon(
                imageVector = if (isSeries) Icons.Filled.Tv else Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp),
              )
            }
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
              imageVector = Icons.Filled.AddPhotoAlternate,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.metadata_change_poster))
          }

          val hasPoster = previewBitmap != null || (!removePoster && !currentPosterUrl.isNullOrBlank())
          if (hasPoster) {
            TextButton(
              onClick = {
                removePoster = true
                selectedPosterUri = null
                previewBitmap = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                stringResource(R.string.metadata_remove_poster),
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      // Title field
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(stringResource(R.string.metadata_field_title)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      // Year field
      OutlinedTextField(
        value = year,
        onValueChange = { input ->
          val digits = input.filter { it.isDigit() }.take(4)
          year = digits
        },
        label = { Text(stringResource(R.string.metadata_field_year)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
      )

      // Reset to automatic button
      OutlinedButton(
        onClick = onResetToAutomatic,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = Icons.Filled.RestartAlt,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.metadata_reset_automatic))
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.generic_cancel))
        }

        Button(
          onClick = {
            onSave(title, year.takeIf { it.isNotBlank() }, selectedPosterUri, removePoster)
          },
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.metadata_save))
        }
      }
    }
  }
}
