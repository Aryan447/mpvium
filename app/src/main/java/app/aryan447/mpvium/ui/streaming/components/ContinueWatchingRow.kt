package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.domain.streaming.model.ContinueWatchingItem

@Composable
fun ContinueWatchingRow(
  items: List<ContinueWatchingItem>,
  onItemClick: (ContinueWatchingItem) -> Unit,
  modifier: Modifier = Modifier,
  onSeeAllClick: (() -> Unit)? = null,
) {
  if (items.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    // Section Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Continue Watching",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.3).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (onSeeAllClick != null) {
        TextButton(onClick = onSeeAllClick) {
          Text("See all", style = MaterialTheme.typography.labelLarge)
          Spacer(modifier = Modifier.width(2.dp))
          Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
        }
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      items(items, key = { "cw_${it.video.id}_${it.playbackPositionMs}" }) { item ->
        ContinueWatchingCard(
          item = item,
          onClick = { onItemClick(item) },
        )
      }
    }
  }
}

@Composable
fun ContinueWatchingCard(
  item: ContinueWatchingItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cardWidth = 220.dp
  val haptic = LocalHapticFeedback.current

  Column(
    modifier = modifier
      .width(cardWidth)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
      }),
  ) {
    // Thumbnail with Progress Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      StreamingImage(
        url = item.backdropUrl ?: item.posterUrl,
        fallbackVideo = item.video,
        isSeries = item.isSeries,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      // Dark gradient overlay
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.5f to Color.Transparent,
              1.0f to Color.Black.copy(alpha = 0.7f),
            )
          )
      )

      // Center Play Icon Button
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(36.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.PlayArrow,
          contentDescription = "Play",
          tint = Color.White,
          modifier = Modifier.size(22.dp),
        )
      }

      // Progress Bar along bottom edge
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
            .fillMaxWidth(item.progressPercentage.coerceIn(0.02f, 1.0f))
            .background(MaterialTheme.colorScheme.primary),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Title
    Text(
      text = item.title,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 6.dp),
    )

    // Subtitle / Episode & Duration remaining
    Text(
      text = item.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .padding(horizontal = 6.dp)
        .padding(bottom = 6.dp),
    )
  }
}
