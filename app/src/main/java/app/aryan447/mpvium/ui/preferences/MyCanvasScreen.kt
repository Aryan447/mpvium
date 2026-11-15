package app.aryan447.mpvium.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.preference.collectAsState
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.theme.CustomTheme
import app.aryan447.mpvium.ui.theme.DarkMode
import app.aryan447.mpvium.ui.utils.LocalBackStack
import java.util.UUID
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * The editor behind the Theme Studio entry card. One accent paints the whole
 * palette, the preview updates live, and Save adds the theme to the carousel.
 */
@Serializable
object MyCanvasScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AppearancePreferences>()
    val backstack = LocalBackStack.current
    val darkMode by preferences.darkMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDark
    }

    var name by rememberSaveable { mutableStateOf("") }
    var seedArgb by rememberSaveable { mutableStateOf(CanvasSeeds.first().toArgb()) }
    val themeId = rememberSaveable { UUID.randomUUID().toString() }
    val draft = remember(themeId, name, seedArgb) {
      CustomTheme.fromSeed(id = themeId, name = name, seed = Color(seedArgb))
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        SettingsTopBar(
          title = stringResource(R.string.my_canvas_title),
          actions = {
            TextButton(
              enabled = name.isNotBlank(),
              onClick = {
                val theme = draft.copy(name = name.trim())
                preferences.saveCustomTheme(theme)
                preferences.selectCustomTheme(theme)
                backstack.removeLastOrNull()
              },
            ) {
              Text(text = stringResource(R.string.my_canvas_save))
            }
          },
        )
      },
    ) { padding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        item {
          CanvasPreview(theme = draft, isDarkMode = isDarkMode)
        }

        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(text = stringResource(R.string.my_canvas_name_label)) },
            placeholder = { Text(text = stringResource(R.string.my_canvas_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        item {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.my_canvas_accent_label),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CanvasSeeds.forEach { color ->
                val isSelected = color.toArgb() == seedArgb
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                      } else {
                        MaterialTheme.colorScheme.outlineVariant
                      },
                      shape = CircleShape,
                    )
                    .selectable(
                      selected = isSelected,
                      onClick = { seedArgb = color.toArgb() },
                      role = Role.RadioButton,
                    ),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CanvasPreview(
  theme: CustomTheme,
  isDarkMode: Boolean,
) {
  val colorScheme = if (isDarkMode) theme.getDarkColorScheme() else theme.getLightColorScheme()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(colorScheme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(R.string.my_canvas_preview_label),
      style = MaterialTheme.typography.labelMedium,
      color = colorScheme.primary,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(colorScheme.primary),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colorScheme.surfaceVariant),
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colorScheme.surfaceVariant),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary).forEach { color ->
        Box(
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        )
      }
    }
  }
}

private val CanvasSeeds = listOf(
  Color(0xFFB4637A), // Rose
  Color(0xFFC2410C), // Ember
  Color(0xFF8A5F00), // Amber
  Color(0xFF2E7D32), // Moss
  Color(0xFF006A7D), // Ocean
  Color(0xFF2B57C3), // Sapphire
  Color(0xFF6D28D9), // Violet
  Color(0xFF34547A), // Indigo
  Color(0xFF9C4A00), // Rust
  Color(0xFF1B6B5A), // Pine
  Color(0xFF5A5A5A), // Slate
  Color(0xFFA63D57), // Rose Gold
)
