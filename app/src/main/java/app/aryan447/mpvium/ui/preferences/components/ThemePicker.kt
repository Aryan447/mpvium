package app.aryan447.mpvium.ui.preferences.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.ThemeSource

/**
 * A horizontal scrollable theme picker with preview cards.
 * Displays all available themes with visual previews, including the dynamic Match Content theme option.
 */
@Composable
fun ThemePicker(
    currentTheme: AppTheme,
    themeSource: ThemeSource,
    isDarkMode: Boolean,
    onThemeSelected: (AppTheme) -> Unit,
    onThemeSourceSelected: (ThemeSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (themeSource == ThemeSource.CONTENT) {
            listState.animateScrollToItem(2)
        } else {
            val index = AppTheme.entries.indexOf(currentTheme)
            if (index >= 0) {
                // If index >= 2 (Cinema and onward), account for Aura at position 2
                val scrollIndex = if (index >= 2) index + 1 else index
                listState.animateScrollToItem(maxOf(0, scrollIndex - 1))
            }
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
            // 1. Default static theme
            item(key = "theme_default") {
                val theme = AppTheme.Default
                val isSelected = themeSource == ThemeSource.APP_THEME && currentTheme == theme
                ThemePreviewCard(
                    theme = theme,
                    isSelected = isSelected,
                    isDarkMode = isDarkMode,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSourceSelected(ThemeSource.APP_THEME)
                            onThemeSelected(theme)
                        }
                    },
                )
            }

            // 2. Dynamic (Material You)
            item(key = "theme_dynamic") {
                val theme = AppTheme.Dynamic
                val isSelected = themeSource == ThemeSource.APP_THEME && currentTheme == theme
                ThemePreviewCard(
                    theme = theme,
                    isSelected = isSelected,
                    isDarkMode = isDarkMode,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSourceSelected(ThemeSource.APP_THEME)
                            onThemeSelected(theme)
                        }
                    },
                )
            }

            // 3. Aura (Artistic Dynamic Content Theme)
            item(key = "theme_aura") {
                val isSelected = themeSource == ThemeSource.CONTENT
                ContentThemePreviewCard(
                    isSelected = isSelected,
                    isDarkMode = isDarkMode,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSourceSelected(ThemeSource.CONTENT)
                        }
                    },
                )
            }

            // 4. Cinema and remaining themes (Noir Cinema, Glass, Forest, etc.)
            items(
                items = AppTheme.entries.filter { it != AppTheme.Default && it != AppTheme.Dynamic },
                key = { "theme_${it.name}" },
            ) { theme ->
                val isSelected = themeSource == ThemeSource.APP_THEME && currentTheme == theme
                ThemePreviewCard(
                    theme = theme,
                    isSelected = isSelected,
                    isDarkMode = isDarkMode,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSourceSelected(ThemeSource.APP_THEME)
                            onThemeSelected(theme)
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
