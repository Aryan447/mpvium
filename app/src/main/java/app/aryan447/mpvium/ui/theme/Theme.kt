package app.aryan447.mpvium.ui.theme

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import org.koin.compose.koinInject
import kotlin.math.hypot

// ============================================================================
// Theme Transition Animation State & Components
// ============================================================================
/**
 * State for managing theme transition animations.
 * This class holds the animation state and provides methods to trigger transitions.
 */
class ThemeTransitionState {
    var isAnimating by mutableStateOf(false)
        private set
    var clickPosition by mutableStateOf(Offset.Zero)
        private set
    var screenshotBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var animationProgress = Animatable(0f)
        private set

    private var captureView: View? = null

    fun setView(view: View) {
        captureView = view
    }

    /**
     * Start a theme transition animation from the given position.
     * Captures the current screen and begins the reveal animation.
     * Will NOT start a new animation if one is already in progress.
     */
    fun startTransition(position: Offset) {
        // Don't allow new animation while one is in progress
        if (isAnimating) return

        captureView?.let { view ->
            try {
                // Capture before setting isAnimating to ensure we get the current state
                val bitmap = view.drawToBitmap()
                screenshotBitmap = bitmap
                clickPosition = position
                isAnimating = true
            } catch (e: Exception) {
                // If capture fails, just skip the animation
                screenshotBitmap = null
                isAnimating = false
            }
        }
    }

    fun finishTransition() {
        val oldBitmap = screenshotBitmap
        screenshotBitmap = null
        clickPosition = Offset.Zero
        isAnimating = false
        // Recycle after state is cleared
        oldBitmap?.recycle()
    }

    suspend fun resetProgress() {
        animationProgress.snapTo(0f)
    }
}

/**
 * CompositionLocal to provide ThemeTransitionState down the composition tree
 */
val LocalThemeTransitionState = staticCompositionLocalOf<ThemeTransitionState?> { null }

@Composable
fun rememberThemeTransitionState(): ThemeTransitionState {
    return remember { ThemeTransitionState() }
}

/**
 * Overlay composable that handles the circular reveal animation.
 * Uses Shape-based clipping for smooth Telegram-like rendering.
 */
@Composable
private fun ThemeTransitionOverlay(
    state: ThemeTransitionState,
    content: @Composable () -> Unit
) {
    // Animation disabled - just render content immediately
    content()
}

/**
 * Custom Shape that creates an inverse circular reveal effect.
 * The circle expands from center, and the shape clips TO THE AREA OUTSIDE the circle.
 */
private class CircularRevealShape(
    private val progress: Float,
    private val center: Offset,
    private val containerSize: Size,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val actualCenter = if (center == Offset.Zero) {
            Offset(size.width / 2f, size.height / 2f)
        } else {
            center
        }

        // Calculate the maximum radius needed to cover entire screen from center point
        val maxRadius = longestDistanceToCorner(size, actualCenter) * 1.1f
        val currentRadius = maxRadius * progress

        // Create a path that represents the area OUTSIDE the circle (inverse clip)
        val path = android.graphics.Path().apply {
            // Add the entire rectangle
            addRect(0f, 0f, size.width, size.height, android.graphics.Path.Direction.CW)
            // Subtract the circle (creates hole in the middle)
            addCircle(actualCenter.x, actualCenter.y, currentRadius, android.graphics.Path.Direction.CCW)
        }

        return androidx.compose.ui.graphics.Outline.Generic(
            path.asComposePath()
        )
    }

    private fun longestDistanceToCorner(size: Size, center: Offset): Float {
        val topLeft = hypot(center.x, center.y)
        val topRight = hypot(size.width - center.x, center.y)
        val bottomLeft = hypot(center.x, size.height - center.y)
        val bottomRight = hypot(size.width - center.x, size.height - center.y)
        return maxOf(topLeft, topRight, bottomLeft, bottomRight)
    }
}

/**
 * Extension to convert Android Path to Compose Path
 */
