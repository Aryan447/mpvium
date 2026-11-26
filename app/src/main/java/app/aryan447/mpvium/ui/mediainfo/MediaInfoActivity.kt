package app.aryan447.mpvium.ui.mediainfo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.ui.theme.DarkMode
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.MpviumTheme
import app.aryan447.mpvium.ui.theme.glassButtonContainerColor
import app.aryan447.mpvium.ui.theme.glassButtonContentColor
import app.aryan447.mpvium.ui.theme.glassCardColors
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.glassRimStroke
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import app.aryan447.mpvium.ui.theme.spacing
import app.aryan447.mpvium.utils.media.MediaInfoOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

private enum class MediaInfoViewMode {
  OVERVIEW,
  TECHNICAL,
}

class MediaInfoActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val tag = "MediaInfoActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      val isDarkMode = dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme)

      enableEdgeToEdge(
        SystemBarStyle.auto(
          lightScrim = Color.White.toArgb(),
          darkScrim = Color.Transparent.toArgb(),
        ) { isDarkMode },
      )

      MpviumTheme {
        MediaInfoScreen(
          onBack = { finish() },
          isDarkMode = isDarkMode,
        )
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun MediaInfoScreen(
    onBack: () -> Unit,
    isDarkMode: Boolean,
  ) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultFileName = stringResource(R.string.mediainfo_default_filename)
    val noFileError = stringResource(R.string.mediainfo_no_file_error)
    val loadError = stringResource(R.string.mediainfo_load_error)

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf<MediaInfoOps.MediaInfoReport?>(null) }
    var qualityReport by remember { mutableStateOf<MediaCapabilityAnalyzer.QualityReport?>(null) }
    var fileName by remember { mutableStateOf(defaultFileName) }
    var viewMode by remember { mutableStateOf(MediaInfoViewMode.OVERVIEW) }

    val isGlass = LocalGlass.current
    val glassHaze = rememberGlassHazeState()
    val glassStyle = glassHazeStyle(isDark = isDarkMode, kind = GlassKind.Bar)

    LaunchedEffect(Unit) {
      val uri = when (intent?.action) {
        Intent.ACTION_SEND -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
          }
        }
        Intent.ACTION_VIEW -> intent.data
        else -> null
      }

      if (uri == null) {
        error = noFileError
        isLoading = false
        return@LaunchedEffect
      }

      fileName = resolveFileName(uri) ?: defaultFileName

      val result = MediaInfoOps.getMediaInfoReport(context, uri, fileName)
      result.onSuccess { loaded ->
        val analyzed = withContext(Dispatchers.Default) {
          val deviceCaps = MediaCapabilityAnalyzer.detectDeviceCapabilities(context)
          MediaCapabilityAnalyzer.evaluate(loaded.data, deviceCaps)
        }
        report = loaded
        qualityReport = analyzed
        isLoading = false
      }.onFailure { e ->
        error = e.message ?: loadError
        isLoading = false
      }
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = stringResource(R.string.mediainfo_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.mediainfo_back_desc),
              )
            }
          },
          actions = {
            val text = report?.textOutput
            if (!isLoading && error == null && text != null) {
              Row(modifier = Modifier.padding(end = MaterialTheme.spacing.small)) {
                FilledTonalIconButton(
                  onClick = { scope.launch { copyToClipboard(text, fileName) } },
                  modifier = if (isGlass) {
                    Modifier.size(48.dp).glassSheen(CircleShape, true)
                  } else {
                    Modifier
                  },
                  colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = glassButtonContainerColor(MaterialTheme.colorScheme.secondaryContainer),
                    contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSecondaryContainer),
                  ),
                ) {
                  Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.mediainfo_copy_desc),
                  )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.smaller))

                FilledTonalIconButton(
                  onClick = { scope.launch { shareMediaInfo(text, fileName) } },
                  modifier = if (isGlass) {
                    Modifier.size(48.dp).glassSheen(CircleShape, true)
                  } else {
                    Modifier
                  },
                  colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = glassButtonContainerColor(MaterialTheme.colorScheme.secondaryContainer),
                    contentColor = glassButtonContentColor(MaterialTheme.colorScheme.onSecondaryContainer),
                  ),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.mediainfo_share_desc),
                  )
                }
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
          modifier = Modifier.glassChrome(
            state = glassHaze,
            style = glassStyle,
            shape = RoundedCornerShape(0.dp),
            enabled = isGlass,
          ),
        )
      },
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        when {
          isLoading -> LoadingContent()
          error != null -> ErrorContent(error!!)
          report != null && qualityReport != null -> {
            val loadedReport = report!!
            val loadedQuality = qualityReport!!
            val overviewLabel = stringResource(R.string.mediainfo_mode_overview)
            val technicalLabel = stringResource(R.string.mediainfo_mode_technical)

            Column(modifier = Modifier.fillMaxSize()) {
              ExpressiveChoiceRow(
                options = listOf(MediaInfoViewMode.OVERVIEW, MediaInfoViewMode.TECHNICAL),
                selected = viewMode,
                labelOf = { mode ->
                  when (mode) {
                    MediaInfoViewMode.OVERVIEW -> overviewLabel
                    MediaInfoViewMode.TECHNICAL -> technicalLabel
                  }
                },
                onSelect = { viewMode = it },
                equalWidth = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.smaller,
                  ),
              )

              val enterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
              val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
              AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                  fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
                },
                label = "mediainfo_mode_switch",
              ) { mode ->
                when (mode) {
                  MediaInfoViewMode.OVERVIEW -> OverviewContent(
                    data = loadedReport.data,
                    quality = loadedQuality,
                    isDarkMode = isDarkMode,
                  )
                  MediaInfoViewMode.TECHNICAL -> TechnicalContent(
                    fullMediaInfoText = loadedReport.textOutput,
                    isDarkMode = isDarkMode,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  private fun resolveFileName(uri: Uri): String? =
    try {
      contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
          cursor.getString(nameIndex) ?: uri.lastPathSegment
        } else {
          uri.lastPathSegment
        }
      } ?: uri.lastPathSegment
    } catch (e: Exception) {
      Log.e(tag, "Error getting file name", e)
      uri.lastPathSegment
    }

  @Composable
  private fun LoadingContent() {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        CircularProgressIndicator(
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 4.dp,
          modifier = Modifier.size(48.dp),
        )
        Text(
          text = stringResource(R.string.mediainfo_analyzing),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  @Composable
  private fun ErrorContent(errorMessage: String) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(MaterialTheme.spacing.large),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(
          text = stringResource(R.string.mediainfo_error_prefix, errorMessage),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(MaterialTheme.spacing.large),
        )
      }
    }
  }

  @Composable
  private fun OverviewContent(
    data: MediaInfoOps.MediaInfoData,
    quality: MediaCapabilityAnalyzer.QualityReport,
    isDarkMode: Boolean,
  ) {
    val spacing = MaterialTheme.spacing
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        horizontal = spacing.medium,
        vertical = spacing.smaller,
      ),
      verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
      item(key = "hero") {
        HeroSummaryCard(
          data = data,
          quality = quality,
          isDarkMode = isDarkMode,
        )
      }

      if (quality.metrics.isNotEmpty()) {
        item(key = "breakdown_header") {
          SectionHeaderText(stringResource(R.string.mediainfo_section_breakdown))
        }

        items(
          items = quality.metrics,
          key = { it.kind.name },
        ) { metric ->
          MetricCompatibilityCard(
            metric = metric,
            isDarkMode = isDarkMode,
          )
        }
      }

      if (data.videoStreams.isNotEmpty() || data.audioStreams.isNotEmpty() || data.textStreams.isNotEmpty()) {
        item(key = "tracks_header") {
          SectionHeaderText(stringResource(R.string.mediainfo_section_tracks))
        }

        items(
          items = data.videoStreams,
          key = { "video_${it.streamIndex}" },
        ) { video ->
          TrackSummaryCard(
            icon = Icons.Filled.VideoFile,
            title = stringResource(R.string.mediainfo_track_video, video.streamIndex + 1),
            subtitle = buildList {
              add(MediaCapabilityAnalyzer.normalizeVideoCodec(video.format))
              if (video.widthPx > 0 && video.heightPx > 0) {
                add("${video.widthPx}×${video.heightPx}")
              }
              if (video.frameRate.isNotBlank()) add(video.frameRate)
              if (video.bitRate.isNotBlank()) add(video.bitRate)
            }.joinToString(" • "),
            isDefault = video.defaultStream.equals("Yes", ignoreCase = true),
            isForced = video.forcedStream.equals("Yes", ignoreCase = true),
            isDarkMode = isDarkMode,
          )
        }

        items(
          items = data.audioStreams,
          key = { "audio_${it.streamIndex}" },
        ) { audio ->
          TrackSummaryCard(
            icon = Icons.Filled.AudioFile,
            title = stringResource(R.string.mediainfo_track_audio, audio.streamIndex + 1) +
              audio.language.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty(),
            subtitle = buildList {
              add(audio.formatCommercial.ifBlank { audio.format })
              if (audio.channels.isNotBlank()) add(audio.channels)
              if (audio.bitRate.isNotBlank()) add(audio.bitRate)
              if (audio.samplingRate.isNotBlank()) add(audio.samplingRate)
            }.joinToString(" • "),
            isDefault = audio.defaultStream.equals("Yes", ignoreCase = true),
            isForced = audio.forcedStream.equals("Yes", ignoreCase = true),
            isDarkMode = isDarkMode,
          )
        }

        items(
          items = data.textStreams,
          key = { "sub_${it.streamIndex}" },
        ) { sub ->
          val subBadge = MediaCapabilityAnalyzer.normalizeSubtitleBadge(sub)
          val isSdh = MediaCapabilityAnalyzer.isSdhSubtitle(sub)
          TrackSummaryCard(
            icon = Icons.Filled.Subtitles,
            title = stringResource(R.string.mediainfo_track_subtitle, sub.streamIndex + 1) +
              sub.language.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty(),
            subtitle = listOfNotNull(
              subBadge.takeIf { it.isNotBlank() },
              sub.title.takeIf { it.isNotBlank() },
            ).joinToString(" • "),
            isDefault = sub.defaultStream.equals("Yes", ignoreCase = true),
            isForced = sub.forcedStream.equals("Yes", ignoreCase = true),
            isSdh = isSdh,
            isDarkMode = isDarkMode,
          )
        }
      }

      item(key = "footer") {
        FooterLabel()
      }
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun HeroSummaryCard(
    data: MediaInfoOps.MediaInfoData,
    quality: MediaCapabilityAnalyzer.QualityReport,
    isDarkMode: Boolean,
  ) {
    val isGlass = LocalGlass.current
    val spacing = MaterialTheme.spacing
    val (badgeBg, badgeFg) = verdictColors(quality.overallTone)
    val animatedSweep by animateFloatAsState(
      targetValue = (quality.overallScore / 100f) * 360f,
      animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
      label = "hero_score_ring",
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      colors = if (isGlass) {
        glassCardColors()
      } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
      },
      border = if (isGlass) {
        glassRimStroke(isDarkMode)
      } else {
        BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      },
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(
        modifier = Modifier.padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
          val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
          val arcColor = badgeFg
          Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val strokeWidth = 8.dp.toPx()
              drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
              )
              drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
              )
            }
            Text(
              text = "${quality.overallScore}",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
          ) {
            Text(
              text = stringResource(R.string.mediainfo_quality_score_label),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = badgeBg,
            ) {
              Text(
                text = verdictLabel(quality.overallVerdict),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = badgeFg,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            val summaryLine = listOfNotNull(
              data.general.duration.takeIf { it.isNotBlank() },
              data.general.fileSize.takeIf { it.isNotBlank() },
              data.general.overallBitRate.takeIf { it.isNotBlank() },
            ).joinToString(" • ")
            if (summaryLine.isNotBlank()) {
              Text(
                text = summaryLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        HorizontalDivider(
          thickness = 0.75.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        )

        TechBadgeRow(badges = quality.badges)
      }
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun MetricCompatibilityCard(
    metric: MediaCapabilityAnalyzer.MetricScore,
    isDarkMode: Boolean,
  ) {
    val isGlass = LocalGlass.current
    val spacing = MaterialTheme.spacing
    val (badgeBg, badgeFg) = verdictColors(metric.tone)
    val animatedProgress by animateFloatAsState(
      targetValue = (metric.score / 100f).coerceIn(0f, 1f),
      animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
      label = "metric_bar_${metric.kind.name}",
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = if (isGlass) {
        glassCardColors()
      } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
      },
      border = if (isGlass) {
        glassRimStroke(isDarkMode)
      } else {
        BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      },
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(
        modifier = Modifier.padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.smaller),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.smaller),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(36.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = metricIcon(metric.kind),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Column {
              Text(
                text = metricTitle(metric.kind),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = metric.primaryValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.smaller),
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = badgeBg,
            ) {
              Text(
                text = verdictLabel(metric.verdict),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = badgeFg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
            Text(
              text = stringResource(R.string.mediainfo_score_out_of_100, metric.score),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
          color = badgeFg,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        Text(
          text = metricExplanation(metric),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
          text = stringResource(R.string.mediainfo_your_device, metric.deviceValue),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }

  @Composable
  private fun TrackSummaryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDefault: Boolean,
    isForced: Boolean,
    isSdh: Boolean = false,
    isDarkMode: Boolean,
  ) {
    val isGlass = LocalGlass.current
    val spacing = MaterialTheme.spacing
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = if (isGlass) {
        glassCardColors()
      } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
      },
      border = if (isGlass) {
        glassRimStroke(isDarkMode)
      } else {
        BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      },
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (subtitle.isNotBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (isSdh) {
          Icon(
            imageVector = Icons.Filled.HearingDisabled,
            contentDescription = stringResource(R.string.mediainfo_badge_sdh),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }

        if (isDefault || isForced) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = if (isForced) {
                stringResource(R.string.mediainfo_badge_forced)
              } else {
                stringResource(R.string.mediainfo_badge_default)
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
  }

  @Composable
  private fun SectionHeaderText(text: String) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(
        start = MaterialTheme.spacing.extraSmall,
        top = MaterialTheme.spacing.extraSmall,
      ),
    )
  }

  @Composable
  private fun TechnicalContent(
    fullMediaInfoText: String,
    isDarkMode: Boolean,
  ) {
    val sections = remember(fullMediaInfoText) { parseMediaInfoText(fullMediaInfoText) }
    val spacing = MaterialTheme.spacing

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        horizontal = spacing.medium,
        vertical = spacing.smaller,
      ),
      verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
      items(
        items = sections,
        key = { "${it.name}_${it.properties.size}" },
      ) { section ->
        MediaInfoSection(section = section, isDarkMode = isDarkMode)
      }
      item(key = "tech_footer") {
        FooterLabel()
      }
    }
  }

  @Composable
  private fun FooterLabel() {
    Text(
      text = stringResource(R.string.mediainfo_footer),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = MaterialTheme.spacing.small),
      textAlign = TextAlign.Center,
    )
  }

  private fun parseMediaInfoText(text: String): List<InfoSection> {
    val sections = mutableListOf<InfoSection>()
    val lines = text.lines()

    var currentSectionName: String? = null
    val currentProperties = mutableListOf<Pair<String, String>>()

    for (line in lines) {
      when {
        line.trim().startsWith("=") || line.trim().isEmpty() -> continue
        line.contains("MEDIA INFO -") || line.contains("Generated by mpvium") -> continue
        !line.startsWith(" ") && !line.contains(":") && line.trim().isNotEmpty() -> {
          if (currentSectionName != null && currentProperties.isNotEmpty()) {
            sections.add(InfoSection(currentSectionName, currentProperties.toList()))
            currentProperties.clear()
          }
          currentSectionName = line.trim()
        }
        line.contains(":") -> {
          val parts = line.split(":", limit = 2)
          if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
              currentProperties.add(key to value)
            }
          }
        }
      }
    }

    if (currentSectionName != null && currentProperties.isNotEmpty()) {
      sections.add(InfoSection(currentSectionName, currentProperties.toList()))
    }

    return sections
  }

  @Composable
  private fun MediaInfoSection(
    section: InfoSection,
    isDarkMode: Boolean,
  ) {
    val isGlass = LocalGlass.current
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = if (isGlass) {
        glassCardColors()
      } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
      },
      shape = RoundedCornerShape(24.dp),
      border = if (isGlass) {
        glassRimStroke(isDarkMode)
      } else {
        BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      },
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(
        modifier = Modifier.padding(MaterialTheme.spacing.medium),
      ) {
        Text(
          text = section.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = MaterialTheme.spacing.small),
        )

        SelectionContainer {
          Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
          ) {
            section.properties.forEach { (key, value) ->
              PropertyRow(key, value)
            }
          }
        }
      }
    }
  }

  @Composable
  private fun PropertyRow(label: String, value: String) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .weight(1f)
          .padding(end = MaterialTheme.spacing.medium),
      )

      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1.5f),
      )
    }
  }

  @Composable
  private fun verdictColors(tone: MediaCapabilityAnalyzer.VerdictTone): Pair<Color, Color> =
    when (tone) {
      MediaCapabilityAnalyzer.VerdictTone.OPTIMAL ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
      MediaCapabilityAnalyzer.VerdictTone.OVERKILL ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
      MediaCapabilityAnalyzer.VerdictTone.ADAPTED ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
      MediaCapabilityAnalyzer.VerdictTone.LOW ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

  private fun metricIcon(kind: MediaCapabilityAnalyzer.MetricKind): ImageVector =
    when (kind) {
      MediaCapabilityAnalyzer.MetricKind.RESOLUTION -> Icons.Filled.DisplaySettings
      MediaCapabilityAnalyzer.MetricKind.BITRATE -> Icons.Filled.Speed
      MediaCapabilityAnalyzer.MetricKind.COLOR_HDR -> Icons.Filled.Palette
      MediaCapabilityAnalyzer.MetricKind.AUDIO -> Icons.Filled.GraphicEq
      MediaCapabilityAnalyzer.MetricKind.PLAYBACK_EFFICIENCY -> Icons.Filled.Memory
    }

  @Composable
  private fun metricTitle(kind: MediaCapabilityAnalyzer.MetricKind): String =
    when (kind) {
      MediaCapabilityAnalyzer.MetricKind.RESOLUTION -> stringResource(R.string.mediainfo_metric_resolution)
      MediaCapabilityAnalyzer.MetricKind.BITRATE -> stringResource(R.string.mediainfo_metric_bitrate)
      MediaCapabilityAnalyzer.MetricKind.COLOR_HDR -> stringResource(R.string.mediainfo_metric_color_hdr)
      MediaCapabilityAnalyzer.MetricKind.AUDIO -> stringResource(R.string.mediainfo_metric_audio)
      MediaCapabilityAnalyzer.MetricKind.PLAYBACK_EFFICIENCY -> stringResource(R.string.mediainfo_metric_playback)
    }

  @Composable
  private fun verdictLabel(key: MediaCapabilityAnalyzer.VerdictKey): String =
    when (key) {
      MediaCapabilityAnalyzer.VerdictKey.SPOT_ON -> stringResource(R.string.mediainfo_verdict_spot_on)
      MediaCapabilityAnalyzer.VerdictKey.OVERKILL -> stringResource(R.string.mediainfo_verdict_overkill)
      MediaCapabilityAnalyzer.VerdictKey.GREAT -> stringResource(R.string.mediainfo_verdict_great)
      MediaCapabilityAnalyzer.VerdictKey.GOOD -> stringResource(R.string.mediainfo_verdict_good)
      MediaCapabilityAnalyzer.VerdictKey.LOW_RES -> stringResource(R.string.mediainfo_verdict_low_res)
      MediaCapabilityAnalyzer.VerdictKey.BLOATED -> stringResource(R.string.mediainfo_verdict_bloated)
      MediaCapabilityAnalyzer.VerdictKey.REFERENCE -> stringResource(R.string.mediainfo_verdict_reference)
      MediaCapabilityAnalyzer.VerdictKey.BALANCED -> stringResource(R.string.mediainfo_verdict_balanced)
      MediaCapabilityAnalyzer.VerdictKey.COMPRESSED -> stringResource(R.string.mediainfo_verdict_compressed)
      MediaCapabilityAnalyzer.VerdictKey.NATIVE_HDR -> stringResource(R.string.mediainfo_verdict_native_hdr)
      MediaCapabilityAnalyzer.VerdictKey.TONE_MAPPED -> stringResource(R.string.mediainfo_verdict_tone_mapped)
      MediaCapabilityAnalyzer.VerdictKey.SMOOTH_10BIT -> stringResource(R.string.mediainfo_verdict_smooth_10bit)
      MediaCapabilityAnalyzer.VerdictKey.STANDARD_COLOR -> stringResource(R.string.mediainfo_verdict_standard_color)
      MediaCapabilityAnalyzer.VerdictKey.HARDWARE_READY -> stringResource(R.string.mediainfo_verdict_hardware_ready)
      MediaCapabilityAnalyzer.VerdictKey.CPU_HEAVY -> stringResource(R.string.mediainfo_verdict_cpu_heavy)
      MediaCapabilityAnalyzer.VerdictKey.SURROUND_READY -> stringResource(R.string.mediainfo_verdict_surround_ready)
      MediaCapabilityAnalyzer.VerdictKey.STUDIO_DOWNMIX -> stringResource(R.string.mediainfo_verdict_studio_downmix)
      MediaCapabilityAnalyzer.VerdictKey.RICH_AUDIO -> stringResource(R.string.mediainfo_verdict_rich_audio)
      MediaCapabilityAnalyzer.VerdictKey.THIN_AUDIO -> stringResource(R.string.mediainfo_verdict_thin_audio)
    }

  @Composable
  private fun metricExplanation(metric: MediaCapabilityAnalyzer.MetricScore): String =
    when {
      metric.kind == MediaCapabilityAnalyzer.MetricKind.AUDIO &&
        metric.verdict == MediaCapabilityAnalyzer.VerdictKey.OVERKILL ->
        stringResource(R.string.mediainfo_explain_audio_overkill)
      else -> when (metric.verdict) {
        MediaCapabilityAnalyzer.VerdictKey.OVERKILL -> stringResource(R.string.mediainfo_explain_resolution_overkill)
        MediaCapabilityAnalyzer.VerdictKey.SPOT_ON -> stringResource(R.string.mediainfo_explain_resolution_spot_on)
        MediaCapabilityAnalyzer.VerdictKey.GREAT,
        MediaCapabilityAnalyzer.VerdictKey.GOOD,
        -> stringResource(R.string.mediainfo_explain_resolution_good)
        MediaCapabilityAnalyzer.VerdictKey.LOW_RES -> stringResource(R.string.mediainfo_explain_resolution_low)
        MediaCapabilityAnalyzer.VerdictKey.BLOATED -> stringResource(R.string.mediainfo_explain_bitrate_bloated)
        MediaCapabilityAnalyzer.VerdictKey.REFERENCE -> stringResource(R.string.mediainfo_explain_bitrate_reference)
        MediaCapabilityAnalyzer.VerdictKey.BALANCED -> stringResource(R.string.mediainfo_explain_bitrate_balanced)
        MediaCapabilityAnalyzer.VerdictKey.COMPRESSED -> stringResource(R.string.mediainfo_explain_bitrate_compressed)
        MediaCapabilityAnalyzer.VerdictKey.NATIVE_HDR -> stringResource(R.string.mediainfo_explain_hdr_native)
        MediaCapabilityAnalyzer.VerdictKey.TONE_MAPPED -> stringResource(R.string.mediainfo_explain_hdr_tonemapped)
        MediaCapabilityAnalyzer.VerdictKey.SMOOTH_10BIT -> stringResource(R.string.mediainfo_explain_hdr_10bit)
        MediaCapabilityAnalyzer.VerdictKey.STANDARD_COLOR -> stringResource(R.string.mediainfo_explain_hdr_standard)
        MediaCapabilityAnalyzer.VerdictKey.HARDWARE_READY -> stringResource(R.string.mediainfo_explain_hw_ready)
        MediaCapabilityAnalyzer.VerdictKey.CPU_HEAVY -> stringResource(R.string.mediainfo_explain_cpu_heavy)
        MediaCapabilityAnalyzer.VerdictKey.SURROUND_READY -> stringResource(R.string.mediainfo_explain_audio_surround_ready)
        MediaCapabilityAnalyzer.VerdictKey.STUDIO_DOWNMIX -> stringResource(R.string.mediainfo_explain_audio_overkill)
        MediaCapabilityAnalyzer.VerdictKey.RICH_AUDIO -> stringResource(R.string.mediainfo_explain_audio_rich)
        MediaCapabilityAnalyzer.VerdictKey.THIN_AUDIO -> stringResource(R.string.mediainfo_explain_audio_thin)
      }
    }

  private data class InfoSection(
    val name: String,
    val properties: List<Pair<String, String>>,
  )

  private suspend fun copyToClipboard(content: String, fileName: String) {
    withContext(Dispatchers.Main) {
      val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
      val clip = android.content.ClipData.newPlainText("Media Info - $fileName", content)
      clipboard.setPrimaryClip(clip)
      Toast.makeText(this@MediaInfoActivity, getString(R.string.mediainfo_copied), Toast.LENGTH_SHORT).show()
    }
  }

  private suspend fun shareMediaInfo(content: String, fileName: String) {
    withContext(Dispatchers.IO) {
      try {
        val safeName = fileName.substringBeforeLast('.').ifBlank { "media" }
        val textFileName = "mediainfo_$safeName.txt"
        val file = File(cacheDir, textFileName)
        file.writeText(content)

        withContext(Dispatchers.Main) {
          val fileUri = FileProvider.getUriForFile(
            this@MediaInfoActivity,
            "${packageName}.provider",
            file,
          )

          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Media Info - $fileName")
            putExtra(Intent.EXTRA_TEXT, "Media information for: $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }

          startActivity(Intent.createChooser(shareIntent, getString(R.string.mediainfo_share_chooser)))
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(
            this@MediaInfoActivity,
            getString(R.string.mediainfo_share_failed, e.message ?: ""),
            Toast.LENGTH_LONG,
          ).show()
        }
      }
    }
  }
}
