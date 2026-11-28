package app.aryan447.mpvium.ui.browser.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.domain.network.NetworkConnection
import app.aryan447.mpvium.domain.network.NetworkProtocol
import app.aryan447.mpvium.domain.network.sanitized
import app.aryan447.mpvium.ui.theme.glassMenuContainerColor
import app.aryan447.mpvium.ui.theme.glassSheetContainerColor
import app.aryan447.mpvium.ui.utils.LocalIsTv
import app.aryan447.mpvium.ui.utils.rememberTvTextFieldState
import app.aryan447.mpvium.ui.utils.tvTextFieldControl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConnectionSheet(
  connection: NetworkConnection,
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onSave: (NetworkConnection) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  var name by remember(connection.id) { mutableStateOf(connection.name) }
  var protocol by remember(connection.id) { mutableStateOf(connection.protocol) }
  var host by remember(connection.id) { mutableStateOf(connection.host) }
  var port by remember(connection.id) { mutableStateOf(connection.port.toString()) }
  var username by remember(connection.id) { mutableStateOf(connection.username) }
  // Never pre-fill the saved password (issue #85): keep it hidden and only
  // update it when the user types a new one. Blank means "keep existing".
  var password by remember(connection.id) { mutableStateOf("") }
  var path by remember(connection.id) { mutableStateOf(connection.path) }
  var isAnonymous by remember(connection.id) { mutableStateOf(connection.isAnonymous) }
  var useHttps by remember(connection.id) { mutableStateOf(connection.useHttps) }
  var protocolMenuExpanded by remember { mutableStateOf(false) }
  val isTv = LocalIsTv.current
  val initialFocusRequester = remember { FocusRequester() }
  val nameTvState = rememberTvTextFieldState(isTv)
  val hostTvState = rememberTvTextFieldState(isTv)
  val portTvState = rememberTvTextFieldState(isTv)
  val pathTvState = rememberTvTextFieldState(isTv)
  val usernameTvState = rememberTvTextFieldState(isTv)
  val passwordTvState = rememberTvTextFieldState(isTv)

  LaunchedEffect(Unit) {
    if (isTv) {
      initialFocusRequester.requestFocus()
    }
  }

  val hasSavedPassword = connection.password.isNotEmpty() && !connection.isAnonymous

  val handleDismiss = {
    onDismiss()
  }

  val handleSave = {
    val updatedConnection =
      connection.copy(
        name = name,
        protocol = protocol,
        host = host,
        port = port.toIntOrNull() ?: protocol.defaultPort,
        username = if (isAnonymous) "" else username,
        // Keep the stored password unless a new one was typed (issue #85).
        password = if (isAnonymous) "" else password.ifBlank { connection.password },
        path = path.ifBlank { "/" },
        isAnonymous = isAnonymous,
        useHttps = useHttps,
      ).sanitized()
    onSave(updatedConnection)
  }

  AlertDialog(
    onDismissRequest = handleDismiss,
    modifier = Modifier
      .widthIn(min = 400.dp, max = 600.dp)
      .then(if (isTv) Modifier.heightIn(max = 480.dp) else Modifier),
    title = {
      Text(
        text = "Edit Connection",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Name and Protocol in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Connection Name
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            readOnly = nameTvState.readOnly,
            label = { Text("Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier
              .weight(0.60f)
              .focusRequester(initialFocusRequester)
              .tvTextFieldControl(nameTvState),
            singleLine = true,
          )

          // Protocol Dropdown
          ExposedDropdownMenuBox(
            expanded = protocolMenuExpanded,
            onExpandedChange = { protocolMenuExpanded = it },
            modifier = Modifier.weight(0.40f),
          ) {
            OutlinedTextField(
              value = protocol.displayName,
              onValueChange = { },
              readOnly = true,
              label = { Text("Protocol", maxLines = 1, overflow = TextOverflow.Ellipsis) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolMenuExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
              expanded = protocolMenuExpanded,
              onDismissRequest = { protocolMenuExpanded = false },
              containerColor = glassMenuContainerColor(MenuDefaults.containerColor),
            ) {
              NetworkProtocol.entries.forEach { proto ->
                DropdownMenuItem(
                  text = { Text(proto.displayName) },
                  onClick = {
                    protocol = proto
                    port = proto.defaultPort.toString()
                    protocolMenuExpanded = false
                  },
                )
              }
            }
          }
        }

        // Host
        OutlinedTextField(
          value = host,
          onValueChange = { host = it },
          readOnly = hostTvState.readOnly,
          label = { Text("Host/IP Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.fillMaxWidth().tvTextFieldControl(hostTvState),
          singleLine = true,
          placeholder = { Text("192.168.1.100", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )

        // Port and Path in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Port
          OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            readOnly = portTvState.readOnly,
            label = { Text("Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.3f).tvTextFieldControl(portTvState),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )

          // Path
          OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            readOnly = pathTvState.readOnly,
            label = { Text("Path", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.7f).tvTextFieldControl(pathTvState),
            singleLine = true,
            placeholder = { Text("/", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
        }

        // Anonymous checkbox row
        var isAnonFocused by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .onFocusChanged { isAnonFocused = it.isFocused || it.hasFocus }
            .toggleable(
              value = isAnonymous,
              role = Role.Checkbox,
              onValueChange = { isAnonymous = it },
            )
            .then(
              if (isTv && isAnonFocused) {
                Modifier
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                  .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
              } else {
                Modifier
              }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
          Checkbox(
            checked = isAnonymous,
            onCheckedChange = null,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Anonymous/Guest Access")
        }

        // HTTPS checkbox (only for WebDAV)
        if (protocol == NetworkProtocol.WEBDAV) {
          var isHttpsFocused by remember { mutableStateOf(false) }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .onFocusChanged { isHttpsFocused = it.isFocused || it.hasFocus }
              .toggleable(
                value = useHttps,
                role = Role.Checkbox,
                onValueChange = {
                  useHttps = it
                  // Auto-update port when toggling HTTPS
                  if (it && port == "80") {
                    port = "443"
                  } else if (!it && port == "443") {
                    port = "80"
                  }
                },
              )
              .then(
                if (isTv && isHttpsFocused) {
                  Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                } else {
                  Modifier
                }
              )
              .padding(horizontal = 8.dp, vertical = 6.dp),
          ) {
            Checkbox(
              checked = useHttps,
              onCheckedChange = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use HTTPS (Secure Connection)")
          }
        }

        // Username and Password in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Username
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            readOnly = usernameTvState.readOnly,
            label = { Text("Username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.50f).tvTextFieldControl(usernameTvState),
            singleLine = true,
            enabled = !isAnonymous,
          )

          // Password (never shows the saved value; blank keeps it)
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            readOnly = passwordTvState.readOnly,
            label = { Text(if (hasSavedPassword) "New Password" else "Password", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.50f).tvTextFieldControl(passwordTvState),
            singleLine = true,
            enabled = !isAnonymous,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            placeholder = {
              if (hasSavedPassword) {
                Text("Saved — blank keeps it", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            },
            supportingText = {
              if (hasSavedPassword) {
                Text("Leave blank to keep saved password")
              }
            },
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = handleSave,
        enabled = host.isNotBlank() && (isAnonymous || username.isNotBlank()),
      ) {
        Text(
          text = "Save",
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = handleDismiss) {
        Text(
          text = "Cancel",
          fontWeight = FontWeight.Medium,
        )
      }
    },
    containerColor = glassSheetContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}
