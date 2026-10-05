## v1.2.0

### Added

- Ambient theme glow rising from the bottom of the app, tinted to match the active theme.
- Customizable bottom bar with labels toggle and selectable tabs.
- Whitelisted-folders-only mode for folder list, file browser, and search.
- Lua/JS Scripts section with per-script manage, custom buttons, and sync.
- Manual "Mark as" watch status (Last played / New / Finished / None) in folder and file browser.
- Clean episode titles in the player with selectable style (single-line, two-line, episode-only).
- Offline intro-skip cache with Wi-Fi prefetch for uncached titles.
- TMDB posters/ratings cached until media deletion (no re-scrape on app open).
- Episode header falls back to cached TMDB title as S2:E12 "Name" when filename has no episode name.
- Home Up Next rail (next unwatched episode per show, in-progress first) and Recently Added rail across shows and movies.
- Shows and Movies grids gain watch-state filter chips, sort menus, result counts, and filter-aware empty states.
- Player bottom-right controls gain a Hold Gesture toggle switching between speed boost and brightness/volume (also available in the layout editor).

### Changed

- Redesigned Settings as a premium expressive dashboard with tactile tiles and embedded search.
- Extended the premium styling to every sub-settings screen: icon rows, section counts, grouped cards, and a unified search-result row.
- Bottom dock rebuilt on Material 3 Expressive's short navigation bar with expressive defaults.
- Refreshed app logo artwork and linked Telegram community.
- Bottom-edge swipes no longer trigger volume/brightness gestures.
- Wavy is now the default style for the seekbar, volume, and brightness sliders.
- Default (not Dynamic) is now the default app theme.

### Fixed

- Launcher icon: smaller play-button foreground to match mpvium proportions.
- Continue Watching / playtime refreshes instantly on video exit.
- Seekbar thumb flutter, tap-seek delay, snap-back, and post-seek stall.
- Seek preview held until seek confirms instead of fixed-delay clear.
- Folder blacklist/whitelist now respected across Home, Movies, Shows, and Insights.
- Player Layout slider previews now match the real seekbar (thin/Thick shapes, true Wavy squiggle) with vertical previews for volume/brightness.
