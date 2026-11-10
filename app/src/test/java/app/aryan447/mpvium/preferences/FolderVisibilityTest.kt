package app.aryan447.mpvium.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderVisibilityTest {

  @Test
  fun isFolderVisible_blacklistMode_unlistedFolderIsVisible() {
    val visible = FolderVisibility.isFolderVisible(
      folderPath = "/storage/emulated/0/Movies",
      whitelist = emptySet(),
      blacklist = setOf("/storage/emulated/0/DCIM/.thumbnails"),
      whitelistOnly = false,
    )
    assertTrue(visible)
  }

  @Test
  fun isFolderVisible_blacklistMode_exactMatchIsHidden() {
    val visible = FolderVisibility.isFolderVisible(
      folderPath = "/storage/emulated/0/Private",
      whitelist = emptySet(),
      blacklist = setOf("/storage/emulated/0/Private"),
      whitelistOnly = false,
    )
    assertFalse(visible)
  }

  @Test
  fun isFolderVisible_blacklistMode_descendantIsHidden() {
    val visible = FolderVisibility.isFolderVisible(
      folderPath = "/storage/emulated/0/Private/SubFolder/Videos",
      whitelist = emptySet(),
      blacklist = setOf("/storage/emulated/0/Private"),
      whitelistOnly = false,
    )
    assertFalse(visible)
  }

  @Test
  fun isFolderVisible_blacklistMode_siblingWithSimilarPrefixIsNotHidden() {
    val visible = FolderVisibility.isFolderVisible(
      folderPath = "/storage/emulated/0/PrivateVideos",
      whitelist = emptySet(),
      blacklist = setOf("/storage/emulated/0/Private"),
      whitelistOnly = false,
    )
    assertTrue(visible)
  }

  @Test
  fun isFolderVisible_blacklistMode_handlesTrailingSlashes() {
    assertFalse(
      FolderVisibility.isFolderVisible(
        folderPath = "/storage/emulated/0/Downloads/",
        whitelist = emptySet(),
        blacklist = setOf("/storage/emulated/0/Downloads"),
        whitelistOnly = false,
      ),
    )
    assertFalse(
      FolderVisibility.isFolderVisible(
        folderPath = "/storage/emulated/0/Downloads",
        whitelist = emptySet(),
        blacklist = setOf("/storage/emulated/0/Downloads/"),
        whitelistOnly = false,
      ),
    )
  }

  @Test
  fun isFolderVisible_whitelistOnlyWithEmptyWhitelist_failsOpenToBlacklistMode() {
    // Empty whitelist fail-open safeguard: users are not locked out of the library
    assertTrue(
      FolderVisibility.isFolderVisible(
        folderPath = "/storage/emulated/0/Movies",
        whitelist = emptySet(),
        blacklist = setOf("/storage/emulated/0/Hidden"),
        whitelistOnly = true,
      ),
    )
    assertFalse(
      FolderVisibility.isFolderVisible(
        folderPath = "/storage/emulated/0/Hidden",
        whitelist = emptySet(),
        blacklist = setOf("/storage/emulated/0/Hidden"),
        whitelistOnly = true,
      ),
    )
  }

  @Test
  fun isFolderVisible_whitelistOnlyWithNonEmptyWhitelist_onlyWhitelistedAndDescendantsVisible() {
    val whitelist = setOf("/storage/emulated/0/Movies", "/storage/emulated/0/TV")
    val blacklist = setOf("/storage/emulated/0/Movies") // Blacklist is ignored in whitelist-only mode

    assertTrue(
      "Whitelisted root folder should be visible",
      FolderVisibility.isFolderVisible("/storage/emulated/0/Movies", whitelist, blacklist, whitelistOnly = true),
    )
    assertTrue(
      "Descendant of whitelisted folder should be visible",
      FolderVisibility.isFolderVisible("/storage/emulated/0/Movies/Action", whitelist, blacklist, whitelistOnly = true),
    )
    assertFalse(
      "Unrelated sibling folder should not be visible",
      FolderVisibility.isFolderVisible("/storage/emulated/0/Downloads", whitelist, blacklist, whitelistOnly = true),
    )
    assertFalse(
      "Prefix matching without path boundary should not be visible",
      FolderVisibility.isFolderVisible("/storage/emulated/0/MoviesExtra", whitelist, blacklist, whitelistOnly = true),
    )
  }

  @Test
  fun isBrowserVisible_ignoresWhitelistAndAppliesBlacklist() {
    val blacklist = setOf("/storage/emulated/0/DCIM/.thumbnails")

    assertTrue(FolderVisibility.isBrowserVisible("/storage/emulated/0/DCIM/Camera", blacklist))
    assertFalse(FolderVisibility.isBrowserVisible("/storage/emulated/0/DCIM/.thumbnails", blacklist))
    assertFalse(FolderVisibility.isBrowserVisible("/storage/emulated/0/DCIM/.thumbnails/cache", blacklist))
  }

  @Test
  fun canContainVisible_blacklistMode_alwaysReturnsTrue() {
    val blacklist = setOf("/storage/emulated/0/Private")
    assertTrue(FolderVisibility.canContainVisible("/storage/emulated/0", emptySet(), blacklist, whitelistOnly = false))
    assertTrue(FolderVisibility.canContainVisible("/storage/emulated/0/Private", emptySet(), blacklist, whitelistOnly = false))
    assertTrue(FolderVisibility.canContainVisible("/storage/emulated/0/Movies", emptySet(), blacklist, whitelistOnly = false))
  }

  @Test
  fun canContainVisible_whitelistOnlyEmptyWhitelist_returnsTrue() {
    assertTrue(
      FolderVisibility.canContainVisible(
        path = "/storage/emulated/0",
        whitelist = emptySet(),
        blacklist = emptySet(),
        whitelistOnly = true,
      ),
    )
  }

  @Test
  fun canContainVisible_whitelistOnlyNonEmpty_returnsTrueForAncestorsAndDescendants() {
    val whitelist = setOf("/storage/emulated/0/Movies")

    assertTrue(
      "Whitelisted path itself can contain visible",
      FolderVisibility.canContainVisible("/storage/emulated/0/Movies", whitelist, emptySet(), whitelistOnly = true),
    )
    assertTrue(
      "Parent directory can contain visible",
      FolderVisibility.canContainVisible("/storage/emulated/0", whitelist, emptySet(), whitelistOnly = true),
    )
    assertTrue(
      "Grandparent directory can contain visible",
      FolderVisibility.canContainVisible("/storage", whitelist, emptySet(), whitelistOnly = true),
    )
    assertTrue(
      "Subfolder of whitelisted path can contain visible",
      FolderVisibility.canContainVisible("/storage/emulated/0/Movies/Action", whitelist, emptySet(), whitelistOnly = true),
    )
    assertFalse(
      "Unrelated sibling branch cannot contain visible",
      FolderVisibility.canContainVisible("/storage/emulated/0/Downloads", whitelist, emptySet(), whitelistOnly = true),
    )
    assertFalse(
      "Unrelated prefix-similar folder cannot contain visible",
      FolderVisibility.canContainVisible("/storage/emulated/0/MoviesExtra", whitelist, emptySet(), whitelistOnly = true),
    )
  }
}
