## Unreleased

### Added

- Aura theme: a built-in content-driven theme positioned third in the theme picker (Default, Dynamic, Aura, Cinema, …) whose accents and ambient glow dynamically adapt to the poster of recently played content in Continue Watching, previewing titles while browsing series and movie detail screens, and gracefully falling back to a neutral built-in identity when contrast or chroma checks require it.
- Opt-in "Lean seek cache" setting (Settings → Advanced) that deepens the demuxer cache around the playhead for faster seeking on local and network sources alike. Automatically sizes forward and backward byte budgets according to memory class and bitrate, clamps to conservative 96 MB forward / 128 MB backward ceilings, and displays seekable cache ranges behind the seekbar rail when enabled.
- Library index cache: media parsing results are now stored in a Room snapshot and refreshed incrementally, avoiding full device scans on every tab change.
- Automatic vs manual library scanning mode in Settings → Folders.
- Promoted Playlists to a first-class bottom-navigation tab (enabled by default) with an empty-state create action and direct access from the main navigation bar.
- Movies: length filtering is a clock button with a dropdown next to the sort control, replacing the second chip row above the grid; its menu offers Any length, 30 min+, 1 h+, and 1.5 h+, and the button fills while a threshold is active.
- Grid or list layout choice per tab across Shows, Movies, and Folders, persisted independently across restarts (`layout_shows`, `layout_movies`, `layout_folders`).
- Tap-to-toggle layout icon in each tab's top bar next to sort controls, matching the tonal icon-button style.
- Dedicated list rows for Shows and Movies with poster thumbnails, title, year, and inline progress/watch state.
- Periodic playback checkpoint: saves progress while playing (default every 30 s, configurable under Settings → Player to 15 s, 30 s, 1 min, or 5 min) when playback moves by at least 5 s, surviving process kills, force stops, PiP, and background playback.

### Changed

- Switching between Home, Shows, Movies, and Insights tabs now shares a single background scan rather than triggering independent scans per tab.
- Appearance settings: bottom-navigation tab manager now includes Playlists (6 total tabs), with individual visibility toggles and at-least-one-tab protection.
- Online subtitle search results are now parsed into a clean title with quality, source, codec, audio, edition, and release-group pills, plus a language badge and provider/format line, instead of a wall of raw release names.
- External subtitle tracks added from online search now show a friendly label (language · quality · source · group) in the subtitle list.

### Fixed

- Player: fixed a launch crash (`NullPointerException` inside `Flow.combine`) when opening the player while the lean seek cache is enabled, caused by the visibility observer reading player state before it was initialized.
- Adding a second subtitle in the same language from online search now saves a separate file and appears as its own external track, instead of overwriting the first download and being silently ignored.
- Online subtitle rows show a per-result progress spinner and ignore repeat taps while that result is downloading.
- Playback state persistence: writes are now application-scoped so activity destruction or force-stop cannot drop the final write; saves on pause, stop, track changes, task removal, and memory trim; and guards against overwriting good positions with 0.
- Subtitle scope setting (Global or Per-title): remember subtitle position and scale per show or per movie instead of one global pair.
- Reset actions: forget subtitle settings for the current title from the subtitle sheet, and a global "Clear all subtitle profiles" option in Settings → Subtitles.
- Metadata overrides: manually edit title, release year, and cover art (via system photo picker) for movies and TV shows from their detail screens; overrides work offline and survive rescans, forced refreshes, and cache migrations.
- Option to toggle video chapter markers in the seekbar (Settings → Appearance → Seekbar).
- Settings search direct jump & highlight: tapping a search result from either the settings dashboard inline search or the standalone search screen now navigates directly to the exact option row or section card, smoothly scrolling it into view and momentarily highlighting it with an accent tint.
- Media filename parsing: anime episodes with years in titles (e.g. `Jujutsu Kaisen (2020) - 05.mkv`, `Kaguya-sama 2019 - 11.mkv`) are now properly recognized as series episodes rather than movies.
- Multi-episode media files (`S01E01E02`, `S01E01-E02`, `1x01-02`, `- 01-02`) now parse deterministically to the first episode without leaking trailing episode numbers into episode titles.
- Series identity: fansub release tags (`[SubsPlease]`, `[1080p]`, CRC hashes, `【】`, underscores) collapse reliably to the same series key as clean titles.
- Folder fallbacks: subfolder episode naming such as `Show/Season 2/01 - Title.mkv` and `Show/01x02.mkv` now correctly resolve show titles, seasons, and episode titles.
- Mixed episode numbering: folders containing explicit `SxxExx` files now map bare anime dash-numbered (`- NN`) episodes into the same season, preventing show splitting and duplicate episode entries.
- Title vs release year: movies with numbers in their title (`1917 (2019)`, `2012 (2009)`, `Blade Runner 2049 (2017)`) retain the number in the title and take the year from parentheses; bare years are never returned as titles.
- Appearance: the Dark/Light/System selector now spans the card width and lines up with the theme preview cards instead of hugging the left edge.
- Choice rows (Appearance theme selector, Home categories, Shows/Movies filters) now use the light segment-tick haptic instead of a long-press buzz, so a tap gives feedback on devices that suppress long-press haptics.
- Fixed horizontal swipe-to-seek keyframe snapping and eliminated post-seek landing delay.
- Fixed post-seek playback freeze on heavy HEVC files when using hardware decoding.

