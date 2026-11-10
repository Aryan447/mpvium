---
name: mpvium playback & native
description: Change mpv/libmpv playback behaviour in mpvium — player options, decoders, shaders, gestures, tracks, PiP, background playback, or the bundled mpv AAR. Use for anything touching PlayerActivity, MPVView, PlayerViewModel, PlayerObserver, Anime4K, hwdec, ABI packaging, or "native" playback code.
---

# Playback & native

## There is no C/C++ in this repo

All player native code ships prebuilt in `app/libs/mpv-android-lib-v0.0.3.aar` (`is.xyz.mpv.*`
— `MPVLib`, `BaseMPVView`, `FastThumbnails`, plus `libmpv.so`, `libplayer.so`, ffmpeg libs and
`libc++_shared.so` for four ABIs). `AGENTS.md` mentions native C/C++; treat the AAR as opaque
and do not create `jni/`, `cpp/`, or `jniLibs/` directories.

The only native-adjacent assets are the shaders: `app/src/main/assets/shaders/*.glsl`, copied
to `filesDir/shaders` by `domain/anime4k/Anime4KManager.kt`.

## Files

| File | Role |
| --- | --- |
| `ui/player/PlayerActivity.kt` | host activity, intents, playlist, PiP, media session, cleanup |
| `ui/player/PlayerViewModel.kt` | reactive state, seeks, volume/brightness, filters, shader peek |
| `ui/player/MPVView.kt` | pre-init options, post-init commands, property observation |
| `ui/player/PlayerObserver.kt` | native event → UI thread hop |
| `ui/player/PlayerUtils.kt` | URI → path or `fd://` |
| `ui/player/TrackSelector.kt` | audio/subtitle auto-selection |
| `ui/player/MediaPlaybackService.kt` | foreground media session, background playback |
| `ui/player/PlayerEnums.kt` | decoder, gesture, filter, orientation enums |
| `ui/player/controls/` | gestures, controls, sheets, panels |

## Init order — do not reorder

`player.initialize(filesDir.path, cacheDir.path)` runs, inside `BaseMPVView`:
create → `config`/`config-dir`/shader caches → **`initOptions()`** → `MPVLib.init()` →
**`postInitOptions()`** → `force-window`/`idle` → surface callback → `observeProperties()`.

- `initOptions()` (`MPVView.kt:96-163`) is pre-init only: `setOptionString`. No `command()`.
- `postInitOptions()` (`MPVView.kt:169-182`) is the first place `command("vf", …)` and
  `script-binding` are legal.
- `glsl-shaders` must be set in `initOptions`; runtime changes use
  `setPropertyString("glsl-shaders", …)`.
- `Utils.copyAssets(this)` and the user mpv.conf sync happen before `initialize` in
  `setupMPV()`.

Option reference: `references/mpv-options.md`.

## Hard rules

1. **One global `MPVLib`.** No second player, ever. `MPVLib.destroy()` only inside
   `PlayerActivity.cleanupMPV()`.
2. **Observer callbacks arrive on the mpv event thread.** `PlayerObserver` posts to the UI
   thread. New handlers must guard `mpvInitialized && !player.isExiting` at the top; the
   boolean `onObserverEvent` overload currently does not.
3. **`loadfile` stays off the main thread** (`Dispatchers.Default`) — network/HLS opens ANR
   otherwise. Do not "simplify" those call sites.
4. **`playFile()` defers loading until `surfaceCreated`.** Use it for first playback; use
   `MPVLib.command("loadfile", …)` only once the surface exists (playlist, new intent).
5. `MPV_EVENT_PLAYBACK_RESTART` resets `player.isExiting = false`. Keep that, or a reload
   after cleanup silently drops every event.
6. **Seeks go through `PlayerViewModel`** (`seekTo`, `seekBy`, `seekToAndResume`) — they are
   sequence-guarded and IO-dispatched. Never issue a raw seek from UI code. Don't add
   main-thread `getProperty*` polling; the existing 42 ms poller is legacy, not a pattern.
7. **Content URIs**: `PlayerUtils.resolveUri` returns a real path or `fd://<fd>`. After
   `detachFd()` mpv owns the fd — never close it in Kotlin.
8. **State flows through `PlayerViewModel`.** UI must not read mpv properties directly.
   Sanctioned exceptions: `MoreSheet`, video filter cards, the Decode sheet.
9. Brightness is **window** brightness; volume is **system** volume up to 100% and mpv
   `volume` above. Keep that split.
10. Vulkan needs API 33+, Vulkan 1.3, GLES 3.1+ (checked in `DecoderPreferencesScreen`).
    Anime4K refuses to apply with GPU-Next + OpenGL, and its 4K/8K guard is UI-only.

## Decoders

| UI label | `hwdec` value | `defaultHwdec` |
| --- | --- | --- |
| Auto-copy | `auto-copy` | `auto-copy` (hidden from the UI) |
| Auto | `auto` | `auto` |
| SW | `no` | `no` |
| HW | `mediacodec-copy` | `mediacodec-copy,no` |
| HW+ | `mediacodec` | `mediacodec,mediacodec-copy,no` |

Default is `HW`; `hwdec-codecs=all` is set at init. Runtime changes use the raw value.

## ABI and packaging

- Splits: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` + universal; versionCode = base×10 +
  1/2/3/4 (universal = base×10).
- `packaging.jniLibs.useLegacyPackaging = true` (libs are extracted at install).
- Release, preview, and pre-release workflows hardcode all five APK names. Keep them in sync
  with `app/build.gradle.kts`.
- Adding or updating a `.so`: verify 16 KB page alignment for every ABI —
  `readelf -lW libfoo.so | grep LOAD` must show `Align 0x4000`. Never ship a second
  `libmpv.so` or `libc++_shared.so`.

## Updating the mpv AAR

Replace `app/libs/mpv-android-lib-vX.Y.Z.aar`, update the `implementation(files(...))` path in
`app/build.gradle.kts`, add a `CHANGELOG.md` note naming the new mpv revision, and ignore
`UPDATE_WORKFLOW.md`'s v0.0.2 commands — it is stale.

## Verify

- [ ] Play, seek, pause, resume, background playback, PiP, rotation on an arm64 device
- [ ] Decoder Auto / SW / HW / HW+ on one 4K and one 10-bit file
- [ ] Audio and subtitle track switching, external subtitle load, "subtitles off"
- [ ] Gestures: vertical volume/brightness, horizontal seek, double-tap seek, pinch subtitle
      size
- [ ] `adb shell dumpsys gfxinfo app.aryan447.mpvium` when touching controls or the seekbar
- [ ] `CHANGELOG.md` entry
