package app.aryan447.mpvium.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_index")
data class LibraryIndexEntity(
  @PrimaryKey val path: String,
  val size: Long,
  val mtimeMs: Long,
  val parsedTitle: String,
  val parsedYear: String?,
  val parsedSeason: Int?,
  val parsedEpisode: Int?,
  val parsedEpisodeTitle: String?,
  val parsedType: String,
  val mediaStoreId: Long,
  val displayName: String,
  val durationMs: Long,
  val dateAdded: Long,
  val mimeType: String,
  val bucketId: String,
  val bucketDisplayName: String,
  val width: Int,
  val height: Int,
  val fps: Float,
  val lastScanned: Long = System.currentTimeMillis(),
)
