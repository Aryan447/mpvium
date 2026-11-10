package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.audio.model.ContinueListeningItem
import app.aryan447.mpvium.ui.audio.components.AlbumArtImage
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassFrostColor
import app.aryan447.mpvium.ui.theme.glassRimBrush
import app.aryan447.mpvium.ui.theme.glassSheen

@Composable
fun ContinueListeningRow(
  items: List<ContinueListeningItem>,
  onItemClick: (ContinueListeningItem) -> Unit,
  modifier: Modifier = Modifier,
  title: String = stringResource(R.string.home_section_continue_listening),
  onItemRemove: ((ContinueListeningItem) -> Unit)? = null,
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
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.3).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      items(items, key = { it.id }) { item ->
        ContinueListeningCard(
          item = item,
          onClick = { onItemClick(item) },
          onRemove = onItemRemove?.let { remove -> { remove(item) } },
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueListeningCard(
  item: ContinueListeningItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onRemove: (() -> Unit)? = null,
) {
  val cardWidth = 220.dp
  val haptic = LocalHapticFeedback.current
  var showMenu by remember { mutableStateOf(false) }

  Box(modifier = modifier.width(cardWidth)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .combinedClickable(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
          onLongClick = if (onRemove != null) {
            {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showMenu = true
            }
          } else {
            null
          },
        ),
    ) {
      // Artwork with Progress Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      ) {
        AlbumArtImage(
          uri = item.artworkUri,
          contentDescription = item.title,
          modifier = Modifier.fillMaxSize(),
        )

        val isGlass = LocalGlass.current
        val dark = isSystemInDarkTheme()
        if (!isGlass) {
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
        }

        // Center Play Icon Button
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .size(36.dp)
            .clip(CircleShape)
            .background(
              if (isGlass) {
                glassFrostColor(isDark = dark, kind = GlassKind.Chip)
              } else {
                Color.Black.copy(alpha = 0.55f)
              }
            )
            .then(
              if (isGlass) {
                Modifier.border(1.dp, glassRimBrush(dark), CircleShape)
              } else {
                Modifier
              }
            )
            .glassSheen(CircleShape, isGlass),
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

      // Subtitle
      Text(
        text = item.subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 6.dp),
      )

      Spacer(modifier = Modifier.height(4.dp))
    }

    if (onRemove != null) {
      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
      ) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.continue_listening_remove)) },
          leadingIcon = {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          },
          onClick = {
            showMenu = false
            onRemove()
          },
        )
      }
    }
  }
}
