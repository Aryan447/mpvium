package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.theme.CustomTheme
import app.aryan447.mpvium.ui.theme.DarkMode
import app.aryan447.mpvium.ui.utils.LocalBackStack
import java.util.UUID
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private val ColorSaver = Saver<Color, Int>(
  save = { it.toArgb() },
  restore = { Color(it) },
)

private enum class ColorRole {
  Primary,
  Secondary,
  Tertiary,
}

private enum class CanvasPreviewMode {
  Player,
  Home,
}

/**
 * The Theme Studio editor (My Canvas).
 *
 * Allows users to choose unlimited colors for Primary, Secondary, and Tertiary
 * accents with HSV sliders and Hex input, preview live player controls
 * (seekbar, volume bar) and homepage elements, and edit or delete custom themes.
 */
@Serializable
data class MyCanvasScreen(val themeId: String? = null) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AppearancePreferences>()
    val backstack = LocalBackStack.current
    val haptic = LocalHapticFeedback.current
    val darkMode by preferences.darkMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isAppDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDark
    }

    val customThemes by preferences.customThemes.collectAsState()
    val existingTheme = remember(themeId, customThemes) {
      themeId?.let { id -> customThemes.find { it.id == id } }
    }

    var name by rememberSaveable(existingTheme?.id) {
      mutableStateOf(existingTheme?.name ?: "")
    }

    var primaryColor by rememberSaveable(existingTheme?.id, stateSaver = ColorSaver) {
      mutableStateOf(existingTheme?.primaryColor ?: Color(0xFFB4637A))
    }
    var secondaryColor by rememberSaveable(existingTheme?.id, stateSaver = ColorSaver) {
      mutableStateOf(existingTheme?.secondaryColor ?: Color(0xFFC2410C))
    }
    var tertiaryColor by rememberSaveable(existingTheme?.id, stateSaver = ColorSaver) {
      mutableStateOf(existingTheme?.tertiaryColor ?: Color(0xFF6D28D9))
    }

    var activeRole by rememberSaveable { mutableStateOf(ColorRole.Primary) }
    var previewMode by rememberSaveable { mutableStateOf(CanvasPreviewMode.Player) }
    var isDarkModePreview by rememberSaveable { mutableStateOf(isAppDark) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val resolvedId = rememberSaveable { themeId ?: UUID.randomUUID().toString() }
    val draft = remember(resolvedId, name, primaryColor, secondaryColor, tertiaryColor) {
      CustomTheme.fromColors(
        id = resolvedId,
        name = name.ifBlank { "Preview" },
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor,
      )
    }

    val activeColor = when (activeRole) {
      ColorRole.Primary -> primaryColor
      ColorRole.Secondary -> secondaryColor
      ColorRole.Tertiary -> tertiaryColor
    }

    fun updateActiveColor(color: Color) {
      when (activeRole) {
        ColorRole.Primary -> primaryColor = color
        ColorRole.Secondary -> secondaryColor = color
        ColorRole.Tertiary -> tertiaryColor = color
      }
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        SettingsTopBar(
          title = if (themeId != null) {
            stringResource(R.string.my_canvas_edit_title)
          } else {
            stringResource(R.string.my_canvas_title)
          },
          actions = {
            if (themeId != null) {
              IconButton(onClick = { showDeleteConfirmDialog = true }) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = stringResource(R.string.my_canvas_delete_theme),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
            TextButton(
              enabled = name.isNotBlank(),
              onClick = {
                val theme = draft.copy(name = name.trim())
                preferences.saveCustomTheme(theme)
                preferences.selectCustomTheme(theme)
                backstack.removeLastOrNull()
              },
            ) {
              Text(text = stringResource(R.string.my_canvas_save))
            }
          },
        )
      },
    ) { padding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // 1. Live Interactive Preview (Player vs Home)
        item {
          LivePreviewSection(
            theme = draft,
            isDarkMode = isDarkModePreview,
            previewMode = previewMode,
            onPreviewModeChange = { previewMode = it },
            onToggleDarkMode = { isDarkModePreview = !isDarkModePreview },
          )
        }

        // 2. What Each Color Changes Legend
        item {
          ColorRolesLegendCard(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
          )
        }

        // 3. Theme Name Input
        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(text = stringResource(R.string.my_canvas_name_label)) },
            placeholder = { Text(text = stringResource(R.string.my_canvas_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        // 4. Color Role Switcher (Primary / Secondary / Tertiary)
        item {
          ColorRoleSelector(
            activeRole = activeRole,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor,
            onSelectRole = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              activeRole = it
            },
          )
        }

        // 5. Unlimited Color Customizer (Hex + Hue/Sat/Val Sliders + Presets)
        item {
          UnlimitedColorPicker(
            color = activeColor,
            onColorChange = { updateActiveColor(it) },
          )
        }
      }
    }

    if (showDeleteConfirmDialog && existingTheme != null) {
      AlertDialog(
        onDismissRequest = { showDeleteConfirmDialog = false },
        title = { Text(text = stringResource(R.string.my_canvas_delete_confirm_title)) },
        text = {
          Text(text = stringResource(R.string.my_canvas_delete_confirm_message, existingTheme.name))
        },
        confirmButton = {
          TextButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              preferences.deleteCustomTheme(existingTheme.id)
              showDeleteConfirmDialog = false
              backstack.removeLastOrNull()
            },
          ) {
            Text(
              text = stringResource(R.string.my_canvas_delete_theme),
              color = MaterialTheme.colorScheme.error,
            )
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirmDialog = false }) {
            Text(text = stringResource(R.string.generic_cancel))
          }
        },
      )
    }
  }
}

