package app.aryan447.mpvium.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_series")
data class CustomSeriesEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val title: String,
  val year: String? = null,
  val posterPath: String? = null,
  val tmdbId: Int? = null,
  val createdAt: Long,
  val updatedAt: Long,
)
