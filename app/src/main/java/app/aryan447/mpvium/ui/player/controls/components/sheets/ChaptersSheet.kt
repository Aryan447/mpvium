package app.aryan447.mpvium.ui.player.controls.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.theme.spacing
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChaptersSheet(
  chapters: ImmutableList<Segment>,
  currentChapter: Segment,
  onClick: (Segment) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(currentChapter, chapters) {
    val index = chapters.indexOf(currentChapter)
    if (index >= 0) {
      listState.scrollToItem(index)
    }
  }

  GenericTracksSheet(
    chapters,
    lazyListState = listState,
    track = {
      ChapterTrack(
        it,
        index = chapters.indexOf(it),
        selected = currentChapter == it,
        onClick = { onClick(it) },
      )
    },
    onDismissRequest = onDismissRequest,
    modifier =
      modifier
        .padding(vertical = MaterialTheme.spacing.medium),
  )
}

@Composable
fun ChapterTrack(
  chapter: Segment,
  index: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var isFocused by remember { mutableStateOf(false) }
  val primary = MaterialTheme.colorScheme.primary
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
        .clip(RoundedCornerShape(12.dp))
        .background(if (isFocused) primary.copy(alpha = 0.15f) else Color.Transparent)
        .then(
          if (isFocused) {
            Modifier.border(BorderStroke(2.dp, primary), RoundedCornerShape(12.dp))
          } else {
            Modifier
          }
        )
        .clickable(onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        })
        .padding(vertical = MaterialTheme.spacing.smaller, horizontal = MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(R.string.player_sheets_track_title_wo_lang, index + 1, chapter.name),
      fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      modifier = Modifier.weight(1f),
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      Utils.prettyTime(chapter.start.toInt()),
      fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
  }
}
