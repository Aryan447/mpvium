package app.aryan447.mpvium.database.repository

import app.aryan447.mpvium.database.dao.CustomSeriesDao
import app.aryan447.mpvium.database.entities.CustomSeriesEntity
import app.aryan447.mpvium.database.entities.CustomSeriesItemEntity
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.utils.media.MediaInfoParser
import kotlinx.coroutines.flow.Flow

data class CustomSeriesItemDraft(
  val id: Int = 0,
  val filePath: String,
  val fileName: String,
  val season: Int = 1,
  val episode: Int = 1,
  val position: Int = 0,
)

class CustomSeriesRepository(
  private val customSeriesDao: CustomSeriesDao,
  private val artworkStore: StreamingArtworkStore,
) {

  suspend fun createSeries(
    title: String,
    year: String? = null,
    posterPath: String? = null,
    tmdbId: Int? = null,
    items: List<CustomSeriesItemDraft> = emptyList(),
  ): Long {
    val now = System.currentTimeMillis()
    val cleanTitle = title.trim()
    val cleanYear = year?.trim()?.takeIf { it.isNotBlank() }
    val cleanPoster = posterPath?.trim()?.takeIf { it.isNotBlank() }

    val seriesId = customSeriesDao.insertSeries(
      CustomSeriesEntity(
        title = cleanTitle,
        year = cleanYear,
        posterPath = cleanPoster,
        tmdbId = tmdbId,
        createdAt = now,
        updatedAt = now,
      )
    )

    if (items.isNotEmpty()) {
      val entities = items.mapIndexed { index, draft ->
        CustomSeriesItemEntity(
          seriesId = seriesId.toInt(),
          filePath = draft.filePath,
          fileName = draft.fileName,
          season = draft.season.coerceAtLeast(0),
          episode = draft.episode.coerceAtLeast(1),
          position = if (draft.position >= 0) draft.position else index,
        )
      }
      customSeriesDao.insertItems(entities)
    }

    return seriesId
  }

  suspend fun updateSeries(
    id: Int,
    title: String,
    year: String? = null,
    posterPath: String? = null,
    tmdbId: Int? = null,
    items: List<CustomSeriesItemDraft>,
  ) {
    val existing = customSeriesDao.getSeriesById(id) ?: return
    val now = System.currentTimeMillis()
    val cleanTitle = title.trim()
    val cleanYear = year?.trim()?.takeIf { it.isNotBlank() }
    val cleanPoster = posterPath?.trim()?.takeIf { it.isNotBlank() }

    // Clean up old artwork if poster was changed or removed
    if (existing.posterPath != null && existing.posterPath != cleanPoster) {
      artworkStore.delete(listOf(existing.posterPath))
    }

    customSeriesDao.updateSeries(
      existing.copy(
        title = cleanTitle,
        year = cleanYear,
        posterPath = cleanPoster,
        tmdbId = tmdbId ?: existing.tmdbId,
        updatedAt = now,
      )
    )

    val entities = items.mapIndexed { index, draft ->
      CustomSeriesItemEntity(
        seriesId = id,
        filePath = draft.filePath,
        fileName = draft.fileName,
        season = draft.season.coerceAtLeast(0),
        episode = draft.episode.coerceAtLeast(1),
        position = index,
      )
    }
    customSeriesDao.replaceSeriesItems(id, entities)
  }

  suspend fun deleteSeries(id: Int) {
    val existing = customSeriesDao.getSeriesById(id) ?: return
    if (!existing.posterPath.isNullOrBlank()) {
      artworkStore.delete(listOf(existing.posterPath))
    }
    customSeriesDao.deleteSeries(existing)
  }

  suspend fun addVideosToSeries(seriesId: Int, videos: List<Video>) {
    val existing = customSeriesDao.getSeriesById(seriesId) ?: return
    val currentItems = customSeriesDao.getItemsForSeries(seriesId)
    val maxPosition = currentItems.maxOfOrNull { it.position } ?: -1
    val maxSeason = currentItems.maxOfOrNull { it.season } ?: 1
    var nextEpisode = (currentItems.filter { it.season == maxSeason }.maxOfOrNull { it.episode } ?: 0) + 1

    val newItems = videos.mapIndexed { index, video ->
      val parsed = MediaInfoParser.parse(video.displayName)
      val season = parsed.season ?: maxSeason
      val episode = if (parsed.season != null && parsed.episode != null) {
        parsed.episode
      } else {
        val ep = nextEpisode
        nextEpisode++
        ep
      }

      CustomSeriesItemEntity(
        seriesId = seriesId,
        filePath = video.path,
        fileName = video.displayName,
        season = season,
        episode = episode,
        position = maxPosition + 1 + index,
      )
    }

    customSeriesDao.insertItems(newItems)
    customSeriesDao.updateSeries(existing.copy(updatedAt = System.currentTimeMillis()))
  }

  suspend fun getSeriesById(id: Int): CustomSeriesEntity? = customSeriesDao.getSeriesById(id)

  fun observeSeriesById(id: Int): Flow<CustomSeriesEntity?> = customSeriesDao.observeSeriesById(id)

  suspend fun getAllSeries(): List<CustomSeriesEntity> = customSeriesDao.getAllSeries()

  fun observeAllSeries(): Flow<List<CustomSeriesEntity>> = customSeriesDao.observeAllSeries()

  suspend fun getItemsForSeries(seriesId: Int): List<CustomSeriesItemEntity> =
    customSeriesDao.getItemsForSeries(seriesId)

  fun observeItemsForSeries(seriesId: Int): Flow<List<CustomSeriesItemEntity>> =
    customSeriesDao.observeItemsForSeries(seriesId)

  suspend fun getAllItems(): List<CustomSeriesItemEntity> = customSeriesDao.getAllItems()

  fun observeAllItems(): Flow<List<CustomSeriesItemEntity>> = customSeriesDao.observeAllItems()

  suspend fun getAllClaimedFilePaths(): Set<String> =
    customSeriesDao.getAllItems().mapTo(HashSet()) { it.filePath }
}
