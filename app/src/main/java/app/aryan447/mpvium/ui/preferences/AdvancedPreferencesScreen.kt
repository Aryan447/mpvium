package app.aryan447.mpvium.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.aryan447.mpvium.R
import app.aryan447.mpvium.database.MpviumDatabase
import app.aryan447.mpvium.domain.thumbnail.ThumbnailRepository
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.CustomScriptButton
import app.aryan447.mpvium.preferences.ScriptFileHelper
import app.aryan447.mpvium.preferences.SettingsManager
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.components.ConfirmDialog
import app.aryan447.mpvium.presentation.crash.CrashActivity
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.utils.history.RecentlyPlayedOps
import app.aryan447.mpvium.utils.media.OpenDocumentTreeContract
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import java.io.File
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.TwoTargetIconButtonPreference
import org.koin.compose.koinInject

@Serializable
object AdvancedPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val settingsManager = koinInject<SettingsManager>()
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var importStats by remember { mutableStateOf<SettingsManager.ImportStats?>(null) }
    var exportStats by remember { mutableStateOf<SettingsManager.ExportStats?>(null) }

    // Export settings launcher
    val exportLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/xml"),
      ) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.exportSettings(it).fold(
              onSuccess = { stats ->
                exportStats = stats
                showExportDialog = true
              },
              onFailure = { error ->
                Toast.makeText(
                  context,
                  "Export failed: ${error.message}",
                  Toast.LENGTH_LONG,
                ).show()
              },
            )
          }
        }
      }

    // Import settings launcher
    val importLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
      ) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.importSettings(it).fold(
              onSuccess = { stats ->
                importStats = stats
                showImportDialog = true
              },
              onFailure = { error ->
                Toast.makeText(
                  context,
                  "Import failed: ${error.message}",
                  Toast.LENGTH_LONG,
                ).show()
              },
            )
          }
        }
      }

    // Export results dialog
    if (showExportDialog && exportStats != null) {
      AlertDialog(
        onDismissRequest = { showExportDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text("Export Complete") },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
          ) {
            Text(
              "Successfully exported ${exportStats?.totalExported} items!\n\n"
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showExportDialog = false }) {
            Text("OK")
          }
        },
      )
    }

    // Import results dialog
    if (showImportDialog && importStats != null) {
      AlertDialog(
        onDismissRequest = { showImportDialog = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text("Import Complete") },
        text = {
          Text(
            "Successfully imported: ${importStats?.imported}\n" +
              "Failed: ${importStats?.failed}\n" +
              "Version: ${importStats?.version}\n\n" +
              "Please restart the app for all changes to take effect.",
          )
        },
        confirmButton = {
          TextButton(onClick = { showImportDialog = false }) {
            Text("OK")
          }
        },
      )
    }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(R.string.pref_advanced))
      },
    ) { padding ->
      ProvidePreferenceLocals {
        val locationPicker =
          rememberLauncherForActivityResult(
            OpenDocumentTreeContract(),
          ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            preferences.mpvConfStorageUri.set(uri.toString())

            // Auto-create standard MPV folder structure
            scope.launch(Dispatchers.IO) {
              runCatching {
                val tree = DocumentFile.fromTreeUri(context, uri)
                if (tree != null && tree.exists() && tree.canWrite()) {
                  val subdirs = listOf("fonts", "script-opts", "scripts", "shaders")
                  for (name in subdirs) {
                    val existing = tree.listFiles().firstOrNull {
                      it.isDirectory && it.name?.equals(name, ignoreCase = true) == true
                    }
                    if (existing == null) {
                      tree.createDirectory(name)
                    }
                  }
                  // Create default mpv.conf if missing
                  val hasConf = tree.listFiles().any {
                    it.isFile && it.name?.equals("mpv.conf", ignoreCase = true) == true
                  }
                  if (!hasConf) {
                    tree.createFile("application/octet-stream", "mpv.conf")
                  }
                  withContext(Dispatchers.Main) {
                    Toast.makeText(context, "MPV directory ready ✓", Toast.LENGTH_SHORT).show()
                  }
                }
              }.onFailure { e ->
                android.util.Log.e("AdvancedPrefs", "Error creating MPV directory structure", e)
              }
            }
          }
        val mpvConfStorageLocation by preferences.mpvConfStorageUri.collectAsState()
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          // Backup & Restore Section
          item {
            PreferenceSectionHeader(title = "Backup & Restore", count = 2)
          }

          item {
            PreferenceCard {
              Preference(
                title = { Text(text = "Export Settings") },
                summary = {
                  Text(
                    text = "Export settings to an XML file",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                icon = {
                  PreferenceIconBox(icon = Icons.Outlined.Upload)
                },
                onClick = {
                  exportLauncher.launch(settingsManager.getDefaultExportFilename())
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(text = "Import Settings") },
                summary = {
                  Text(
                    text = "Import settings from an XML file",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                icon = {
                  PreferenceIconBox(icon = Icons.Outlined.Download)
                },
                onClick = {
                  importLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                },
              )
            }
          }

          // MPV Configuration Section
          item {
            PreferenceSectionHeader(title = "MPV Configuration", count = 3)
          }

          item {
            PreferenceCard {
              var mpvConf by remember { mutableStateOf(preferences.mpvConf.get()) }
              var inputConf by remember { mutableStateOf(preferences.inputConf.get()) }

              // Load config files when storage location changes
              LaunchedEffect(mpvConfStorageLocation) {
                if (mpvConfStorageLocation.isBlank()) return@LaunchedEffect
                withContext(Dispatchers.IO) {
                  val tempFile = kotlin.io.path.createTempFile()
                  runCatching {
                    val tree =
                      DocumentFile.fromTreeUri(
                        context,
                        mpvConfStorageLocation.toUri(),
                      )
                    val mpvConfFile = tree?.findFile("mpv.conf")
                    if (mpvConfFile != null && mpvConfFile.exists()) {
                      context.contentResolver
                        .openInputStream(
                          mpvConfFile.uri,
                        )?.copyTo(tempFile.outputStream())
                      val content = tempFile.readLines().fastJoinToString("\n")
                      preferences.mpvConf.set(content)
                      File(context.filesDir, "mpv.conf").writeText(content)
                      withContext(Dispatchers.Main) {
                        mpvConf = content
                      }
                    }
                  }
                  tempFile.deleteIfExists()
                }
              }

              // Load input.conf when storage location changes
              LaunchedEffect(mpvConfStorageLocation) {
                if (mpvConfStorageLocation.isBlank()) return@LaunchedEffect
                withContext(Dispatchers.IO) {
                  val tempFile = kotlin.io.path.createTempFile()
                  runCatching {
                    val tree =
                      DocumentFile.fromTreeUri(
                        context,
                        mpvConfStorageLocation.toUri(),
                      )
                    val inputConfFile = tree?.findFile("input.conf")
                    if (inputConfFile != null && inputConfFile.exists()) {
                      context.contentResolver
                        .openInputStream(
                          inputConfFile.uri,
                        )?.copyTo(tempFile.outputStream())
                      val content = tempFile.readLines().fastJoinToString("\n")
                      preferences.inputConf.set(content)
                      File(context.filesDir, "input.conf").writeText(content)
                      withContext(Dispatchers.Main) {
                        inputConf = content
                      }
                    }
                  }
                  tempFile.deleteIfExists()
                }
              }

              TwoTargetIconButtonPreference(
                title = { Text(stringResource(R.string.pref_advanced_mpv_conf_storage_location)) },
                summary = {
                  if (mpvConfStorageLocation.isNotBlank()) {
                    Text(
                      getSimplifiedPathFromUri(mpvConfStorageLocation),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                onClick = { locationPicker.launch(null) },
                iconButtonIcon = {
                  Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onIconButtonClick = { preferences.mpvConfStorageUri.delete() },
                iconButtonEnabled = mpvConfStorageLocation.isNotBlank(),
              )

              PreferenceDivider()

              Preference(
                title = { Text(stringResource(R.string.pref_advanced_mpv_conf)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Terminal) },
                summary = {
                  val firstLine = mpvConf.lines().firstOrNull()
                  if (firstLine != null && firstLine.isNotBlank()) {
                    Text(
                      firstLine,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  } else {
                    Text(
                      "Tap to edit configuration",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                onClick = {
                  backStack.add(ConfigEditorScreen(ConfigEditorScreen.ConfigType.MPV_CONF))
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(stringResource(R.string.pref_advanced_input_conf)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Tune) },
                summary = {
                  val firstLine = inputConf.lines().firstOrNull()
                  if (firstLine != null && firstLine.isNotBlank()) {
                    Text(
                      firstLine,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  } else {
                    Text(
                      "Tap to edit configuration",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                onClick = {
                  backStack.add(ConfigEditorScreen(ConfigEditorScreen.ConfigType.INPUT_CONF))
                },
              )
            }
          }

          // Scripts Section
          item {
            PreferenceSectionHeader(title = "Scripts (Lua / JS)", count = 3)
          }

          item {
            val enableScripts by preferences.enableScripts.collectAsState()
            val disabledScripts by preferences.disabledScripts.collectAsState()
            val rawCustomButtons by preferences.customScriptButtons.collectAsState()
            var internalScriptCount by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(mpvConfStorageLocation, disabledScripts) {
              withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "scripts")
                val count =
                  if (dir.exists()) {
                    dir.listFiles()?.count { it.isFile && isScriptFile(it.name) } ?: 0
                  } else {
                    0
                  }
                withContext(Dispatchers.Main) { internalScriptCount = count }
              }
            }
            val enabledCount =
              if (!enableScripts) {
                0
              } else {
                val total = internalScriptCount ?: 0
                (total - disabledScripts.size).coerceAtLeast(0)
              }
            val customButtonCount =
              remember(rawCustomButtons) {
                CustomScriptButton.decode(rawCustomButtons).size
              }
            PreferenceCard {
              HapticSwitchPreference(
                value = enableScripts,
                onValueChange = preferences.enableScripts::set,
                title = { Text("Enable scripts") },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Code) },
                summary = {
                  Text(
                    "Load scripts from your MPV configuration directory",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text("Manage scripts") },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Folder) },
                summary = {
                  Text(
                    when {
                      !enableScripts -> "Scripts disabled"
                      enabledCount == 1 -> "1 script enabled"
                      else -> "$enabledCount scripts enabled"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = { backStack.add(ManageScriptsScreen) },
              )

              PreferenceDivider()

              Preference(
                title = { Text("Custom buttons") },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Layers) },
                summary = {
                  Text(
                    if (customButtonCount == 0) {
                      "Create and manage script-powered player buttons"
                    } else if (customButtonCount == 1) {
                      "1 custom button"
                    } else {
                      "$customButtonCount custom buttons"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = { backStack.add(CustomButtonsScreen) },
              )
            }
          }
          // History Section
          item {
            PreferenceSectionHeader(title = "History", count = 2)
          }

          item {
            PreferenceCard {
              var isConfirmDialogShown by remember { mutableStateOf(false) }
              val mpviumDatabase = koinInject<MpviumDatabase>()
              val enableRecentlyPlayed by preferences.enableRecentlyPlayed.collectAsState()

              HapticSwitchPreference(
                value = enableRecentlyPlayed,
                onValueChange = preferences.enableRecentlyPlayed::set,
                title = { Text(stringResource(R.string.pref_advanced_enable_recently_played_title)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.History) },
                summary = {
                  Text(
                    stringResource(R.string.pref_advanced_enable_recently_played_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(stringResource(R.string.pref_advanced_clear_playback_history)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Delete) },
                onClick = { isConfirmDialogShown = true },
              )

              if (isConfirmDialogShown) {
                ConfirmDialog(
                  stringResource(R.string.pref_advanced_clear_playback_history_confirm_title),
                  stringResource(R.string.pref_advanced_clear_playback_history_confirm_subtitle),
                  onConfirm = {
                    scope.launch(Dispatchers.IO) {
                      runCatching {
                        mpviumDatabase.videoDataDao().clearAllPlaybackStates()
                        RecentlyPlayedOps.clearAll()
                      }.onSuccess {
                        withContext(Dispatchers.Main) {
                          isConfirmDialogShown = false
                          Toast
                            .makeText(
                              context,
                              context.getString(R.string.pref_advanced_cleared_playback_history),
                              Toast.LENGTH_SHORT,
                            ).show()
                        }
                      }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                          isConfirmDialogShown = false
                          Toast
                            .makeText(
                              context,
                              "Failed to clear: ${error.message}",
                              Toast.LENGTH_LONG,
                            ).show()
                        }
                      }
                    }
                  },
                  onCancel = { isConfirmDialogShown = false },
                )
              }
            }
          }

          // Cache Section
          item {
            PreferenceSectionHeader(title = "Cache", count = 3)
          }

          item {
            PreferenceCard {
              var mpvConf by remember { mutableStateOf(preferences.mpvConf.get()) }
              var isClearThumbsConfirmShown by remember { mutableStateOf(false) }
              val thumbnailRepository = koinInject<ThumbnailRepository>()

              Preference(
                title = { Text(text = "Clear config cache") },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Refresh) },
                summary = {
                  Text(
                    text = "Clear the cached mpv.conf settings",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    val mpvConfFile = File(context.filesDir, "mpv.conf")
                    mpvConfFile.delete()
                    // Clear preferences too
                    preferences.mpvConf.delete()
                    withContext(Dispatchers.Main) {
                      mpvConf = ""
                      Toast
                        .makeText(
                          context,
                          "Config cache cleared",
                          Toast.LENGTH_SHORT,
                        ).show()
                    }
                  }
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(text = "Clear thumbnail cache") },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Storage) },
                summary = {
                  Text(
                    text = "Delete all cached video thumbnails (will regenerate as you browse folders)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = { isClearThumbsConfirmShown = true },
              )

              if (isClearThumbsConfirmShown) {
                ConfirmDialog(
                  title = "Clear thumbnail cache?",
                  subtitle = "This will delete cached thumbnails from storage and memory.",
                  onConfirm = {
                    scope.launch(Dispatchers.IO) {
                      runCatching {
                        thumbnailRepository.clearThumbnailCache()
                      }.onSuccess {
                        withContext(Dispatchers.Main) {
                          isClearThumbsConfirmShown = false
                          Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                        }
                      }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                          isClearThumbsConfirmShown = false
                          Toast.makeText(context, "Failed to clear: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                      }
                    }
                  },
                  onCancel = { isClearThumbsConfirmShown = false },
                )
              }

              PreferenceDivider()

              Preference(
                title = { Text(text = stringResource(id = R.string.pref_advanced_clear_fonts_cache)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Description) },
                summary = {
                  Text(
                    text = "Remove all cached subtitle fonts",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    val fontsDir = File(context.filesDir.path + "/fonts")
                    if (fontsDir.exists()) {
                      fontsDir.listFiles()?.forEach { file ->
                        // Delete all font files
                        if (file.isFile &&
                          file.name
                            .lowercase()
                            .matches(".*\\.[ot]tf$".toRegex())
                        ) {
                          file.delete()
                        }
                      }
                    }
                    withContext(Dispatchers.Main) {
                      Toast
                        .makeText(
                          context,
                          context.getString(R.string.pref_advanced_cleared_fonts_cache),
                          Toast.LENGTH_SHORT,
                        ).show()
                    }
                  }
                },
              )
            }
          }

          // Logging Section
          item {
            PreferenceSectionHeader(title = "Logging", count = 2)
          }

          item {
            PreferenceCard {
              val activity = LocalActivity.current!!
              val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
              val verboseLogging by preferences.verboseLogging.collectAsState()

              HapticSwitchPreference(
                value = verboseLogging,
                onValueChange = preferences.verboseLogging::set,
                title = { Text(stringResource(R.string.pref_advanced_verbose_logging_title)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.BugReport) },
                summary = {
                  Text(
                    stringResource(R.string.pref_advanced_verbose_logging_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(stringResource(R.string.pref_advanced_dump_logs_title)) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Share) },
                summary = {
                  Text(
                    stringResource(R.string.pref_advanced_dump_logs_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    val deviceInfo = CrashActivity.collectDeviceInfo()
                    val logcat = CrashActivity.collectLogcat()

                    clipboard.setText(AnnotatedString(CrashActivity.concatLogs(deviceInfo, null, logcat)))
                    CrashActivity.shareLogs(deviceInfo, null, logcat, activity)
                  }
                },
              )
            }
          }
        }
      }
    }
  }
}

fun getSimplifiedPathFromUri(uri: String): String =
  Environment.getExternalStorageDirectory().canonicalPath + "/" + Uri.decode(uri).substringAfterLast(":")

private fun isScriptFile(name: String?): Boolean = ScriptFileHelper.isScriptFile(name)
