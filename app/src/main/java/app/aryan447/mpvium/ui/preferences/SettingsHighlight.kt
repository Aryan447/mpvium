package app.aryan447.mpvium.ui.preferences

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.theme.DarkMode
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * One-shot holder for settings navigation highlight targets.
 * Carrying (screen, key), consumed and cleared on first composition of the target screen
 * so back-navigation and rotation never replay the highlight.
 */
object SettingsHighlight {
  private var pendingTarget: Pair<Screen, String>? = null

  @Synchronized
  fun set(screen: Screen, key: String) {
    pendingTarget = screen to key
  }

  @Synchronized
  fun consume(screen: Screen): String? {
    val current = pendingTarget
    return if (current != null && current.first == screen) {
      pendingTarget = null
      current.second
    } else {
      null
    }
  }

  @Synchronized
  fun clear() {
    pendingTarget = null
  }
}

/**
 * Shared navigation helper for settings search results.
 * Used by both inline dashboard search and standalone search screen to prevent drift.
 */
fun openSearchResult(
  backstack: NavBackStack<Screen>,
  preference: SearchablePreference,
) {
  SettingsHighlight.set(preference.screen, preference.key)
  backstack.add(preference.screen)
}

/**
 * State for managing the scroll jump and highlight of a settings row.
 */
@Stable
class SettingsHighlightState internal constructor(
  val screen: Screen,
  val listState: LazyListState,
  val keyToIndex: Map<String, Int>,
  val targetKey: String?,
) {
  var activeHighlightKey by mutableStateOf<String?>(null)
    private set

  fun isHighlighted(key: String): Boolean = activeHighlightKey == key

  fun onHighlightFinished(key: String) {
    if (activeHighlightKey == key) {
      activeHighlightKey = null
    }
  }

  suspend fun runHighlight() {
    if (targetKey == null) return
    val index = keyToIndex[targetKey]
    if (index == null) {
      Log.d("SettingsHighlight", "Unknown highlight key '$targetKey' for screen $screen")
      return
    }

    try {
      listState.animateScrollToItem(index)
    } catch (e: Exception) {
      Log.d("SettingsHighlight", "Failed to scroll to index $index", e)
    }

    activeHighlightKey = targetKey
  }
}

/**
 * Creates and remembers a [SettingsHighlightState] for a preferences screen.
 * Consumes the highlight target once on first composition.
 */
@Composable
fun rememberSettingsHighlightState(
  screen: Screen,
  listState: LazyListState,
  keyToIndex: Map<String, Int>,
): SettingsHighlightState {
  val targetKey = remember { SettingsHighlight.consume(screen) }
  val state = remember(screen, listState) {
    SettingsHighlightState(
      screen = screen,
      listState = listState,
      keyToIndex = keyToIndex,
      targetKey = targetKey,
    )
  }

  LaunchedEffect(targetKey) {
    state.runHighlight()
  }

  return state
}

/**
 * Attaches scroll-into-view and highlight animation to a preference row or section card.
 *
 * @param state the screen's [SettingsHighlightState]
 * @param key unique namespaced key matching [SearchablePreference.key]
 * @param title optional title to announce to screen readers; falls back to the registry title if omitted
 * @param isCard whether this target is a whole section card (affects shape and overlay layering)
 * @param shape the outline shape of the highlight tint
 */
/**
 * Attaches scroll-into-view and highlight animation to a preference row or section card.
 *
 * @param state the screen's [SettingsHighlightState]
 * @param key unique namespaced key matching [SearchablePreference.key]
 * @param title optional title to announce to screen readers; falls back to the registry title if omitted
 * @param isCard whether this target is a whole section card (affects shape and overlay layering)
 * @param shape the outline shape of the highlight tint
 */
@Composable
fun Modifier.settingsHighlightTarget(
  state: SettingsHighlightState,
  key: String,
  title: String? = null,
  isCard: Boolean = false,
  shape: Shape = if (isCard) SettingsCardShape else RoundedCornerShape(20.dp),
): Modifier {
  val isHighlighted = state.isHighlighted(key)
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val view = LocalView.current
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val preferences = koinInject<AppearancePreferences>()
  val amoledMode by preferences.amoledMode.collectAsState()
  val darkMode by preferences.darkMode.collectAsState()
  val useDark = when (darkMode) {
    DarkMode.Dark -> true
    DarkMode.Light -> false
    DarkMode.System -> isDark
  }
  val isAmoled = useDark && amoledMode

  // Tune alpha per light / dark / AMOLED:
  // light: ~0.28, dark: ~0.30, AMOLED: ~0.36
  val baseAlpha = when {
    isAmoled -> 0.36f
    useDark -> 0.30f
    else -> 0.28f
  }

  val animatable = remember { Animatable(0f) }

  LaunchedEffect(isHighlighted) {
    if (isHighlighted) {
      try {
        bringIntoViewRequester.bringIntoView()
      } catch (e: Exception) {
        Log.d("SettingsHighlight", "bringIntoView failed for key $key", e)
      }

      val displayTitle = title ?: SearchablePreferences.allPreferences
        .find { it.key == key }
        ?.let { pref -> pref.titleRes?.let { context.getString(it) } ?: pref.title }
      val announcement = if (!displayTitle.isNullOrBlank()) {
        "Highlighted: $displayTitle"
      } else {
        "Highlighted"
      }
      view.announceForAccessibility(announcement)

      val durationScale = try {
        Settings.Global.getFloat(
          context.contentResolver,
          Settings.Global.ANIMATOR_DURATION_SCALE,
          1.0f,
        )
      } catch (_: Exception) {
        1.0f
      }

      if (durationScale == 0f) {
        animatable.snapTo(baseAlpha)
        delay(600)
        animatable.snapTo(0f)
      } else {
        animatable.snapTo(baseAlpha)
        delay(600)
        animatable.animateTo(
          targetValue = 0f,
          animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing,
          ),
        )
      }

      state.onHighlightFinished(key)
    }
  }

  val currentAlpha = animatable.value
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer

  return this
    .bringIntoViewRequester(bringIntoViewRequester)
    .drawWithContent {
      if (isCard) {
        drawContent()
        if (currentAlpha > 0f) {
          drawHighlightOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = primaryContainer.copy(alpha = currentAlpha),
          )
        }
      } else {
        if (currentAlpha > 0f) {
          drawHighlightOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = primaryContainer.copy(alpha = currentAlpha),
          )
        }
        drawContent()
      }
    }
}

private fun DrawScope.drawHighlightOutline(outline: Outline, color: Color) {
  when (outline) {
    is Outline.Rectangle -> drawRect(
      color = color,
      topLeft = outline.rect.topLeft,
      size = outline.rect.size,
    )
    is Outline.Rounded -> drawRoundRect(
      color = color,
      topLeft = androidx.compose.ui.geometry.Offset(outline.roundRect.left, outline.roundRect.top),
      size = androidx.compose.ui.geometry.Size(outline.roundRect.width, outline.roundRect.height),
      cornerRadius = outline.roundRect.topLeftCornerRadius,
    )
    is Outline.Generic -> drawPath(path = outline.path, color = color)
  }
}
