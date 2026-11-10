package app.aryan447.mpvium.ui.audio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.domain.audio.model.LocalDiscourse
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimBrush

@Composable
fun AudiobookCard(
  audiobook: LocalAudiobook,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp? = null,
) {
  val haptic = LocalHapticFeedback.current
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()

  Column(
    modifier = modifier
      .then(if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
      .clip(RoundedCornerShape(14.dp))
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }
      .padding(4.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .then(
          if (isGlass) {
            Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(14.dp))
          } else {
            Modifier
          }
        ),
    ) {
      AlbumArtImage(
        artworkUri = audiobook.artworkUri,
        contentDescription = audiobook.title,
        modifier = Modifier.fillMaxWidth(),
      )

      if (audiobook.progressPercentage > 0f) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(3.5.dp)
            .background(Color.White.copy(alpha = 0.3f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(audiobook.progressPercentage.coerceIn(0.02f, 1f))
              .background(MaterialTheme.colorScheme.primary),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = audiobook.title,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Text(
      text = audiobook.author,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Text(
      text = stringResource(R.string.audiobook_chapters_count, audiobook.chapterCount),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
    )
  }
}

@Composable
fun AudiobookRow(
  audiobook: LocalAudiobook,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .then(
          if (isGlass) {
            Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(10.dp))
          } else {
            Modifier
          }
        ),
    ) {
      AlbumArtImage(
        artworkUri = audiobook.artworkUri,
        contentDescription = audiobook.title,
        modifier = Modifier.size(56.dp),
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = audiobook.title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = audiobook.author,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = "${stringResource(R.string.audiobook_chapters_count, audiobook.chapterCount)} • ${audiobook.totalDurationFormatted}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun DiscourseCard(
  discourse: LocalDiscourse,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp? = null,
) {
  val haptic = LocalHapticFeedback.current
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()

  Column(
    modifier = modifier
      .then(if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
      .clip(RoundedCornerShape(14.dp))
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }
      .padding(4.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .then(
          if (isGlass) {
            Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(14.dp))
          } else {
            Modifier
          }
        ),
    ) {
      AlbumArtImage(
        artworkUri = discourse.artworkUri,
        contentDescription = discourse.collection,
        modifier = Modifier.fillMaxWidth(),
      )

      if (discourse.progressPercentage > 0f) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(3.5.dp)
            .background(Color.White.copy(alpha = 0.3f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(discourse.progressPercentage.coerceIn(0.02f, 1f))
              .background(MaterialTheme.colorScheme.primary),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = discourse.collection,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Text(
      text = discourse.speaker,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Text(
      text = stringResource(R.string.discourse_parts_count, discourse.partCount),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
    )
  }
}

@Composable
fun DiscourseRow(
  discourse: LocalDiscourse,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .then(
          if (isGlass) {
            Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(10.dp))
          } else {
            Modifier
          }
        ),
    ) {
      AlbumArtImage(
        artworkUri = discourse.artworkUri,
        contentDescription = discourse.collection,
        modifier = Modifier.size(56.dp),
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = discourse.collection,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = discourse.speaker,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = "${stringResource(R.string.discourse_parts_count, discourse.partCount)} • ${discourse.totalDurationFormatted}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
    }
  }
}
