package app.aryan447.mpvium.domain.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConnectionSanitizationTest {

  @Test
  fun `host with embedded path is split into host and path for webdav`() {
    val connection = NetworkConnection(
      name = "Offcloud",
      protocol = NetworkProtocol.WEBDAV,
      host = "offcloud.com/webdav",
      port = 443,
      path = "/",
      useHttps = true,
    ).sanitized()

    assertEquals("offcloud.com", connection.host)
    assertEquals(443, connection.port)
    assertEquals("/webdav", connection.path)
    assertTrue(connection.useHttps)
  }

  @Test
  fun `host with https scheme and embedded path auto-configures https and splits path`() {
    val connection = NetworkConnection(
      name = "Offcloud",
      protocol = NetworkProtocol.WEBDAV,
      host = "https://offcloud.com/webdav",
      port = 80,
      path = "/",
      useHttps = false,
    ).sanitized()

    assertEquals("offcloud.com", connection.host)
    assertEquals(443, connection.port)
    assertEquals("/webdav", connection.path)
    assertTrue(connection.useHttps)
    assertEquals(NetworkProtocol.WEBDAV, connection.protocol)
  }

  @Test
  fun `host with port and path extracts all parts correctly`() {
    val connection = NetworkConnection(
      name = "Custom NAS",
      protocol = NetworkProtocol.WEBDAV,
      host = "nas.local:8080/dav/files",
      port = 80,
      path = "/",
      useHttps = false,
    ).sanitized()

    assertEquals("nas.local", connection.host)
    assertEquals(8080, connection.port)
    assertEquals("/dav/files", connection.path)
  }

  @Test
  fun `smb and ftp URLs extract protocol and clean host`() {
    val smb = NetworkConnection(
      name = "Share",
      protocol = NetworkProtocol.SMB,
      host = "smb://192.168.1.100/movies",
      port = 445,
      path = "/",
    ).sanitized()

    assertEquals("192.168.1.100", smb.host)
    assertEquals(445, smb.port)
    assertEquals("/movies", smb.path)
    assertEquals(NetworkProtocol.SMB, smb.protocol)

    val ftp = NetworkConnection(
      name = "FTP",
      protocol = NetworkProtocol.FTP,
      host = "ftp://ftp.example.com:2121/pub",
      port = 21,
      path = "/",
    ).sanitized()

    assertEquals("ftp.example.com", ftp.host)
    assertEquals(2121, ftp.port)
    assertEquals("/pub", ftp.path)
    assertEquals(NetworkProtocol.FTP, ftp.protocol)
  }

  @Test
  fun `ipv6 address with port is preserved correctly`() {
    val connection = NetworkConnection(
      name = "IPv6",
      protocol = NetworkProtocol.WEBDAV,
      host = "[2001:db8::1]:8443/webdav",
      port = 80,
      path = "/",
      useHttps = true,
    ).sanitized()

    assertEquals("[2001:db8::1]", connection.host)
    assertEquals(8443, connection.port)
    assertEquals("/webdav", connection.path)
  }
}
