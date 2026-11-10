# mpv options set by mpvium

Source of truth: `app/src/main/java/app/aryan447/mpvium/ui/player/MPVView.kt`.

## `initOptions()` — before `MPVLib.init()`

| Area | Option | Value |
| --- | --- | --- |
| profile | `profile` | user preference, default `fast` |
| video output | `vo` | `gpu-next` when enabled, else `gpu` |
| Vulkan | `gpu-context` | `androidvk` when Vulkan is selected |
| decode | `hwdec`, `hwdec-codecs` | decoder preference, `all` |
| pixel format | `vf` | `format=yuv420p` when the preference is on |
| demuxer cache | `demuxer-max-bytes` | 64 MB (API ≥ 27) else 32 MB |
| demuxer cache | `demuxer-max-back-bytes` | 128 MB (API ≥ 27) else 32 MB |
| streaming | `demuxer-readahead-secs`, `cache-secs`, `stream-buffer-size` | 10, 20, 1 MiB |
| logging | `msg-level` | `all=warn`, `all=v` in debug |
| behaviour | `keep-open`, `input-default-bindings` | `true` |
| scripts | `load-scripts` | user preference |
| TLS | `tls-verify`, `tls-ca-file` | `yes`, `$filesDir/cacert.pem` |
| screenshots | `screenshot-directory` | public Pictures dir |
| video filters | brightness/saturation/contrast/gamma/hue/sharpen | from preferences |
| playback | `speed` | configured default speed |
| film grain | `vd-lavc-film-grain` | `cpu` |
| seeking | `hr-seek`, `hr-seek-framedrop` | preference, `yes` |
| Anime4K | `glsl-shaders`, `opengl-pbo`, `opengl-early-flush`, `vd-lavc-dr` | shader chain, GL-only extras, `yes` |
| subtitles | see below | |
| audio | see below | |

## `postInitOptions()` — after init

| Preference | Command |
| --- | --- |
| Deband `None` | nothing |
| Deband `CPU` | `vf add @deband:gradfun=radius=12` |
| Deband `GPU` | `deband=yes` |
| Stats page | `script-binding stats/display-stats-toggle`, `stats/display-page-N` |

## Audio

- `alang=""` disables mpv auto-selection; `TrackSelector` picks the track.
- `audio-delay` = preference ms / 1000, `audio-pitch-correction` follows the preference,
  `volume-max` = boost cap + 100, optional `af=dynaudnorm`.

## Subtitles

- `slang=""`, `sub-auto=no`, `sub-file-paths=""`, `subs-fallback=no` — selection is app-driven.
- `sub-fonts-dir=$filesDir/fonts/`; user fonts are copied there.
- Primary and secondary delay/speed/font/size/bold/italic/justify/colors/border/shadow/scale/
  position are set here; `secondary-sub-pos = subPos - 10`.
- `sub-scale-by-window` and `sub-use-margins` are mirrored for the secondary track.
- `stretch-dvd-subs=yes` keeps VOBSUB/DVD subtitles at the right aspect ratio.
- Runtime re-apply lives in `PlayerActivity.applySubtitlePreferences()`. It writes
  `sub-outline-size` while init writes `sub-border-size`; the App stream has no border-size
  alias, so change both deliberately.

## Anime4K

- Modes: `OFF`, `A`, `B`, `C`, `A_PLUS`, `B_PLUS`, `C_PLUS`; qualities `FAST`/`BALANCED`/`HIGH`
  map to shader suffixes `S`/`M`/`L`.
- The chain always starts with `Anime4K_Clamp_Highlights.glsl` and is joined with `:`.
- `Anime4KManager.copyShaderFromAssets` skips existing non-empty files, so a changed bundled
  shader will not reach devices that already copied it — version the directory if shader
  content changes.
- Application bails out with GPU-Next + OpenGL; the 4K/8K guard exists only in the UI.
- Shader Peek caches the active chain; changing mode/quality while peeked can restore a stale
  chain.

## Gestures and playback state

- Gesture defaults: 10 s double-tap seek, 35% seek-area width, single tap left/center/right
  actions, media-button gestures.
- Horizontal swipe maps distance to time (7 s/cm default, configurable 1–10); a full swipe is
  3 minutes or the whole video. Swiping pauses, exact-seeks, then resumes, and never lands
  exactly on the end.
- `PlayerViewModel.seekTo` uses a sequence counter so out-of-order IO seeks can't win.
  `seekToAndResume` pauses and waits for `video-pts` to cross the target.
- Playback state is saved before teardown (`runBlocking { savePlaybackStateJob.join() }`);
  `cleanupMPV()` flags `isExiting`, pauses/quits, sleeps 100 ms on the main thread, then
  destroys. The sleep is deliberate — hardware decoder teardown depends on it.