private fun android.graphics.Path.asComposePath(): androidx.compose.ui.graphics.Path {
    val composePath = androidx.compose.ui.graphics.Path()
    composePath.asAndroidPath().set(this)
    return composePath
}

@Composable
private fun ThemeTransitionContent(content: @Composable () -> Unit) {
    val state = LocalThemeTransitionState.current

    if (state != null) {
        ThemeTransitionOverlay(state = state, content = content)
    } else {
        content()
    }
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.Default }
val LocalThemeSource = staticCompositionLocalOf { ThemeSource.APP_THEME }

/**
 * Clean neutral cinema base color schemes for the content-reactive Aura theme.
 * Completely free of the purple/pink tints from [AppTheme.Default].
 */
fun getAuraDarkColorScheme(isAmoled: Boolean = false): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = Color(0xFFE5A93C),
        onPrimary = Color(0xFF3F2E00),
        primaryContainer = Color(0xFF5A4300),
        onPrimaryContainer = Color(0xFFFFDF9E),
        secondary = Color(0xFFD4C4A8),
        onSecondary = Color(0xFF38301B),
        secondaryContainer = Color(0xFF50462F),
        onSecondaryContainer = Color(0xFFF1E0C3),
        tertiary = Color(0xFFE5BEA2),
        onTertiary = Color(0xFF432A15),
        tertiaryContainer = Color(0xFF5C3F28),
        onTertiaryContainer = Color(0xFFFFDCBE),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = if (isAmoled) backgroundPureBlack else Color(0xFF111113),
        onBackground = Color(0xFFE4E4E8),
        surface = if (isAmoled) surfacePureBlack else Color(0xFF111113),
        onSurface = Color(0xFFE4E4E8),
        surfaceVariant = if (isAmoled) Color(0xFF242426) else Color(0xFF2A2A2F),
        onSurfaceVariant = Color(0xFFC6C6CD),
        outline = Color(0xFF8E8E95),
        outlineVariant = Color(0xFF45454B),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E4E8),
        inverseOnSurface = Color(0xFF1B1B1E),
        inversePrimary = Color(0xFF7A5800),
        surfaceDim = if (isAmoled) surfaceDimPureBlack else Color(0xFF0E0E10),
        surfaceBright = if (isAmoled) surfaceBrightPureBlack else Color(0xFF323236),
        surfaceContainerLowest = if (isAmoled) surfaceContainerLowestPureBlack else Color(0xFF09090B),
        surfaceContainerLow = if (isAmoled) surfaceContainerLowPureBlack else Color(0xFF161618),
        surfaceContainer = if (isAmoled) surfaceContainerPureBlack else Color(0xFF1C1C1F),
        surfaceContainerHigh = if (isAmoled) surfaceContainerHighPureBlack else Color(0xFF242428),
        surfaceContainerHighest = if (isAmoled) surfaceContainerHighestPureBlack else Color(0xFF2C2C31),
    )
}

fun getAuraLightColorScheme(): androidx.compose.material3.ColorScheme {
    return lightColorScheme(
        primary = Color(0xFF7A5800),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDF9E),
        onPrimaryContainer = Color(0xFF261900),
        secondary = Color(0xFF685E48),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1E0C3),
        onSecondaryContainer = Color(0xFF221A08),
        tertiary = Color(0xFF775940),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDCBE),
        onTertiaryContainer = Color(0xFF2B1705),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        background = Color(0xFFF9F9FB),
        onBackground = Color(0xFF1A1A1E),
        surface = Color(0xFFF9F9FB),
        onSurface = Color(0xFF1A1A1E),
        surfaceVariant = Color(0xFFE1E1E7),
        onSurfaceVariant = Color(0xFF45454B),
        outline = Color(0xFF75757C),
        outlineVariant = Color(0xFFC5C5CC),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2E2E33),
        inverseOnSurface = Color(0xFFF2F2F7),
        inversePrimary = Color(0xFFE5A93C),
        surfaceDim = Color(0xFFDADAE0),
        surfaceBright = Color(0xFFF9F9FB),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF3F3F7),
        surfaceContainer = Color(0xFFEDEDF2),
        surfaceContainerHigh = Color(0xFFE7E7EC),
        surfaceContainerHighest = Color(0xFFE1E1E7),
    )
}

