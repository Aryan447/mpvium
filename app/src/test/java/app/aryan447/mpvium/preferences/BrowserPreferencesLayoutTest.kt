package app.aryan447.mpvium.preferences

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import app.aryan447.mpvium.preferences.preference.Preference
import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.preferences.preference.getEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class BrowserPreferencesLayoutTest {

  private class InMemoryPreferenceStore : PreferenceStore {
    val store = mutableMapOf<String, Any>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()

    private fun getOrCreateFlow(key: String, default: Any?): MutableStateFlow<Any?> {
      return flows.getOrPut(key) { MutableStateFlow(store[key] ?: default) }
    }

    override fun getString(key: String, defaultValue: String): Preference<String> =
      createPref(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
      createPref(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
      createPref(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
      createPref(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
      createPref(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
      createPref(key, defaultValue)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObject(
      key: String,
      defaultValue: T,
      serializer: (T) -> String,
      deserializer: (String) -> T,
    ): Preference<T> {
      val flow = getOrCreateFlow(key, serializer(defaultValue))
      return object : Preference<T> {
        override fun key(): String = key

        override fun get(): T {
          val raw = store[key] as? String ?: return defaultValue
          return deserializer(raw)
        }

        override fun set(value: T) {
          val serialized = serializer(value)
          store[key] = serialized
          flow.value = serialized
        }

        override fun isSet(): Boolean = store.containsKey(key)

        override fun delete() {
          store.remove(key)
          flow.value = serializer(defaultValue)
        }

        override fun defaultValue(): T = defaultValue

        override fun changes(): Flow<T> {
          TODO("Not needed for basic unit test")
        }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
          TODO("Not needed for basic unit test")
        }
      }
    }

    private fun <T : Any> createPref(key: String, defaultValue: T): Preference<T> {
      val flow = getOrCreateFlow(key, defaultValue)
      return object : Preference<T> {
        override fun key(): String = key
        @Suppress("UNCHECKED_CAST")
        override fun get(): T = (store[key] as? T) ?: defaultValue
        override fun set(value: T) {
          store[key] = value
          flow.value = value
        }
        override fun isSet(): Boolean = store.containsKey(key)
        override fun delete() {
          store.remove(key)
          flow.value = defaultValue
        }
        override fun defaultValue(): T = defaultValue
        override fun changes(): Flow<T> = TODO("Not needed")
        override fun stateIn(scope: CoroutineScope): StateFlow<T> = TODO("Not needed")
      }
    }

    override fun getAll(): Map<String, *> = store
  }

  private fun createMockContext(): Context {
    val config = Configuration().apply {
      smallestScreenWidthDp = 360
    }
    val resourcesHandler = InvocationHandler { _, method, _ ->
      when (method.name) {
        "getConfiguration" -> config
        else -> null
      }
    }
    val resources = Proxy.newProxyInstance(
      Resources::class.java.classLoader,
      arrayOf(Resources::class.java),
      resourcesHandler,
    ) as Resources

    val contextHandler = InvocationHandler { _, method, _ ->
      when (method.name) {
        "getResources" -> resources
        else -> null
      }
    }
    return Proxy.newProxyInstance(
      Context::class.java.classLoader,
      arrayOf(Context::class.java),
      contextHandler,
    ) as Context
  }

  @Test
  fun mediaLayoutMode_hasExpectedValuesAndDisplayNames() {
    assertEquals(2, MediaLayoutMode.entries.size)
    assertEquals(MediaLayoutMode.LIST, MediaLayoutMode.valueOf("LIST"))
    assertEquals(MediaLayoutMode.GRID, MediaLayoutMode.valueOf("GRID"))
    assertEquals("List", MediaLayoutMode.LIST.displayName)
    assertEquals("Grid", MediaLayoutMode.GRID.displayName)
  }

  @Test
  fun layoutPreferences_haveRequiredKeysAndDefaults() {
    val store = InMemoryPreferenceStore()
    val context = createMockContext()
    val preferences = BrowserPreferences(store, context)

    // Requirement 1: Three keys on the existing enum:
    // layout_shows (default GRID), layout_movies (default GRID), layout_folders (default LIST)
    assertEquals("layout_shows", preferences.layoutShows.key())
    assertEquals(MediaLayoutMode.GRID, preferences.layoutShows.defaultValue())
    assertEquals(MediaLayoutMode.GRID, preferences.layoutShows.get())

    assertEquals("layout_movies", preferences.layoutMovies.key())
    assertEquals(MediaLayoutMode.GRID, preferences.layoutMovies.defaultValue())
    assertEquals(MediaLayoutMode.GRID, preferences.layoutMovies.get())

    assertEquals("layout_folders", preferences.layoutFolders.key())
    assertEquals(MediaLayoutMode.LIST, preferences.layoutFolders.defaultValue())
    assertEquals(MediaLayoutMode.LIST, preferences.layoutFolders.get())

    // mediaLayoutMode backward-compat reference
    assertEquals(MediaLayoutMode.LIST, preferences.mediaLayoutMode.get())
  }

  @Test
  fun layoutPreferences_persistAndToggleIndependently() {
    val store = InMemoryPreferenceStore()
    val context = createMockContext()
    val preferences = BrowserPreferences(store, context)

    // Toggle Shows to LIST
    preferences.layoutShows.set(MediaLayoutMode.LIST)
    assertEquals(MediaLayoutMode.LIST, preferences.layoutShows.get())
    assertEquals("LIST", store.store["layout_shows"])

    // Movies and Folders remain at their independent defaults
    assertEquals(MediaLayoutMode.GRID, preferences.layoutMovies.get())
    assertEquals(MediaLayoutMode.LIST, preferences.layoutFolders.get())

    // Toggle Movies to LIST and Folders to GRID
    preferences.layoutMovies.set(MediaLayoutMode.LIST)
    preferences.layoutFolders.set(MediaLayoutMode.GRID)

    assertEquals(MediaLayoutMode.LIST, preferences.layoutMovies.get())
    assertEquals("LIST", store.store["layout_movies"])
    assertEquals(MediaLayoutMode.GRID, preferences.layoutFolders.get())
    assertEquals("GRID", store.store["layout_folders"])
    assertEquals(MediaLayoutMode.GRID, preferences.mediaLayoutMode.get())
  }
}
