## Unreleased

### Added

- Subtitle position and scale can now be remembered per show or movie. Choose "Per show or movie" under Settings → Subtitles; episodes of one show share a setting, other shows and movies keep their own, and each profile is restored before the first subtitle appears.
- "Forget this title's subtitle settings" in the player's subtitle sheet, and "Clear all subtitle profiles" in Settings → Subtitles.
- Settings search now lands on the exact option: the page scrolls to the matching row, brings it fully into view, and tints it for about 1.6 seconds while screen readers announce it. Results that point at a whole section highlight that card instead of an inner row.
- Toggle to hide chapter markers in the seekbar (Settings → Appearance → Seekbar). Chapters, the chapters sheet and skip-chapter gestures are unaffected.

### Changed

- Online subtitle search results now show a parsed title plus compact quality tags (resolution, source, codec, audio, release group) instead of raw, wrapping release filenames.

### Fixed

- Downloading another subtitle in the same language no longer overwrites the previous file or gets skipped, so multiple external subtitles can be added from online search.
- External subtitle tracks in the player sheet now show a readable title instead of the raw downloaded file name.
- Appearance: the Dark/Light/System selector now spans the card width and lines up with the theme preview cards instead of hugging the left edge.
- Choice rows (Appearance theme selector, Home categories, Shows/Movies filters) now use the light segment-tick haptic instead of a long-press buzz, so a tap gives feedback on devices that suppress long-press haptics.