// ============================================================================
// Preview Section
// ============================================================================

@Composable
private fun LivePreviewSection(
  theme: CustomTheme,
  isDarkMode: Boolean,
  previewMode: CanvasPreviewMode,
  onPreviewModeChange: (CanvasPreviewMode) -> Unit,
  onToggleDarkMode: () -> Unit,
) {
  val colorScheme = if (isDarkMode) theme.getDarkColorScheme() else theme.getLightColorScheme()

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TabRow(
        selectedTabIndex = previewMode.ordinal,
        modifier = Modifier
          .width(220.dp)
          .clip(RoundedCornerShape(12.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      ) {
        Tab(
          selected = previewMode == CanvasPreviewMode.Player,
          onClick = { onPreviewModeChange(CanvasPreviewMode.Player) },
          text = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp))
              Text(stringResource(R.string.my_canvas_preview_player), fontSize = 12.sp)
            }
          },
        )
        Tab(
          selected = previewMode == CanvasPreviewMode.Home,
          onClick = { onPreviewModeChange(CanvasPreviewMode.Home) },
          text = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
              Text(stringResource(R.string.my_canvas_preview_home), fontSize = 12.sp)
            }
          },
        )
      }

      IconButton(
        onClick = onToggleDarkMode,
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
      ) {
        Icon(
          imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
          contentDescription = "Toggle Light/Dark Preview",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
          shape = RoundedCornerShape(18.dp),
        ),
    ) {
      when (previewMode) {
        CanvasPreviewMode.Player -> PlayerLivePreview(colorScheme = colorScheme)
        CanvasPreviewMode.Home -> HomepageLivePreview(colorScheme = colorScheme)
      }
    }
  }
}

/**
 * A realistic preview of the player controls screen, highlighting how the
 * primary accent styles the seekbar progress track and the modern volume slider.
 */
@Composable
private fun PlayerLivePreview(colorScheme: ColorScheme) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(205.dp)
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF161922), Color(0xFF0C0E14))
        )
      )
      .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top row: Back / PiP on left, Modern Volume Pill & Brightness on right
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
          }
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          // Modern volume slider pill
          Surface(
            modifier = Modifier
              .width(115.dp)
              .height(28.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              // Volume track
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.CenterStart,
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorScheme.primary),
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
            }
          }

          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }

      // Center controls: Replay10, Play circle, Forward10
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Outlined.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.70f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Outlined.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }

      // Bottom section: Title & Actions, Seekbar with played primary track
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Big Buck Bunny",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          }

          // Action pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              Icon(Icons.Outlined.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
          }
        }

        // Seekbar track
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(text = "03:21", fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f))

          Box(
            modifier = Modifier
              .weight(1f)
              .height(14.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            // Unfilled track
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f)),
            )
            // Played track in primary color
            Box(
              modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colorScheme.primary),
            )
            // Thumb
            Box(
              modifier = Modifier
                .padding(start = 68.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(colorScheme.primary),
            )
          }

          Text(text = "10:34", fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f))
        }
      }
    }
  }
}

/**
 * A realistic preview of the main homepage screen showing:
 * - App title in primary accent
 * - Filter pills with primary active selection
 * - Video thumbnail progress bar in primary
 * - Folder icon in secondary accent
 * - Section count badges in tertiary accent
 * - Floating navigation bar with active tab in secondary container
 */
