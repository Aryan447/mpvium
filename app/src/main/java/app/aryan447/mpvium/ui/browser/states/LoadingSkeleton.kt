package app.aryan447.mpvium.ui.browser.states

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared shimmer placeholders for library screens.
 * Matches card geometry (2:3 posters, 16:9 continue-watching, 360dp hero)
 * so content swaps in place without layout jumps.
 */
@Composable
fun shimmerBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
  val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
  val translate by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(durationMillis = 1200, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
    label = "skeleton_translate",
  )
  val baseColor = if (isDark) {
    Color.White.copy(alpha = 0.08f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest
  }
  val shimmerColor = if (isDark) {
    Color.White.copy(alpha = 0.16f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
  }
  return Brush.linearGradient(
    colors = listOf(baseColor, shimmerColor, baseColor),
    start = Offset(translate - 200f, 0f),
    end = Offset(translate, 0f),
  )
}

@Composable
private fun ShimmerBox(
  modifier: Modifier = Modifier,
  brush: Brush = shimmerBrush(),
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(brush),
  )
}

@Composable
fun HomeLoadingSkeleton(
  modifier: Modifier = Modifier,
  brush: Brush = shimmerBrush(),
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(top = 8.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    // Hero banner placeholder
    ShimmerBox(
      brush = brush,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(200.dp),
    )
    // Continue-watching row placeholder (16:9 cards)
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      userScrollEnabled = false,
    ) {
      items(3) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .width(220.dp)
              .aspectRatio(16f / 9f),
          )
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .width(140.dp)
              .height(14.dp),
          )
        }
      }
    }
    // Poster row placeholder (2:3 cards)
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      userScrollEnabled = false,
    ) {
      items(5) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .width(140.dp)
              .aspectRatio(2f / 3f),
          )
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .width(100.dp)
              .height(14.dp),
          )
        }
      }
    }
  }
}

@Composable
fun GridLoadingSkeleton(
  columns: Int,
  modifier: Modifier = Modifier,
  brush: Brush = shimmerBrush(),
  navigationBarHeight: Dp = 0.dp,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(columns),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 12.dp,
      end = 12.dp,
      top = 8.dp,
      bottom = navigationBarHeight + 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    userScrollEnabled = false,
  ) {
    items(9) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShimmerBox(
          brush = brush,
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f),
        )
        ShimmerBox(
          brush = brush,
          modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(14.dp),
        )
      }
    }
  }
}

@Composable
fun InsightsLoadingSkeleton(
  modifier: Modifier = Modifier,
  brush: Brush = shimmerBrush(),
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(4) {
      ShimmerBox(
        brush = brush,
        modifier = Modifier
          .fillMaxWidth()
          .height(96.dp),
      )
    }
  }
}

@Composable
fun DetailListLoadingSkeleton(
  rows: Int = 5,
  modifier: Modifier = Modifier,
  brush: Brush = shimmerBrush(),
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    repeat(rows) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBox(
          brush = brush,
          modifier = Modifier
            .width(56.dp)
            .height(56.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .fillMaxWidth(0.6f)
              .height(16.dp),
          )
          ShimmerBox(
            brush = brush,
            modifier = Modifier
              .fillMaxWidth(0.4f)
              .height(12.dp),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
  }
}