// ============================================================================
// Main Theme
// ============================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpviumTheme(content: @Composable () -> Unit) {
    val preferences = koinInject<AppearancePreferences>()
    val contentThemeManager = koinInject<ContentThemeManager>()
    val darkMode by preferences.darkMode.collectAsState()
    val amoledMode by preferences.amoledMode.collectAsState()
    val appTheme by preferences.appTheme.collectAsState()
    val themeSource by preferences.themeSource.collectAsState()
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val useDarkTheme = when (darkMode) {
        DarkMode.Dark -> true
        DarkMode.Light -> false
        DarkMode.System -> darkTheme
    }

    val baseColorScheme = when {
        themeSource == ThemeSource.CONTENT -> {
            if (useDarkTheme) getAuraDarkColorScheme(amoledMode) else getAuraLightColorScheme()
        }
        appTheme.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            when {
                useDarkTheme && amoledMode -> {
                    dynamicDarkColorScheme(context).copy(
                        background = backgroundPureBlack,
                        surface = surfacePureBlack,
                        surfaceDim = surfaceDimPureBlack,
                        surfaceBright = surfaceBrightPureBlack,
                        surfaceContainerLowest = surfaceContainerLowestPureBlack,
                        surfaceContainerLow = surfaceContainerLowPureBlack,
                        surfaceContainer = surfaceContainerPureBlack,
                        surfaceContainerHigh = surfaceContainerHighPureBlack,
                        surfaceContainerHighest = surfaceContainerHighestPureBlack,
                    )
                }
                useDarkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        useDarkTheme && amoledMode -> appTheme.getAmoledColorScheme()
        useDarkTheme -> appTheme.getDarkColorScheme()
        else -> appTheme.getLightColorScheme()
    }

    // Dynamic content palette extraction: derives accents from Continue Watching poster
    val contentPalette by contentThemeManager.observePalette(
        isDark = useDarkTheme,
        isAmoled = amoledMode && useDarkTheme,
        surfaceColor = baseColorScheme.surface,
    ).collectAsState()

    val shouldUseContentTheme = themeSource == ThemeSource.CONTENT && contentPalette != null
    val targetPrimary = if (shouldUseContentTheme) contentPalette!!.primary else baseColorScheme.primary
    val targetSecondary = if (shouldUseContentTheme) contentPalette!!.secondary else baseColorScheme.secondary
    val targetTertiary = if (shouldUseContentTheme) contentPalette!!.tertiary else baseColorScheme.tertiary
    val targetPrimaryContainer = if (shouldUseContentTheme) contentPalette!!.primaryContainer else baseColorScheme.primaryContainer
    val targetSecondaryContainer = if (shouldUseContentTheme) contentPalette!!.secondaryContainer else baseColorScheme.secondaryContainer
    val targetTertiaryContainer = if (shouldUseContentTheme) contentPalette!!.tertiaryContainer else baseColorScheme.tertiaryContainer

    // Cross-fade on change matching the glow entrance timing (900ms)
    val animSpec = tween<Color>(durationMillis = 900, easing = FastOutSlowInEasing)
    val animatedPrimary by animateColorAsState(targetPrimary, animationSpec = animSpec, label = "ThemePrimary")
    val animatedSecondary by animateColorAsState(targetSecondary, animationSpec = animSpec, label = "ThemeSecondary")
    val animatedTertiary by animateColorAsState(targetTertiary, animationSpec = animSpec, label = "ThemeTertiary")
    val animatedPrimaryContainer by animateColorAsState(targetPrimaryContainer, animationSpec = animSpec, label = "ThemePrimaryContainer")
    val animatedSecondaryContainer by animateColorAsState(targetSecondaryContainer, animationSpec = animSpec, label = "ThemeSecondaryContainer")
    val animatedTertiaryContainer by animateColorAsState(targetTertiaryContainer, animationSpec = animSpec, label = "ThemeTertiaryContainer")

    val colorScheme = baseColorScheme.copy(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary,
        primaryContainer = animatedPrimaryContainer,
        secondaryContainer = animatedSecondaryContainer,
        tertiaryContainer = animatedTertiaryContainer,
    )

    // Provide theme transition state first, OUTSIDE MaterialTheme
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalThemeTransitionState provides rememberThemeTransitionState(),
        LocalAppTheme provides appTheme,
        LocalThemeSource provides themeSource,
        LocalGlass provides (appTheme == AppTheme.Glass && themeSource == ThemeSource.APP_THEME),
    ) {
        ThemeTransitionContent {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography,
                content = content,
                motionScheme = MotionScheme.expressive(),
            )
        }
    }
}

