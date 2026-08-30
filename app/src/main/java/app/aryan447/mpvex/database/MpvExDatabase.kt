package app.aryan447.mpvium.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.aryan447.mpvium.database.converters.NetworkProtocolConverter
import app.aryan447.mpvium.database.dao.NetworkConnectionDao
import app.aryan447.mpvium.database.dao.PlaybackStateDao
import app.aryan447.mpvium.database.dao.PlaylistDao
import app.aryan447.mpvium.database.dao.RecentlyPlayedDao
import app.aryan447.mpvium.database.dao.VideoMetadataDao
import app.aryan447.mpvium.database.entities.PlaybackStateEntity
import app.aryan447.mpvium.database.entities.PlaylistEntity
import app.aryan447.mpvium.database.entities.PlaylistItemEntity
import app.aryan447.mpvium.database.entities.RecentlyPlayedEntity
import app.aryan447.mpvium.database.entities.VideoMetadataEntity
import app.aryan447.mpvium.domain.network.NetworkConnection

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    VideoMetadataEntity::class,
    NetworkConnection::class,
    PlaylistEntity::class,
    PlaylistItemEntity::class,
  ],
  version = 8,
  exportSchema = true,
)
@TypeConverters(NetworkProtocolConverter::class)
abstract class MpvExDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun videoMetadataDao(): VideoMetadataDao

  abstract fun networkConnectionDao(): NetworkConnectionDao

  abstract fun playlistDao(): PlaylistDao
}
