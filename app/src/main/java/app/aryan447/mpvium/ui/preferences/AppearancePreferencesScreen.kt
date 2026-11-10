package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WrapText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.HomeSection
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.GesturePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ExpressiveChoiceRow
import app.aryan447.mpvium.ui.browser.MainTab
import app.aryan447.mpvium.ui.preferences.components.ThemePicker
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.DarkMode
import app.aryan447.mpvium.ui.theme.spacing
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import org.koin.compose.koinInject

@Serializable
object AppearancePreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val preferences = koinInject<AppearancePreferences>()
        val browserPreferences = koinInject<BrowserPreferences>()
        val gesturePreferences = koinInject<GesturePreferences>()
        val systemDarkTheme = isSystemInDarkTheme()

        val darkMode by preferences.darkMode.collectAsState()
        val darkModeLabels = DarkMode.entries.associateWith { stringResource(it.titleRes) }
        val appTheme by preferences.appTheme.collectAsState()
        val haptic = LocalHapticFeedback.current

        val unifiedTitlesTab by preferences.unifiedTitlesTab.collectAsState()
        val bottomNavTabs by preferences.bottomNavTabs.collectAsState()
        val tabsToShow = remember(unifiedTitlesTab) {
            if (unifiedTitlesTab) {
                listOf(
                    MainTab.HOME,
                    MainTab.TITLES,
                    MainTab.FOLDERS,
                    MainTab.LIBRARY,
                    MainTab.PLAYLISTS,
                    MainTab.AUDIO,
                )
            } else {
                listOf(
                    MainTab.HOME,
                    MainTab.SHOWS,
                    MainTab.MOVIES,
                    MainTab.FOLDERS,
                    MainTab.LIBRARY,
                    MainTab.PLAYLISTS,
                    MainTab.AUDIO,
                )
            }
        }

        // Determine if we're in dark mode for theme preview
        val isDarkMode = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }

        val listState = rememberLazyListState()
        val highlightState = rememberSettingsHighlightState(
            screen = AppearancePreferencesScreen,
            listState = listState,
            keyToIndex = mapOf(
                "appearance.theme" to 1,
                "appearance.amoled" to 1,
                "appearance.pill_nav" to 1,
                "appearance.bottom_nav_labels" to 1,
                "appearance.bottom_nav_tabs" to 3,
                "appearance.unified_titles_tab" to 3,
                "appearance.unlimited_name_lines" to 5,
                "appearance.show_unplayed_old_video_label" to 5,
                "appearance.unplayed_old_video_days" to 5,
                "appearance.auto_scroll" to 5,
                "appearance.tap_thumbnail_to_select" to 5,
                "appearance.show_network_thumbnails" to 5,
                "appearance.show_featured_hero" to 7,
                "appearance.home_sections" to 7,
            ),
        )

        Scaffold(
            topBar = {
                SettingsTopBar(title = stringResource(R.string.pref_appearance_title))
            },
        ) { padding ->
            ProvidePreferenceLocals {
                LazyColumn(
                    state = listState,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        PreferenceSectionHeader(
                            title = stringResource(id = R.string.pref_appearance_category_theme),
                            count = 5,
                        )
                    }

                    item {
                        PreferenceCard {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .settingsHighlightTarget(highlightState, "appearance.theme"),
                            ) {
                                // Dark mode selector
                                ExpressiveChoiceRow(
                                    options = DarkMode.entries,
                                    selected = darkMode,
                                    labelOf = { darkModeLabels.getValue(it) },
                                    onSelect = { preferences.darkMode.set(it) },
                                    equalWidth = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(MaterialTheme.spacing.medium),
                                )
                            }

                            PreferenceDivider()

                            // AMOLED mode state - need it before theme picker
                            val amoledMode by preferences.amoledMode.collectAsState()

                            // Theme picker - Aniyomi style
                            ThemePicker(
                                currentTheme = appTheme,
                                isDarkMode = isDarkMode,
                                onThemeSelected = { preferences.appTheme.set(it) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )

                            PreferenceDivider()

                            // AMOLED mode toggle
                            HapticSwitchPreference(
                                value = amoledMode,
                                onValueChange = { newValue ->
                                    preferences.amoledMode.set(newValue)
                                },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.DarkMode) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_amoled_mode_title)) },
                                summary = {
                                    Text(
                                        text = if (appTheme == AppTheme.Glass) {
                                            stringResource(id = R.string.pref_appearance_amoled_mode_summary) + " (not used by Glass)"
                                        } else {
                                            stringResource(id = R.string.pref_appearance_amoled_mode_summary)
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.amoled"),
                                enabled = darkMode != DarkMode.Light && appTheme != AppTheme.Glass
                            )

                            PreferenceDivider()

                            // Floating pill navigation bar toggle
                            val pillNavigationBar by preferences.pillNavigationBar.collectAsState()
                            HapticSwitchPreference(
                                value = pillNavigationBar,
                                onValueChange = { newValue ->
                                    preferences.pillNavigationBar.set(newValue)
                                },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.Smartphone) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_pill_nav_title)) },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_pill_nav_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.pill_nav"),
                            )

                            PreferenceDivider()

                            // Bottom navigation tab labels toggle (issue #17)
                            val showBottomNavLabels by preferences.showBottomNavLabels.collectAsState()
                            HapticSwitchPreference(
                                value = showBottomNavLabels,
                                onValueChange = { newValue ->
                                    preferences.showBottomNavLabels.set(newValue)
                                },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.TextFields) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_show_bottom_nav_labels_title)) },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_bottom_nav_labels_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.bottom_nav_labels"),
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(
                            title = stringResource(id = R.string.pref_appearance_category_seekbar),
                            count = 1,
                        )
                    }

                    item {
                        PreferenceCard {
                            val chaptersInSeekbar by preferences.chaptersInSeekbar.collectAsState()
                            HapticSwitchPreference(
                                value = chaptersInSeekbar,
                                onValueChange = { preferences.chaptersInSeekbar.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.Bookmarks) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_chapters_in_seekbar_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_chapters_in_seekbar_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(
                            title = stringResource(id = R.string.pref_appearance_category_bottom_nav),
                            count = tabsToShow.size + 1,
                        )
                    }

                    item {
                        PreferenceCard(
                            modifier = Modifier.settingsHighlightTarget(
                                state = highlightState,
                                key = "appearance.bottom_nav_tabs",
                                isCard = true,
                            ),
                        ) {
                            Text(
                                text = stringResource(id = R.string.pref_appearance_bottom_nav_tabs_summary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            HapticSwitchPreference(
                                value = unifiedTitlesTab,
                                onValueChange = { enabled ->
                                    preferences.unifiedTitlesTab.set(enabled)
                                    val current = preferences.bottomNavTabs.get()
                                    val updated = if (enabled) {
                                        (current - "SHOWS" - "MOVIES") + "TITLES"
                                    } else {
                                        (current - "TITLES") + setOf("SHOWS", "MOVIES")
                                    }
                                    if (updated.isNotEmpty()) {
                                        preferences.bottomNavTabs.set(updated)
                                    }
                                },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.Movie) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_unified_titles_tab_title)) },
                                summary = { Text(text = stringResource(id = R.string.pref_appearance_unified_titles_tab_summary)) },
                            )
                            PreferenceDivider()
                            tabsToShow.forEachIndexed { tabIndex, tab ->
                                val isEnabled = tab.key in bottomNavTabs
                                // Keep at least one tab: disable turning off the last one.
                                val canToggle = isEnabled && bottomNavTabs.size > 1 || !isEnabled
                                HapticSwitchPreference(
                                    value = isEnabled,
                                    onValueChange = { checked ->
                                        val updated =
                                            if (checked) bottomNavTabs + tab.key
                                            else bottomNavTabs - tab.key
                                        if (updated.isNotEmpty()) {
                                            preferences.bottomNavTabs.set(updated)
                                        }
                                    },
                                    icon = { PreferenceRowIcon(icon = tab.icon) },
                                    title = { Text(text = tab.label) },
                                    enabled = canToggle,
                                )
                                if (tabIndex < tabsToShow.size - 1) {
                                    PreferenceDivider()
                                }
                            }
                        }
                    }

                    item {
                        PreferenceSectionHeader(
                            title = stringResource(id = R.string.pref_appearance_category_file_browser),
                            count = 7,
                        )
                    }

                    item {
                        PreferenceCard {
                            val unlimitedNameLines by preferences.unlimitedNameLines.collectAsState()
                            HapticSwitchPreference(
                                value = unlimitedNameLines,
                                onValueChange = { preferences.unlimitedNameLines.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.WrapText) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.unlimited_name_lines"),
                            )

                            PreferenceDivider()

                            val showUnplayedOldVideoLabel by preferences.showUnplayedOldVideoLabel.collectAsState()
                            HapticSwitchPreference(
                                value = showUnplayedOldVideoLabel,
                                onValueChange = { preferences.showUnplayedOldVideoLabel.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.NewReleases) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.show_unplayed_old_video_label"),
                            )

                            PreferenceDivider()

                            val unplayedOldVideoDays by preferences.unplayedOldVideoDays.collectAsState()
                            SliderPreference(
                                value = unplayedOldVideoDays.toFloat(),
                                onValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.Schedule) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_unplayed_old_video_days_title)) },
                                valueRange = 1f..30f,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_unplayed_old_video_days_summary,
                                            unplayedOldVideoDays,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onSliderValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                sliderValue = unplayedOldVideoDays.toFloat(),
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.unplayed_old_video_days"),
                                enabled = showUnplayedOldVideoLabel
                            )

                            PreferenceDivider()

                            val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
                            HapticSwitchPreference(
                                value = autoScrollToLastPlayed,
                                onValueChange = { browserPreferences.autoScrollToLastPlayed.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.ArrowDownward) },
                                title = {
                                    Text(text = stringResource(R.string.pref_appearance_auto_scroll_title))
                                },
                                summary = {
                                    Text(
                                        text = stringResource(R.string.pref_appearance_auto_scroll_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.auto_scroll"),
                            )

                            PreferenceDivider()

                            val watchedThreshold by browserPreferences.watchedThreshold.collectAsState()
                            SliderPreference(
                                value = watchedThreshold.toFloat(),
                                onValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.CheckCircle) },
                                sliderValue = watchedThreshold.toFloat(),
                                onSliderValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_watched_threshold_title)) },
                                valueRange = 50f..100f,
                                valueSteps = 9,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_watched_threshold_summary,
                                            watchedThreshold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )

                            PreferenceDivider()

                            val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
                            HapticSwitchPreference(
                                value = tapThumbnailToSelect,
                                onValueChange = { gesturePreferences.tapThumbnailToSelect.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.TouchApp) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.tap_thumbnail_to_select"),
                            )

                            PreferenceDivider()

                            val showNetworkThumbnails by preferences.showNetworkThumbnails.collectAsState()
                            HapticSwitchPreference(
                                value = showNetworkThumbnails,
                                onValueChange = { preferences.showNetworkThumbnails.set(it) },
                                icon = { PreferenceRowIcon(icon = Icons.Outlined.Cloud) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_summary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.settingsHighlightTarget(highlightState, "appearance.show_network_thumbnails"),
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(
                            title = stringResource(id = R.string.pref_appearance_category_home),
                            count = HomeSection.entries.size,
                        )
                    }

                    item {
                        PreferenceCard(
                            modifier = Modifier.settingsHighlightTarget(
                                state = highlightState,
                                key = "appearance.home_sections",
                                isCard = true,
                            ),
                        ) {
                            Text(
                                text = stringResource(id = R.string.pref_appearance_home_sections_summary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            val orderRaw by preferences.homeSectionOrder.collectAsState()
                            val order = remember(orderRaw) { HomeSection.parseOrder(orderRaw) }
                            val showFeaturedHero by preferences.showFeaturedHero.collectAsState()
                            val showContinueWatching by preferences.showContinueWatching.collectAsState()
                            val showContinueListening by preferences.showContinueListening.collectAsState()
                            val showUpNext by preferences.showUpNext.collectAsState()
                            val showRecentlyAdded by preferences.showRecentlyAdded.collectAsState()
                            val showShowsSection by preferences.showShowsSection.collectAsState()
                            val showMoviesSection by preferences.showMoviesSection.collectAsState()
                            val showFoldersSection by preferences.showFoldersSection.collectAsState()
                            fun isVisible(section: HomeSection): Boolean = when (section) {
                                HomeSection.FEATURED -> showFeaturedHero
                                HomeSection.CONTINUE_WATCHING -> showContinueWatching
                                HomeSection.CONTINUE_LISTENING -> showContinueListening
                                HomeSection.UP_NEXT -> showUpNext
                                HomeSection.RECENTLY_ADDED -> showRecentlyAdded
                                HomeSection.SHOWS -> showShowsSection
                                HomeSection.MOVIES -> showMoviesSection
                                HomeSection.FOLDERS -> showFoldersSection
                            }
                            order.forEachIndexed { index, section ->
                                PreferenceDivider()
                                HomeSectionRow(
                                    title = section.title,
                                    icon = homeSectionIcon(section),
                                    visible = isVisible(section),
                                    onVisibleChange = { preferences.setHomeSectionVisible(section, it) },
                                    canMoveUp = index > 0,
                                    canMoveDown = index < order.lastIndex,
                                    onMoveUp = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        preferences.moveHomeSection(section, -1)
                                    },
                                    onMoveDown = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        preferences.moveHomeSection(section, 1)
                                    },
                                    modifier = if (section == HomeSection.FEATURED) {
                                        Modifier.settingsHighlightTarget(highlightState, "appearance.show_featured_hero")
                                    } else {
                                        Modifier
                                    },
                                )
                            }
                            PreferenceDivider()
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    preferences.resetHomeSections()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text(text = stringResource(id = R.string.pref_layout_reset_default))
                            }
                        }
                    }

                }
            }
        }
    }
}

private fun homeSectionIcon(section: HomeSection): ImageVector =
    when (section) {
        HomeSection.FEATURED -> Icons.Outlined.Movie
        HomeSection.CONTINUE_WATCHING -> Icons.Outlined.History
        HomeSection.CONTINUE_LISTENING -> Icons.Outlined.Audiotrack
        HomeSection.UP_NEXT -> Icons.Outlined.SkipNext
        HomeSection.RECENTLY_ADDED -> Icons.Outlined.NewReleases
        HomeSection.SHOWS -> Icons.Outlined.Tv
        HomeSection.MOVIES -> Icons.Outlined.VideoLibrary
        HomeSection.FOLDERS -> Icons.Outlined.Folder
    }

@Composable
private fun HomeSectionRow(
    title: String,
    icon: ImageVector,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreferenceIconBox(icon = icon)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(id = R.string.pref_appearance_home_section_move_up),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.pref_appearance_home_section_move_down),
            )
        }
        Switch(
            checked = visible,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onVisibleChange(it)
            },
        )
    }
}
