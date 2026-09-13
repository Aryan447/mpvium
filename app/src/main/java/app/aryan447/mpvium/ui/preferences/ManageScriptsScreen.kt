package app.aryan447.mpvium.ui.preferences

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
              Icon(Icons.Default.Add, contentDescription = "Import script")
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
          if (scriptFiles.isEmpty()) {
            item {
              PreferenceCard {
                Preference(
                  title = { Text("No scripts found") },
                  summary = {
                    Text(
                      "Copy .lua or .js files into the scripts folder of your MPV configuration directory, or tap + to import.",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  onClick = {},
                )
              }
            }
          } else {
            item {
              PreferenceCard {
                scriptFiles.forEachIndexed { index, file ->
                  val checked = file.name !in disabledScripts
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                      )
                      Text(
                        text =
                          if (!scriptsEnabled) "Scripts disabled"
                          else if (checked) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                      )
                    }
                    Switch(
                      checked = checked,
                      enabled = scriptsEnabled,
                      onCheckedChange = { on ->
                        val updated = disabledScripts.toMutableSet()
                        if (on) updated.remove(file.name) else updated.add(file.name)
                        preferences.disabledScripts.set(updated)
                      },
                    )
                    IconButton(
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
                    ) {
                      Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${file.name}",
                        tint = MaterialTheme.colorScheme.error,
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
                  color = MaterialTheme.colorScheme.outline,
                )
              }
            }
          }
        }
      }
    }
  }
}
