package app.aryan447.mpvium.ui.preferences

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchablePreferencesKeyTest {
  private val preferences: List<SearchablePreference>
    get() = SearchablePreferences.allPreferences

  @Test
  fun keysAreUnique() {
    val duplicates =
      preferences
        .groupBy { it.key }
        .filterValues { it.size > 1 }
        .keys

    assertTrue("Duplicate searchable preference keys: $duplicates", duplicates.isEmpty())
  }

  @Test
  fun keysAreNotBlank() {
    val blankKeys =
      preferences
        .filter { it.key.isBlank() }
        .map { it.title ?: it.titleRes?.toString().orEmpty() }

    assertTrue("Blank searchable preference keys for: $blankKeys", blankKeys.isEmpty())
  }

  @Test
  fun keysAreNamespaced() {
    val unnamespaced =
      preferences
        .map { it.key }
        .filterNot { KeyPattern.matches(it) }

    assertTrue(
      "Searchable preference keys must be lowercase and dot-namespaced: $unnamespaced",
      unnamespaced.isEmpty(),
    )
  }

  private companion object {
    private val KeyPattern = Regex("^[a-z0-9]+(\\.[a-z0-9_]+)+$")
  }
}
