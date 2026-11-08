package app.aryan447.mpvium.ui.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.PlayerButton
import app.aryan447.mpvium.preferences.PlayerLayout
import app.aryan447.mpvium.ui.player.Panels
import app.aryan447.mpvium.ui.player.PlayerActivity
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.player.Sheets
import app.aryan447.mpvium.ui.player.VideoAspect
import app.aryan447.mpvium.ui.player.controls.components.ControlsButton
import app.aryan447.mpvium.ui.theme.spacing
import dev.vivvvek.seeker.Segment

@Composable
fun TopLeftPlayerControlsLandscape(
  mediaTitle: String?,
  mediaSubtitle: String?,
  mediaSubtitleIsPrimary: Boolean,
  hideBackground: Boolean,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
) {
  val playlistModeEnabled = viewModel.hasPlaylistSupport()
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val colors = LocalPlayerColors.current
  val isCinema = LocalPlayerLayout.current == PlayerLayout.CINEMA

  Row(
    modifier = Modifier.width(IntrinsicSize.Max),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
  ) {
    ControlsButton(
      icon = Icons.AutoMirrored.Default.ArrowBack,
      onClick = onBackPress,
      modifier = Modifier.size(45.dp),
      hideBackground = hideBackground,
    )

    val titleInteractionSource = remember { MutableInteractionSource() }

    Box(
      modifier =
        Modifier
          .heightIn(min = 45.dp)
          .clip(RoundedCornerShape(50))
          .clickable(
            enabled = playlistModeEnabled,
            onClick = {
              clickEvent()
              onOpenSheet(Sheets.Playlist)
            },
          ),
    ) {
      Surface(
        shape = RoundedCornerShape(50),
        color = if (hideBackground) Color.Transparent else colors.controlFill,
        contentColor = if (hideBackground) colors.onVideoContent else colors.controlContent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = if (hideBackground) null else BorderStroke(1.dp, colors.controlBorder),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
          modifier =
            Modifier.padding(
              horizontal = if (hideBackground) MaterialTheme.spacing.extraSmall else MaterialTheme.spacing.medium,
              vertical = MaterialTheme.spacing.small,
            ),
        ) {
          viewModel.getPlaylistInfo()?.let { playlistInfo ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (hideBackground) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent,
              modifier = Modifier.padding(end = MaterialTheme.spacing.extraSmall),
            ) {
              Text(
                text = playlistInfo,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  shadow = if (hideBackground) Shadow(
                    color = Color.Black.copy(alpha = 0.75f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f,
                  ) else null,
                ),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                fontFamily = FontFamily.Monospace,
                color = if (hideBackground) colors.activeOnVideoContent else colors.accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          if (isCinema) {
            // Cinema's editorial marker: a short accent rule instead of a chip.
            Box(
              modifier =
                Modifier
                  .width(2.dp)
                  .height(18.dp)
                  .background(colors.accent, RoundedCornerShape(1.dp)),
            )
          }
          Column(
            modifier = Modifier.weight(1f, fill = false),
          ) {
            if (mediaSubtitle != null) {
              val textColor = if (hideBackground) colors.onVideoContent else colors.controlContent
              val firstLineIsPrimary = !mediaSubtitleIsPrimary
              Text(
                text = mediaTitle ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                  if (firstLineIsPrimary) {
                    if (hideBackground) {
                      MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                          color = Color.Black.copy(alpha = 0.85f),
                          offset = Offset(0f, 2f),
                          blurRadius = 6f,
                        ),
                      )
                    } else {
                      MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    }
                  } else {
                    MaterialTheme.typography.bodySmall.copy(
                      shadow = if (hideBackground) Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 1f),
                        blurRadius = 4f,
                      ) else null,
                    )
                  },
                color = textColor.copy(alpha = if (firstLineIsPrimary) 1f else 0.75f),
              )
              Text(
                text = mediaSubtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                  if (mediaSubtitleIsPrimary) {
                    if (hideBackground) {
                      MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                          color = Color.Black.copy(alpha = 0.85f),
                          offset = Offset(0f, 2f),
                          blurRadius = 6f,
                        ),
                      )
                    } else {
                      MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    }
                  } else {
                    MaterialTheme.typography.bodySmall.copy(
                      shadow = if (hideBackground) Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 1f),
                        blurRadius = 4f,
                      ) else null,
                    )
                  },
                color = textColor.copy(alpha = if (mediaSubtitleIsPrimary) 1f else 0.75f),
              )
            } else {
              Text(
                text = mediaTitle ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                  when {
                    isCinema ->
                      MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                          color = Color.Black.copy(alpha = 0.85f),
                          offset = Offset(0f, 2f),
                          blurRadius = 6f,
                        ),
                      )
                    hideBackground ->
                      MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                          color = Color.Black.copy(alpha = 0.85f),
                          offset = Offset(0f, 2f),
                          blurRadius = 6f,
                        ),
                      )
                    else -> MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                  },
                color = if (hideBackground) colors.onVideoContent else colors.controlContent,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun TopRightPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.aryan447.mpvium.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(
      if (LocalPlayerLayout.current == PlayerLayout.CINEMA) {
        MaterialTheme.spacing.smaller
      } else {
        MaterialTheme.spacing.extraSmall
      },
    ),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 45.dp,
      )
    }
  }
}

@Composable
fun BottomRightPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.aryan447.mpvium.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(
      if (LocalPlayerLayout.current == PlayerLayout.CINEMA) {
        MaterialTheme.spacing.smaller
      } else {
        MaterialTheme.spacing.extraSmall
      },
    ),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 45.dp,
      )
    }
  }
}

@Composable
fun BottomLeftPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.aryan447.mpvium.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(
      if (LocalPlayerLayout.current == PlayerLayout.CINEMA) {
        MaterialTheme.spacing.smaller
      } else {
        MaterialTheme.spacing.extraSmall
      },
    ),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 45.dp,
      )
    }
  }
}


