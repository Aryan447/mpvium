package app.aryan447.mpvium.domain.network

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a network connection configuration
 */
@Entity(tableName = "network_connections")
data class NetworkConnection(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val protocol: NetworkProtocol,
  val host: String,
  val port: Int,
  val username: String = "",
  val password: String = "",
  val path: String = "/",
  val isAnonymous: Boolean = false,
  val lastConnected: Long = 0,
  val autoConnect: Boolean = false,
  val useHttps: Boolean = false,  // For WebDAV: use HTTPS instead of HTTP
)

/**
 * Supported network protocols
 */
enum class NetworkProtocol(val displayName: String, val defaultPort: Int) {
  SMB("SMB", 445),
  FTP("FTP", 21),
  WEBDAV("WebDAV", 80),
}

/**
 * Runtime status of a network connection
 */
data class ConnectionStatus(
  val connectionId: Long,
  val isConnected: Boolean = false,
  val isConnecting: Boolean = false,
  val error: String? = null,
)

/**
 * Sanitized connection details after extracting scheme, embedded port, or path from host.
 */
data class SanitizedConnection(
  val host: String,
  val port: Int,
  val path: String,
  val useHttps: Boolean,
  val protocol: NetworkProtocol,
)

/**
 * Sanitizes connection input when a user enters/pastes a URL or host with a path or port.
 * E.g., "offcloud.com/webdav" -> host: "offcloud.com", path: "/webdav"
 * "https://offcloud.com:443/webdav" -> host: "offcloud.com", port: 443, path: "/webdav", useHttps: true
 */
fun sanitizeConnectionInput(
  rawHost: String,
  rawPort: Int,
  rawPath: String,
  currentUseHttps: Boolean,
  currentProtocol: NetworkProtocol,
): SanitizedConnection {
  var trimmed = rawHost.trim()
  var detectedHttps = false
  var detectedProtocol: NetworkProtocol? = null

  val schemes = listOf(
    "https://" to (NetworkProtocol.WEBDAV to true),
    "webdavs://" to (NetworkProtocol.WEBDAV to true),
    "http://" to (NetworkProtocol.WEBDAV to false),
    "webdav://" to (NetworkProtocol.WEBDAV to false),
    "smb://" to (NetworkProtocol.SMB to false),
    "ftp://" to (NetworkProtocol.FTP to false),
    "ftps://" to (NetworkProtocol.FTP to false),
  )

  for ((prefix, info) in schemes) {
    if (trimmed.startsWith(prefix, ignoreCase = true)) {
      trimmed = trimmed.substring(prefix.length)
      detectedProtocol = info.first
      if (info.second) detectedHttps = true
      break
    }
  }

  val (hostAndPort, extractedPath) = if (trimmed.contains('/')) {
    trimmed.substringBefore('/') to trimmed.substringAfter('/')
  } else {
    trimmed to ""
  }

  val (cleanHost, extractedPort) = when {
    hostAndPort.startsWith("[") && hostAndPort.contains("]:") -> {
      val h = hostAndPort.substringBefore("]:") + "]"
      val p = hostAndPort.substringAfter("]:").toIntOrNull()
      h to p
    }
    !hostAndPort.startsWith("[") && hostAndPort.contains(':') && hostAndPort.count { it == ':' } == 1 -> {
      val h = hostAndPort.substringBefore(':')
      val p = hostAndPort.substringAfter(':').toIntOrNull()
      h to p
    }
    else -> hostAndPort to null
  }

  val finalProto = detectedProtocol ?: currentProtocol
  val finalHttps = detectedHttps || currentUseHttps
  var finalPort = extractedPort ?: rawPort
  if (finalHttps && finalPort == NetworkProtocol.WEBDAV.defaultPort && finalProto == NetworkProtocol.WEBDAV) {
    finalPort = 443
  }

  val cleanExt = extractedPath.trim().trim('/')
  val cleanRaw = rawPath.trim().trim('/')
  val finalPath = when {
    cleanExt.isNotEmpty() && cleanRaw.isNotEmpty() && cleanExt != cleanRaw -> "/$cleanExt/$cleanRaw"
    cleanExt.isNotEmpty() -> "/$cleanExt"
    cleanRaw.isNotEmpty() -> "/$cleanRaw"
    else -> "/"
  }

  return SanitizedConnection(
    host = cleanHost.trim(),
    port = finalPort,
    path = finalPath,
    useHttps = finalHttps,
    protocol = finalProto,
  )
}

/**
 * Returns a copy of [NetworkConnection] with normalized host, port, path, and protocol.
 */
fun NetworkConnection.sanitized(): NetworkConnection {
  val s = sanitizeConnectionInput(
    rawHost = host,
    rawPort = port,
    rawPath = path,
    currentUseHttps = useHttps,
    currentProtocol = protocol,
  )
  return copy(
    host = s.host,
    port = s.port,
    path = s.path,
    useHttps = s.useHttps,
    protocol = s.protocol,
  )
}
