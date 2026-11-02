package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.cinemaFilmStrip
import app.aryan447.mpvium.ui.theme.glassRimBrush
import org.koin.compose.koinInject

@Composable
fun SeriesListRow(
  series: LocalSeries,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  thumbWidth: Dp = 80.dp,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  val isCinema = LocalAppTheme.current == AppTheme.Cinema
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()
  val haptic = LocalHapticFeedback.current
  val fallbackVideo = series.seasons.values.firstOrNull()?.firstOrNull()?.video

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
      )
      .padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Poster thumbnail with identical corner radius (14.dp) and aspect ratio (2:3) as SeriesPosterCard
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
        url = series.posterUrl,
        fallbackVideo = fallbackVideo,
        isSeries = true,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      if (!isGlass) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.65f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.75f),
              ),
            ),
        )
      }

      // Rating Badge (Top-End)
      if (series.rating != null && series.rating > 0f) {
        Surface(
          color = Color(0xFFE5A00D).copy(alpha = 0.95f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.Star,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(9.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = String.format(java.util.Locale.US, "%.1f", series.rating),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
              ),
              color = Color.Black,
            )
          }
        }
      }

      // Season Count Pill (Bottom-Start)
      Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(4.dp),
      ) {
        Text(
          text = "${series.seasonCount}S • ${series.totalEpisodes}Ep",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
          ),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
      }

      // Watched progress bar (if in progress)
      if (series.watchedEpisodesCount > 0 && !series.isCompleted) {
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
              .fillMaxWidth(series.progressPercentage)
              .background(MaterialTheme.colorScheme.primary),
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Information Column: Title, Year, and Progress/Watch state inline
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(vertical = 4.dp),
    ) {
      Text(
        text = series.title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
      )

      val inlineInfo = remember(
        series.year,
        series.isCompleted,
        series.watchedEpisodesCount,
        series.totalEpisodes,
      ) {
        buildList {
          if (!series.year.isNullOrBlank()) {
            add(series.year)
          }
          if (series.isCompleted) {
            add("Watched")
          } else if (series.watchedEpisodesCount > 0) {
            add("${series.watchedEpisodesCount}/${series.totalEpisodes} watched")
          } else {
            add("Unwatched")
          }
        }.joinToString(" • ")
      }

      if (inlineInfo.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = inlineInfo,
          style = MaterialTheme.typography.bodySmall,
          color = if (series.isCompleted) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
