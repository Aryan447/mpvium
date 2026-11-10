package app.aryan447.mpvium.ui.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.aryan447.mpvium.domain.audio.AudioCollectionsRepository
import app.aryan447.mpvium.domain.audio.model.AudioCollectionType
import app.aryan447.mpvium.domain.audio.model.ContinueListeningItem
import app.aryan447.mpvium.domain.audio.model.LocalAudiobook
import app.aryan447.mpvium.domain.audio.model.LocalDiscourse
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import app.aryan447.mpvium.ui.player.PlayerActivity
import org.koin.java.KoinJavaComponent.get

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

  /**
   * Launches PlayerActivity for an audiobook resuming from the saved part or specified index,
   * restoring the remembered per-book playback speed.
   */
  fun playAudiobook(
    context: Context,
    audiobook: LocalAudiobook,
    startIndex: Int = audiobook.currentPartIndex,
  ) {
    if (audiobook.parts.isEmpty()) return
    val index = startIndex.coerceIn(audiobook.parts.indices)
    val selectedTrack = audiobook.parts[index]
    val playlistUris = ArrayList(audiobook.parts.map { it.uri })

    // Apply remembered playback speed per book across all parts
    runCatching {
      val repo = get<AudioCollectionsRepository>(AudioCollectionsRepository::class.java)
      kotlinx.coroutines.runBlocking {
        audiobook.parts.forEach { track ->
          repo.prepareTrackForPlayback(track, audiobook.playbackSpeed)
        }
      }
    }

    val intent = Intent(context, PlayerActivity::class.java).apply {
      data = selectedTrack.uri
      putParcelableArrayListExtra("playlist", playlistUris)
      putExtra("playlist_index", index)
      putExtra("is_audio_mode", true)
      putExtra("media_title", selectedTrack.title)
      putExtra("media_artist", audiobook.author)
      putExtra("album_title", audiobook.title)
      audiobook.artworkUri?.let { putExtra("album_art_uri", it.toString()) }
    }
    context.startActivity(intent)
  }

  /**
   * Launches PlayerActivity for a discourse/talk series resuming from the active part.
   */
  fun playDiscourse(
    context: Context,
    discourse: LocalDiscourse,
    startIndex: Int = discourse.currentPartIndex,
  ) {
    if (discourse.parts.isEmpty()) return
    val index = startIndex.coerceIn(discourse.parts.indices)
    val selectedTrack = discourse.parts[index]
    val playlistUris = ArrayList(discourse.parts.map { it.uri })

    val intent = Intent(context, PlayerActivity::class.java).apply {
      data = selectedTrack.uri
      putParcelableArrayListExtra("playlist", playlistUris)
      putExtra("playlist_index", index)
      putExtra("is_audio_mode", true)
      putExtra("media_title", selectedTrack.title)
      putExtra("media_artist", discourse.speaker)
      putExtra("album_title", discourse.collection)
      discourse.artworkUri?.let { putExtra("album_art_uri", it.toString()) }
    }
    context.startActivity(intent)
  }

  /**
   * Launches PlayerActivity to resume a Continue Listening item (audiobook or discourse).
   * Ensures the remembered playback speed is applied across parts before launching.
   */
  fun playContinueListening(
    context: Context,
    item: ContinueListeningItem,
  ) {
    val targetTrack = item.targetTrack
    val queue = if (item.queue.isNotEmpty()) item.queue else listOf(targetTrack)
    val index = queue.indexOf(targetTrack).coerceAtLeast(0)
    val playlistUris = ArrayList(queue.map { it.uri })

    if (item.type == AudioCollectionType.AUDIOBOOK && item.playbackSpeed != 1.0) {
      runCatching {
        val repo = get<AudioCollectionsRepository>(AudioCollectionsRepository::class.java)
        kotlinx.coroutines.runBlocking {
          queue.forEach { track ->
            repo.prepareTrackForPlayback(track, item.playbackSpeed)
          }
        }
      }
    }

    val intent = Intent(context, PlayerActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      data = targetTrack.uri
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putParcelableArrayListExtra("playlist", playlistUris)
      putExtra("playlist_index", index)
      putExtra("is_audio_mode", true)
      putExtra("title", targetTrack.title)
      putExtra("media_title", targetTrack.title)
      putExtra("media_artist", item.subtitle)
      putExtra("album_title", item.title)
      item.artworkUri?.let { putExtra("album_art_uri", it.toString()) }
    }
    context.startActivity(intent)
  }
}