@Composable
private fun HomepageLivePreview(colorScheme: ColorScheme) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(265.dp)
      .background(colorScheme.background)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top bar: mpvium logo in primary color + search/refresh/settings icons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "mpvium",
            color = colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Ready for another episode?",
            color = colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(15.dp))
          Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(15.dp))
          Icon(Icons.Default.Settings, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(15.dp))
        }
      }

      // Filter chips: "All" active in primary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
          Text("All", color = colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        listOf("Shows", "Movies", "In Progress").forEach { tab ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
              .padding(horizontal = 8.dp, vertical = 3.dp),
          ) {
            Text(tab, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
          }
        }
      }

      // Section 1: Movies & Videos with Tertiary badge "3"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Movies & Videos",
            color = colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
          Box(
            modifier = Modifier
              .size(15.dp)
              .clip(CircleShape)
              .background(colorScheme.tertiary),
            contentAlignment = Alignment.Center,
          ) {
            Text("3", color = colorScheme.onTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
          }
        }
        Text("See all →", color = colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
      }

      // Mini video card with progress line in primary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.surfaceVariant),
          contentAlignment = Alignment.BottomStart,
        ) {
          // Watched progress bar in primary
          Box(
            modifier = Modifier
              .fillMaxWidth(0.55f)
              .height(2.5.dp)
              .background(colorScheme.primary),
          )
        }
        Column {
          Text("Big Buck Bunny", color = colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium)
          Text("2008 • 10:34", color = colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
      }

      // Section 2: Browse Folders with Tertiary badge "1" and Secondary Folder icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Browse Folders",
          color = colorScheme.onSurface,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
        )
        Box(
          modifier = Modifier
            .size(15.dp)
            .clip(CircleShape)
            .background(colorScheme.tertiary),
          contentAlignment = Alignment.Center,
        ) {
          Text("1", color = colorScheme.onTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Mini folder card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = colorScheme.secondary,
            modifier = Modifier.size(16.dp),
          )
          Text("Movies", color = colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium)
          Text("• 3 videos", color = colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
      }
    }

    // Floating pill navigation bar at bottom
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
          width = 1.dp,
          color = colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Active Home pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.secondaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.Home, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(13.dp))
              Text("Home", color = colorScheme.onSecondaryContainer, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
          Icon(Icons.Default.Folder, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
          Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
        }
      }
    }
  }
}

// ============================================================================
// Color Roles Legend Card
// ============================================================================

@Composable
private fun ColorRolesLegendCard(
  primary: Color,
  secondary: Color,
  tertiary: Color,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    ),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.my_canvas_preview_legend_title),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      LegendRow(
        color = primary,
        roleName = stringResource(R.string.my_canvas_color_primary),
        description = stringResource(R.string.my_canvas_primary_desc),
      )
      LegendRow(
        color = secondary,
        roleName = stringResource(R.string.my_canvas_color_secondary),
        description = stringResource(R.string.my_canvas_secondary_desc),
      )
      LegendRow(
        color = tertiary,
        roleName = stringResource(R.string.my_canvas_color_tertiary),
        description = stringResource(R.string.my_canvas_tertiary_desc),
      )
    }
  }
}

@Composable
private fun LegendRow(
  color: Color,
  roleName: String,
  description: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(12.dp)
        .clip(CircleShape)
        .background(color)
        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
    )
    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = roleName,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "• $description",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

// ============================================================================
// Role Selector (Primary / Secondary / Tertiary)
// ============================================================================

