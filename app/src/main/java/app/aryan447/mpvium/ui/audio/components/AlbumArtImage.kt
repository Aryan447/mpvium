package app.aryan447.mpvium.ui.audio.components

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.aryan447.mpvium.domain.audio.AudioArtworkStore
import app.aryan447.mpvium.domain.media.model.LocalAlbum
import app.aryan447.mpvium.domain.media.model.LocalAudioTrack
import org.koin.compose.koinInject

/**
 * Modern Compose Image component for audio album covers.
 *
 * Implements Spec 15 requirements:
 * 1. Embedded art first via [MediaMetadataRetriever]; network fallback next.
 * 2. Network source: MusicBrainz + Cover Art Archive (keyless).
 * 3. Offline cache following [AudioArtworkStore]'s durable pattern, keyed by album+artist.
 * 4. Graceful fallback: a generated placeholder tile (initials on a tonal surface)
 *    when both embedded and network art are missing — no broken-image icon.
 * 5. Extends the existing image-loading and caching stack without secondary libraries.
 */
@Composable
fun AlbumArtImage(
  artworkUri: Uri? = null,
  modifier: Modifier = Modifier,
  album: LocalAlbum? = null,
  track: LocalAudioTrack? = null,
  albumTitle: String? = null,
  artistName: String? = null,
  contentDescription: String? = null,
  contentScale: ContentScale = ContentScale.Crop,
  targetSizePx: Int = 300,
) {
  val artworkStore = koinInject<AudioArtworkStore>()

  val resolvedTitle = album?.title ?: track?.album ?: albumTitle.orEmpty()
  val resolvedArtist = album?.artist ?: track?.artist ?: artistName.orEmpty()
  val resolvedTracks = album?.tracks ?: (track?.let { listOf(it) } ?: emptyList())
  val resolvedUri = artworkUri ?: album?.artworkUri ?: track?.albumArtUri

  val resolvedKey = when {
    album != null -> album.id
    track != null -> track.albumKey
    resolvedTitle.isNotBlank() || resolvedArtist.isNotBlank() -> LocalAlbum.createId(resolvedTitle, resolvedArtist)
    resolvedUri != null -> resolvedUri.toString()
    else -> ""
  }

  val cappedDimension = targetSizePx.coerceAtLeast(1)

  var bitmap by remember(resolvedKey, cappedDimension) {
    mutableStateOf(if (resolvedKey.isNotBlank()) artworkStore.getFromMemory(resolvedKey, cappedDimension) else null)
  }

  LaunchedEffect(resolvedKey, cappedDimension, resolvedUri) {
    if (bitmap != null) return@LaunchedEffect

    if (resolvedTitle.isNotBlank() || resolvedArtist.isNotBlank() || resolvedTracks.isNotEmpty() || resolvedUri != null) {
      val loaded = artworkStore.loadArtwork(
        album = resolvedTitle,
        artist = resolvedArtist,
        tracks = resolvedTracks,
        albumArtUri = resolvedUri,
        maxDimension = cappedDimension,
      )
      if (loaded != null) {
        bitmap = loaded
      }
    }
  }

  Crossfade(
    targetState = bitmap,
    animationSpec = tween(250),
    label = "AlbumArtCrossfade",
    modifier = modifier,
  ) { currentBitmap ->
    if (currentBitmap != null) {
      Image(
        bitmap = currentBitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = contentScale,
      )
    } else {
      AlbumPlaceholderTile(
        title = resolvedTitle,
        artist = resolvedArtist,
        key = resolvedKey,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
