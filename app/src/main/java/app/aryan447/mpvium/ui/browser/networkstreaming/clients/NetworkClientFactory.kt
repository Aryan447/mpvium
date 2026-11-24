package app.aryan447.mpvium.ui.browser.networkstreaming.clients

import app.aryan447.mpvium.domain.network.NetworkConnection
import app.aryan447.mpvium.domain.network.NetworkProtocol
import app.aryan447.mpvium.domain.network.sanitized

object NetworkClientFactory {
  fun createClient(connection: NetworkConnection): NetworkClient {
    val sanitized = connection.sanitized()
    return when (sanitized.protocol) {
      NetworkProtocol.SMB -> SmbClient(sanitized)
      NetworkProtocol.FTP -> FtpClient(sanitized)
      NetworkProtocol.WEBDAV -> WebDavClient(sanitized)
    }
  }
}