@Composable
private fun ColorRoleSelector(
  activeRole: ColorRole,
  primaryColor: Color,
  secondaryColor: Color,
  tertiaryColor: Color,
  onSelectRole: (ColorRole) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.my_canvas_accent_label),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ColorRoleChip(
        role = ColorRole.Primary,
        label = stringResource(R.string.my_canvas_color_primary),
        color = primaryColor,
        isSelected = activeRole == ColorRole.Primary,
        onClick = { onSelectRole(ColorRole.Primary) },
        modifier = Modifier.weight(1f),
      )
      ColorRoleChip(
        role = ColorRole.Secondary,
        label = stringResource(R.string.my_canvas_color_secondary),
        color = secondaryColor,
        isSelected = activeRole == ColorRole.Secondary,
        onClick = { onSelectRole(ColorRole.Secondary) },
        modifier = Modifier.weight(1f),
      )
      ColorRoleChip(
        role = ColorRole.Tertiary,
        label = stringResource(R.string.my_canvas_color_tertiary),
        color = tertiaryColor,
        isSelected = activeRole == ColorRole.Tertiary,
        onClick = { onSelectRole(ColorRole.Tertiary) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun ColorRoleChip(
  role: ColorRole,
  label: String,
  color: Color,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  }

  Surface(
    modifier = modifier.clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .background(color)
          .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

// ============================================================================
// Unlimited Color Customizer
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnlimitedColorPicker(
  color: Color,
  onColorChange: (Color) -> Unit,
) {
  val hsv = remember(color) {
    val arr = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), arr)
    arr
  }

  val hue = hsv[0]
  val saturation = hsv[1]
  val value = hsv[2]

  var hexText by remember(color) {
    mutableStateOf(String.format("#%06X", 0xFFFFFF and color.toArgb()))
  }

  val rainbowBrush = remember {
    Brush.horizontalGradient(
      listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
      )
    )
  }

  val saturationBrush = remember(hue, value) {
    Brush.horizontalGradient(
      listOf(
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))),
      )
    )
  }

  val valueBrush = remember(hue, saturation) {
    Brush.horizontalGradient(
      listOf(
        Color.Black,
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))),
      )
    )
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    ),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Swatch + Hex Input Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(
              width = 1.5.dp,
              color = MaterialTheme.colorScheme.outlineVariant,
              shape = RoundedCornerShape(12.dp),
            ),
        )

        OutlinedTextField(
          value = hexText,
          onValueChange = { input ->
            hexText = input
            val clean = input.trim().removePrefix("#")
            if (clean.length == 6) {
              runCatching {
                val parsed = android.graphics.Color.parseColor("#$clean")
                onColorChange(Color(parsed))
              }
            }
          },
          label = { Text(stringResource(R.string.my_canvas_hex)) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      // Sliders: Hue, Saturation, Brightness
      GradientSliderRow(
        label = stringResource(R.string.my_canvas_hue),
        valueText = "${hue.toInt()}°",
        value = hue,
        valueRange = 0f..360f,
        trackBrush = rainbowBrush,
        onValueChange = { newHue ->
          val newColorInt = android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturation, value))
          onColorChange(Color(newColorInt))
        },
      )

      GradientSliderRow(
        label = stringResource(R.string.my_canvas_saturation),
        valueText = "${(saturation * 100).toInt()}%",
        value = saturation,
        valueRange = 0f..1f,
        trackBrush = saturationBrush,
        onValueChange = { newSat ->
          val newColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, newSat, value))
          onColorChange(Color(newColorInt))
        },
      )

      GradientSliderRow(
        label = stringResource(R.string.my_canvas_value),
        valueText = "${(value * 100).toInt()}%",
        value = value,
        valueRange = 0f..1f,
        trackBrush = valueBrush,
        onValueChange = { newVal ->
          val newColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, newVal))
          onColorChange(Color(newColorInt))
        },
      )

      // Quick Curated Swatches
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.my_canvas_presets),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CuratedPresets.forEach { preset ->
            val isSelected = preset.toArgb() == color.toArgb()
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(preset)
                .border(
                  width = if (isSelected) 3.dp else 1.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  shape = CircleShape,
                )
                .clickable { onColorChange(preset) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GradientSliderRow(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  trackBrush: Brush,
  onValueChange: (Float) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp)
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(trackBrush)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(4.dp),
          ),
      )
      Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
          thumbColor = MaterialTheme.colorScheme.onSurface,
          activeTrackColor = Color.Transparent,
          inactiveTrackColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private val CuratedPresets = listOf(
  Color(0xFFB4637A), // Rose
  Color(0xFFE11D48), // Crimson
  Color(0xFFF97316), // Orange
  Color(0xFFF59E0B), // Amber
  Color(0xFFEAB308), // Gold
  Color(0xFF84CC16), // Lime
  Color(0xFF10B981), // Emerald
  Color(0xFF14B8A6), // Teal
  Color(0xFF06B6D4), // Cyan
  Color(0xFF0EA5E9), // Sky Blue
  Color(0xFF3B82F6), // Sapphire
  Color(0xFF6366F1), // Indigo
  Color(0xFF8B5CF6), // Violet
  Color(0xFFA855F7), // Purple
  Color(0xFFD946EF), // Fuchsia
  Color(0xFFEC4899), // Pink
  Color(0xFF9C4A00), // Rust
  Color(0xFF1B6B5A), // Pine
  Color(0xFF64748B), // Slate
  Color(0xFFFFFFFF), // Pure White
)
