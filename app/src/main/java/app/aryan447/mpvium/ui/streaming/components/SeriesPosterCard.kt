package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.streaming.model.LocalSeries

@Composable
fun SeriesPosterCard(
  series: LocalSeries,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 140.dp,
) {
  val fallbackVideo = series.seasons.values.firstOrNull()?.firstOrNull()?.video

  Column(
    modifier = modifier
      .width(cardWidth)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
  ) {
    // Poster (2:3 Aspect Ratio)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      StreamingImage(
        url = series.posterUrl,
        fallbackVideo = fallbackVideo,
        isSeries = true,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      // Gradient overlay at the bottom of the poster
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.65f to Color.Transparent,
              1.0f to Color.Black.copy(alpha = 0.75f),
            )
          )
      )

      // Rating Badge (Top-End)
      if (series.rating != null && series.rating > 0f) {
        Surface(
          color = Color(0xFFE5A00D).copy(alpha = 0.95f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.Star,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(10.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = String.format(java.util.Locale.US, "%.1f", series.rating),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
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
          .padding(6.dp),
      ) {
        Text(
          text = "${series.seasonCount}S • ${series.totalEpisodes}Ep",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
          ),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
      }

      // Watched progress bar (if partially watched)
      if (series.watchedEpisodesCount > 0) {
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

    Spacer(modifier = Modifier.height(6.dp))

    // Series Title
    Text(
      text = series.title,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    // Year or Genre info
    val infoText = buildString {
      if (!series.year.isNullOrBlank()) append(series.year)
      if (series.isCompleted) {
        if (isNotEmpty()) append(" • ")
        append("Watched")
      }
    }
    if (infoText.isNotBlank()) {
      Text(
        text = infoText,
        style = MaterialTheme.typography.bodySmall,
        color = if (series.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