/**
 * Local theme override for detail screens (Movie/Series) to follow their own artwork poster
 * when [ThemeSource.CONTENT] is active.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentThemed(
    artworkUrl: String?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled || artworkUrl.isNullOrBlank()) {
        content()
        return
    }
    val contentThemeManager = koinInject<ContentThemeManager>()
    val currentScheme = MaterialTheme.colorScheme
    val background = currentScheme.background
    val isDark = background.luminance() < 0.5f
    val isAmoled = isDark && background.luminance() < 0.001f

    val palette by contentThemeManager.observePaletteForArtwork(
        artworkUrlOrPath = artworkUrl,
        isDark = isDark,
        isAmoled = isAmoled,
        surfaceColor = currentScheme.surface,
    ).collectAsState()

    if (palette == null) {
        content()
    } else {
        val animSpec = tween<Color>(durationMillis = 900, easing = FastOutSlowInEasing)
        val animatedPrimary by animateColorAsState(palette!!.primary, animationSpec = animSpec, label = "DetailPrimary")
        val animatedSecondary by animateColorAsState(palette!!.secondary, animationSpec = animSpec, label = "DetailSecondary")
        val animatedTertiary by animateColorAsState(palette!!.tertiary, animationSpec = animSpec, label = "DetailTertiary")
        val animatedPrimaryContainer by animateColorAsState(palette!!.primaryContainer, animationSpec = animSpec, label = "DetailPrimaryContainer")
        val animatedSecondaryContainer by animateColorAsState(palette!!.secondaryContainer, animationSpec = animSpec, label = "DetailSecondaryContainer")
        val animatedTertiaryContainer by animateColorAsState(palette!!.tertiaryContainer, animationSpec = animSpec, label = "DetailTertiaryContainer")

        MaterialTheme(
            colorScheme = currentScheme.copy(
                primary = animatedPrimary,
                secondary = animatedSecondary,
                tertiary = animatedTertiary,
                primaryContainer = animatedPrimaryContainer,
                secondaryContainer = animatedSecondaryContainer,
                tertiaryContainer = animatedTertiaryContainer,
            ),
            typography = MaterialTheme.typography,
            motionScheme = MaterialTheme.motionScheme,
            content = content,
        )
    }
}

enum class DarkMode(
    @StringRes val titleRes: Int,
) {
    Dark(R.string.pref_appearance_darkmode_dark),
    Light(R.string.pref_appearance_darkmode_light),
    System(R.string.pref_appearance_darkmode_system),
}

private const val RIPPLE_DRAGGED_ALPHA = .5f
private const val RIPPLE_FOCUSED_ALPHA = .6f
private const val RIPPLE_HOVERED_ALPHA = .4f
private const val RIPPLE_PRESSED_ALPHA = .6f

@OptIn(ExperimentalMaterial3Api::class)
val playerRippleConfiguration
    @Composable get() =
        RippleConfiguration(
            color = MaterialTheme.colorScheme.primaryContainer,
            rippleAlpha =
            RippleAlpha(
                draggedAlpha = RIPPLE_DRAGGED_ALPHA,
                focusedAlpha = RIPPLE_FOCUSED_ALPHA,
                hoveredAlpha = RIPPLE_HOVERED_ALPHA,
                pressedAlpha = RIPPLE_PRESSED_ALPHA,
            ),
        )
