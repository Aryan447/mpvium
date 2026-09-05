package app.aryan447.mpvium.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.folderlist.FolderListScreen
import app.aryan447.mpvium.ui.browser.selection.SelectionManager
import app.aryan447.mpvium.ui.streaming.home.StreamingHomeScreen
import app.aryan447.mpvium.ui.streaming.more.MoreLibraryScreen
import app.aryan447.mpvium.ui.streaming.movies.MoviesGridScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesGridScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
  private var persistentSelectedTab: Int = 0

  // Shared state that can be updated by FileSystemBrowserScreen
  @Volatile
  private var isInSelectionModeShared: Boolean = false  // Controls FAB visibility

  @Volatile
  private var shouldHideNavigationBar: Boolean = false  // Controls navigation bar visibility

  @Volatile
  private var isBrowserBottomBarVisible: Boolean = false  // Tracks browser bottom bar visibility

  @Volatile
  private var sharedVideoSelectionManager: Any? = null

  // Check if the selection contains only videos and update navigation bar visibility accordingly
  @Volatile
  private var onlyVideosSelected: Boolean = false

  // Track when permission denied screen is showing to hide FAB
  @Volatile
  private var isPermissionDenied: Boolean = false

  @Volatile
  private var pendingTabRequest: Int? = null

  /**
   * Request a tab switch from outside composition (e.g. launcher shortcuts).
   * Persisted so it survives process recreation, and consumed by [Content].
   */
  fun requestTab(index: Int) {
    val clamped = index.coerceIn(0, 4)
    persistentSelectedTab = clamped
    pendingTabRequest = clamped
  }

  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    this.isInSelectionModeShared = isInSelectionMode
    this.onlyVideosSelected = isOnlyVideosSelected
    this.sharedVideoSelectionManager = selectionManager

    // Only hide navigation bar when videos are selected AND in selection mode
    this.shouldHideNavigationBar = isInSelectionMode && isOnlyVideosSelected
  }

  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    this.isPermissionDenied = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = isPermissionDenied

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    this.shouldHideNavigationBar = !shouldShow
  }

  @Composable
  private fun RowScope.BottomNavItems(
    navItems: List<Triple<ImageVector, String, String>>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
  ) {
    navItems.forEachIndexed { index, (icon, label, desc) ->
      NavigationBarItem(
        icon = { Icon(icon, contentDescription = desc) },
        label = { Text(label) },
        selected = selectedTab == index,
        onClick = { onSelectTab(index) }
      )
    }
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by rememberSaveable {
      mutableIntStateOf(persistentSelectedTab)
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val appearancePreferences = koinInject<AppearancePreferences>()
    val pillNavigationBar by appearancePreferences.pillNavigationBar.collectAsState()

    fun selectTab(index: Int) {
      if (selectedTab != index) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedTab = index
      }
    }

    // Shared state (across the app)
    val isInSelectionMode = remember { mutableStateOf(isInSelectionModeShared) }
    val hideNavigationBar = remember { mutableStateOf(shouldHideNavigationBar) }
    val videoSelectionManager = remember { mutableStateOf<SelectionManager<*, *>?>(sharedVideoSelectionManager as? SelectionManager<*, *>) }

    // Check for state changes to ensure UI updates
    LaunchedEffect(Unit) {
      // Consume any pending external tab request (e.g. launcher shortcut)
      pendingTabRequest?.let { requested ->
        pendingTabRequest = null
        if (selectedTab != requested) {
          selectedTab = requested
        }
      }
      while (true) {
        if (isInSelectionMode.value != isInSelectionModeShared) {
          isInSelectionMode.value = isInSelectionModeShared
        }

        if (hideNavigationBar.value != shouldHideNavigationBar) {
          hideNavigationBar.value = shouldHideNavigationBar
        }

        pendingTabRequest?.let { requested ->
          pendingTabRequest = null
          if (selectedTab != requested) {
            selectedTab = requested
          }
        }

        val currentManager = sharedVideoSelectionManager as? SelectionManager<*, *>
        if (videoSelectionManager.value != currentManager) {
          videoSelectionManager.value = currentManager
        }

        delay(16)
      }
    }

    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      persistentSelectedTab = selectedTab
    }

    val navItems =
      listOf(
        Triple(Icons.Filled.Home, "Home", "Home"),
        Triple(Icons.Filled.Tv, "Series", "TV Shows"),
        Triple(Icons.Filled.Movie, "Movies", "Movies"),
        Triple(Icons.Filled.Folder, "Folders", "Folders"),
        Triple(Icons.Filled.VideoLibrary, "Library", "Library"),
      )

    // Adaptive navigation: rail on tablets / wide screens, bar on phones.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isWide = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
          if (!isWide) {
            AnimatedVisibility(
              visible = !hideNavigationBar.value,
              enter = slideInVertically(
                animationSpec = tween(durationMillis = 300),
                initialOffsetY = { fullHeight -> fullHeight }
              ),
              exit = slideOutVertically(
                animationSpec = tween(durationMillis = 300),
                targetOffsetY = { fullHeight -> fullHeight }
              )
            ) {
              if (pillNavigationBar) {
                // Floating pill bar: detached from screen edges, fully
                // rounded, with a soft shadow. The capsule Surface owns the
                // background + shadow while the inner bar stays transparent
                // with real content padding, so edge items and their
                // indicator pills never collide with the curved corners.
                // Window insets are disabled on the bar itself; the outer
                // padding clears the gesture navigation area instead.
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                  ) {
                    NavigationBar(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      containerColor = Color.Transparent,
                      tonalElevation = 0.dp,
                      windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                      BottomNavItems(
                        navItems = navItems,
                        selectedTab = selectedTab,
                        onSelectTab = ::selectTab,
                      )
                    }
                  }
                }
              } else {
                NavigationBar(
                  modifier = Modifier
                    .clip(
                      RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                      )
                    ),
                  tonalElevation = 3.dp
                ) {
                  BottomNavItems(
                    navItems = navItems,
                    selectedTab = selectedTab,
                    onSelectTab = ::selectTab,
                  )
                }
              }
            }
          }
        }
      ) { _ ->
        // NOTE: Scaffold padding is intentionally ignored here (as before):
        // screens draw edge-to-edge and handle status/navigation insets
        // themselves, including the manual bottom offset via
        // LocalNavigationBarHeight.
        Row(modifier = Modifier.fillMaxSize()) {
          if (isWide) {
            AnimatedVisibility(visible = !hideNavigationBar.value) {
              NavigationRail {
                navItems.forEachIndexed { index, (icon, label, desc) ->
                  NavigationRailItem(
                    icon = { Icon(icon, contentDescription = desc) },
                    label = { Text(label) },
                    selected = selectedTab == index,
                    onClick = { selectTab(index) }
                  )
                }
              }
            }
          }
          Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            val fabBottomPadding = if (isWide) 24.dp else 80.dp

        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            val slideDistance = with(density) { 48.dp.roundToPx() }
            val animationDuration = 250

            if (targetState > initialState) {
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { -slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            } else {
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { -slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            }
          },
          label = "tab_animation"
        ) { targetTab ->
          CompositionLocalProvider(
            LocalNavigationBarHeight provides fabBottomPadding
          ) {
            when (targetTab) {
              0 -> StreamingHomeScreen.Content()
              1 -> SeriesGridScreen.Content()
              2 -> MoviesGridScreen.Content()
              3 -> FolderListScreen.Content()
              4 -> MoreLibraryScreen.Content()
            }
          }
        }
          }
      }
    }
  }
}
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }
