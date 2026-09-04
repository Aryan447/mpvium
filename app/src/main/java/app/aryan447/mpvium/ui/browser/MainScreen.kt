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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.folderlist.FolderListScreen
import app.aryan447.mpvium.ui.browser.selection.SelectionManager
import app.aryan447.mpvium.ui.streaming.home.StreamingHomeScreen
import app.aryan447.mpvium.ui.streaming.more.MoreLibraryScreen
import app.aryan447.mpvium.ui.streaming.movies.MoviesGridScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesGridScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

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
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by rememberSaveable {
      mutableIntStateOf(persistentSelectedTab)
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

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
      while (true) {
        if (isInSelectionMode.value != isInSelectionModeShared) {
          isInSelectionMode.value = isInSelectionModeShared
        }

        if (hideNavigationBar.value != shouldHideNavigationBar) {
          hideNavigationBar.value = shouldHideNavigationBar
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

    // Scaffold with bottom navigation bar
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
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
            NavigationBarItem(
              icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
              label = { Text("Home") },
              selected = selectedTab == 0,
              onClick = { selectTab(0) }
            )
            NavigationBarItem(
              icon = { Icon(Icons.Filled.Tv, contentDescription = "TV Shows") },
              label = { Text("Series") },
              selected = selectedTab == 1,
              onClick = { selectTab(1) }
            )
            NavigationBarItem(
              icon = { Icon(Icons.Filled.Movie, contentDescription = "Movies") },
              label = { Text("Movies") },
              selected = selectedTab == 2,
              onClick = { selectTab(2) }
            )
            NavigationBarItem(
              icon = { Icon(Icons.Filled.Folder, contentDescription = "Folders") },
              label = { Text("Folders") },
              selected = selectedTab == 3,
              onClick = { selectTab(3) }
            )
            NavigationBarItem(
              icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "Library") },
              label = { Text("Library") },
              selected = selectedTab == 4,
              onClick = { selectTab(4) }
            )
          }
        }
      }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        val fabBottomPadding = 80.dp

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

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }
