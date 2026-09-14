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

### Changed

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
