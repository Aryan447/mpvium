package app.aryan447.mpvium.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.aryan447.mpvium.database.entities.LibraryIndexEntity

@Dao
interface LibraryIndexDao {
  @Query("SELECT * FROM library_index")
  suspend fun getAll(): List<LibraryIndexEntity>

  @Query("SELECT * FROM library_index WHERE path = :path LIMIT 1")
  suspend fun getByPath(path: String): LibraryIndexEntity?

  @Query("SELECT * FROM library_index WHERE path IN (:paths)")
  suspend fun getBatch(paths: List<String>): List<LibraryIndexEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entity: LibraryIndexEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateBatch(entities: List<LibraryIndexEntity>)

  @Query("DELETE FROM library_index WHERE path = :path")
  suspend fun deleteByPath(path: String)

  @Query("DELETE FROM library_index WHERE path IN (:paths)")
  suspend fun deleteBatch(paths: List<String>)

  @Query("DELETE FROM library_index")
  suspend fun clearAll()

  @Query("SELECT COUNT(*) FROM library_index")
  suspend fun getCount(): Int
}
