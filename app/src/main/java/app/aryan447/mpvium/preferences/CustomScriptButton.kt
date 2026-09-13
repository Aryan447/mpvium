package app.aryan447.mpvium.preferences

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CustomScriptButton(
  val id: String,
  val title: String,
  val code: String,
) {
  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): List<CustomScriptButton> =
      runCatching {
        if (raw.isBlank()) emptyList() else json.decodeFromString<List<CustomScriptButton>>(raw)
      }.getOrDefault(emptyList())

    fun encode(buttons: List<CustomScriptButton>): String =
      runCatching { json.encodeToString(buttons) }.getOrDefault("[]")
  }
}

object ScriptFileHelper {
  val SUPPORTED_EXTENSIONS = setOf("lua", "js")

  fun isScriptFile(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in SUPPORTED_EXTENSIONS
  }
}
