package app.aryan447.mpvium.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates poster palette extraction, caching, and active state for the Aura theme.
 *
 * Requirements:
 * - Caches extracted palette keyed by artwork path so each artwork is decoded once.
 * - Decodes downscaled bitmaps only (<= 128 px long edge) on background threads.
 * - Derives colors for Continue Watching, with detail-screen preview and fallback.
 */
object AuraThemeManager {
  private const val MAX_BITMAP_DIMENSION = 128
  private val paletteCache = ConcurrentHashMap<String, AuraPalette>()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var extractionJob: Job? = null

  @Volatile
  private var appContext: Context? = null

  private val _currentPalette = MutableStateFlow(AuraFallbackPalette)
  val currentPalette: StateFlow<AuraPalette> = _currentPalette.asStateFlow()

  private val _previewArtwork = MutableStateFlow<String?>(null)
  private val _continueWatchingArtwork = MutableStateFlow<String?>(null)
  private val _lastDetailArtwork = MutableStateFlow<String?>(null)

  @Volatile
  private var activeArtworkPath: String? = null

  fun init(context: Context) {
    if (appContext == null) {
      appContext = context.applicationContext
    }
  }

  fun setPreviewArtwork(artwork: String?) {
    if (_previewArtwork.value == artwork) return
    _previewArtwork.value = artwork
    recomputeActiveArtwork()
  }

  fun clearPreviewArtwork(artwork: String?) {
    if (!artwork.isNullOrBlank()) {
      _lastDetailArtwork.value = artwork
    }
    if (_previewArtwork.value == artwork) {
      _previewArtwork.value = null
      recomputeActiveArtwork()
    }
  }

  fun setContinueWatchingArtwork(artwork: String?) {
    if (_continueWatchingArtwork.value == artwork) return
    _continueWatchingArtwork.value = artwork
    recomputeActiveArtwork()
  }

  fun setLastDetailArtwork(artwork: String?) {
    if (_lastDetailArtwork.value == artwork) return
    _lastDetailArtwork.value = artwork
    recomputeActiveArtwork()
  }

  private fun recomputeActiveArtwork() {
    val target = _previewArtwork.value?.takeIf { it.isNotBlank() }
      ?: _continueWatchingArtwork.value?.takeIf { it.isNotBlank() }
      ?: _lastDetailArtwork.value?.takeIf { it.isNotBlank() }

    if (target == activeArtworkPath) return
    activeArtworkPath = target

    if (target == null) {
      _currentPalette.value = AuraFallbackPalette
      return
    }

    paletteCache[target]?.let { cached ->
      _currentPalette.value = cached
      return
    }

    extractionJob?.cancel()
    extractionJob = scope.launch {
      val palette = extractPalette(target)
      _currentPalette.value = palette
    }
  }

  suspend fun extractPalette(artworkPath: String): AuraPalette = withContext(Dispatchers.IO) {
    if (artworkPath.isBlank()) return@withContext AuraFallbackPalette
    paletteCache[artworkPath]?.let { return@withContext it }

    val bitmap = decodeSampledBitmap(artworkPath, MAX_BITMAP_DIMENSION)
    val result = if (bitmap != null) {
      extractFromBitmap(bitmap)
    } else {
      AuraFallbackPalette
    }

    paletteCache[artworkPath] = result
    result
  }

  fun extractFromBitmap(bitmap: Bitmap): AuraPalette {
    val longEdge = maxOf(bitmap.width, bitmap.height)
    val scaledBitmap = if (longEdge > MAX_BITMAP_DIMENSION) {
      val scale = MAX_BITMAP_DIMENSION.toFloat() / longEdge
      val destWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
      val destHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, true)
    } else {
      bitmap
    }

    val palette = runCatching {
      Palette.from(scaledBitmap).generate()
    }.getOrNull() ?: return AuraFallbackPalette

    val candidates = mutableListOf<RawPaletteCandidate>()
    palette.vibrantSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population + 10000)) }
    palette.lightVibrantSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population + 5000)) }
    palette.darkVibrantSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population + 5000)) }
    palette.dominantSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population)) }
    palette.mutedSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population)) }
    palette.lightMutedSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population)) }
    palette.darkMutedSwatch?.let { candidates.add(RawPaletteCandidate(it.rgb, it.population)) }
    for (swatch in palette.swatches) {
      if (candidates.none { it.rgb == swatch.rgb }) {
        candidates.add(RawPaletteCandidate(swatch.rgb, swatch.population))
      }
    }

    return AuraPaletteExtractor.derivePaletteFromCandidates(candidates)
  }

  private fun decodeSampledBitmap(artworkPath: String, maxDimension: Int): Bitmap? {
    val file = resolveArtworkFile(artworkPath) ?: return null
    return runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
      if (longEdge <= 0) return null
      var sampleSize = 1
      while (longEdge / sampleSize > maxDimension) {
        sampleSize *= 2
      }
      val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeFile(file.absolutePath, opts)
    }.getOrNull()
  }

  private fun resolveArtworkFile(path: String): File? {
    val clean = path.removePrefix("file://")
    val direct = File(clean)
    if (direct.isFile && direct.length() > 0L) return direct

    val ctx = appContext ?: return null
    val diskDir = File(ctx.filesDir, "streaming_art")
    val hashed = File(diskDir, "${hash(path)}.img")
    if (hashed.isFile && hashed.length() > 0L) return hashed

    return null
  }

  private fun hash(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun getCachedPalette(artworkPath: String): AuraPalette? = paletteCache[artworkPath]

  fun clearCache() {
    paletteCache.clear()
    _previewArtwork.value = null
    _continueWatchingArtwork.value = null
    _lastDetailArtwork.value = null
    activeArtworkPath = null
    _currentPalette.value = AuraFallbackPalette
  }
}
