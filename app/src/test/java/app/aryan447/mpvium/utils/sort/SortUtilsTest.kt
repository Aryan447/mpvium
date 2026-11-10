package app.aryan447.mpvium.utils.sort

import app.aryan447.mpvium.domain.browser.FileSystemItem
import app.aryan447.mpvium.domain.media.model.Video
import app.aryan447.mpvium.domain.media.model.VideoFolder
import app.aryan447.mpvium.preferences.FolderSortType
import app.aryan447.mpvium.preferences.SortOrder
import app.aryan447.mpvium.preferences.VideoSortType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SortUtilsTest {

  private val comparator = SortUtils.NaturalOrderComparator.DEFAULT

  private fun createVideo(
    id: Long,
    displayName: String,
    durationMs: Long = 1000L,
    dateModified: Long = 1000L,
    size: Long = 1000L,
  ): Video = Video(
    id = id,
    title = displayName.substringBeforeLast('.'),
    displayName = displayName,
    path = "/storage/emulated/0/Movies/$displayName",
    duration = durationMs,
    size = size,
    dateModified = dateModified,
  )

  private fun createFolder(
    name: String,
    lastModified: Long = 1000L,
    totalSize: Long = 1000L,
    videoCount: Int = 1,
  ): VideoFolder = VideoFolder(
    bucketId = name.hashCode().toString(),
    name = name,
    path = "/storage/emulated/0/$name",
    videoCount = videoCount,
    totalSize = totalSize,
    lastModified = lastModified,
  )

  // -------------------------------------------------------------
  // NaturalOrderComparator tests
  // -------------------------------------------------------------

  @Test
  fun naturalOrder_ordersNumbersNaturally() {
    assertTrue(comparator.compare("ep1", "ep2") < 0)
    assertTrue(comparator.compare("ep2", "ep10") < 0)
    assertTrue(comparator.compare("ep10", "ep100") < 0)
    assertTrue(comparator.compare("10", "2") > 0)
  }

  @Test
  fun naturalOrder_caseInsensitiveByDefault() {
    assertEquals(0, comparator.compare("episode 1", "EPISODE 1"))
    assertEquals(0, comparator.compare("Movie", "movie"))
    assertTrue(comparator.compare("apple", "Banana") < 0)
  }

  @Test
  fun naturalOrder_handlesMultipleNumberGroups() {
    assertTrue(comparator.compare("S01E02", "S01E10") < 0)
    assertTrue(comparator.compare("S01E10", "S02E01") < 0)
    assertTrue(comparator.compare("Part 1 Ep 2", "Part 1 Ep 10") < 0)
    assertTrue(comparator.compare("Part 2 Ep 1", "Part 10 Ep 1") < 0)
  }

  @Test
  fun naturalOrder_handlesPrefixesAndLengths() {
    assertTrue(comparator.compare("item", "item 1") < 0)
    assertTrue(comparator.compare("item 1", "item") > 0)
    assertEquals(0, comparator.compare("item", "item"))
  }

  @Test
  fun naturalOrder_skipsWhitespaceByDefault() {
    assertEquals(0, comparator.compare("Movie  1", "Movie 1"))
    assertEquals(0, comparator.compare("A B C", "ABC"))
  }

  // -------------------------------------------------------------
  // SortUtils.sortVideos tests
  // -------------------------------------------------------------

  @Test
  fun sortVideos_byTitle_usesNaturalOrder() {
    val videos = listOf(
      createVideo(1, "Episode 10.mp4"),
      createVideo(2, "Episode 2.mp4"),
      createVideo(3, "Episode 1.mp4"),
    )

    val asc = SortUtils.sortVideos(videos, VideoSortType.Title, SortOrder.Ascending)
    assertEquals(listOf("Episode 1.mp4", "Episode 2.mp4", "Episode 10.mp4"), asc.map { it.displayName })

    val desc = SortUtils.sortVideos(videos, VideoSortType.Title, SortOrder.Descending)
    assertEquals(listOf("Episode 10.mp4", "Episode 2.mp4", "Episode 1.mp4"), desc.map { it.displayName })
  }

  @Test
  fun sortVideos_byDuration() {
    val videos = listOf(
      createVideo(1, "Medium.mp4", durationMs = 5000L),
      createVideo(2, "Long.mp4", durationMs = 10000L),
      createVideo(3, "Short.mp4", durationMs = 1000L),
    )

    val asc = SortUtils.sortVideos(videos, VideoSortType.Duration, SortOrder.Ascending)
    assertEquals(listOf("Short.mp4", "Medium.mp4", "Long.mp4"), asc.map { it.displayName })

    val desc = SortUtils.sortVideos(videos, VideoSortType.Duration, SortOrder.Descending)
    assertEquals(listOf("Long.mp4", "Medium.mp4", "Short.mp4"), desc.map { it.displayName })
  }

  @Test
  fun sortVideos_byDate() {
    val videos = listOf(
      createVideo(1, "Old.mp4", dateModified = 100L),
      createVideo(2, "Newest.mp4", dateModified = 500L),
      createVideo(3, "Middle.mp4", dateModified = 250L),
    )

    val asc = SortUtils.sortVideos(videos, VideoSortType.Date, SortOrder.Ascending)
    assertEquals(listOf("Old.mp4", "Middle.mp4", "Newest.mp4"), asc.map { it.displayName })

    val desc = SortUtils.sortVideos(videos, VideoSortType.Date, SortOrder.Descending)
    assertEquals(listOf("Newest.mp4", "Middle.mp4", "Old.mp4"), desc.map { it.displayName })
  }

  @Test
  fun sortVideos_bySize() {
    val videos = listOf(
      createVideo(1, "Huge.mp4", size = 50_000L),
      createVideo(2, "Tiny.mp4", size = 100L),
      createVideo(3, "Normal.mp4", size = 1_000L),
    )

    val asc = SortUtils.sortVideos(videos, VideoSortType.Size, SortOrder.Ascending)
    assertEquals(listOf("Tiny.mp4", "Normal.mp4", "Huge.mp4"), asc.map { it.displayName })

    val desc = SortUtils.sortVideos(videos, VideoSortType.Size, SortOrder.Descending)
    assertEquals(listOf("Huge.mp4", "Normal.mp4", "Tiny.mp4"), desc.map { it.displayName })
  }

  // -------------------------------------------------------------
  // SortUtils.sortFolders tests
  // -------------------------------------------------------------

  @Test
  fun sortFolders_byTitle_usesNaturalOrder() {
    val folders = listOf(
      createFolder("Season 10"),
      createFolder("Season 2"),
      createFolder("Season 1"),
    )

    val asc = SortUtils.sortFolders(folders, FolderSortType.Title, SortOrder.Ascending)
    assertEquals(listOf("Season 1", "Season 2", "Season 10"), asc.map { it.name })

    val desc = SortUtils.sortFolders(folders, FolderSortType.Title, SortOrder.Descending)
    assertEquals(listOf("Season 10", "Season 2", "Season 1"), desc.map { it.name })
  }

  @Test
  fun sortFolders_byVideoCount() {
    val folders = listOf(
      createFolder("Many", videoCount = 50),
      createFolder("Few", videoCount = 2),
      createFolder("Some", videoCount = 15),
    )

    val asc = SortUtils.sortFolders(folders, FolderSortType.VideoCount, SortOrder.Ascending)
    assertEquals(listOf("Few", "Some", "Many"), asc.map { it.name })

    val desc = SortUtils.sortFolders(folders, FolderSortType.VideoCount, SortOrder.Descending)
    assertEquals(listOf("Many", "Some", "Few"), desc.map { it.name })
  }

  // -------------------------------------------------------------
  // SortUtils.sortFileSystemItems tests
  // -------------------------------------------------------------

  @Test
  fun sortFileSystemItems_foldersAlwaysPrecedeVideos_inBothOrders() {
    val items = listOf(
      FileSystemItem.VideoFile(name = "A_Video.mp4", path = "/storage/A_Video.mp4", lastModified = 100L, video = createVideo(1, "A_Video.mp4")),
      FileSystemItem.Folder(name = "Z_Folder", path = "/storage/Z_Folder", lastModified = 200L),
      FileSystemItem.VideoFile(name = "Z_Video.mp4", path = "/storage/Z_Video.mp4", lastModified = 300L, video = createVideo(2, "Z_Video.mp4")),
      FileSystemItem.Folder(name = "A_Folder", path = "/storage/A_Folder", lastModified = 400L),
    )

    val asc = SortUtils.sortFileSystemItems(items, FolderSortType.Title, SortOrder.Ascending)
    // Expect folders first (A_Folder, Z_Folder), then videos (A_Video.mp4, Z_Video.mp4)
    assertEquals(
      listOf("A_Folder", "Z_Folder", "A_Video.mp4", "Z_Video.mp4"),
      asc.map { it.name },
    )

    val desc = SortUtils.sortFileSystemItems(items, FolderSortType.Title, SortOrder.Descending)
    // In descending order: folders still first (Z_Folder, A_Folder), then videos (Z_Video.mp4, A_Video.mp4)
    assertEquals(
      listOf("Z_Folder", "A_Folder", "Z_Video.mp4", "A_Video.mp4"),
      desc.map { it.name },
    )
  }
}
