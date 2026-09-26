# What's New

User-facing changelog for preview and release builds.
Source of truth for GitHub release notes.

## How to update

1. Add new user-facing changes under `Unreleased` while you work.
2. When tagging a preview (`v*-preview*`) or release (`v*`), rename
   `Unreleased` to the new version and add a fresh empty `Unreleased`.
3. Keep entries short and user-facing. Group by `Added`, `Changed`, `Fixed`.
4. Paste the version section into the GitHub release body (checksums are
   added automatically by the release workflows).

## Unreleased

### Added

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
- Four new seekbar styles (Slim, Neon glow, Segmented, Retro blocky) for the seekbar, volume slider, and brightness slider, with live previews in the picker.

### Changed

- Redesigned Settings as a premium expressive dashboard with gradient hero, tactile tiles, and embedded search.
- Extended the premium styling to every sub-settings screen: icon rows, section counts, grouped cards, and a unified search-result row.
- Default app theme is now Default instead of Dynamic on fresh installs.
- Default seekbar, volume, and brightness slider styles are now Standard on fresh installs.
- Refreshed app logo artwork and linked Telegram community.
- Bottom-edge swipes no longer trigger volume/brightness gestures.

### Fixed

- Launcher icon: smaller play-button foreground to match mpvium proportions.
- Continue Watching / playtime refreshes instantly on video exit.
- Seekbar thumb flutter, tap-seek delay, snap-back, and post-seek stall.
- Seek preview held until seek confirms instead of fixed-delay clear.
- Folder blacklist/whitelist now respected across Home, Movies, Shows, and Insights.

## v1.1.6-preview.5

### Added

- Player hold gestures for brightness/volume controls with speed-boost option.
- 1-step volume swipes and coarser hardware-key volume handling.

### Changed

- Refactored TV Shows label back to Shows.
- Hold-volume gesture now uses the gesture volume API.

### Fixed

- Player smart-cast crash on delegated `currentPlayer` update.

## v1.1.6-preview.4

### Added

- Home setting to hide the Featured banner.
- Instant Continue Watching updates with remove option.

### Changed

- Renamed Series to TV Shows throughout the UI.

### Fixed

- Keep series in Continue Watching while episodes remain.
- Fetch hi-res artwork on tablets to fix blurry backdrops.

## v1.1.6-preview.3

### Added

- Library Insights dashboard.
- Continue Watching home-screen widget.
- Explain button, Smart Skip, and Shader Peek in the player.
- First-run onboarding and replayable welcome tour.
- Customizable volume step and limits.

### Changed

- Tablet master-detail two-pane for Series and Movies.
- Pill navigation bar on by default with matching outline.
- Actionable network-error and history-disabled states.

### Fixed

- Swipe-seek normalization for tablets (gesture-width based).
- App shortcut / context-menu crash.
- Player overlay startup crash, subtitle touch zones, and chip anchoring.
- Smaller thumbnail/artwork caches with bounded disk eviction.
- Reactive browser state (replaces 60fps polling loop).
