package app.aryan447.mpvium.ui.preferences

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.CustomScriptButton
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
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
    val preferences = koinInject<AdvancedPreferences>()
    val rawButtons by preferences.customScriptButtons.collectAsState()
    val buttons = remember(rawButtons) { CustomScriptButton.decode(rawButtons) }

    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftCode by remember { mutableStateOf("") }

    fun saveButtons(next: List<CustomScriptButton>) {
      preferences.customScriptButtons.set(CustomScriptButton.encode(next))
    }

    fun openAdd() {
      editingId = null
      draftTitle = ""
      draftCode = ""
      showEditor = true
    }

    fun openEdit(button: CustomScriptButton) {
      editingId = button.id
      draftTitle = button.title
      draftCode = button.code
      showEditor = true
    }

    if (showEditor) {
      AlertDialog(
        onDismissRequest = { showEditor = false },
        containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surface),
        title = { Text(if (editingId == null) "New custom button" else "Edit custom button") },
        text = {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedTextField(
              value = draftTitle,
              onValueChange = { draftTitle = it },
              label = { Text("Button title") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = draftCode,
              onValueChange = { draftCode = it },
              label = { Text("MPV command, e.g. cycle pause") },
              minLines = 3,
              textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
              modifier = Modifier.fillMaxWidth(),
            )
            Text(
              "Runs inside the player as an mpv command. Use script bindings such as script-binding stats/display-stats-toggle.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              if (draftTitle.isBlank() || draftCode.isBlank()) {
                Toast.makeText(context, "Title and command are required", Toast.LENGTH_SHORT).show()
                return@TextButton
              }
              val next =
                if (editingId == null) {
                  buttons + CustomScriptButton(UUID.randomUUID().toString(), draftTitle.trim(), draftCode.trim())
                } else {
                  buttons.map {
                    if (it.id == editingId) it.copy(title = draftTitle.trim(), code = draftCode.trim()) else it
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
          title = "Custom buttons",
          actions = {
            IconButton(onClick = ::openAdd) {
              Icon(Icons.Outlined.Add, contentDescription = "Add button")
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
            PreferenceSectionHeader(title = "Buttons", count = buttons.size)
          }
          if (buttons.isEmpty()) {
            item {
              PreferenceCard {
                Preference(
                  title = { Text("No custom buttons yet") },
                  summary = {
                    Text(
                      "Create script-powered player buttons, e.g. a stats toggle or a chapter skip.",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  icon = { PreferenceIconBox(icon = Icons.Outlined.PlayArrow) },
                  onClick = ::openAdd,
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
                      Text(button.title, style = MaterialTheme.typography.titleMedium)
                      Text(
                        button.code,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = { openEdit(button) }),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        Icons.Outlined.Edit,
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
                        .clickable(onClick = { saveButtons(buttons.filterNot { it.id == button.id }) }),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        Icons.Outlined.Delete,
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
        }
      }
    }
  }
}
