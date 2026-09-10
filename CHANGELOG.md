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

- Smooth Volume Slider setting (Audio): swiping the volume slider animates
  the displayed value one step at a time; volume keys are unchanged.

### Fixed

- Launcher icon: smaller play-button foreground to match mpvEx proportions.

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
