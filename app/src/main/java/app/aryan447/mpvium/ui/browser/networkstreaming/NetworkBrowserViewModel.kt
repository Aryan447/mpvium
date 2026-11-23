package app.aryan447.mpvium.ui.browser.networkstreaming

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.aryan447.mpvium.domain.network.NetworkConnection
import app.aryan447.mpvium.domain.network.NetworkFile
import app.aryan447.mpvium.domain.network.NetworkMediaTypes
import app.aryan447.mpvium.domain.network.NetworkProtocol
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.repository.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for browsing files on a network share
 * Follows MVVM pattern with proper separation of concerns
 */
class NetworkBrowserViewModel(
  private val application: Application,
  private val connectionId: Long,
  private val currentPath: String,
) : AndroidViewModel(application),
  KoinComponent {
  private val repository: NetworkRepository by inject()
  private val playerPreferences: PlayerPreferences by inject()

  private val _files = MutableStateFlow<List<NetworkFile>>(emptyList())
  val files: StateFlow<List<NetworkFile>> = _files.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  /**
   * Load files in the current directory
   */
  fun loadFiles() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      try {
        val connection = repository.getConnectionById(connectionId)
          ?: throw Exception("Connection not found")

        repository.listFiles(connection, currentPath)
          .onSuccess { fileList ->
            _files.value = fileList.sortedWith(
              compareBy<NetworkFile> { !it.isDirectory }
                .thenBy { it.name.lowercase() },
            )
          }
          .onFailure { e ->
            _error.value = e.message ?: "Unknown error"
          }
      } catch (e: Exception) {
        _error.value = e.message ?: "Unknown error"
      } finally {
        _isLoading.value = false
      }
    }
  }



  /**
   * Play a video file
   */
  fun playVideo(file: NetworkFile) {
    viewModelScope.launch {
      try {
        val connection = repository.getConnectionById(connectionId)
          ?: throw Exception("Connection not found")

        // Use proxy server for protocols that need seeking support
        val useProxy = connection.protocol in PROXY_PROTOCOLS
        val isPlaylistMode = playerPreferences.playlistMode.get()

        val videoFiles = if (isPlaylistMode) {
          val filtered = _files.value.filter {
            !it.isDirectory && (
              it.mimeType?.startsWith("video/") == true ||
                NetworkMediaTypes.isPlaylistMimeType(it.mimeType) ||
                NetworkMediaTypes.isVideoFileName(it.name)
            )
          }
          if (filtered.any { it.path == file.path }) filtered else filtered + file
        } else {
          listOf(file)
        }

        val startIndex = videoFiles.indexOfFirst { it.path == file.path }.coerceAtLeast(0)

        val playlistUris: ArrayList<android.net.Uri>
        if (useProxy) {
          val proxy = app.aryan447.mpvium.ui.browser.networkstreaming.proxy.NetworkStreamingProxy.getInstance()
          val now = System.currentTimeMillis()
          playlistUris = ArrayList(
            videoFiles.mapIndexed { index, video ->
              val streamId = "${connectionId}_${now}_$index"
              val proxyBaseUrl = proxy.registerStream(
                streamId = streamId,
                connection = connection,
                filePath = video.path,
                fileSize = video.size,
                mimeType = video.mimeType ?: "video/mp4",
              )
              val encodedName = android.net.Uri.encode(video.name)
              val encodedPath = android.net.Uri.encode(video.path)
              android.net.Uri.parse("$proxyBaseUrl/$encodedName?network_path=$encodedPath&connection_id=$connectionId")
            }
          )
        } else {
          NetworkStreamingProvider.setConnection(connectionId, connection)
          playlistUris = ArrayList(
            videoFiles.map { video ->
              NetworkStreamingProvider.getUri(application, connectionId, video.path)
            }
          )
        }

        val targetUri = playlistUris[startIndex]

        // Launch the player
        val intent = Intent(Intent.ACTION_VIEW, targetUri)
        intent.setClass(application, app.aryan447.mpvium.ui.player.PlayerActivity::class.java)
        intent.putExtra("internal_launch", true)
        intent.putExtra("launch_source", if (playlistUris.size > 1) "playlist" else "network_stream")
        intent.putExtra("title", file.name)
        intent.putExtra("filename", file.name)
        // Pass the original network file path for stable media identifier (position saving)
        intent.putExtra("network_file_path", file.path)
        intent.putExtra("network_connection_id", connectionId)
        if (playlistUris.size > 1) {
          intent.putParcelableArrayListExtra("playlist", playlistUris)
          intent.putExtra("playlist_index", startIndex)
        }
        intent.setDataAndType(targetUri, file.mimeType ?: "video/*")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (!useProxy) {
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        application.startActivity(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Error playing video", e)
        _error.value = e.message ?: "Unknown error"
      }
    }
  }

  companion object {
    private const val TAG = "NetworkBrowserVM"

    // Protocols that require proxy server for seeking support
    private val PROXY_PROTOCOLS = setOf(
      NetworkProtocol.SMB,
      NetworkProtocol.FTP,
      NetworkProtocol.WEBDAV,
    )

    fun factory(
      application: Application,
      connectionId: Long,
      currentPath: String,
    ): ViewModelProvider.Factory =
      viewModelFactory {
        initializer {
          NetworkBrowserViewModel(application, connectionId, currentPath)
        }
      }
  }
}
