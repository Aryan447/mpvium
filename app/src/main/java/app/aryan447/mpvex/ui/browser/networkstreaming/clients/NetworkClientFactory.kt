package app.aryan447.mpvex.ui.browser.networkstreaming.clients

import app.aryan447.mpvex.domain.network.NetworkConnection
import app.aryan447.mpvex.domain.network.NetworkProtocol

object NetworkClientFactory {
  fun createClient(connection: NetworkConnection): NetworkClient =
    when (connection.protocol) {
      NetworkProtocol.SMB -> SmbClient(connection)
      NetworkProtocol.FTP -> FtpClient(connection)
      NetworkProtocol.WEBDAV -> WebDavClient(connection)
    }
}
