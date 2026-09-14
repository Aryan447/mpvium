package app.aryan447.mpvium.ui.browser

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    navItems: List<MainTab>,
    selectedCanonicalIndex: Int,
    showLabels: Boolean,
    onSelectTab: (Int) -> Unit,
  ) {
    navItems.forEach { tab ->
      NavigationBarItem(
        icon = { Icon(tab.icon, contentDescription = tab.label) },
        label = if (showLabels) ({ Text(tab.label) }) else null,
        alwaysShowLabel = showLabels,
        selected = selectedCanonicalIndex == tab.canonicalIndex,
        onClick = { onSelectTab(tab.canonicalIndex) }
      )
    }
  }

  /**
   * Compact tab item for the floating pill bar (issue #41).
   *
   * Unlike [NavigationBarItem], this never stretches to fill the screen
   * width: each tab keeps a fixed [itemWidth], so hiding tabs shrinks the
   * whole pill instead of leaving a full-width bar behind.
   */
  @Composable
  private fun PillNavItem(
    tab: MainTab,
    selected: Boolean,
    showLabels: Boolean,
    itemWidth: Dp,
    indicatorWidth: Dp,
    onClick: () -> Unit,
  ) {
    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    val selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
    val selectedLabelColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
      modifier = Modifier
        .width(itemWidth)
        .clip(RoundedCornerShape(24.dp))
        .selectable(
          selected = selected,
          onClick = onClick,
          role = Role.Tab,
        )
        .padding(vertical = if (showLabels) 4.dp else 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier
          .width(indicatorWidth)
          .height(32.dp)
          .clip(CircleShape)
          .background(if (selected) indicatorColor else Color.Transparent),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          tab.icon,
          contentDescription = tab.label,
          tint = if (selected) selectedIconColor else unselectedColor,
        )
      }
      if (showLabels) {
        Text(
          text = tab.label,
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) selectedLabelColor else unselectedColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    // Canonical 0..4 tab id (matches launcher shortcuts). The visible list
    // below may hide tabs, so selection is always stored canonically.
    var selectedTabId by rememberSaveable {
      mutableIntStateOf(persistentSelectedTab)
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val backstack = LocalBackStack.current

    val appearancePreferences = koinInject<AppearancePreferences>()
    val pillNavigationBar by appearancePreferences.pillNavigationBar.collectAsState()
    val showBottomNavLabels by appearancePreferences.showBottomNavLabels.collectAsState()
    val enabledBottomTabs by appearancePreferences.bottomNavTabs.collectAsState()
    val navItems = remember(enabledBottomTabs) {
      MainTab.visibleTabs(enabledBottomTabs)
    }

    fun selectTab(index: Int) {
      if (selectedTabId != index) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedTabId = index
      }
    }

    // If the selected tab was hidden in settings, fall back to the first
    // visible tab so content and the indicator never disagree.
    LaunchedEffect(navItems, selectedTabId) {
      if (navItems.none { it.canonicalIndex == selectedTabId }) {
        selectedTabId = navItems.first().canonicalIndex
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

    // Consume any pending external tab request (e.g. launcher shortcut).
    // A hidden tab falls back to the first visible tab so the bar always
    // shows a selected indicator.
    LaunchedEffect(tabRequest, navItems) {
      tabRequest?.let { requested ->
        _tabRequest.value = null
        val target =
          if (navItems.any { it.canonicalIndex == requested }) {
            requested
          } else {
            navItems.first().canonicalIndex
          }
        if (selectedTabId != target) {
          selectedTabId = target
        }
      }
    }

    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTabId) {
      persistentSelectedTab = selectedTabId
    }

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
                // Floating pill bar (issue #41): a compact capsule centered
                // on screen, like JusPlayer. Each tab keeps a fixed width so
                // hiding tabs shrinks the whole pill instead of leaving a
                // full-width bar behind. The outer Box clears the system
                // gesture area; content padding is intentionally fixed here
                // and the adaptive bottom offset is provided via
                // LocalNavigationBarHeight below.
                // Per-item widths stay compact on narrow phones: 5 visible
                // tabs must fit in ~328dp (360dp screen minus margins).
                val itemWidth = if (showBottomNavLabels) {
                  when {
                    navItems.size <= 3 -> 72.dp
                    navItems.size == 4 -> 64.dp
                    else -> 56.dp
                  }
                } else {
                  when {
                    navItems.size <= 3 -> 56.dp
                    navItems.size == 4 -> 52.dp
                    else -> 48.dp
                  }
                }
                val indicatorWidth = when {
                  itemWidth >= 72.dp -> 64.dp
                  itemWidth >= 60.dp -> 52.dp
                  itemWidth >= 52.dp -> 48.dp
                  else -> 44.dp
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Surface(
                    modifier = Modifier
                      .animateContentSize(
                        animationSpec = tween(
                          durationMillis = 300,
                          easing = FastOutSlowInEasing,
                        ),
                      ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
                  ) {
                    Row(
                      modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = if (showBottomNavLabels) 6.dp else 4.dp,
                      ),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      navItems.forEach { tab ->
                        PillNavItem(
                          tab = tab,
                          selected = selectedTabId == tab.canonicalIndex,
                          showLabels = showBottomNavLabels,
                          itemWidth = itemWidth,
                          indicatorWidth = indicatorWidth,
                          onClick = { selectTab(tab.canonicalIndex) },
                        )
                      }
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
                    selectedCanonicalIndex = selectedTabId,
                    showLabels = showBottomNavLabels,
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
                navItems.forEach { tab ->
                  NavigationRailItem(
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = if (showBottomNavLabels) ({ Text(tab.label) }) else null,
                    alwaysShowLabel = showBottomNavLabels,
                    selected = selectedTabId == tab.canonicalIndex,
                    onClick = { selectTab(tab.canonicalIndex) }
                  )
                }
              }
            }
          }
          Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Screens draw edge-to-edge and offset their content with this
            // value, so it must track the visible bar: the compact pill is
            // much shorter than the full-width bar, and both sit above the
            // system gesture inset on phones.
            val systemNavBottom =
              WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val fabBottomPadding = if (isWide) {
              24.dp
            } else {
              val barHeight = when {
                pillNavigationBar && showBottomNavLabels -> 76.dp
                pillNavigationBar -> 60.dp
                else -> 80.dp
              }
              val barBottomMargin = if (pillNavigationBar) 12.dp else 0.dp
              barHeight + barBottomMargin + systemNavBottom + 8.dp
            }

        AnimatedContent(
          targetState = selectedTabId,
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