## V1.3.0

### Added

- Clean titles now apply to movies too, showing the movie name and release year in the player.
- Default decoder setting with Auto, SW, HW, and HW+ options.
- SubSource support in online subtitle search: add your SubSource API key in Settings → Subtitles to include SubSource results; downloaded subtitles are unzipped automatically.
- "Full player colors" toggle (Player Layout > Appearance, off by default) that puts the app theme accent on player icon glyphs while keeping button, pill, and chip backgrounds neutral glass; the seekbar, volume, and brightness sliders also use the accent. With it off, icons stay neutral and only the seekbar, volume, and brightness sliders use the theme accent.
- Vertical swipe sensitivity setting (Player → Gestures) to control how much volume and brightness change per swipe.
- Horizontal swipe seeking now maps finger travel to playback time, with a 1–10 seconds-per-centimetre slider under Player → Gestures (7 s/cm by default: a 5 cm swipe covers ~35 seconds and 10 cm ~1:10) and a full-swipe choice of 3 minutes (default) or the whole video.
- Manual "Mark as" watch status on Home Continue Watching / Up Next / Recently Added (long-press), series episodes (long-press) and whole shows, movie details, and Recently Played selection.
- Discord community, linked next to Telegram in About and across the website and README.

### Changed

- The clean-title setting now describes shows and movies, and the "Episode only" style is now "Episode/title only".
- Auto check for updates on startup is now enabled by default.
- Intro/recap skip lookups now use TheIntroDB API v4, ahead of the v2/v3 shutdown.
- Updated the bundled mpv-android library to v0.0.3 (mpv `v0.41.0-1042-g7e4cb538a`).
- Hardware decoding now defaults to HW instead of HW+.
- Whitelist-only mode now filters only the Home, Shows, and Movies library. Folders and Files always show full storage (blacklist still applies).
- A horizontal swipe can no longer land exactly on the end of the video, so overshooting a swipe can't close the player through "Close after the end of playback".
- Updated Compose Material 3 to 1.5.0-beta01.
- Settings dashboard now has a flexible, collapsing top app bar and tiles whose corners morph on press.
- Settings now use Material 3 Expressive controls: connected toggle buttons for choices, plus morphing loading indicators for font loading and update checks.
- Single-choice settings now open one shared expressive picker sheet with connected, animated selection rows instead of stock list dialogs.
- Settings search now transitions between the dashboard and results, cross-fades empty states, and animates filtered rows, result counts, and the clear action.
- Player zoom and playback-speed sheets now group their actions with Material 3 Expressive button groups that expand the pressed action and overflow when space is tight.
- Home category filters and Shows/Movies watch-state filters now use connected expressive toggle groups that morph the selected filter into a pill.
- The Appearance dark/light/system selector now uses the same connected expressive toggle group as the Home category filters.
- Screen and tab navigation now uses expressive fade-through transitions driven by the Material motion scheme instead of fixed tween slides.
- The ambient theme glow now uses a shorter, layered bloom over the resolved theme background; AMOLED setups fade into true black instead of grey.
- The Folders tab now uses tonal, press-morphing folder cards with rounded containers and pill-shaped new-video badges.
- Onboarding now has scroll-reactive pages, shape-changing icon tiles, spring-driven progress dots, a fading skip action, and an animated primary button with press feedback.
- About now groups Libraries and GitHub into an expressive action group, confirms device-info copies with a tactile icon swap, and cross-fades the update-check indicator.
- Movies now hides clips of five minutes or less, along with reels, trailers, social/download exports, and camera recordings; those files remain available in Folders.
- Removed the library-sources hint banner from Home, Shows, and Movies; whitelist settings remain in Settings → Folders.

