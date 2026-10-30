package app.aryan447.mpvium.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.preferences.AppearancePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates Content-driven theming across the application.
 *
 * Derives a dynamic palette from the poster of the most recent in-progress
 * Continue Watching item, falling back to the last opened detail screen.
 */
class ContentThemeManager(
  private val context: Context,
  private val artworkStore: StreamingArtworkStore,
  private val appearancePreferences: AppearancePreferences,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val continueWatchingArtwork = MutableStateFlow<String?>(null)
  private val lastDetailArtwork = MutableStateFlow<String?>(
    appearancePreferences.lastDetailArtworkPath.get().takeIf { it.isNotBlank() }
  )

  /**
   * The current artwork source for the main application theme.
   * Priority: most recent Continue Watching poster -> last opened detail screen.
   */
  val currentArtworkSource: StateFlow<String?> = combine(
    continueWatchingArtwork,
    lastDetailArtwork,
  ) { cw, lastDetail ->
    cw ?: lastDetail
  }.distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, null)

  fun setContinueWatchingItem(artworkUrlOrPath: String?) {
    scope.launch(Dispatchers.IO) {
      val resolvedPath = resolveArtworkFilePath(artworkUrlOrPath)
      continueWatchingArtwork.value = resolvedPath
    }
  }

  fun onDetailScreenOpened(artworkUrlOrPath: String?) {
    scope.launch(Dispatchers.IO) {
      val resolvedPath = resolveArtworkFilePath(artworkUrlOrPath)
      if (!resolvedPath.isNullOrBlank()) {
        appearancePreferences.lastDetailArtworkPath.set(resolvedPath)
        lastDetailArtwork.value = resolvedPath
      }
    }
  }

  fun observePalette(
    isDark: Boolean,
    isAmoled: Boolean,
    surfaceColor: Color,
  ): StateFlow<ContentPalette?> {
    return currentArtworkSource.map { artworkPath ->
      if (artworkPath.isNullOrBlank()) {
        null
      } else {
        withContext(Dispatchers.IO) {
          ContentColorExtractor.extractFromArtworkPath(
            filePath = artworkPath,
            isDark = isDark,
            isAmoled = isAmoled,
            surfaceColor = surfaceColor,
          )
        }
      }
    }.flowOn(Dispatchers.IO)
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
  }

  fun observePaletteForArtwork(
    artworkUrlOrPath: String?,
    isDark: Boolean,
    isAmoled: Boolean,
    surfaceColor: Color,
  ): StateFlow<ContentPalette?> {
    val state = MutableStateFlow<ContentPalette?>(null)
    scope.launch(Dispatchers.IO) {
      val resolved = resolveArtworkFilePath(artworkUrlOrPath)
      val palette = if (resolved.isNullOrBlank()) {
        null
      } else {
        ContentColorExtractor.extractFromArtworkPath(
          filePath = resolved,
          isDark = isDark,
          isAmoled = isAmoled,
          surfaceColor = surfaceColor,
        )
      }
      state.value = palette
    }
    return state
  }

  suspend fun resolveArtworkFilePath(urlOrPath: String?): String? = withContext(Dispatchers.IO) {
    if (urlOrPath.isNullOrBlank()) return@withContext null

    val file = artworkStore.findArtworkFile(urlOrPath)
    if (file != null && file.isFile && file.length() > 0L) {
      return@withContext file.absolutePath
    }

    // If it's a URL that wasn't yet downloaded/cached, trigger an offline check / cache attempt
    if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
      artworkStore.getCachedFile(urlOrPath)?.absolutePath
    } else {
      null
    }
  }
}
