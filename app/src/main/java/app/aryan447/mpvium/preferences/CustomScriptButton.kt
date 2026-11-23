package app.aryan447.mpvium.preferences

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.Element

@Serializable
data class CustomScriptButton(
  val id: String,
  val title: String,
  val code: String,
  val longPressCode: String = "",
  val enabled: Boolean = true,
) {
  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): List<CustomScriptButton> =
      runCatching {
        if (raw.isBlank()) emptyList() else json.decodeFromString<List<CustomScriptButton>>(raw)
      }.getOrDefault(emptyList())

    fun encode(buttons: List<CustomScriptButton>): String =
      runCatching { json.encodeToString(buttons) }.getOrDefault("[]")

    fun fromXml(xml: String): List<CustomScriptButton> =
      runCatching {
        if (xml.isBlank()) return emptyList()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
        val buttonNodes = doc.getElementsByTagName("button")
        val buttons = mutableListOf<CustomScriptButton>()
        for (i in 0 until buttonNodes.length) {
          val btnElem = buttonNodes.item(i) as? Element ?: continue
          val title = getTagText(btnElem, "title")
          val content = getTagText(btnElem, "content")
          val longPressContent = getTagText(btnElem, "longPressContent")
          val enabledStr = getTagText(btnElem, "enabled")
          val enabled = if (enabledStr.isBlank()) true else enabledStr.lowercase() != "false"
          val id = getTagText(btnElem, "id").ifBlank { UUID.randomUUID().toString() }
          if (title.isNotBlank() || content.isNotBlank()) {
            buttons.add(
              CustomScriptButton(
                id = id,
                title = title.ifBlank { "Button ${i + 1}" },
                code = content,
                longPressCode = longPressContent,
                enabled = enabled,
              )
            )
          }
        }
        buttons
      }.getOrDefault(emptyList())

    fun toXml(buttons: List<CustomScriptButton>): String =
      runCatching {
        val factory = DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().newDocument()
        val root = doc.createElement("customButtons")
        doc.appendChild(root)
        buttons.forEachIndexed { index, button ->
          val btn = doc.createElement("button")
          btn.setAttribute("slot", index.toString())

          val id = doc.createElement("id")
          id.appendChild(doc.createTextNode(button.id))
          btn.appendChild(id)

          val title = doc.createElement("title")
          title.appendChild(doc.createTextNode(button.title))
          btn.appendChild(title)

          val enabled = doc.createElement("enabled")
          enabled.appendChild(doc.createTextNode(button.enabled.toString()))
          btn.appendChild(enabled)

          val content = doc.createElement("content")
          content.appendChild(doc.createCDATASection(button.code))
          btn.appendChild(content)

          val longPress = doc.createElement("longPressContent")
          longPress.appendChild(doc.createCDATASection(button.longPressCode))
          btn.appendChild(longPress)

          val onStartup = doc.createElement("onStartup")
          onStartup.appendChild(doc.createCDATASection(""))
          btn.appendChild(onStartup)

          root.appendChild(btn)
        }

        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        writer.toString()
      }.getOrDefault("")

    private fun getTagText(parent: Element, tagName: String): String {
      val list = parent.getElementsByTagName(tagName)
      if (list.length > 0 && list.item(0).textContent != null) {
        return list.item(0).textContent.trim()
      }
      return ""
    }
  }
}

object ScriptFileHelper {
  val SUPPORTED_EXTENSIONS = setOf("lua", "js")

  fun isScriptFile(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in SUPPORTED_EXTENSIONS
  }

  fun normalizeScriptName(name: String?): String? {
    if (name.isNullOrBlank()) return null
    val trimmed = name.trim()
    val ext = trimmed.substringAfterLast('.', "").lowercase()
    return when {
      ext in SUPPORTED_EXTENSIONS -> trimmed
      ext == "txt" -> {
        val base = trimmed.substringBeforeLast('.')
        val innerExt = base.substringAfterLast('.', "").lowercase()
        if (innerExt in SUPPORTED_EXTENSIONS) {
          base
        } else {
          "$base.lua"
        }
      }
      else -> null
    }
  }
}
