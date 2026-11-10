package app.aryan447.mpvium.ui.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.ui.player.PlayerActivity

object AudioPlayerLauncher {

  /**
   * Launches PlayerActivity in audio mode with the album tracks as the playback queue.
   */
  fun playAlbum(context: Context, album: LocalAlbum, startIndex: Int = 0) {
    if (album.tracks.isEmpty()) return
    val index = startIndex.coerceIn(album.tracks.indices)
    val selectedTrack = album.tracks[index]
    val playlistUris = ArrayList(album.tracks.map { it.uri })

    val intent = Intent(context, PlayerActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      data = selectedTrack.uri
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putParcelableArrayListExtra("playlist", playlistUris)
      putExtra("playlist_index", index)
      putExtra("is_audio_mode", true)
      putExtra("title", selectedTrack.title)
      putExtra("media_title", selectedTrack.title)
      putExtra("media_artist", selectedTrack.artist)
      putExtra("album_title", album.title)
      album.artworkUri?.let { putExtra("album_art_uri", it.toString()) }
    }
    context.startActivity(intent)
  }

  /**
   * Launches PlayerActivity for a track or list of tracks.
   */
  fun playTrack(context: Context, track: LocalAudioTrack, queue: List<LocalAudioTrack> = listOf(track)) {
    val index = queue.indexOf(track).coerceAtLeast(0)
    val playlistUris = ArrayList(queue.map { it.uri })

    val intent = Intent(context, PlayerActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      data = track.uri
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putParcelableArrayListExtra("playlist", playlistUris)
      putExtra("playlist_index", index)
      putExtra("is_audio_mode", true)
      putExtra("title", track.title)
      putExtra("media_title", track.title)
      putExtra("media_artist", track.artist)
      putExtra("album_title", track.album)
      track.albumArtUri?.let { putExtra("album_art_uri", it.toString()) }
    }
    context.startActivity(intent)
  }
}
