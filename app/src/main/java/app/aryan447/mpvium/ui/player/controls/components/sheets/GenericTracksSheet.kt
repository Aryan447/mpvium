package app.aryan447.mpvium.ui.player.controls.components.sheets

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.components.PlayerSheet
import app.aryan447.mpvium.ui.player.TrackNode
import app.aryan447.mpvium.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T> GenericTracksSheet(
  tracks: ImmutableList<T>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  customMaxWidth: androidx.compose.ui.unit.Dp? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  val listState = lazyListState ?: rememberLazyListState()

  PlayerSheet(onDismissRequest, customMaxWidth = customMaxWidth) {
    Column(modifier) {
      header()
      LazyColumn(state = listState) {
        items(tracks) {
          track(it)
        }
        item {
          footer()
        }
      }
    }
  }
}

@Composable
fun AddTrackRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  focusRequester: FocusRequester? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  val primary = MaterialTheme.colorScheme.primary
  var isFocused by remember { mutableStateOf(false) }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .onFocusChanged { isFocused = it.isFocused }
        .clip(RoundedCornerShape(12.dp))
        .background(if (isFocused) primary.copy(alpha = 0.15f) else Color.Transparent)
        .then(
          if (isFocused) {
            Modifier.border(BorderStroke(2.dp, primary), RoundedCornerShape(12.dp))
          } else {
            Modifier
          }
        )
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .clickable {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        }
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actions()
    }
  }
}

@Composable
fun SheetActionButton(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var isFocused by remember { mutableStateOf(false) }
  val primary = MaterialTheme.colorScheme.primary

  IconButton(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clip(CircleShape)
      .background(if (isFocused) primary.copy(alpha = 0.25f) else Color.Transparent)
      .then(
        if (isFocused) {
          Modifier.border(BorderStroke(2.dp, primary), CircleShape)
        } else {
          Modifier
        }
      ),
  ) {
    Icon(icon, contentDescription = contentDescription)
  }
}

/**
 * Get a displayable title for a track node.
 * Uses title, language, or a default substitute.
 */
@Composable
fun getTrackTitle(
  track: TrackNode,
): String {
  // Handle external subtitles
  if (track.isSubtitle && track.external == true && track.externalFilename != null) {
    val decoded = Uri.decode(track.externalFilename)
    val fileName = decoded.substringAfterLast("/")
    return stringResource(R.string.player_sheets_track_title_wo_lang, track.id, fileName)
  }

  // Build title from available metadata
  val hasTitle = !track.title.isNullOrBlank()
  val hasLang = !track.lang.isNullOrBlank()

  return when {
    hasTitle && hasLang ->
      stringResource(
        R.string.player_sheets_track_title_w_lang,
        track.id,
        track.title,
        track.lang,
      )
    hasTitle -> stringResource(R.string.player_sheets_track_title_wo_lang, track.id, track.title)
    hasLang -> stringResource(R.string.player_sheets_track_lang_wo_title, track.id, track.lang)
    track.isSubtitle -> stringResource(R.string.player_sheets_chapter_title_substitute_subtitle, track.id)
    track.isAudio -> stringResource(R.string.player_sheets_chapter_title_substitute_audio, track.id)
    else -> ""
  }
}


