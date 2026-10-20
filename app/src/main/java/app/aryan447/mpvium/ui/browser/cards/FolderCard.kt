package app.aryan447.mpvium.ui.browser.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import kotlin.math.pow
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolderCard(
  folder: VideoFolder,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isRecentlyPlayed: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  onThumbClick: () -> Unit = {},
  showDateModified: Boolean = false,
  customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  newVideoCount: Int = 0,
  customChipContent: @Composable (() -> Unit)? = null,
  isGridMode: Boolean = false,
  expressive: Boolean = false,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  // Remove the redundant folder name from the path
  val parentPath = folder.path.substringBeforeLast("/", folder.path)

  val cardInteraction = remember { MutableInteractionSource() }
  val thumbInteraction = remember { MutableInteractionSource() }
  val cardPressed by cardInteraction.collectIsPressedAsState()
  val thumbPressed by thumbInteraction.collectIsPressedAsState()
  val isPressed = cardPressed || thumbPressed
  val pressScale by animateFloatAsState(
    targetValue = if (expressive && isPressed) 0.98f else 1f,
    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    label = "folderCardPress",
  )
  val cardCorner by animateDpAsState(
    targetValue = when {
      !expressive -> 12.dp
      isSelected || isPressed -> 28.dp
      else -> 20.dp
    },
    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    label = "folderCardShape",
  )
  val indication = LocalIndication.current
  val cardColor = when {
    !expressive -> Color.Transparent
    isSelected -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
    else -> MaterialTheme.colorScheme.surfaceContainer
  }
  val cardBorder = if (expressive) {
    BorderStroke(
      1.dp,
      if (isSelected) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
      } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
      },
    )
  } else {
    null
  }
  val thumbContainer = if (expressive) {
    MaterialTheme.colorScheme.surfaceContainerHighest
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
  }
  val badgeShape = if (expressive) RoundedCornerShape(percent = 50) else RoundedCornerShape(4.dp)
  val badgeContainer = if (expressive) MaterialTheme.colorScheme.error else Color(0xFFD32F2F)
  val badgeContent = if (expressive) MaterialTheme.colorScheme.onError else Color.White

  Card(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
      }
      .combinedClickable(
        interactionSource = cardInteraction,
        indication = indication,
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = if (expressive) RoundedCornerShape(cardCorner) else CardDefaults.shape,
    colors = CardDefaults.cardColors(containerColor = cardColor),
    border = cardBorder,
  ) {
    if (isGridMode) {
      // GRID LAYOUT - Vertical arrangement
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            if (!expressive && isSelected) {
              MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            } else {
              Color.Transparent
            },
          )
          .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val folderGridColumnsPortrait by browserPreferences.folderGridColumnsPortrait.collectAsState()
        val folderGridColumnsLandscape by browserPreferences.folderGridColumnsLandscape.collectAsState()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val folderGridColumns = if (isLandscape) folderGridColumnsLandscape else folderGridColumnsPortrait
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
        val horizontalPadding = 32.dp
        val spacing = 8.dp

        val thumbWidthDp = if (folderGridColumns > 1) {
          // (screen - padding - total spacing) / columns
          val totalSpacing = spacing * (folderGridColumns - 1)
          ((screenWidthDp - horizontalPadding - totalSpacing) / folderGridColumns).coerceAtLeast(120.dp)
        } else {
          // single column fallback
          160.dp
        }
        val aspect = 16f / 9f
        val thumbHeightDp = thumbWidthDp / aspect

        val context = LocalContext.current

        Box(
          modifier = Modifier
            .width(thumbWidthDp)
            .height(thumbHeightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(thumbContainer)
            .combinedClickable(
              interactionSource = thumbInteraction,
              indication = indication,
              onClick = onThumbClick,
              onLongClick = onLongClick,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            customIcon ?: Icons.Filled.Folder,
            contentDescription = "Folder",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.secondary,
          )

          if (newVideoCount > 0) {
            Box(
              modifier =
                Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .clip(badgeShape)
                  .background(badgeContainer)
                  .padding(horizontal = if (expressive) 8.dp else 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = newVideoCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                ),
                color = badgeContent,
              )
            }
          }

          if (showTotalDurationChip && folder.totalDuration > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = formatDuration(folder.totalDuration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          folder.name,
          style = MaterialTheme.typography.titleSmall,
          color = if (isRecentlyPlayed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )

        if (showTotalVideosChip && folder.videoCount > 0) {
          Text(
            if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme. onSurfaceVariant,
          )
        }
      }
    } else {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(
              if (!expressive && isSelected) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
              } else {
                Color.Transparent
              },
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(thumbContainer)
              .combinedClickable(
                interactionSource = thumbInteraction,
                indication = indication,
                onClick = onThumbClick,
                onLongClick = onLongClick,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            customIcon ?: Icons.Filled.Folder,
            contentDescription = "Folder",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.secondary,
          )

          // Show new video count badge if folder contains new videos
          if (newVideoCount > 0) {
            Box(
              modifier =
                Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .clip(badgeShape)
                  .background(badgeContainer)
                  .padding(horizontal = if (expressive) 8.dp else 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = newVideoCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                ),
                color = badgeContent,
              )
            }
          }


        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            folder.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (isRecentlyPlayed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
          )
          if (showFolderPath && parentPath.isNotEmpty()) {
            Text(
              parentPath,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = maxLines,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
          } else {
            Spacer(modifier = Modifier.height(4.dp))
          }
          FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
          ) {
            // Render custom chip content first if provided
            var hasChip = false
          if (customChipContent != null) {
            customChipContent()
            hasChip = true
          }

          // Hide chips at storage root level (when videoCount is 0)
            if (showTotalVideosChip && folder.videoCount > 0) {
              Text(
                if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceContainerHigh,
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
              )
              hasChip = true
            }

            if (showTotalSizeChip && folder.totalSize > 0) {
              Text(
                formatFileSize(folder.totalSize),
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceContainerHigh,
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
              )
              hasChip = true
            }

            if (showTotalDurationChip && folder.totalDuration > 0) {
              Text(
                formatDuration(folder.totalDuration),
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceContainerHigh,
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
              )
              hasChip = true
            }



            if (showDateChip && folder.lastModified > 0) {
              Text(
                formatDate(folder.lastModified),
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceContainerHigh,
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

private fun formatDuration(durationMs: Long): String {
  val seconds = durationMs / 1000
  val hours = seconds / 3600
  val minutes = (seconds % 3600) / 60
  val secs = seconds % 60

  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m"
    else -> "${secs}s"
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
  val value = bytes / 1024.0.pow(digitGroups.toDouble())
  return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}

private fun formatDate(timestampSeconds: Long): String {
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}
