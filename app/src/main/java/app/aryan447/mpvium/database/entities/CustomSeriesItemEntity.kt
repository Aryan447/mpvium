package app.aryan447.mpvium.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "custom_series_item",
  foreignKeys = [
    ForeignKey(
      entity = CustomSeriesEntity::class,
      parentColumns = ["id"],
      childColumns = ["seriesId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["seriesId"]),
    Index(value = ["filePath"]),
  ],
)
data class CustomSeriesItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val seriesId: Int,
  val filePath: String,
  val fileName: String,
  val season: Int,
  val episode: Int,
  val position: Int,
)
