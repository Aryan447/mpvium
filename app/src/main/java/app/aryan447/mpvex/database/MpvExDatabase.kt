package app.aryan447.mpvex.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.aryan447.mpvex.database.converters.NetworkProtocolConverter
import app.aryan447.mpvex.database.dao.NetworkConnectionDao
import app.aryan447.mpvex.database.dao.PlaybackStateDao
import app.aryan447.mpvex.database.dao.PlaylistDao
import app.aryan447.mpvex.database.dao.RecentlyPlayedDao
import app.aryan447.mpvex.database.dao.VideoMetadataDao
import app.aryan447.mpvex.database.entities.PlaybackStateEntity
import app.aryan447.mpvex.database.entities.PlaylistEntity
import app.aryan447.mpvex.database.entities.PlaylistItemEntity
import app.aryan447.mpvex.database.entities.RecentlyPlayedEntity
import app.aryan447.mpvex.database.entities.VideoMetadataEntity
import app.aryan447.mpvex.domain.network.NetworkConnection

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
