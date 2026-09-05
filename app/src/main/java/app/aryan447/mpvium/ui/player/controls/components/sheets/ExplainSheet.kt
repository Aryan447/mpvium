package app.aryan447.mpvium.ui.player.controls.components.sheets

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.aryan447.mpvium.R
import app.aryan447.mpvium.presentation.components.PlayerSheet
import app.aryan447.mpvium.ui.player.PlayerViewModel
import app.aryan447.mpvium.ui.theme.spacing

/**
 * Explain sheet: shows the current subtitle line with tappable words for
 * keyless dictionary lookups, plus keyless Wikipedia explanations of the
 * references in the dialogue.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplainSheet(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val liveLine by viewModel.subtitleLine.collectAsState()
  val wordState by viewModel.wordState.collectAsState()
  val refsState by viewModel.refsState.collectAsState()

  // The line being explained. Captured when the sheet opens so results
  // stay stable while subtitles keep changing; Refresh re-captures.
  var activeLine by rememberSaveable { mutableStateOf(viewModel.subtitleLine.value) }
  var tappedWord by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(activeLine) {
    if (activeLine.isNotBlank()) {
      viewModel.explainReferences(activeLine)
    }
  }

  val words = rememberWords(activeLine)

  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(id = R.string.player_explain_title),
          style = MaterialTheme.typography.headlineMedium,
        )
        TextButton(
          onClick = {
            tappedWord = null
            activeLine = liveLine
            if (liveLine.isNotBlank()) {
              viewModel.explainReferences(liveLine)
            }
          },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(text = stringResource(id = R.string.player_explain_refresh))
          }
        }
      }

      if (activeLine.isBlank()) {
        Text(
          text = stringResource(id = R.string.player_explain_empty_line),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Card(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "“$activeLine”",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
          )
        }

        Text(
          text = stringResource(id = R.string.player_explain_tap_word),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
          words.forEach { word ->
            AssistChip(
              onClick = {
                tappedWord = word
                viewModel.lookupWord(word)
              },
              label = { Text(text = word) },
            )
          }
        }

        when (val state = wordState) {
          is PlayerViewModel.WordLookupState.Idle -> {}
          is PlayerViewModel.WordLookupState.Loading -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is PlayerViewModel.WordLookupState.Done -> {
            WordDefinitionCard(
              word = state.result.word,
              phonetic = state.result.phonetic,
              partOfSpeech = state.result.partOfSpeech,
              definition = state.result.definition,
              example = state.result.example,
            )
          }
          is PlayerViewModel.WordLookupState.NotFound -> {
            Text(
              text = stringResource(id = R.string.player_explain_no_definition),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is PlayerViewModel.WordLookupState.Error -> {
            ErrorRow(
              message = stringResource(id = R.string.player_explain_failed),
              actionLabel = stringResource(id = R.string.player_explain_retry),
              onRetry = { tappedWord?.let { viewModel.lookupWord(it) } },
            )
          }
        }

        Text(
          text = stringResource(id = R.string.player_explain_references),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )

        when (val state = refsState) {
          is PlayerViewModel.RefsState.Idle,
          is PlayerViewModel.RefsState.Loading -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is PlayerViewModel.RefsState.Done -> {
            state.refs.forEach { ref ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(enabled = ref.url != null) {
                    ref.url?.let { url ->
                      context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                  },
              ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                  Text(
                    text = ref.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = ref.extract,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
          is PlayerViewModel.RefsState.Empty -> {
            Text(
              text = stringResource(id = R.string.player_explain_no_references),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is PlayerViewModel.RefsState.Error -> {
            ErrorRow(
              message = stringResource(id = R.string.player_explain_failed),
              actionLabel = stringResource(id = R.string.player_explain_retry),
              onRetry = { viewModel.explainReferences(activeLine) },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
    }
  }
}

private fun rememberWords(line: String): List<String> {
  if (line.isBlank()) return emptyList()
  return line.split(Regex("\\s+"))
    .map { it.trim { c -> !c.isLetterOrDigit() } }
    .filter { it.length >= 2 }
    .distinct()
}

@Composable
private fun WordDefinitionCard(
  word: String,
  phonetic: String?,
  partOfSpeech: String?,
  definition: String,
  example: String?,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.MenuBook,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
        Text(
          text = word,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        if (!phonetic.isNullOrBlank()) {
          Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
          Text(
            text = phonetic,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (!partOfSpeech.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = partOfSpeech,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = definition,
        style = MaterialTheme.typography.bodyLarge,
      )
      if (!example.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "“$example”",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ErrorRow(
  message: String,
  actionLabel: String,
  onRetry: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.error,
      modifier = Modifier.weight(1f),
    )
    TextButton(onClick = onRetry) {
      Text(text = actionLabel)
    }
  }
}
