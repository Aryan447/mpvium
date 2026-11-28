package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.aryan447.mpvium.ui.utils.tvFocusHighlight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem
import app.aryan447.mpvium.domain.streaming.model.LocalMovie
import app.aryan447.mpvium.domain.streaming.model.LocalSeries
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalAppTheme
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.cinemaFilmStrip
import app.aryan447.mpvium.ui.theme.glassFrostColor
import app.aryan447.mpvium.ui.theme.glassRimBrush

/**
 * Items displayable in the Speed Binge home grid dial.
 */
@Immutable
sealed interface SpeedBingeItem {
  val key: String
  val title: String
  val posterUrl: String?
  val fallbackVideo: Video?
  val isSeries: Boolean
  val progressPercentage: Float

  @Immutable
  data class LastPlayed(
    val continueItem: ContinueWatchingItem,
  ) : SpeedBingeItem {
    override val key: String get() = "speed_last_${continueItem.video.id}_${continueItem.lastPlayedTimestamp}"
    override val title: String get() = continueItem.title
    override val posterUrl: String? get() = continueItem.posterUrl ?: continueItem.backdropUrl
    override val fallbackVideo: Video get() = continueItem.video
    override val isSeries: Boolean get() = continueItem.isSeries
    override val progressPercentage: Float get() = continueItem.progressPercentage
  }

  @Immutable
  data class SeriesItem(
    val series: LocalSeries,
  ) : SpeedBingeItem {
    override val key: String get() = "speed_series_${series.id}"
    override val title: String get() = series.title
    override val posterUrl: String? get() = series.posterUrl ?: series.backdropUrl
    override val fallbackVideo: Video? get() = series.seasons.values.firstOrNull()?.firstOrNull()?.video
    override val isSeries: Boolean get() = true
    override val progressPercentage: Float get() = series.progressPercentage
  }

  @Immutable
  data class MovieItem(
    val movie: LocalMovie,
  ) : SpeedBingeItem {
    override val key: String get() = "speed_movie_${movie.video.id}"
    override val title: String get() = movie.title
    override val posterUrl: String? get() = movie.posterUrl ?: movie.backdropUrl
    override val fallbackVideo: Video get() = movie.video
    override val isSeries: Boolean get() = false
    override val progressPercentage: Float get() = movie.progressPercentage
  }
}

/**
 * Rectangular poster card component for the Speed Binge horizontal grid dial on Home.
 * Displays vertical poster artwork, smooth dark gradient, clean title, and bottom progress bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedBingeCard(
  item: SpeedBingeItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val isCinema = LocalAppTheme.current == AppTheme.Cinema
  val isGlass = LocalGlass.current
  val dark = isSystemInDarkTheme()
  val isLastPlayed = item is SpeedBingeItem.LastPlayed
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .aspectRatio(2f / 3f)
      .tvFocusHighlight(
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource,
        scale = 1.05f,
      )
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .then(if (isCinema) Modifier.cinemaFilmStrip(enabled = true) else Modifier)
      .then(
        when {
          isGlass -> Modifier.border(1.dp, glassRimBrush(dark), RoundedCornerShape(12.dp))
          isLastPlayed -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
          else -> Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
        }
      )
      .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
        onLongClick = onLongClick?.let {
          {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            it()
          }
        },
      ),
  ) {
    // Poster Artwork Image
    StreamingImage(
      url = item.posterUrl,
      fallbackVideo = item.fallbackVideo,
      isSeries = item.isSeries,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Gradient overlay for clean, readable title typography
    if (!isGlass) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.4f to Color.Transparent,
              0.7f to Color.Black.copy(alpha = 0.6f),
              1.0f to Color.Black.copy(alpha = 0.9f),
            )
          )
      )
    }

    // Top-Start Badge: "RESUME" tag for 1st grid item (Last Played)
    if (isLastPlayed) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(6.dp),
        shadowElevation = 2.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(9.dp),
          )
          Text(
            text = "RESUME",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 8.5.sp,
              letterSpacing = 0.3.sp,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }

      // Frosted Play Button in Center for Last Played item
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(34.dp)
          .clip(CircleShape)
          .background(
            if (isGlass) glassFrostColor(isDark = dark, kind = GlassKind.Chip)
            else Color.Black.copy(alpha = 0.55f)
          )
          .border(
            1.dp,
            if (isGlass) glassRimBrush(dark) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))),
            CircleShape,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = "Resume",
          tint = Color.White,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    // Show or movie title at bottom (no length, no duration)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(start = 7.dp, end = 7.dp, bottom = 6.dp),
    ) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          lineHeight = 13.5.sp,
        ),
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // Progress Bar along bottom edge
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(3.dp)
        .background(Color.White.copy(alpha = 0.25f)),
    ) {
      if (item.progressPercentage > 0f) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(item.progressPercentage.coerceIn(0.04f, 1.0f))
            .background(MaterialTheme.colorScheme.primary),
        )
      }
    }
  }
}
