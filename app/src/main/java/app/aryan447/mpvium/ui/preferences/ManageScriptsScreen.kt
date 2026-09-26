package app.aryan447.mpvium.ui.preferences

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.ScriptFileHelper
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object ManageScriptsScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val preferences = koinInject<AdvancedPreferences>()
    val scope = rememberCoroutineScope()
    val disabledScripts by preferences.disabledScripts.collectAsState()
    val scriptsEnabled by preferences.enableScripts.collectAsState()

    var scriptFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
      withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "scripts")
        val files =
          if (dir.exists()) {
            dir.listFiles()
              ?.filter { it.isFile && ScriptFileHelper.isScriptFile(it.name) }
              ?.sortedBy { it.name.lowercase() } ?: emptyList()
          } else {
            emptyList()
          }
        withContext(Dispatchers.Main) { scriptFiles = files }
      }
    }

    val importLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
      ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
          var imported = 0
          val dir = File(context.filesDir, "scripts").apply { mkdirs() }
          for (uri in uris) {
            val name =
              runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                  val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                  if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
                }
              }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: continue
            if (!ScriptFileHelper.isScriptFile(name)) continue
            runCatching {
              context.contentResolver.openInputStream(uri)?.use { input ->
                File(dir, name).outputStream().use { output -> input.copyTo(output) }
              }
              imported++
            }
          }
          withContext(Dispatchers.Main) {
            Toast.makeText(
              context,
              if (imported > 0) "Imported $imported script(s)" else "No Lua/JS scripts imported",
              Toast.LENGTH_SHORT,
            ).show()
            refreshTick++
          }
        }
      }

    Scaffold(
      topBar = {
        SettingsTopBar(
          title = "Manage scripts",
          actions = {
            IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
              Icon(Icons.Outlined.Add, contentDescription = "Import script")
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
            PreferenceSectionHeader(title = "Scripts", count = scriptFiles.size)
          }
          if (scriptFiles.isEmpty()) {
            item {
              PreferenceCard {
                Preference(
                  title = { Text("No scripts found") },
                  summary = {
                    Text(
                      "Copy .lua or .js files into the scripts folder of your MPV configuration directory, or tap + to import.",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  icon = { PreferenceIconBox(icon = Icons.Outlined.Code) },
                  onClick = {},
                )
              }
            }
          } else {
            item {
              PreferenceCard {
                scriptFiles.forEachIndexed { index, file ->
                  val checked = file.name !in disabledScripts
                  val statusText =
                    if (!scriptsEnabled) "Scripts disabled"
                    else if (checked) "Enabled" else "Disabled"
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    HapticSwitchPreference(
                      value = checked,
                      onValueChange = { on ->
                        val updated = disabledScripts.toMutableSet()
                        if (on) updated.remove(file.name) else updated.add(file.name)
                        preferences.disabledScripts.set(updated)
                      },
                      title = { Text(text = file.name) },
                      modifier = Modifier.weight(1f),
                      enabled = scriptsEnabled,
                      summary = {
                        Text(
                          text = statusText,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      },
                      icon = {
                        PreferenceIconBox(
                          icon =
                            if (file.extension.lowercase() == "js") {
                              Icons.Outlined.Terminal
                            } else {
                              Icons.Outlined.Code
                            },
                        )
                      },
                    )
                    Box(
                      modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(
                          onClick = {
                            scope.launch(Dispatchers.IO) {
                              file.delete()
                              val updated = disabledScripts.toMutableSet().apply { remove(file.name) }
                              withContext(Dispatchers.Main) {
                                preferences.disabledScripts.set(updated)
                                refreshTick++
                              }
                            }
                          },
                        ),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete ${file.name}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  if (index < scriptFiles.lastIndex) PreferenceDivider()
                }
              }
            }

            item {
              Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  "Changes apply on next playback.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
