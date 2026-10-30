package app.aryan447.mpvium.ui.preferences

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.NavBackStack
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.Screen
import kotlinx.coroutines.delay

private const val TAG = "SettingsHighlight"

/** The row keeps its tint for this long before the fade starts. */
private const val HIGHLIGHT_HOLD_MS = 600L

/** Fade from the tinted surface back to transparent. */
private const val HIGHLIGHT_FADE_MS = 1000L

/**
 * How long the highlight stays active before it is cleared. Covers the hold,
 * the fade, and any scroll the row itself still has to do to be fully visible.
 */
private const val HIGHLIGHT_CLEAR_MS = HIGHLIGHT_HOLD_MS + HIGHLIGHT_FADE_MS + 1000L

/**
 * One-shot hand-off for "open this settings page and point at this row".
 *
 * Settings routes are @Serializable objects, so the target is carried out of
 * band instead of through route arguments: the search result stores it here
 * before pushing the screen, and the target screen's first composition consumes
 * it. Consuming clears the request, so back navigation and configuration
 * changes never replay the highlight.
 */
object SettingsHighlight {
    internal data class Target(val screen: Screen, val key: String)

    private var pending: Target? = null

    /** Key of the row currently tinted on screen, if any. */
    internal var activeKey by mutableStateOf<String?>(null)
        private set

    fun request(screen: Screen, key: String) {
        pending = Target(screen, key)
    }

    internal fun consume(screen: Screen): String? {
        val target = pending ?: return null
        if (target.screen != screen) return null
        pending = null
        return target.key
    }

    internal fun activate(key: String) {
        activeKey = key
    }

    internal fun deactivate(key: String) {
        if (activeKey == key) activeKey = null
    }
}

/**
 * Single entry point for both search surfaces, so the dashboard search and the
 * standalone search route can never drift apart.
 */
fun openSettingsPreference(backstack: NavBackStack<Screen>, preference: SearchablePreference) {
    SettingsHighlight.request(preference.screen, preference.key)
    backstack.add(preference.screen)
}

/**
 * Consumes a pending highlight for [screen], scrolls [listState] to the item
 * that owns the key, then lets the row tint itself and be brought fully into
 * view. Unknown keys leave the page at the top and only log in debug builds.
 *
 * Call once per settings screen, before the LazyColumn is declared.
 */
@Composable
fun SettingsHighlightEffect(
    screen: Screen,
    listState: LazyListState?,
    anchors: Map<String, Int>,
) {
    // Consumed on the first composition and never re-read afterwards.
    val requestedKey = remember(screen) { SettingsHighlight.consume(screen) }

    LaunchedEffect(requestedKey) {
        val key = requestedKey ?: return@LaunchedEffect
        val itemIndex = anchors[key]
        if (itemIndex == null) {
            Log.d(TAG, "No anchor for '$key' on ${screen::class.simpleName}; opening at the top")
            return@LaunchedEffect
        }

        listState?.animateScrollToItem(itemIndex)
        SettingsHighlight.activate(key)
        try {
            delay(HIGHLIGHT_CLEAR_MS)
        } finally {
            SettingsHighlight.deactivate(key)
        }
    }
}

/**
 * Row container tint for a search target. Pass the registry key of the row;
 * null leaves the row untouched, which keeps rows without a search entry free
 * of highlight plumbing. Apply it to the row itself (not the card around it) so
 * one card holding several rows only tints the matching row.
 */
@Composable
fun Modifier.settingsHighlight(key: String?): Modifier {
    if (key == null) return this
    val visual = rememberSettingsHighlightVisual(key)
    return this
        .bringIntoViewRequester(visual.requester)
        .background(visual.color)
}

/**
 * Animated tint and bring-into-view requester for one highlighted container.
 * Shared by rows and by [PreferenceCard] (for results that point at a whole
 * section card).
 */
internal class SettingsHighlightVisual(
    val color: Color,
    val requester: BringIntoViewRequester,
)

@Composable
internal fun rememberSettingsHighlightVisual(key: String?): SettingsHighlightVisual {
    val requester = remember { BringIntoViewRequester() }
    if (key == null) {
        return SettingsHighlightVisual(color = Color.Transparent, requester = requester)
    }

    val active = SettingsHighlight.activeKey == key
    val context = LocalContext.current
    val view = LocalView.current
    val animationsEnabled = remember(context) { animatorDurationScale(context) > 0f }
    val fraction = remember { Animatable(0f) }

    // Leaving the screen (back navigation, rotation, the row scrolling away)
    // ends the highlight immediately so it can never replay on return.
    DisposableEffect(key) {
        onDispose { SettingsHighlight.deactivate(key) }
    }

    LaunchedEffect(active) {
        if (!active) {
            fraction.snapTo(0f)
            return@LaunchedEffect
        }
        fraction.snapTo(1f)
        // Let this row (and the item that owns it) finish layout first.
        withFrameNanos { }
        // The container can leave composition while the scroll settles.
        try {
            requester.bringIntoView()
        } catch (_: IllegalStateException) {
            // Row is gone; the item scroll already put the reader in place.
        }
        announceHighlight(context, view, key)
        delay(HIGHLIGHT_HOLD_MS)
        if (animationsEnabled) {
            fraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = HIGHLIGHT_FADE_MS.toInt(), easing = LinearEasing),
            )
        } else {
            fraction.snapTo(0f)
        }
    }

    val tint = settingsHighlightTint()
    val color =
        if (fraction.value > 0f) tint.copy(alpha = tint.alpha * fraction.value) else Color.Transparent
    return SettingsHighlightVisual(color = color, requester = requester)
}

/**
 * primaryContainer tint, slightly stronger on dark and AMOLED surfaces where a
 * 0.28 wash would otherwise barely read.
 */
@Composable
private fun settingsHighlightTint(): Color {
    val background = MaterialTheme.colorScheme.background
    val luminance = background.luminance()
    val isDark = luminance < 0.5f
    val isAmoled = isDark && luminance < 0.001f
    val alpha = when {
        isAmoled -> 0.36f
        isDark -> 0.30f
        else -> 0.24f
    }
    return MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
}

/** Announces "Highlighted: <title>" once, from the container that lights up. */
private fun announceHighlight(context: Context, view: View, key: String?) {
    val preference = key?.let(SearchablePreferences::byKey) ?: return
    val title = preference.titleRes?.let(context::getString) ?: preference.title
    if (title.isNullOrBlank()) return
    view.announceForAccessibility(context.getString(R.string.settings_highlight_announced, title))
}

/** System animator duration scale; 0 means the user turned animations off. */
private fun animatorDurationScale(context: Context): Float =
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
