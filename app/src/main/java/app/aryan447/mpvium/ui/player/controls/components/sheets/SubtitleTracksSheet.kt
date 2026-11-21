package app.aryan447.mpvium.ui.player.controls.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.player.TrackNode
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.ui.utils.LocalIsTv
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class SubtitleItem {
  data class Track(val node: TrackNode) : SubtitleItem()
  data class Header(val title: String) : SubtitleItem()
  object Divider : SubtitleItem()
}

@Composable
fun SubtitlesSheet(
  tracks: ImmutableList<TrackNode>,
  onToggleSubtitle: (Int) -> Unit,
  isSubtitleSelected: (Int) -> Boolean,
  onAddSubtitle: () -> Unit,
  onOpenSubtitleSettings: () -> Unit,
  onOpenSubtitleDelay: () -> Unit,
  onRemoveSubtitle: (Int) -> Unit,
  onOpenOnlineSearch: () -> Unit,
  onDismissRequest: () -> Unit,
  onForgetSubtitleSettings: (() -> Unit)? = null,
  externalSubtitleLabel: (TrackNode) -> String? = { null },
  modifier: Modifier = Modifier
) {
  val items = remember(tracks) {
    val list = mutableListOf<SubtitleItem>()

    // Internal/Local tracks section
    val internal = tracks.filter { it.external != true }
    val external = tracks.filter { it.external == true }

    if (internal.isNotEmpty() || external.isNotEmpty()) {
        list.add(SubtitleItem.Header(if (internal.isNotEmpty()) "Embedded Subtitles" else "Local Subtitles"))
        list.addAll(internal.map { SubtitleItem.Track(it) })
        if (internal.isNotEmpty() && external.isNotEmpty()) {
          list.add(SubtitleItem.Header("External Subtitles"))
        }
        list.addAll(external.map { SubtitleItem.Track(it) })
    }

    list.toImmutableList()
  }

  val isTv = LocalIsTv.current
  val initialFocusRequester = remember { FocusRequester() }

  LaunchedEffect(isTv) {
    if (isTv) {
      kotlinx.coroutines.delay(100)
      try {
        initialFocusRequester.requestFocus()
      } catch (_: Exception) {}
    }
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      AddTrackRow(
        title = stringResource(R.string.player_sheets_add_ext_sub),
        onClick = onAddSubtitle,
        focusRequester = initialFocusRequester,
        actions = {
          SheetActionButton(
            icon = Icons.Default.Search,
            contentDescription = stringResource(R.string.search),
            onClick = onOpenOnlineSearch,
          )
          SheetActionButton(
            icon = Icons.Default.Palette,
            contentDescription = "Subtitle Settings",
            onClick = onOpenSubtitleSettings,
          )
          SheetActionButton(
            icon = Icons.Default.MoreTime,
            contentDescription = "Subtitle Delay",
            onClick = onOpenSubtitleDelay,
          )
        },
      )
    },
    track = { item ->
      when (item) {
        is SubtitleItem.Track -> {
          val track = item.node
          SubtitleTrackRow(
            title = externalSubtitleLabel(track) ?: getTrackTitle(track),
            isSelected = isSubtitleSelected(track.id),
            isExternal = track.external == true,
            onToggle = { onToggleSubtitle(track.id) },
            onRemove = { onRemoveSubtitle(track.id) },
          )
        }
        is SubtitleItem.Header -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        SubtitleItem.Divider -> {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
      }
    },
    footer = {
      if (onForgetSubtitleSettings != null) {
        val haptic = LocalHapticFeedback.current
        var isForgetFocused by remember { mutableStateOf(false) }
        val errorColor = MaterialTheme.colorScheme.error
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .onFocusChanged { isForgetFocused = it.isFocused }
              .clip(RoundedCornerShape(12.dp))
              .background(if (isForgetFocused) errorColor.copy(alpha = 0.15f) else Color.Transparent)
              .then(
                if (isForgetFocused) {
                  Modifier.border(BorderStroke(2.dp, errorColor), RoundedCornerShape(12.dp))
                } else {
                  Modifier
                }
              )
              .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onForgetSubtitleSettings()
              })
              .height(56.dp)
              .padding(horizontal = MaterialTheme.spacing.medium),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          Icon(
            Icons.Default.RestartAlt,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = errorColor,
          )
          Text(
            text = stringResource(R.string.player_sheets_forget_subtitle_settings),
            style = MaterialTheme.typography.bodyLarge,
            color = errorColor,
            modifier = Modifier.weight(1f),
          )
        }
      }
      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    },
    modifier = modifier,
  )
}

@Composable
fun SubtitleTrackRow(
  title: String,
  isSelected: Boolean,
  isExternal: Boolean,
  onToggle: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  var isFocused by remember { mutableStateOf(false) }
  val primary = MaterialTheme.colorScheme.primary
  val tickToggle = {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    onToggle()
  }
  Row(
    modifier = modifier
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
      .clickable(onClick = tickToggle)
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    Checkbox(checked = isSelected, onCheckedChange = null)
    Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
    if (isExternal) {
      var isDeleteFocused by remember { mutableStateOf(false) }
      val errorColor = MaterialTheme.colorScheme.error
      IconButton(
        onClick = onRemove,
        modifier = Modifier
          .onFocusChanged { isDeleteFocused = it.isFocused }
          .clip(RoundedCornerShape(8.dp))
          .background(if (isDeleteFocused) errorColor.copy(alpha = 0.25f) else Color.Transparent)
          .then(
            if (isDeleteFocused) {
              Modifier.border(BorderStroke(2.dp, errorColor), RoundedCornerShape(8.dp))
            } else {
              Modifier
            }
          ),
      ) {
        Icon(
          Icons.Default.Delete,
          contentDescription = null,
          tint = if (isDeleteFocused) errorColor else LocalContentColor.current,
        )
      }
    }
  }
}
