package app.aryan447.mpvium.ui.browser.networkstreaming.clients

import android.net.Uri
import android.util.Log
import app.aryan447.mpvium.domain.network.NetworkConnection
import app.aryan447.mpvium.domain.network.NetworkFile
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.DavResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class WebDavClient(private val connection: NetworkConnection) : NetworkClient {
  companion object {
    private const val TAG = "WebDavClient"

    /**
     * Video extensions treated as playable files. Must stay in sync with
     * FileTypeUtils.VIDEO_EXTENSIONS (utils/storage/VideoScanUtils.kt).
     */
    private val VIDEO_EXTENSIONS = setOf(
      "mp4", "m4v", "mkv", "mk3d", "avi", "mov", "wmv", "flv", "webm",
      "mpeg", "mpg", "m2v", "ogv", "ogm", "ts", "mts", "m2ts", "vob",
      "divx", "xvid", "f4v", "rm", "rmvb", "asf", "3gp", "3g2",
    )
  }

  // Note: Sardine-Android uses OkHttp which properly handles UTF-8 encoding by default
  private var sardine: Sardine? = null

  /**
   * Build full WebDAV URL from connection and relative path.
   * Standard approach: protocol://host:port/basePath/relativePath
   * relativePath is a decoded logical path (e.g. "My Movie/file.mkv").
   * Each segment is percent-encoded so names with spaces or unicode
   * (e.g. Torbox tags like "linux isos") produce valid request URLs.
   */
  private fun buildUrl(relativePath: String): String {
    val protocol = if (connection.useHttps) "https" else "http"
    val baseSegments = connection.path.split('/').filter { it.isNotEmpty() }.map { Uri.encode(it) }
    // Strip any query/fragment that may have leaked into a display path, then encode per segment.
    val relSegments = relativePath.split('/').filter { it.isNotEmpty() }.map {
      Uri.encode(it.substringBefore('?').substringBefore('#'))
    }

    val allSegments = baseSegments + relSegments
    val pathPart = if (allSegments.isEmpty()) "/" else "/" + allSegments.joinToString("/")
    return "$protocol://${connection.host}:${connection.port}$pathPart"
  }

  /**
   * Normalize an href/request path for self-entry comparison.
   * Percent-decoding + trailing-slash insensitivity, case sensitive
   * (servers like Torbox may host "Movie.mkv" folders containing a
   * same-named "Movie.mkv" file, so never compare by bare name).
   */
  private fun normalizePathForCompare(raw: String?): String {
    if (raw.isNullOrEmpty()) return "/"
    val decoded = try {
      Uri.decode(raw) ?: raw
    } catch (_: Exception) {
      raw
    }
    val trimmed = decoded.trim().trim('/')
    return if (trimmed.isEmpty()) "/" else "/$trimmed"
  }

  /**
   * Decide whether a DavResource is a directory.
   *
   * Sardine's isDirectory trusts getcontenttype == "httpd/unix-directory",
   * which misfires on some servers (e.g. Torbox's rewritten WebDAV marks
   * single-file ".mkv" entries as collections). Combine signals:
   * - href trailing slash is authoritative for collections per RFC 4918,
   * - a non-slash href with a video extension is treated as a file,
   * - otherwise fall back to Sardine, letting concrete size/content-type
   *   overrule a directory claim.
   */
  private fun isDirectoryResource(resource: DavResource, decodedName: String): Boolean {
    val hrefPath = try {
      resource.path ?: ""
    } catch (_: Exception) {
      ""
    }
    // Collections are advertised with a trailing slash on their href.
    if (hrefPath.endsWith("/")) return true

    val sardineSaysDir = try {
      resource.isDirectory
    } catch (_: Exception) {
      false
    }
    if (!sardineSaysDir) return false

    val contentLength = try {
      resource.contentLength ?: -1L
    } catch (_: Exception) {
      -1L
    }
    val contentType = try {
      resource.contentType ?: ""
    } catch (_: Exception) {
      ""
    }
    val ext = decodedName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()

    if (ext.isEmpty()) return true

    // A "Movie.mkv" href without trailing slash is a playable file, not a
    // folder — even when the server claims collection (Torbox issue #84).
    if (ext in VIDEO_EXTENSIONS) return false

    // Any other entry with a concrete size and file-like content type is a file.
    if (contentLength > 0 && contentType.isNotEmpty() &&
      contentType != DavResource.HTTPD_UNIX_DIRECTORY_CONTENT_TYPE &&
      contentType != DavResource.DEFAULT_CONTENT_TYPE
    ) {
      return false
    }
    return true
  }

  override suspend fun connect(): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        val client = OkHttpSardine()
        if (!connection.isAnonymous) {
          client.setCredentials(connection.username, connection.password)
        }

        // Validate with PROPFIND (depth 0): some compliant servers (and
        // Torbox's custom WebDAV) do not implement HEAD, which Sardine's
        // exists() relies on.
        val testUrl = buildUrl("")
        try {
          client.exists(testUrl)
        } catch (_: Exception) {
          if (client.list(testUrl, 0).isEmpty()) {
            throw IllegalStateException("WebDAV base path returned no resources")
          }
        }

        sardine = client
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun disconnect() {
    withContext(Dispatchers.IO) {
      sardine = null
    }
  }

  override fun isConnected(): Boolean = sardine != null

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> =
    withContext(Dispatchers.IO) {
      try {
        val client = sardine ?: return@withContext Result.failure(Exception("Not connected"))

        val url = buildUrl(path)
        Log.d(TAG, "listFiles path='$path' url='$url'")

        val resources = try {
          client.list(url)
        } catch (e: Exception) {
          // Strict servers (e.g. Torbox) may require a trailing slash on collections.
          if (!url.endsWith("/")) {
            Log.d(TAG, "listFiles retry with trailing slash for '$url'", e)
            try {
              client.list("$url/")
            } catch (e2: Exception) {
              return@withContext Result.failure(e2)
            }
          } else {
            throw e
          }
        }

        // Filter out the directory itself by href, not by position: response
        // order is not guaranteed, and blind drop(1) deletes real files when
        // the server omits self or returns children first.
        val requestPath = try {
          normalizePathForCompare(Uri.parse(url).path)
        } catch (_: Exception) {
          normalizePathForCompare(path)
        }
        val children = resources.filterNot { resource ->
          val resPath = try {
            resource.path
          } catch (_: Exception) {
            null
          }
          normalizePathForCompare(resPath) == requestPath
        }
        val effective = if (children.size == resources.size && resources.size > 1) {
          Log.d(TAG, "listFiles: no self entry in ${resources.size} resources, keeping all")
          resources
        } else {
          children
        }
        if (effective.isEmpty()) {
          Log.d(TAG, "listFiles: empty after self-filter (got ${resources.size}) path='$path'")
        }

        val files =
          effective.mapNotNull { resource: DavResource ->
            try {
              // DavResource.getName() derives from URI.getPath(), so it is
              // already percent-decoded and safe for display + child paths.
              val decodedName = try {
                resource.name ?: ""
              } catch (_: Exception) {
                ""
              }
              if (decodedName.isEmpty()) return@mapNotNull null

              val isDir = isDirectoryResource(resource, decodedName)
              val sardineDir = try {
                resource.isDirectory.toString()
              } catch (_: Exception) {
                "?"
              }
              val entryType = try {
                resource.contentType ?: "?"
              } catch (_: Exception) {
                "?"
              }
              val entryLen = try {
                (resource.contentLength ?: "?").toString()
              } catch (_: Exception) {
                "?"
              }
              val entryHref = try {
                resource.path ?: "?"
              } catch (_: Exception) {
                "?"
              }
              Log.d(
                TAG,
                "entry name='$decodedName' dir=$isDir sardineDir=$sardineDir type=$entryType len=$entryLen href=$entryHref",
              )

              // Child paths stay decoded; buildUrl() encodes on the next request.
              val filePath = if (path.isEmpty() || path == "/") {
                decodedName
              } else {
                "${path.trim().trimEnd('/')}/$decodedName"
              }

              NetworkFile(
                name = decodedName,
                path = filePath,
                isDirectory = isDir,
                size = try { resource.contentLength ?: 0L } catch (_: Exception) { 0L },
                lastModified = try { resource.modified?.time ?: 0L } catch (_: Exception) { 0L },
                mimeType = if (!isDir) getMimeType(decodedName) else null,
              )
            } catch (e: Exception) {
              Log.w(TAG, "Skipping unreadable WebDAV entry", e)
              null
            }
          }

        Result.success(files)
      } catch (e: Exception) {
        Log.e(TAG, "listFiles failed for path='$path'", e)
        Result.failure(e)
      }
    }

  /**
   * Get file size for a specific file path
   * This is useful for the proxy server to support range requests
   */
  suspend fun getFileSize(path: String): Result<Long> =
    withContext(Dispatchers.IO) {
      try {
        val client = sardine ?: return@withContext Result.failure(Exception("Not connected"))

        val url = buildUrl(path)

        // Use PROPFIND to get file properties including size
        val resources = client.list(url, 0) // depth 0 = only the resource itself
        if (resources.isEmpty()) {
          return@withContext Result.failure(Exception("File not found"))
        }
        val resource = resources[0]
        val decodedName = try {
          resource.name ?: path.substringAfterLast('/')
        } catch (_: Exception) {
          path.substringAfterLast('/')
        }
        val size = try {
          resource.contentLength ?: -1L
        } catch (_: Exception) {
          -1L
        }
        // A misclassified collection with a concrete size is still a file (issue #84).
        if (!isDirectoryResource(resource, decodedName) || size > 0) {
          Result.success(size)
        } else {
          Result.failure(Exception("File not found or is a directory"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileStream(path: String): Result<InputStream> =
    withContext(Dispatchers.IO) {
      try {
        // Create a fresh Sardine client for this stream to avoid connection conflicts
        val streamClient = OkHttpSardine()

        if (!connection.isAnonymous) {
          streamClient.setCredentials(connection.username, connection.password)
        }

        val url = buildUrl(path)
        val rawStream = streamClient.get(url)

        if (rawStream == null) {
          return@withContext Result.failure(Exception("Failed to open WebDAV stream"))
        }

        // Wrap the stream
        val wrappedStream = object : InputStream() {
          override fun read(): Int = rawStream.read()

          override fun read(b: ByteArray): Int = rawStream.read(b)

          override fun read(b: ByteArray, off: Int, len: Int): Int = rawStream.read(b, off, len)

          override fun available(): Int = rawStream.available()

          override fun close() {
            try {
              rawStream.close()
            } catch (e: Exception) {
              // Ignore
            }
          }
        }

        Result.success(wrappedStream)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileUri(path: String): Result<Uri> =
    withContext(Dispatchers.IO) {
      try {
        val protocol = if (connection.useHttps) "https" else "http"
        val baseSegments = connection.path.split('/').filter { it.isNotEmpty() }.map { Uri.encode(it) }
        val cleanSegments = path.split('/').filter { it.isNotEmpty() }.map {
          Uri.encode(it.substringBefore('?').substringBefore('#'))
        }

        val fullPath = (baseSegments + cleanSegments).joinToString("/")

        // Build WebDAV URI with credentials embedded for mpv
        val uriString = if (connection.isAnonymous) {
          "$protocol://${connection.host}:${connection.port}/$fullPath"
        } else {
          "$protocol://${connection.username}:${connection.password}@${connection.host}:${connection.port}/$fullPath"
        }

        Result.success(Uri.parse(uriString))
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun getMimeType(fileName: String): String? {
    val extension = fileName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
    return when (extension) {
      "mp4", "m4v" -> "video/mp4"
      "mkv", "mk3d" -> "video/x-matroska"
      "avi", "divx", "xvid" -> "video/x-msvideo"
      "mov" -> "video/quicktime"
      "wmv", "asf" -> "video/x-ms-wmv"
      "flv", "f4v" -> "video/x-flv"
      "webm" -> "video/webm"
      "mpeg", "mpg", "m2v", "vob" -> "video/mpeg"
      "3gp", "3g2" -> "video/3gpp"
      "ts", "mts", "m2ts" -> "video/mp2t"
      "ogv", "ogm" -> "video/ogg"
      "rm", "rmvb" -> "video/vnd.rn-realvideo"
      else -> if (extension in VIDEO_EXTENSIONS) "video/*" else null
    }
  }
}
