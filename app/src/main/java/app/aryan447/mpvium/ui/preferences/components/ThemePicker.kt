package app.aryan447.mpvium.ui.preferences.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.ui.theme.AppTheme
import app.aryan447.mpvium.ui.theme.CustomTheme
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor

/**
 * A horizontal scrollable theme picker with preview cards.
 * Built-in themes come first, followed by the Theme Studio creation card and
 * every saved custom theme.
 *
 * Holding a custom theme lets the user change its colors or delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onEditCustomTheme: (CustomTheme) -> Unit = {},
    onDeleteCustomTheme: (CustomTheme) -> Unit = {},
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

    var themeForOptions by remember { mutableStateOf<CustomTheme?>(null) }
    var themeToDelete by remember { mutableStateOf<CustomTheme?>(null) }

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

            item(key = "theme_studio_create") {
                ThemeStudioCard(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateTheme()
                    },
                )
            }

            items(customThemes, key = { "custom_theme_${it.id}" }) { theme ->
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
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeForOptions = theme
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Modal sheet displayed on holding a custom theme
    if (themeForOptions != null) {
        val target = themeForOptions!!
        ModalBottomSheet(
            onDismissRequest = { themeForOptions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(target.primaryColor),
                    )
                    Column {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.pref_appearance_theme_options_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Surface(
                    onClick = {
                        val toEdit = target
                        themeForOptions = null
                        onEditCustomTheme(toEdit)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.pref_appearance_theme_edit),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(R.string.pref_appearance_theme_edit_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        val toDelete = target
                        themeForOptions = null
                        themeToDelete = toDelete
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.pref_appearance_theme_delete),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(R.string.pref_appearance_theme_delete_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog before deleting
    if (themeToDelete != null) {
        val target = themeToDelete!!
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            title = {
                Text(text = stringResource(R.string.my_canvas_delete_confirm_title))
            },
            text = {
                Text(text = stringResource(R.string.my_canvas_delete_confirm_message, target.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteCustomTheme(target)
                        themeToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.my_canvas_delete_theme),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { themeToDelete = null }) {
                    Text(text = stringResource(R.string.generic_cancel))
                }
            },
        )
    }
}