### Fixed

- Movie and show posters now stay stored on device until that movie or show is deleted, so covers keep working offline instead of being evicted from a size-capped image cache or re-downloaded.
- Cached poster/backdrop files are removed together with their library entry when media is deleted or pruned.
- Metadata cache writes are now atomic, so a write interrupted while the app is closed can no longer corrupt the cache and force every cover to be fetched again.
- Movies and shows now render their cached cover immediately on open instead of briefly falling back to a video thumbnail before metadata enrichment finishes.
- Refreshing metadata while offline now keeps the last saved cover instead of clearing it.
- Swipe seeking no longer jumps by an unpredictable number of minutes: it uses a configurable seconds-per-centimetre distance mapping (7 s/cm by default, so a 5 cm swipe covers ~35 seconds and 10 cm ~1:10), and a full swipe covers 3 minutes or the whole video.
- Compose Material 3 1.5.0-beta01 compatibility for dropdown menus, the seekbar, and glass navigation item colors.
- Clean show titles no longer include trailing release-group tags such as `-edge2020`.
- Home sections can be reordered and toggled (featured banner, Continue Watching, Up Next, Recently Added, Shows, Movies, folders) in Appearance > Home.
- Player: pinch on subtitles to resize them, with two-finger double-tap to reset to 100%; the gesture can be toggled in Settings → Player → Gestures.
- Edit network connection no longer shows the saved password; leave blank to keep it, type a new one to change it. Password input is now masked in add/edit dialogs.
- Manual "Mark as New" now always restores the red NEW badge, even for older videos.
- WebDAV browsing on servers that mark video files as collections (e.g. Torbox): `.mkv` entries without a folder href now show as playable videos with size, listings filter self by href instead of dropping the first entry, and paths with spaces/unicode are percent-encoded for listing, playback, and seeking.
- Shows and Movies sort choices now persist across app restarts.
- The Shows and Movies sort button now has a visible tonal surface, so it no longer disappears on dark and AMOLED themes.
- VOBSUB/DVD and other bitmap subtitles now scale correctly: they keep their aspect ratio on anamorphic video instead of being stretched, and always follow the Scale and Position sliders.
- Vertical volume/brightness swipes no longer use a stale starting volume, so consecutive and fast swipes no longer feel stuck or need repeating.
- Watched threshold setting (e.g. 90%) now honored in Continue Watching, Up Next, widget, and playlist instead of staying stuck at 95%.
- Whitelisted library no longer hides folders such as DCIM in the Folders and Files browsers and their searches.
- Movie and show posters now use a stricter TMDb match: title similarity, original title, and release year are scored, movie/show types are never mixed, and uncertain matches no longer attach a wrong cover.
- Filenames with year-like title numbers (1917, 2012, Blade Runner 2049) keep the number in the title, and folders such as "Spider-Man" are no longer mistaken for season folders.
- Bare episode names (`S01E01.mkv`, `Episode 1.mkv`, `01.mkv`) and generic movie files (`movie.mkv`) now take their title from the show/movie folder, so their covers resolve correctly.
- Non-Latin movie and show titles no longer collapse into one library entry, metadata cache key, or shared poster.
- Existing automatic poster matches are refreshed once after upgrading; user-picked matches are kept.
