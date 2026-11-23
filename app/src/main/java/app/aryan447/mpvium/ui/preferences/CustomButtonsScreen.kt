package app.aryan447.mpvium.ui.preferences

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.CustomScriptButton
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.LocalBackStack
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object CustomButtonsScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val rawButtons by preferences.customScriptButtons.collectAsState()
    val buttons = remember(rawButtons) { CustomScriptButton.decode(rawButtons) }

    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftCode by remember { mutableStateOf("") }
    var draftLongPressCode by remember { mutableStateOf("") }

    fun saveButtons(next: List<CustomScriptButton>) {
      preferences.customScriptButtons.set(CustomScriptButton.encode(next))
    }

    val importLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
      uri?.let {
        runCatching {
          context.contentResolver.openInputStream(it)?.use { stream ->
            val xml = stream.bufferedReader().readText()
            val imported = CustomScriptButton.fromXml(xml)
            if (imported.isNotEmpty()) {
              val next = buttons + imported.map { b -> b.copy(id = UUID.randomUUID().toString()) }
              saveButtons(next)
              val scriptsDir = File(context.filesDir, "scripts")
              val missingScripts = imported.mapNotNull { btn ->
                val combined = btn.code + " " + btn.longPressCode
                Regex("""script-binding\s+([A-Za-z0-9_-]+)/""").find(combined)?.groupValues?.get(1)
              }.distinct().filter { scriptName ->
                !scriptsDir.exists() || scriptsDir.listFiles()?.none { f ->
                  f.isFile && f.name.substringBeforeLast('.').equals(scriptName, ignoreCase = true)
                } == true
              }
              if (missingScripts.isNotEmpty()) {
                val scriptList = missingScripts.joinToString(", ") { "$it.lua" }
                Toast.makeText(
                  context,
                  "Imported ${imported.size} buttons. Remember to add $scriptList in Manage scripts.",
                  Toast.LENGTH_LONG,
                ).show()
              } else {
                Toast.makeText(
                  context,
                  context.getString(R.string.pref_custom_buttons_import_toast, imported.size),
                  Toast.LENGTH_SHORT,
                ).show()
              }
            } else {
              Toast.makeText(
                context,
                context.getString(R.string.pref_custom_buttons_import_failed),
                Toast.LENGTH_SHORT,
              ).show()
            }
          }
        }.onFailure {
          Toast.makeText(
            context,
            context.getString(R.string.pref_custom_buttons_import_failed),
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }

    val exportLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri: Uri? ->
      uri?.let {
        runCatching {
          context.contentResolver.openOutputStream(it)?.use { stream ->
            val xml = CustomScriptButton.toXml(buttons)
            stream.write(xml.toByteArray())
            Toast.makeText(
              context,
              context.getString(R.string.pref_custom_buttons_export_toast, buttons.size),
              Toast.LENGTH_SHORT,
            ).show()
          }
        }.onFailure {
          Toast.makeText(
            context,
            context.getString(R.string.pref_custom_buttons_export_failed),
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }

    fun openAdd() {
      editingId = null
      draftTitle = ""
      draftCode = ""
      draftLongPressCode = ""
      showEditor = true
    }

    fun openEdit(button: CustomScriptButton) {
      editingId = button.id
      draftTitle = button.title
      draftCode = button.code
      draftLongPressCode = button.longPressCode
      showEditor = true
    }

    if (showEditor) {
      AlertDialog(
        onDismissRequest = { showEditor = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = {
          Text(
            if (editingId == null) {
              stringResource(R.string.pref_custom_buttons_dialog_new_title)
            } else {
              stringResource(R.string.pref_custom_buttons_dialog_edit_title)
            }
          )
        },
        text = {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedTextField(
              value = draftTitle,
              onValueChange = { draftTitle = it },
              label = { Text(stringResource(R.string.pref_custom_buttons_field_title)) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = draftCode,
              onValueChange = { draftCode = it },
              label = { Text(stringResource(R.string.pref_custom_buttons_field_tap)) },
              minLines = 2,
              textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = draftLongPressCode,
              onValueChange = { draftLongPressCode = it },
              label = { Text(stringResource(R.string.pref_custom_buttons_field_hold)) },
              minLines = 2,
              textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
              modifier = Modifier.fillMaxWidth(),
            )
            Text(
              stringResource(R.string.pref_custom_buttons_field_help),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              if (draftTitle.isBlank() || draftCode.isBlank()) {
                Toast.makeText(
                  context,
                  context.getString(R.string.pref_custom_buttons_validation_error),
                  Toast.LENGTH_SHORT,
                ).show()
                return@TextButton
              }
              val next =
                if (editingId == null) {
                  buttons + CustomScriptButton(
                    id = UUID.randomUUID().toString(),
                    title = draftTitle.trim(),
                    code = draftCode.trim(),
                    longPressCode = draftLongPressCode.trim(),
                    enabled = true,
                  )
                } else {
                  buttons.map {
                    if (it.id == editingId) {
                      it.copy(
                        title = draftTitle.trim(),
                        code = draftCode.trim(),
                        longPressCode = draftLongPressCode.trim(),
                      )
                    } else {
                      it
                    }
                  }
                }
              saveButtons(next)
              showEditor = false
            },
          ) { Text("Save") }
        },
        dismissButton = {
          TextButton(onClick = { showEditor = false }) { Text("Cancel") }
        },
      )
    }

    Scaffold(
      topBar = {
        SettingsTopBar(
          title = stringResource(R.string.pref_custom_buttons_title),
          actions = {
            IconButton(
              onClick = {
                importLauncher.launch(arrayOf("text/xml", "application/xml", "text/plain", "*/*"))
              },
            ) {
              Icon(
                Icons.Outlined.FileDownload,
                contentDescription = stringResource(R.string.pref_custom_buttons_import),
              )
            }
            IconButton(
              onClick = {
                if (buttons.isEmpty()) {
                  Toast.makeText(context, "No buttons to export", Toast.LENGTH_SHORT).show()
                } else {
                  exportLauncher.launch("custom_buttons.xml")
                }
              },
            ) {
              Icon(
                Icons.Outlined.FileUpload,
                contentDescription = stringResource(R.string.pref_custom_buttons_export),
              )
            }
            IconButton(onClick = ::openAdd) {
              Icon(Icons.Default.Add, contentDescription = "Add button")
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
            PreferenceSectionHeader(
              title = stringResource(R.string.pref_custom_buttons_title),
              count = buttons.size,
            )
          }
          if (buttons.isEmpty()) {
            item {
              PreferenceCard {
                Preference(
                  title = { Text(stringResource(R.string.pref_custom_buttons_empty_title)) },
                  summary = {
                    Text(
                      stringResource(R.string.pref_custom_buttons_empty_summary),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  onClick = ::openAdd,
                )
                PreferenceDivider()
                Preference(
                  title = { Text(stringResource(R.string.pref_custom_buttons_import)) },
                  summary = {
                    Text(
                      "Import buttons from mpvEx or mpvium XML file",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  icon = {
                    PreferenceRowIcon(icon = Icons.Outlined.FileDownload)
                  },
                  onClick = {
                    importLauncher.launch(arrayOf("text/xml", "application/xml", "text/plain", "*/*"))
                  },
                )
              }
            }
          } else {
            item {
              PreferenceCard {
                buttons.forEachIndexed { index, button ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    PreferenceIconBox(icon = Icons.Outlined.PlayArrow)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        button.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "Tap: ${button.code}" +
                          if (button.longPressCode.isNotBlank()) "\nHold: ${button.longPressCode}" else "",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                      checked = button.enabled,
                      onCheckedChange = { isChecked ->
                        saveButtons(buttons.map { if (it.id == button.id) it.copy(enabled = isChecked) else it })
                      },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { openEdit(button) },
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${button.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { saveButtons(buttons.filterNot { it.id == button.id }) },
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${button.title}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  if (index < buttons.lastIndex) PreferenceDivider()
                }
              }
            }
          }

          item {
            PreferenceSectionHeader(title = "Scripts")
          }
          item {
            PreferenceCard {
              Preference(
                title = { Text("Manage scripts") },
                summary = {
                  Text(
                    "Import and manage Lua or JS scripts used by custom buttons",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                icon = {
                  PreferenceRowIcon(icon = Icons.Outlined.Code)
                },
                onClick = { backStack.add(ManageScriptsScreen) },
              )
            }
          }
        }
      }
    }
  }
}
