package app.aryan447.mpvium.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.aryan447.mpvium.database.entities.CustomSeriesEntity
import app.aryan447.mpvium.database.entities.CustomSeriesItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSeriesDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSeries(series: CustomSeriesEntity): Long

  @Update
  suspend fun updateSeries(series: CustomSeriesEntity)

  @Delete
  suspend fun deleteSeries(series: CustomSeriesEntity)

  @Query("DELETE FROM custom_series WHERE id = :seriesId")
  suspend fun deleteSeriesById(seriesId: Int)

  @Query("SELECT * FROM custom_series WHERE id = :seriesId")
  suspend fun getSeriesById(seriesId: Int): CustomSeriesEntity?

  @Query("SELECT * FROM custom_series WHERE id = :seriesId")
  fun observeSeriesById(seriesId: Int): Flow<CustomSeriesEntity?>

  @Query("SELECT * FROM custom_series ORDER BY updatedAt DESC")
  fun observeAllSeries(): Flow<List<CustomSeriesEntity>>

  @Query("SELECT * FROM custom_series ORDER BY updatedAt DESC")
  suspend fun getAllSeries(): List<CustomSeriesEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItem(item: CustomSeriesItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(items: List<CustomSeriesItemEntity>)

  @Update
  suspend fun updateItem(item: CustomSeriesItemEntity)

  @Delete
  suspend fun deleteItem(item: CustomSeriesItemEntity)

  @Query("DELETE FROM custom_series_item WHERE seriesId = :seriesId")
  suspend fun deleteItemsBySeriesId(seriesId: Int)

  @Query("DELETE FROM custom_series_item WHERE id = :itemId")
  suspend fun deleteItemById(itemId: Int)

  @Query("SELECT * FROM custom_series_item WHERE seriesId = :seriesId ORDER BY position ASC")
  suspend fun getItemsForSeries(seriesId: Int): List<CustomSeriesItemEntity>

  @Query("SELECT * FROM custom_series_item WHERE seriesId = :seriesId ORDER BY position ASC")
  fun observeItemsForSeries(seriesId: Int): Flow<List<CustomSeriesItemEntity>>

  @Query("SELECT * FROM custom_series_item ORDER BY seriesId ASC, position ASC")
  suspend fun getAllItems(): List<CustomSeriesItemEntity>

  @Query("SELECT * FROM custom_series_item ORDER BY seriesId ASC, position ASC")
  fun observeAllItems(): Flow<List<CustomSeriesItemEntity>>

  @Query("SELECT MAX(position) FROM custom_series_item WHERE seriesId = :seriesId")
  suspend fun getMaxPosition(seriesId: Int): Int?

  @Query("UPDATE custom_series_item SET position = :newPosition WHERE id = :itemId")
  suspend fun updateItemPosition(itemId: Int, newPosition: Int)

  @Transaction
  suspend fun replaceSeriesItems(seriesId: Int, items: List<CustomSeriesItemEntity>) {
    deleteItemsBySeriesId(seriesId)
    insertItems(items)
  }
}
