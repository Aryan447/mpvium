---
name: mpvium MediaInfo
description: Work with the MediaInfo integration in mpvium — the media info screen, metadata extraction, file-descriptor handling, or the JitPack dependency. Use when touching MediaInfoOps, MediaInfoActivity, codecs/rotation/duration extraction, media scanning, or the mediainfo Android library version.
---

# MediaInfo integration

## Two entry points only

| Path | Role |
| --- | --- |
| `utils/media/MediaInfoOps.kt` | the only file that touches `net.mediaarea.mediainfo.lib.MediaInfo` |
| `ui/mediainfo/MediaInfoActivity.kt` | the media info screen (renders text) |

`utils/media/MediaInfoParser.kt` is **filename parsing**, not MediaInfo. Don't conflate them.

Other callers go through `MediaInfoOps`: `VideoMetadataCacheRepository`, `MediaFileRepository`,
`VideoScanUtils`, and `ThumbnailRepository` (rotation).

## File-descriptor contract (easiest thing to get wrong)

```kotlin
val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return failure
val fd = pfd.detachFd()
val mi = MediaInfo()
try {
  mi.Open(fd, fileName)   // native JNI_OpenFd reads the fd, seeks with lseek64, then close(fd)
  // …Get / Inform…
} finally {
  mi.Close()
  pfd.close()             // no-op after detachFd
}
```

- `detachFd()` transfers ownership. **Never close or use the fd after `Open` returns.**
- Always pass the real filename — MediaInfo uses the extension for format detection.
- `Open` returns 0/1 and is currently discarded; check it in new code.
- `MediaInfo()` allocates a native object that `Close()` does **not** free. `Destroy()` is
  never called today, so every scan leaks one native object. If you touch this file, add
  `mi.Destroy()` in the `finally` (after `Close()`) and add a CHANGELOG fix entry.
- Detaching before a constructor that can throw leaks the fd — construct first, then detach.

## Threading

Every public `MediaInfoOps` method is `withContext(Dispatchers.IO) { runCatching { … } }`.
Never call it from the main thread, and don't add a main-thread wrapper.

`runCatching` swallows `CancellationException`, so a cancelled scan returns
`Result.failure` instead of propagating. Batch extraction runs up to 16 parallel coroutines;
memory growth there comes from the native leak above, not from Kotlin.

## API used

`Open(fd, name)`, `Option("Inform", "Text")`, `Inform()`, `Get(stream, index, param)`,
`Count_Get(stream)`, `Close()`. The library's `MediaInfoUtil` is unused — don't start using it
without a reason.

`extractBasicMetadata` normalises subtitle codecs to `PGS`, `ASS`, `SSA`, `SRT`, `DVD`, `VTT`,
`DVB`, `TX3G`. `getRotation` reads `Video/0/Rotation`.

## Current UI facts (do not copy blindly)

- `MediaInfoActivity` calls `getMediaInfo()` **and then** `generateTextOutput()`, opening and
  scanning the file twice; the structured `MediaInfoData` is unused for rendering. Prefer one
  open that serves both.
- `generateTextOutput()` returns `"Error: Could not open file"` as a **successful** `Result`,
  and `isLoading` is only cleared on `getMediaInfo` success — a failed text dump leaves
  "Loading detailed information…" forever.
- Rendering is a single scrollable column of name/value cards (`MediaInfoSection`), monospace
  values, `SelectionContainer` for copy.
- Every string in that file is a hardcoded English literal. Use `stringResource` for anything
  new, and move existing ones into `strings.xml` when you touch them.
- The activity is `exported="true"` with broad `SEND`/`VIEW` filters (`video/*`, `audio/*`,
  `image/*`, `text/*`, subtitle types, `application/*`). Treat incoming URIs as untrusted:
  validate the MIME type and handle null grants.
- Share writes `cacheDir/mediainfo_<name>.txt` and exposes it through `FileProvider`
  `${applicationId}.provider` (`res/xml/provider_paths.xml` includes `<cache-path path=".">`).

## Dependency

```toml
mediainfo-lib = "com.github.aryan447:mediainfoAndroid:v1.0.0-fix"
```

- JitPack, from the separate `aryan447/mediainfoAndroid` repo. The tag must exist there and its
  JitPack build must succeed before changing this version.
- `settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS` with an explicit `includeGroup` list; a new
  `com.github.*` publisher needs an entry there.
- The public Kotlin package stays `net.mediaarea.mediainfo.lib` regardless of repo owner.
- Published classes are **Java 21 bytecode** while the app targets 17. Don't "fix" that by
  lowering the app target; verify through CI.
- ProGuard: `app/proguard-rules.pro` keeps `net.mediaarea.mediainfo.lib.**` and `is.xyz.mpv.**`;
  the library adds `-keepclasseswithmembernames class * { native <methods>; }`. `-dontobfuscate`
  is on — keep the explicit rules anyway.
- The AAR ships `libmediainfo.so` and `libzen.so` for all four ABIs, aligned for 16 KB pages.

## Bumping the library

1. Confirm the new tag exists and JitPack built it.
2. Update `gradle/libs.versions.toml` only.
3. Build through CI; check R8/ProGuard and the media info screen on a device.
4. Add a `CHANGELOG.md` entry if users see any difference.

## Verify

- [ ] Media info opens for a local video, a `content://` picked file, and an audio file
- [ ] Copy and Share both produce the full report
- [ ] Rotation still correct for a portrait-shot video (thumbnail orientation)
- [ ] Folder scans don't grow memory unbounded if you changed `MediaInfoOps`
- [ ] `CHANGELOG.md` entry for user-visible changes
