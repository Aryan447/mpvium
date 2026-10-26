package app.aryan447.mpvium.preferences

/**
 * Home feed sections in default top-to-bottom order.
 * Keys are persisted in the home_section_order CSV preference.
 */
enum class HomeSection(
  val key: String,
  val title: String,
) {
  FEATURED("FEATURED", "Featured banner"),
  CONTINUE_WATCHING("CONTINUE_WATCHING", "Continue Watching"),
  UP_NEXT("UP_NEXT", "Up Next"),
  RECENTLY_ADDED("RECENTLY_ADDED", "Recently Added"),
  SHOWS("SHOWS", "Shows"),
  MOVIES("MOVIES", "Movies & Videos"),
  FOLDERS("FOLDERS", "Browse Folders"),
  ;

  companion object {
    val defaultOrder: List<HomeSection> = entries.toList()
    val defaultOrderCsv: String = defaultOrder.joinToString(",") { it.key }

    fun fromKey(key: String): HomeSection? = entries.firstOrNull { it.key == key }

    // Parse persisted CSV; unknown keys are dropped, missing sections are
    // appended in default order so new sections appear without a migration.
    fun parseOrder(raw: String): List<HomeSection> {
      val parsed = raw.split(",").mapNotNull { fromKey(it.trim().uppercase()) }.distinct()
      if (parsed.isEmpty()) return defaultOrder
      return parsed + (defaultOrder - parsed.toSet())
    }
  }
}
