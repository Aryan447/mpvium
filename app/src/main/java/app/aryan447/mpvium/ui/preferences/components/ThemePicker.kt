package app.aryan447.mpvium.ui.preferences.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.CustomTheme

private val StudioHeadingHeight = 22.dp
private val StudioColumnWidth = 100.dp

/**
 * A horizontal scrollable theme picker with preview cards.
 * Built-in themes come first, followed by the Theme Studio creation card and
 * every saved custom theme.
 */
@Composable
fun ThemePicker(
    currentTheme: AppTheme,
    isDarkMode: Boolean,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    customThemes: List<CustomTheme> = emptyList(),
    selectedCustomThemeId: String = "",
    onCustomThemeSelected: (CustomTheme) -> Unit = {},
    onCreateTheme: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val builtInCount = AppTheme.entries.size
    val selectedCustomIndex = customThemes.indexOfFirst { it.id == selectedCustomThemeId }
    val targetIndex =
        if (selectedCustomIndex >= 0) {
            builtInCount + 1 + selectedCustomIndex
        } else {
            AppTheme.entries.indexOf(currentTheme)
        }

    LaunchedEffect(targetIndex, customThemes.size, selectedCustomThemeId) {
        if (targetIndex >= 0) {
            listState.animateScrollToItem(maxOf(0, targetIndex - 1))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.pref_appearance_theme_picker_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(AppTheme.entries) { theme ->
                ThemePickerItem {
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = theme == currentTheme && selectedCustomThemeId.isEmpty(),
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (theme != currentTheme || selectedCustomThemeId.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onThemeSelected(theme)
                            }
                        },
                    )
                }
            }

            item(key = "theme_studio_create") {
                ThemePickerItem(header = stringResource(R.string.pref_appearance_theme_studio)) {
                    ThemeStudioCard(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCreateTheme()
                        },
                    )
                }
            }

            items(customThemes, key = { "custom_theme_${it.id}" }) { theme ->
                ThemePickerItem {
                    CustomThemePreviewCard(
                        theme = theme,
                        isSelected = theme.id == selectedCustomThemeId,
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (theme.id != selectedCustomThemeId) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCustomThemeSelected(theme)
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Reserves the same heading slot for every card so the Theme Studio heading
 * can sit above its group without pushing the creation card out of line with
 * the built-in previews.
 */
@Composable
private fun ThemePickerItem(
    header: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(StudioColumnWidth)
                .height(StudioHeadingHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (header != null) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        content()
    }
}
