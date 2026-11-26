## Unreleased

<!-- Scratch pad. One short line per user-facing change while you work.
     At release time these are rewritten into the announcement style used in
     CHANGELOG.md — see AGENTS.md → "What's New". -->

### Added

- Built-in custom button presets and installable example scripts in Settings → Player controls → Custom buttons and Manage scripts.
- Create custom series action in the unified Titles tab top bar and empty states.
- Detail screen poster toggle in Settings → Appearance and directly from the screen menu, allowing users to hide artwork and display the title cleanly beneath the navigation bar.
- Videos in WebDAV, SMB, and FTP folders are automatically queued into the playlist when playlist mode is enabled, advancing to the next file when playback ends or fails.
- Custom Lua and mpv script buttons in player overlay with single tap and long-press actions, plus XML import and export.
- Speed Binge grid dial on Home puts your last played show or movie first with instant resume, brings progress bars to every title, and replaces featured banners with a clean media grid.
- mpv.conf ownership in Settings → Advanced lets you choose which mpv option groups your mpv.conf controls instead of the app.
- Theme Studio card opens My Canvas with full primary, secondary, and tertiary color customization, continuous HSV and hex color pickers, and live player controls and homepage previews.
- Holding any custom theme in the theme picker allows editing its colors or deleting it.
- The new r/mpvium subreddit joins Discord and Telegram in About and on the website.
- The doctrine — the rules mpvium is built against — lives in DOCTRINE.md and is published as a Doctrine page on the website.

### Changed

- Modern player layout places custom buttons directly between the title and bottom-right controls or between Close/PiP and volume controls without extra rows.
- Custom script buttons render as individual floating glass pill chips matching the Modern Player aesthetic.
- Movie and TV series detail screens redesigned with Material 3 styling, compact backdrop sizing optimized for tablets, consolidating top actions into an overflow menu and removing the dark poster gradient overlay for pure artwork clarity.
- Modern is the default player layout; Classic is one tap away in Settings → Appearance → Player Layout.
- Player button backgrounds are now opt-in by default, giving controls clean floating icons that can be re-enabled in Settings → Player controls.

### Fixed

- Switching directly between SW and HW (mediacodec-copy) decoders during 10-bit HEVC playback now reloads the video pipeline cleanly instead of getting stuck.

- Custom script buttons resolve case-insensitive script bindings and import text-formatted script files directly into Manage scripts.
- Unified Titles tab keeps filter chips accessible, provides Show all and Back navigation, and allows creating custom series when filtering with zero matching TV shows or movies.
- Series and movie poster cards and detail backdrops now display in high-definition on tablets and phones without blurriness or downsampling.
- Bottom navigation bar now respects 3-button navigation insets without overlapping system navigation buttons.

- HLS playlists (.m3u8) now appear and play from WebDAV, SMB, and FTP network connections.
- Standalone movies now retain their own subtitle position and scale under per-title scope instead of sharing and overwriting a single profile.
- Deleting or renaming a movie automatically removes or migrates its saved subtitle profile entry.
- Modern player layout custom action buttons scale consistently with built-in controls.
- Shader peek upscale chip no longer covers top-right player buttons when smart skip is absent.
- Anime releases without dash episode separators are properly detected as TV series episodes instead of standalone movies.
- Movie sequels with quality metadata and without release years are no longer misclassified as TV series episodes.
- Network connections entered with URLs or embedded paths (such as `offcloud.com/webdav` in the Host field) are automatically sanitized and split into clean host and directory path.
- Folders preferences whitelist and blacklist dialogs now list parent directories containing videos.
- Media control buttons and keys now follow their configured action in Preferences → Gestures.
