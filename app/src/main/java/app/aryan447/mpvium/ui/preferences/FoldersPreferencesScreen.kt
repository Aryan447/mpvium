package app.aryan447.mpvium.ui.preferences

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.selection.SelectionState
import app.aryan447.mpvium.ui.browser.states.EmptyState
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object FoldersPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<FoldersPreferences>()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val blacklistedFolders by preferences.blacklistedFolders.collectAsState()
    val whitelistedFolders by preferences.whitelistedFolders.collectAsState()
    val whitelistOnlyEnabled by preferences.whitelistOnlyEnabled.collectAsState()
    var availableFolders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var availableWhitelistFolders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isWhitelistLoading by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearWhitelistDialog by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    // Long-press multi-select states (bulk delete); preserved from BrowserTopBar era.
    var whitelistSelection by remember { mutableStateOf(SelectionState<String>()) }
    var blacklistSelection by remember { mutableStateOf(SelectionState<String>()) }

    val blacklistedFoldersList = remember(blacklistedFolders) { blacklistedFolders.toList() }
    val whitelistedFoldersList = remember(whitelistedFolders) { whitelistedFolders.toList() }

    val backstack = LocalBackStack.current
    val selectionCount = whitelistSelection.selectedCount + blacklistSelection.selectedCount
    val inSelectionMode = whitelistSelection.isInSelectionMode || blacklistSelection.isInSelectionMode
    fun clearSelection() {
      whitelistSelection = whitelistSelection.clear()
      blacklistSelection = blacklistSelection.clear()
    }

    Scaffold(
      topBar = {
        SettingsTopBar(
          title = if (inSelectionMode) {
            stringResource(
              R.string.selected_items,
              selectionCount,
              whitelistedFolders.size + blacklistedFolders.size,
            )
          } else {
            stringResource(R.string.pref_folders_title)
          },
          onBack = {
            if (inSelectionMode) clearSelection() else backstack.removeLastOrNull()
          },
          actions = {
            if (inSelectionMode) {
              IconButton(onClick = {
                if (whitelistSelection.isInSelectionMode) {
                  preferences.whitelistedFolders.set(whitelistedFolders - whitelistSelection.selectedIds)
                }
                if (blacklistSelection.isInSelectionMode) {
                  preferences.blacklistedFolders.set(blacklistedFolders - blacklistSelection.selectedIds)
                }
                clearSelection()
              }) {
                Icon(
                  Icons.Outlined.Delete,
                  contentDescription = stringResource(R.string.delete),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
              Box {
                IconButton(onClick = { showSelectionMenu = true }) {
                  Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.selection_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                DropdownMenu(
                  expanded = showSelectionMenu,
                  onDismissRequest = { showSelectionMenu = false },
                  containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
                ) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.select_all)) },
                    onClick = {
                      whitelistSelection = whitelistSelection.selectAll(whitelistedFoldersList)
                      blacklistSelection = blacklistSelection.selectAll(blacklistedFoldersList)
                      showSelectionMenu = false
                    },
                  )
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.invert_selection)) },
                    onClick = {
                      whitelistSelection = whitelistSelection.invertSelection(whitelistedFoldersList)
                      blacklistSelection = blacklistSelection.invertSelection(blacklistedFoldersList)
                      showSelectionMenu = false
                    },
                  )
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.deselect_all)) },
                    onClick = {
                      clearSelection()
                      showSelectionMenu = false
                    },
                  )
                }
              }
            } else if (blacklistedFolders.isNotEmpty()) {
              IconButton(onClick = { showClearAllDialog = true }) {
                Icon(
                  Icons.Outlined.Restore,
                  contentDescription = stringResource(R.string.pref_folders_clear_all),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          item {
            Text(
              text = stringResource(R.string.pref_folders_summary),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
          }

          item {
            PreferenceCard {
              HapticSwitchPreference(
                value = whitelistOnlyEnabled,
                onValueChange = preferences.whitelistOnlyEnabled::set,
                icon = { PreferenceIconBox(icon = Icons.Outlined.Visibility) },
                title = { Text(stringResource(R.string.pref_folders_whitelist_only_title)) },
                summary = {
                  Text(
                    text = stringResource(R.string.pref_folders_whitelist_only_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
              )
            }
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PreferenceSectionHeader(
                title = stringResource(R.string.pref_folders_whitelist_title),
                count = whitelistedFoldersList.size,
                modifier = Modifier.weight(1f),
              )
              if (whitelistedFolders.isNotEmpty()) {
                IconButton(
                  onClick = { showClearWhitelistDialog = true },
                  modifier = Modifier.padding(end = 16.dp),
                ) {
                  Icon(
                    Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.pref_folders_clear_all_whitelist),
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }

          item {
            if (whitelistedFolders.isEmpty()) {
              PreferenceCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                  Text(
                    text = stringResource(R.string.pref_folders_whitelist_empty_title),
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    text = stringResource(R.string.pref_folders_whitelist_empty_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            } else {
              PreferenceCard {
                whitelistedFoldersList.forEachIndexed { index, folderPath ->
                  FolderRow(
                    folderPath = folderPath,
                    icon = Icons.Outlined.FolderOpen,
                    selected = whitelistSelection.isSelected(folderPath),
                    inSelectionMode = whitelistSelection.isInSelectionMode,
                    onToggleSelect = { whitelistSelection = whitelistSelection.toggle(folderPath) },
                    onEnterSelection = { whitelistSelection = whitelistSelection.toggle(folderPath) },
                    onRemove = {
                      if (whitelistSelection.isSelected(folderPath)) {
                        whitelistSelection = whitelistSelection.toggle(folderPath)
                      }
                      val updated = whitelistedFolders.toMutableSet().apply { remove(folderPath) }
                      preferences.whitelistedFolders.set(updated)
                    },
                  )
                  if (index != whitelistedFoldersList.lastIndex) {
                    PreferenceDivider()
                  }
                }
              }
            }
          }

          item {
            PreferenceCard(
              modifier = Modifier.clickable {
                showAddWhitelistDialog = true
                isWhitelistLoading = true
                coroutineScope.launch(Dispatchers.IO) {
                  try {
                    availableWhitelistFolders = scanAllVideoFolders(context.applicationContext as Application)
                  } finally {
                    isWhitelistLoading = false
                  }
                }
              },
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                PreferenceIconBox(icon = Icons.Outlined.CreateNewFolder)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = stringResource(R.string.pref_folders_add_folder_whitelist),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          item {
            PreferenceSectionHeader(
              title = stringResource(R.string.pref_folders_blacklist),
              count = blacklistedFoldersList.size,
            )
          }

          if (whitelistOnlyEnabled) {
            item {
              Text(
                text = stringResource(R.string.pref_folders_whitelist_only_ignore_blacklist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
              )
            }
          }

          item {
            if (blacklistedFolders.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(200.dp),
                contentAlignment = Alignment.Center,
              ) {
                EmptyState(
                  icon = Icons.Outlined.FolderOff,
                  title = stringResource(R.string.pref_folders_empty_title),
                  message = stringResource(R.string.pref_folders_empty_message),
                )
              }
            } else {
              PreferenceCard {
                blacklistedFoldersList.forEachIndexed { index, folderPath ->
                  FolderRow(
                    folderPath = folderPath,
                    icon = Icons.Outlined.FolderOff,
                    selected = blacklistSelection.isSelected(folderPath),
                    inSelectionMode = blacklistSelection.isInSelectionMode,
                    onToggleSelect = { blacklistSelection = blacklistSelection.toggle(folderPath) },
                    onEnterSelection = { blacklistSelection = blacklistSelection.toggle(folderPath) },
                    onRemove = {
                      if (blacklistSelection.isSelected(folderPath)) {
                        blacklistSelection = blacklistSelection.toggle(folderPath)
                      }
                      val updated = blacklistedFolders.toMutableSet().apply { remove(folderPath) }
                      preferences.blacklistedFolders.set(updated)
                    },
                  )
                  if (index != blacklistedFoldersList.lastIndex) {
                    PreferenceDivider()
                  }
                }
              }
            }
          }

          item {
            PreferenceCard(
              modifier = Modifier.clickable {
                showAddDialog = true
                isLoading = true
                coroutineScope.launch(Dispatchers.IO) {
                  try {
                    availableFolders = scanAllVideoFolders(context.applicationContext as Application)
                  } finally {
                    isLoading = false
                  }
                }
              },
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                PreferenceIconBox(icon = Icons.Outlined.CreateNewFolder)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = stringResource(R.string.pref_folders_add_folder),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          item {
            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }
    }

    if (showAddDialog) {
      AddFolderDialog(
        folders = availableFolders,
        blacklistedFolders = blacklistedFolders,
        isLoading = isLoading,
        onDismiss = { showAddDialog = false },
        onAddFolders = { folderPaths ->
          val updated = blacklistedFolders.toMutableSet().apply { addAll(folderPaths) }
          preferences.blacklistedFolders.set(updated)
        },
      )
    }

    if (showAddWhitelistDialog) {
      AddFolderDialog(
        folders = availableWhitelistFolders,
        blacklistedFolders = whitelistedFolders,
        isLoading = isWhitelistLoading,
        onDismiss = { showAddWhitelistDialog = false },
        onAddFolders = { folderPaths ->
          val updated = whitelistedFolders.toMutableSet().apply { addAll(folderPaths) }
          preferences.whitelistedFolders.set(updated)
        },
      )
    }

    if (showClearWhitelistDialog) {
      AlertDialog(
        onDismissRequest = { showClearWhitelistDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text(stringResource(R.string.pref_folders_clear_all_whitelist_confirm_title)) },
        text = { Text(stringResource(R.string.pref_folders_clear_all_whitelist_confirm_message)) },
        confirmButton = {
          TextButton(
            onClick = {
              preferences.whitelistedFolders.set(emptySet())
              showClearWhitelistDialog = false
            },
          ) {
            Text(stringResource(R.string.generic_confirm))
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearWhitelistDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }

    if (showClearAllDialog) {
      AlertDialog(
        onDismissRequest = { showClearAllDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text(stringResource(R.string.pref_folders_clear_all_confirm_title)) },
        text = { Text(stringResource(R.string.pref_folders_clear_all_confirm_message)) },
        confirmButton = {
          TextButton(
            onClick = {
              preferences.blacklistedFolders.set(emptySet())
              showClearAllDialog = false
            },
          ) {
            Text(stringResource(R.string.generic_confirm))
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearAllDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
  folderPath: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onRemove: () -> Unit,
  selected: Boolean = false,
  inSelectionMode: Boolean = false,
  onToggleSelect: () -> Unit = {},
  onEnterSelection: () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { if (inSelectionMode) onToggleSelect() },
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onEnterSelection()
        },
      )
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PreferenceIconBox(icon = icon)
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folderPath.substringAfterLast('/'),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = folderPath,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    if (inSelectionMode) {
      Checkbox(
        checked = selected,
        onCheckedChange = { onToggleSelect() },
      )
    } else {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest)
          .clickable(onClick = onRemove),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = stringResource(R.string.delete),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun AddFolderDialog(
  folders: List<VideoFolder>,
  blacklistedFolders: Set<String>,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onAddFolders: (Set<String>) -> Unit,
) {
  var selectionState by remember { mutableStateOf(app.aryan447.mpvium.ui.browser.selection.SelectionState<String>()) }
  var showDropdown by remember { mutableStateOf(false) }

  // Filter folders that are not already blacklisted
  val availableFolders = remember(folders, blacklistedFolders) {
    folders.filter { it.path !in blacklistedFolders }
  }

  // Get all available folder paths
  val availableFolderPaths = remember(availableFolders) {
    availableFolders.map { it.path }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !isLoading && availableFolders.isNotEmpty()) {
          showDropdown = true
        },
      ) {
        Text(
          text = if (selectionState.isInSelectionMode) {
            stringResource(R.string.selected_items, selectionState.selectedCount, availableFolders.size)
          } else {
            stringResource(R.string.pref_folders_select_folders)
          },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (!isLoading && availableFolders.isNotEmpty()) {
          Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = stringResource(R.string.selection_options),
            modifier = Modifier.size(24.dp),
          )
        }

        DropdownMenu(
          expanded = showDropdown,
          onDismissRequest = { showDropdown = false },
          containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.select_all)) },
            onClick = {
              selectionState = selectionState.selectAll(availableFolderPaths)
              showDropdown = false
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.invert_selection)) },
            onClick = {
              selectionState = selectionState.invertSelection(availableFolderPaths)
              showDropdown = false
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.deselect_all)) },
            onClick = {
              selectionState = selectionState.clear()
              showDropdown = false
            },
          )
        }
      }
    },
    text = {
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(R.string.pref_folders_loading))
        }
      } else if (availableFolders.isEmpty()) {
        Text(stringResource(R.string.pref_folders_no_folders))
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        ) {
          items(availableFolders) { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectionState = selectionState.toggle(folder.path)
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = selectionState.isSelected(folder.path),
                onCheckedChange = {
                  selectionState = selectionState.toggle(folder.path)
                },
              )
              Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                  text = folder.name,
                  style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                  text = folder.path,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onAddFolders(selectionState.selectedIds)
          onDismiss()
        },
        enabled = selectionState.isInSelectionMode && !isLoading,
      ) {
        Text(stringResource(R.string.generic_ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.generic_cancel))
      }
    },
  )
}

/**
 * Scans all storage volumes for folders containing videos
 * Uses optimized fast scanning for better performance
 */
private suspend fun scanAllVideoFolders(context: Application): List<VideoFolder> {
  // Use fast optimized scanning - 5-10x faster for large libraries
  return app.aryan447.mpvium.repository.MediaFileRepository
    .getAllVideoFoldersFast(
      context = context
    )
}
