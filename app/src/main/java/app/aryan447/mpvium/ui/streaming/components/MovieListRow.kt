package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.cinemaFilmStrip
import app.aryan447.mpvium.ui.theme.glassRimBrush
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieListRow(
  movie: LocalMovie,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  thumbWidth: Dp = 64.dp,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  val isCinema = LocalAppTheme.current == AppTheme.Cinema
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()
  val haptic = LocalHapticFeedback.current

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
        onLongClick = onLongClick,
      )
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Poster Thumb (2:3 aspect ratio, same corner radius as the cards)
    Box(
      modifier = Modifier
        .width(thumbWidth)
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .then(if (isCinema) Modifier.cinemaFilmStrip(enabled = true) else Modifier)
        .then(
          if (isGlass) {
            Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(14.dp))
          } else {
            Modifier
          }
        ),
    ) {
      StreamingImage(
        url = movie.posterUrl,
        fallbackVideo = movie.video,
        isSeries = false,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      // Progress bar if in progress
      if (movie.progressPercentage > 0f && !movie.isWatched) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.3f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(movie.progressPercentage)
              .background(MaterialTheme.colorScheme.primary),
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

    // Info Column: title, year, and progress/watch state inline
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = movie.title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
      )

      // Year, Duration, Rating inline
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!movie.year.isNullOrBlank()) {
          Text(
            text = movie.year,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (movie.video.durationFormatted.isNotBlank() && movie.video.durationFormatted != "0s") {
          Text(
            text = movie.video.durationFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }

        if (movie.rating != null && movie.rating > 0f) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.Star,
              contentDescription = null,
              tint = Color(0xFFE5A00D),
              modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = String.format(java.util.Locale.US, "%.1f", movie.rating),
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Inline progress / watch state
      if (movie.isWatched) {
        Text(
          text = "Watched",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary,
        )
      } else if (movie.progressPercentage > 0f) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LinearProgressIndicator(
            progress = { movie.progressPercentage },
            modifier = Modifier
              .widthIn(min = 60.dp, max = 100.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          )
          Text(
            text = "${(movie.progressPercentage * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
