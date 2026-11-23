package app.aryan447.mpvium.preferences

import app.aryan447.mpvium.preferences.preference.Preference
import app.aryan447.mpvium.preferences.preference.PreferenceStore
import app.aryan447.mpvium.ui.player.CustomKeyCodes
import app.aryan447.mpvium.ui.player.SingleActionGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturePreferencesTest {

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

        override fun changes(): Flow<T> = TODO("Not needed")

        override fun stateIn(scope: CoroutineScope): StateFlow<T> = TODO("Not needed")
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

  @Test
  fun mediaControlGestures_defaultValues() {
    val store = InMemoryPreferenceStore()
    val preferences = GesturePreferences(store)

    assertEquals(SingleActionGesture.Seek, preferences.mediaPreviousGesture.get())
    assertEquals(SingleActionGesture.PlayPause, preferences.mediaPlayGesture.get())
    assertEquals(SingleActionGesture.Seek, preferences.mediaNextGesture.get())
  }

  @Test
  fun mediaControlGestures_canBeChanged() {
    val store = InMemoryPreferenceStore()
    val preferences = GesturePreferences(store)

    preferences.mediaPreviousGesture.set(SingleActionGesture.PlayPause)
    preferences.mediaPlayGesture.set(SingleActionGesture.Custom)
    preferences.mediaNextGesture.set(SingleActionGesture.None)

    assertEquals(SingleActionGesture.PlayPause, preferences.mediaPreviousGesture.get())
    assertEquals(SingleActionGesture.Custom, preferences.mediaPlayGesture.get())
    assertEquals(SingleActionGesture.None, preferences.mediaNextGesture.get())
  }

  @Test
  fun legacyMedaPreviousGesture_migratesToMediaPreviousGesture() {
    val store = InMemoryPreferenceStore()
    store.store["meda_previous_gesture"] = SingleActionGesture.PlayPause.name

    val preferences = GesturePreferences(store)

    assertEquals(SingleActionGesture.PlayPause, preferences.mediaPreviousGesture.get())
    assertFalse(store.store.containsKey("meda_previous_gesture"))
    assertTrue(store.store.containsKey("media_previous_gesture"))
    assertEquals(SingleActionGesture.PlayPause.name, store.store["media_previous_gesture"])
  }

  @Test
  fun customKeyCodes_matchMpvCodes() {
    assertEquals("PREV", CustomKeyCodes.MediaPrevious.keyCode)
    assertEquals("PLAYPAUSE", CustomKeyCodes.MediaPlay.keyCode)
    assertEquals("NEXT", CustomKeyCodes.MediaNext.keyCode)
  }
}
