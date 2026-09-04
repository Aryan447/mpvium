package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.theme.cinemaFilmStrip

@Composable
fun StreamingHeroBanner(
  series: LocalSeries,
  onPlayClick: () -> Unit,
  onDetailsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val fallbackVideo = series.nextEpisodeToWatch?.video ?: series.seasons.values.firstOrNull()?.firstOrNull()?.video
  val backdropUrl = series.backdropUrl ?: series.posterUrl
  val haptic = LocalHapticFeedback.current
  val isCinema = LocalAppTheme.current == AppTheme.Cinema

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(360.dp)
      .then(if (isCinema) Modifier.cinemaFilmStrip(enabled = true) else Modifier)
  ) {
    // Backdrop Image
    StreamingImage(
      url = backdropUrl,
      fallbackVideo = fallbackVideo,
      isSeries = true,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Gradient Scrim overlays (top subtle, bottom heavy) + side vignette
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.25f),
            0.4f to Color.Black.copy(alpha = 0.40f),
            0.8f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
            1.0f to MaterialTheme.colorScheme.background,
          )
        )
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.horizontalGradient(
            0.0f to Color.Black.copy(alpha = 0.35f),
            0.15f to Color.Transparent,
            0.85f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.35f),
          )
        )
    )

    // Content Overlay
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // Featured kicker
      Text(
        text = "FEATURED SERIES",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 3.sp,
        ),
        color = MaterialTheme.colorScheme.primary,
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Badges Row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Rating Badge
        if (series.rating != null && series.rating > 0f) {
          Surface(
            color = Color(0xFFE5A00D).copy(alpha = 0.95f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = String.format(java.util.Locale.US, "%.1f", series.rating),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                ),
                color = Color.Black,
              )
            }
          }
        }

        // Season count pill
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${series.seasonCount} ${if (series.seasonCount == 1) "Season" else "Seasons"}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }

        // Year pill
        if (!series.year.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = series.year,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }

        // Resolution / Quality chip
        fallbackVideo?.resolution?.takeIf { it != "--" }?.let { resolution ->
          val displayRes = resolution.substringBefore("@")
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = displayRes,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
              ),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Series Title
      Text(
        text = series.title,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.5).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      // Overview / Synopsis
      val overviewText = series.overview ?: series.nextEpisodeToWatch?.let {
        "Next: ${it.formattedEpisodeTag} • ${it.displayTitle}"
      } ?: "${series.totalEpisodes} episodes available"

      Text(
        text = overviewText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
      )

      // Series progress (streaming-style resume bar)
      val seriesProgress = series.progressPercentage
      if (seriesProgress > 0f && seriesProgress < 1f) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(4.dp)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(seriesProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
            )
          }
          Text(
            text = "${series.watchedEpisodesCount}/${series.totalEpisodes}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val playActionLabel = when {
          series.lastWatchedEpisode != null && !series.lastWatchedEpisode.isWatched -> {
            "Resume ${series.lastWatchedEpisode.formattedEpisodeTag}"
          }
          series.nextEpisodeToWatch != null -> {
            "Play ${series.nextEpisodeToWatch.formattedEpisodeTag}"
          }
          else -> "Play Series"
        }

        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onPlayClick()
          },
          modifier = Modifier.weight(1f).height(44.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = playActionLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        FilledTonalButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDetailsClick()
          },
          modifier = Modifier.height(44.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Details",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          )
        }
      }
    }
  }
}
