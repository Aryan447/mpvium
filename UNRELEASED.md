## Unreleased

<!-- Scratch pad. One short line per user-facing change while you work.
     At release time these are rewritten into the announcement style used in
     CHANGELOG.md — see AGENTS.md → "What's New". -->

### Added

- Android TV and Google TV support with Leanback launcher banner, 10-foot D-Pad remote navigation, clear visual focus rings, landscape enforcement, and dedicated TV playback controls.
- Comprehensive TV remote optimization for Modern player layout: auto-focused hero play/pause button, D-Pad Left/Right seekbar scrubbing, and interactive focus rings across transport controls, action pill buttons, and sheets.
- TV settings layout with centered content, D-Pad focus indicators on preference rows and choice sheets, and remote-navigable theme preview cards.
- Theme Studio card opens My Canvas with full primary, secondary, and tertiary color customization, continuous HSV and hex color pickers, and live player controls and homepage previews.
- Holding any custom theme in the theme picker allows editing its colors or deleting it.
- The new r/mpvium subreddit joins Discord and Telegram in About and on the website.
- The doctrine — the rules mpvium is built against — lives in DOCTRINE.md and is published as a Doctrine page on the website.

### Changed

- Modern is the default player layout; Classic is one tap away in Settings → Appearance → Player Layout.
- Player button backgrounds are now opt-in by default, giving controls clean floating icons that can be re-enabled in Settings → Player controls.

### Fixed

- Downloaded and sideloaded media on Android TV is no longer suppressed from the Titles library by mobile transient folder thresholds.
- Software decoder selection strictly disables hardware decoding across playback reloads, playlist items, and singleTask session switches.
- Main navigation rail on TV uses clean native Material 3 indicator pills without distorted selection outlines.
- Modern player layout custom action buttons scale consistently with built-in controls.
- Shader peek upscale chip no longer covers top-right player buttons when smart skip is absent.
