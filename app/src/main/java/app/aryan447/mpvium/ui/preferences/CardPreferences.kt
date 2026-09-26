package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aryan447.mpvium.ui.theme.GlassKind
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassCardColors
import app.aryan447.mpvium.ui.theme.glassChrome
import app.aryan447.mpvium.ui.theme.glassHazeStyle
import app.aryan447.mpvium.ui.theme.glassRimStroke
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.theme.rememberGlassHazeState
import app.aryan447.mpvium.ui.utils.LocalBackStack

val SettingsCardShape = RoundedCornerShape(28.dp)
val SettingsTileShape = RoundedCornerShape(24.dp)
private val IconBoxShape = RoundedCornerShape(18.dp)

/**
 * Premium card container for grouped preferences.
 * Non-glass theme gets a hairline outline + soft tonal fill so groups
 * read as layered surfaces; glass theme keeps its specular treatment.
 */
@Composable
fun PreferenceCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val isGlass = LocalGlass.current
  val dark = androidx.compose.foundation.isSystemInDarkTheme()
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .glassSheen(SettingsCardShape, isGlass),
    shape = SettingsCardShape,
    colors = if (isGlass) glassCardColors() else CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    border = if (isGlass) glassRimStroke(dark) else BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      content()
    }
  }
}

/**
 * A divider to separate preferences within a card.
 */
@Composable
fun PreferenceDivider(
  modifier: Modifier = Modifier,
) {
  HorizontalDivider(
    modifier = modifier.padding(horizontal = 20.dp),
    thickness = 0.75.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
  )
}

/**
 * Premium eyebrow section header: uppercase kickers with letterspacing
 * plus an optional count pill, used by the dashboard and sub-screens.
 */
@Composable
fun PreferenceSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  count: Int? = null,
) {
  Row(
    modifier = modifier
      .padding(horizontal = 24.dp)
      .padding(top = 20.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp),
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false),
    )
    if (count != null && count > 0) {
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
      }
    }
  }
}

/**
 * Shared top app bar for all settings screens: bold title, circular back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
  title: String,
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior? = null,
  actions: @Composable () -> Unit = {},
  onBack: (() -> Unit)? = null,
) {
  val backstack = LocalBackStack.current
  val isGlass = LocalGlass.current
  val glassHaze = rememberGlassHazeState()
  val glassStyle = glassHazeStyle(
    isDark = androidx.compose.foundation.isSystemInDarkTheme(),
    kind = GlassKind.Bar,
  )
  TopAppBar(
    modifier = modifier.glassChrome(
      state = glassHaze,
      style = glassStyle,
      shape = RoundedCornerShape(0.dp),
      enabled = isGlass,
    ),
    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
      containerColor = if (isGlass) androidx.compose.ui.graphics.Color.Transparent
      else MaterialTheme.colorScheme.surface,
    ),
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      IconButton(onClick = { onBack?.invoke() ?: backstack.removeLastOrNull() }) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    },
    actions = { actions() },
    scrollBehavior = scrollBehavior,
  )
}

/**
 * Premium tonal icon box with a soft vertical gradient sheen.
 * Pass custom [container] / [content] colors for dashboard tiles; defaults
 * keep every sub-screen on the primary tonal treatment.
 */
@Composable
fun PreferenceIconBox(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  container: Color? = null,
  content: Color? = null,
  boxSize: androidx.compose.ui.unit.Dp = 44.dp,
  iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
  val scheme = MaterialTheme.colorScheme
  val base = container ?: scheme.primaryContainer
  Box(
    modifier =
      modifier
        .size(boxSize)
        .clip(IconBoxShape)
        .background(
          Brush.verticalGradient(
            colors = listOf(base, base.copy(alpha = 0.72f)),
          ),
        )
        .padding(0.dp),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      icon,
      contentDescription = contentDescription,
      tint = content ?: scheme.onPrimaryContainer,
      modifier = Modifier.size(iconSize),
    )
  }
}

/**
 * A switch preference row that plays a haptic tick on toggle.
 * Same API as the underlying library switch.
 */
@Composable
fun HapticSwitchPreference(
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  summary: @Composable (() -> Unit)? = null,
  icon: @Composable (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  me.zhanghai.compose.preference.SwitchPreference(
    value = value,
    onValueChange = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onValueChange(it)
    },
    title = title,
    modifier = modifier,
    enabled = enabled,
    summary = summary,
    icon = icon,
  )
}

/**
 * A navigation row for settings: gradient tonal icon, title, summary,
 * and a chevron in a subtle circular affordance.
 */
@Composable
fun SettingsPreferenceRow(
  title: String,
  summary: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  container: Color? = null,
  content: Color? = null,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        })
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PreferenceIconBox(icon = icon, container = container, content = content)
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

/**
 * Small category pill used by embedded + standalone search results.
 */
@Composable
fun SettingsCategoryPill(
  text: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
