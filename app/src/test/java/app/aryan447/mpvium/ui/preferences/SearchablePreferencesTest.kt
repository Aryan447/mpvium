package app.aryan447.mpvium.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchablePreferencesTest {

    @Test
    fun allPreferencesHaveNonBlankKeys() {
        val all = SearchablePreferences.allPreferences
        assertFalse("SearchablePreferences registry should not be empty", all.isEmpty())
        for (pref in all) {
            assertFalse(
                "SearchablePreference with titleRes=${pref.titleRes} title=${pref.title} has a blank key",
                pref.key.isBlank(),
            )
        }
    }

    @Test
    fun allPreferencesHaveUniqueKeys() {
        val all = SearchablePreferences.allPreferences
        val keys = all.map { it.key }
        val duplicates = keys.groupingBy { it }.eachCount().filter { it.value > 1 }

        assertTrue(
            "Found duplicate keys in SearchablePreferences: ${duplicates.keys}",
            duplicates.isEmpty(),
        )
        assertEquals(
            "Unique key count should match total preferences count",
            keys.size,
            keys.toSet().size,
        )
    }

    @Test
    fun allPreferencesHaveNamespacedKeys() {
        val all = SearchablePreferences.allPreferences
        for (pref in all) {
            assertTrue(
                "Key '${pref.key}' should be namespaced (contain at least one dot)",
                pref.key.contains('.'),
            )
        }
    }
}
