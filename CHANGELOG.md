## Unreleased

### Added

- Clean titles now apply to movies too, showing the movie name and release year in the player.
- Default decoder setting with Auto, SW, HW, and HW+ options.
- SubSource support in online subtitle search: add your SubSource API key in Settings → Subtitles to include SubSource results; downloaded subtitles are unzipped automatically.
- "Full player colors" toggle (Player Layout > Appearance, on by default) that puts the app theme accent on player icon glyphs while keeping button, pill, and chip backgrounds neutral glass; the seekbar, volume, and brightness sliders also use the accent. Turn it off for neutral icons, where only the seekbar, volume, and brightness sliders use the theme accent.
- Vertical swipe sensitivity setting (Player → Gestures) to control how much volume and brightness change per swipe.
- Speed-adaptive horizontal swipe seeking, on by default with a toggle under Player → Gestures: short drags stay precise while longer or faster swipes cover minutes instead of seconds in one sweep.
- Library-sources hint on Home, Shows, and Movies when the whitelist filter is active, with one-tap access to Settings → Folders.
- Manual "Mark as" watch status on Home Continue Watching / Up Next / Recently Added (long-press), series episodes (long-press) and whole shows, movie details, and Recently Played selection.

### Changed

- The clean-title setting now describes shows and movies, and the "Episode only" style is now "Episode/title only".
- Auto check for updates on startup is now enabled by default.
- Intro/recap skip lookups now use TheIntroDB API v4, ahead of the v2/v3 shutdown.
- Updated the bundled mpv-android library to v0.0.3 (mpv `v0.41.0-1042-g7e4cb538a`).
- Hardware decoding now defaults to HW instead of HW+.
- Whitelist-only mode now filters only the Home, Shows, and Movies library. Folders and Files always show full storage (blacklist still applies).
- A horizontal swipe can no longer land exactly on the end of the video, so overshooting a swipe can't close the player through "Close after the end of playback".
- Updated Compose Material 3 to 1.5.0-beta01.

### Fixed

- Compose Material 3 1.5.0-beta01 compatibility for dropdown menus, the seekbar, and glass navigation item colors.
- Clean show titles no longer include trailing release-group tags such as `-edge2020`.
- Home sections can be reordered and toggled (featured banner, Continue Watching, Up Next, Recently Added, Shows, Movies, folders) in Appearance > Home.
- Player: pinch on subtitles to resize them, with two-finger double-tap to reset to 100%; the gesture can be toggled in Settings → Player → Gestures.
- Edit network connection no longer shows the saved password; leave blank to keep it, type a new one to change it. Password input is now masked in add/edit dialogs.
- Manual "Mark as New" now always restores the red NEW badge, even for older videos.
- WebDAV browsing on servers that mark video files as collections (e.g. Torbox): `.mkv` entries without a folder href now show as playable videos with size, listings filter self by href instead of dropping the first entry, and paths with spaces/unicode are percent-encoded for listing, playback, and seeking.
- Shows and Movies sort choices now persist across app restarts.
- VOBSUB/DVD and other bitmap subtitles now scale correctly: they keep their aspect ratio on anamorphic video instead of being stretched, and always follow the Scale and Position sliders.
- Vertical volume/brightness swipes no longer use a stale starting volume, so consecutive and fast swipes no longer feel stuck or need repeating.
- Watched threshold setting (e.g. 90%) now honored in Continue Watching, Up Next, widget, and playlist instead of staying stuck at 95%.
- Whitelisted library no longer hides folders such as DCIM in the Folders and Files browsers and their searches.
- Movie and show posters now use a stricter TMDb match: title similarity, original title, and release year are scored, movie/show types are never mixed, and uncertain matches no longer attach a wrong cover.
- Filenames with year-like title numbers (1917, 2012, Blade Runner 2049) keep the number in the title, and folders such as "Spider-Man" are no longer mistaken for season folders.
- Bare episode names (`S01E01.mkv`, `Episode 1.mkv`, `01.mkv`) and generic movie files (`movie.mkv`) now take their title from the show/movie folder, so their covers resolve correctly.
- Non-Latin movie and show titles no longer collapse into one library entry, metadata cache key, or shared poster.
- Existing automatic poster matches are refreshed once after upgrading; user-picked matches are kept.
