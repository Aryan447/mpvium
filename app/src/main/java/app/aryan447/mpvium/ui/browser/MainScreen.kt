package app.aryan447.mpvium.ui.browser

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.folderlist.FolderListScreen
import app.aryan447.mpvium.ui.streaming.home.StreamingHomeScreen
import app.aryan447.mpvium.ui.streaming.more.MoreLibraryScreen
import app.aryan447.mpvium.ui.streaming.movies.MoviesGridScreen
import app.aryan447.mpvium.ui.streaming.series.SeriesGridScreen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
  private var persistentSelectedTab: Int = 0

  // Reactive shared state so observers recompose on change instead of polling.
  // This avoids a wake-every-frame loop that would keep the CPU awake and
  // drain battery while MainScreen is composed.
  private val _hideNavigationBar = MutableStateFlow(false)
  val hideNavigationBarFlow: StateFlow<Boolean> = _hideNavigationBar.asStateFlow()

  private val _isPermissionDenied = MutableStateFlow(false)
  val isPermissionDeniedFlow: StateFlow<Boolean> = _isPermissionDenied.asStateFlow()

  private val _tabRequest = MutableStateFlow<Int?>(null)

  /**
   * Request a tab switch from outside composition (e.g. launcher shortcuts).
   * Persisted so it survives process recreation, and consumed by [Content].
   */
  fun requestTab(index: Int) {
    val clamped = index.coerceIn(0, 4)
    persistentSelectedTab = clamped
    _tabRequest.value = clamped
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
    // Only hide navigation bar when videos are selected AND in selection mode.
    // selectionManager is intentionally untracked: MainScreen never reads it,
    // keeping the parameter only for API compatibility with callers.
    _hideNavigationBar.value = isInSelectionMode && isOnlyVideosSelected
  }

  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    _isPermissionDenied.value = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = _isPermissionDenied.value

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    _hideNavigationBar.value = !shouldShow
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
    val backstack = LocalBackStack.current

    val appearancePreferences = koinInject<AppearancePreferences>()
    val pillNavigationBar by appearancePreferences.pillNavigationBar.collectAsState()

    fun selectTab(index: Int) {
      if (selectedTab != index) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedTab = index
      }
    }

    // Double-press back to exit at the tab root. Deeper screens handle
    // their own back presses with higher-priority BackHandlers.
    var lastBackPress by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = backstack.size == 1) {
      val now = System.currentTimeMillis()
      if (now - lastBackPress < 2000) {
        (context as? android.app.Activity)?.finish()
      } else {
        lastBackPress = now
        android.widget.Toast.makeText(
          context,
          context.getString(R.string.press_back_again_to_exit),
          android.widget.Toast.LENGTH_SHORT,
        ).show()
      }
    }

    // Reactive navigation-bar visibility shared with browser screens.
    // Previously this was synced via a 16ms polling loop; collecting the
    // flow suspends until a real change, using zero CPU while idle.
    val hideNavigationBar by hideNavigationBarFlow.collectAsState()
    val tabRequest by _tabRequest.collectAsState()

    // Consume any pending external tab request (e.g. launcher shortcut)
    LaunchedEffect(tabRequest) {
      tabRequest?.let { requested ->
        _tabRequest.value = null
        if (selectedTab != requested) {
          selectedTab = requested
        }
      }
    }

    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      persistentSelectedTab = selectedTab
    }

    val navItems =
      listOf(
        Triple(Icons.Filled.Home, "Home", "Home"),
        Triple(Icons.Filled.Tv, "Shows", "Shows"),
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
              visible = !hideNavigationBar,
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
                    border = BorderStroke(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
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
            AnimatedVisibility(visible = !hideNavigationBar) {
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
