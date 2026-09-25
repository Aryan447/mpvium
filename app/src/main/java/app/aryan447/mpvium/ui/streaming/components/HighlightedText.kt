package app.aryan447.mpvium.ui.streaming.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Title text with the search match bolded in the primary color.
 * Falls back to plain text when the query is blank or absent.
 */
@Composable
fun HighlightedText(
  text: String,
  query: String?,
  modifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  val trimmedQuery = query?.trim()
  if (trimmedQuery.isNullOrEmpty()) {
    Text(
      text = text,
      style = style,
      color = color,
      maxLines = maxLines,
      overflow = overflow,
      modifier = modifier,
    )
    return
  }
  val highlightColor = MaterialTheme.colorScheme.primary
  val annotated = remember(text, trimmedQuery, highlightColor) {
    buildAnnotatedString {
      append(text)
      var start = text.indexOf(trimmedQuery, ignoreCase = true)
      while (start >= 0) {
        addStyle(
          style = SpanStyle(
            color = highlightColor,
            fontWeight = FontWeight.Bold,
          ),
          start = start,
          end = (start + trimmedQuery.length).coerceAtMost(text.length),
        )
        start = text.indexOf(trimmedQuery, startIndex = start + trimmedQuery.length, ignoreCase = true)
      }
    }
  }
  Text(
    text = annotated,
    style = style,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
    modifier = modifier,
  )
}
