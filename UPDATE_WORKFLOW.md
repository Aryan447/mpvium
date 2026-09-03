# Mpvium & Ecosystem Update Workflow Guide

This document outlines the architecture, repository roles, and exact step-by-step workflow for keeping **`mpvium`** up to date whenever upstream developers make changes to **`mpv-android`**.

---

## 1. Ecosystem Architecture & Repository Roles

```
                      ┌──────────────────────────────────────┐
                      │             mpv-android              │
                      │  (Upstream Core Reference Player)    │
                      │ - native buildscripts (ffmpeg, mpv)  │
                      │ - C++ JNI bridge & BaseMPVView       │
                      │ - legacy View-based player UI        │
                      └──────────────────┬───────────────────┘
                                         │
                   Synchronize native,   │
                   JNI, & player engine  │
                                         ▼
                      ┌──────────────────────────────────────┐
                      │            mpvlibAndroid             │
                      │     (Custom Android Library)         │
                      │ - Standalone .aar build              │
                      │ - FastThumbnails (FFmpeg direct)     │
                      │ - MPVNode & Kotlin Coroutine Flows   │
                      └──────────────────┬───────────────────┘
                                         │
                    Compiled AAR         │
              (app/libs/*.aar)           │
                                         ▼
┌──────────────────────┐      ┌──────────────────────────────────────┐
│   mediainfoAndroid   │      │                mpvium                │
│ (MediaInfoLib/ZenLib)├─────►│     (Modern Compose Video Player)    │
│  - JitPack dependency│      │ - Jetpack Compose & Material 3 UI    │
│  - Codec & track info│      │ - Room Database & Koin DI            │
│  - Independent repo  │      │ - Embeds BaseMPVView into Compose    │
└──────────────────────┘      └──────────────────────────────────────┘
```

### Folder / Repository Breakdown

| Repository / Folder | What it is | How it relates to `mpvium` |
| :--- | :--- | :--- |
| **`mpv-android`** | The upstream open-source project by `mpv-android` team (`sfan5`, `Ilya Zhuravlev`). | Serves as the upstream source for native toolchains, C/C++ build scripts, libmpv versions, and player bug fixes. |
| **`mpvlibAndroid`** | Your standalone Android library extracting the mpv core engine. | Builds the `.aar` binary (`mpv-android-lib-vX.Y.Z.aar`) used by `mpvium`. Contains custom extensions: `FastThumbnails`, `MPVNode`, and reactive Kotlin `StateFlow` bindings. |
| **`mediainfoAndroid`** | Your Android wrapper around MediaArea's `MediaInfoLib` + `ZenLib`. | Published via JitPack (`com.github.aryan447:mediainfoAndroid:...`). Provides deep audio/video codec metadata for the "Media Info" inspection sheet. |
| **`mpvium`** | Your modern consumer Android app (Compose + M3). | The final application consuming `mpvlibAndroid` (via local `.aar` in `app/libs/`) and `mediainfoAndroid` (via JitPack). |

---

## 2. Upstream Change Classification

When `mpv-android` developers commit updates, they fall into four categories:

### Category A: Native Toolchain & Dependency Updates (`buildscripts/`)
* **Upstream changes**: Bumping NDK, FFmpeg, HarfBuzz, FreeType, MbedTLS, Libplacebo, Libass, adding new dependencies (e.g. `curl`, `fontconfig`, `libxml2`).
* **Where to port**: Port directly into `mpvlibAndroid/buildscripts/`.
* **Impact on `mpvium`**: Requires rebuilding the `mpvlibAndroid` `.aar` and copying the new AAR into `mpvium/app/libs/`.

### Category B: JNI Bridge & Base View Updates (`app/src/main/jni/` & `BaseMPVView.kt`)
* **Upstream changes**: Memory leak fixes, local reference cleanups (`DeleteLocalRef`), property handling, surface attachment logic.
* **Where to port**: Port into `mpvlibAndroid/app/src/main/jni/` and `mpvlibAndroid/app/src/main/java/is/xyz/mpv/BaseMPVView.kt`.
* **Important**: Always preserve your custom additions in `mpvlibAndroid`:
  - `node.cpp`, `node.h`, and `MPVNode.kt`
  - `thumbnail.cpp` and `FastThumbnails.kt`
  - Flow / StateFlow properties (`propInt`, `propDouble`, `propString`, `logFlow`, `eventFlow`)
* **Impact on `mpvium`**: Rebuild `.aar` and update `mpvium/app/libs/`.

### Category C: Assets Updates (`cacert.pem`, Fonts)
* **Upstream changes**: Updating Mozilla CA certificates or font configuration.
* **Where to port**: Copy `cacert.pem` to `mpvlibAndroid/app/src/main/assets/`.
* **Impact on `mpvium`**: Rebuild `.aar` and update `mpvium/app/libs/`.

### Category D: App-Level Features, Keycodes, & Options (`MPVActivity.kt`, Gestures, UI)
* **Upstream changes**: New playback features, property shortcuts, key mappings, brightness/volume logic, or mpv options.
* **Where to port**: Because `mpvium` replaces `MPVActivity` with Jetpack Compose (`PlayerViewModel.kt` and `MPVView.kt`), these changes are ported directly into **`mpvium`**, not `mpvlibAndroid`.

---

## 3. Step-by-Step Update Procedure

Whenever you want to pull updates from `mpv-android`:

### Step 1: Inspect Upstream Changes
Navigate to `mpv-android` and pull the latest commits:
```bash
cd /home/aryan/dev/mpv-android
git fetch origin
git log HEAD..origin/master --oneline
git pull origin master
```
Review the commits using `git diff <last_sync_commit>..HEAD` to identify what was changed:
- Look for changes in `buildscripts/`
- Look for changes in `app/src/main/jni/`
- Look for changes in `app/src/main/java/is/xyz/mpv/`
- Look for changes in `app/src/main/assets/`

---

### Step 2: Propagate Changes to `mpvlibAndroid`

Navigate to `mpvlibAndroid`:
```bash
cd /home/aryan/dev/mpvlibAndroid
```

1. **Native Buildscripts**:
   - If `buildscripts/include/depinfo.sh` changed (new NDK, library versions, dependency tree), copy/merge the updates.
   - If `buildscripts/scripts/*.sh` changed or new scripts were added, copy/merge them.
   - If dependencies were added/removed (e.g., Fontconfig, Curl, Libxml2), update `download-deps.sh` and `buildall.sh`.
   - Ensure `buildscripts/scripts/mpv-android.sh` remains configured to build the library AAR (`assembleRelease`) and does not attempt APK signing.

2. **JNI Layer**:
   - Merge changes from `app/src/main/jni/` (`main.cpp`, `property.cpp`, `event.cpp`).
   - **Crucial**: Keep `Android.mk` configured with `node.cpp`, `thumbnail.cpp`, and their shared libraries (`avformat`, `avutil`, `swscale`, `avcodec`).
   - Apply any reference cleanups (`DeleteLocalRef`) to custom JNI functions (`commandNode`, `grabThumbnailFast`) if needed.

3. **Kotlin Layer & Assets**:
   - Merge changes into `BaseMPVView.kt` and `Utils.kt`.
   - Update `cacert.pem`.
   - Keep `FastThumbnails.kt`, `MPVNode.kt`, and `MPVLib.kt` reactive Flow properties.

4. **Verify Script Syntax**:
   ```bash
   bash -n buildscripts/buildall.sh
   bash -n buildscripts/scripts/*.sh
   ```

5. **Commit and Tag**:
   ```bash
   git add -A
   git commit -m "Update from mpv-android: <summary of changes>"
   git tag vX.Y.Z
   git push origin master --tags
   ```

---

### Step 3: Build the New AAR

You can build the new `.aar` either locally via Docker or through GitHub Actions:

#### Option A: Via GitHub Actions (Recommended)
Push the new tag to GitHub. The `.github/workflows/release.yml` workflow will automatically build across all 4 ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) and publish `mpv-android-lib-vX.Y.Z.aar` to the GitHub Release.

#### Option B: Via Local Docker Build
```bash
cd /home/aryan/dev/mpvlibAndroid/buildscripts
./docker-build.sh
```
The output AAR will be located at:
```
app/build/outputs/aar/app-release.aar
```

---

### Step 4: Update `mpvium` with the New AAR

1. Rename the newly built AAR to match your version tag (e.g., `mpv-android-lib-v0.0.2.aar`).
2. Copy it into `mpvium`:
   ```bash
   cp app/build/outputs/aar/app-release.aar /home/aryan/dev/mpvium/app/libs/mpv-android-lib-v0.0.2.aar
   ```
3. Update `mpvium/app/build.gradle.kts`:
   ```kotlin
   // Replace the old AAR dependency:
   implementation(files("libs/mpv-android-lib-v0.0.2.aar"))
   ```
4. (Optional) Remove the old `.aar` from `mpvium/app/libs/` to save repository space.

---

### Step 5: Port Relevant Player Features into `mpvium`

Check if `mpv-android` added any features you want in `mpvium`:
- **New mpv properties / options**: Check `MPVActivity.kt` in `mpv-android` and add corresponding properties or settings to `mpvium/app/src/main/java/app/aryan447/mpvium/ui/player/PlayerViewModel.kt` or `MPVView.kt`.
- **New Key Mappings**: Check `KeyMapping.kt` in `mpv-android` and update keybindings in `mpvium`.
- **New Codecs / Formats**: Check `Utils.MEDIA_EXTENSIONS` or `PROTOCOLS` and verify file picker filtering.

---

## 4. What About `mediainfoAndroid`?

**`mediainfoAndroid` is independent of `mpv-android`**.

- **When to update it**:
  1. When MediaArea releases a new version of `MediaInfoLib` or `ZenLib` (e.g. `v26.05`).
  2. When Android NDK or toolchain standards change (e.g. 16 KB page size alignment or Java 17/21 bytecode requirements).
- **How to update it**:
  1. Update dependencies or build flags in `mediainfoAndroid/mediainfo-lib/build.gradle`.
  2. Push a new tag to GitHub (e.g. `v1.0.1`).
  3. JitPack will build and host `com.github.aryan447:mediainfoAndroid:v1.0.1`.
  4. Update `mpvium/gradle/libs.versions.toml`:
     ```toml
     mediainfo-lib = "com.github.aryan447:mediainfoAndroid:v1.0.1"
     ```

---

## 5. Quick Reference Checklist

When updating from `mpv-android`:

- [ ] Fetch & review `mpv-android` git diff.
- [ ] Merge `buildscripts/` changes into `mpvlibAndroid/buildscripts/`.
- [ ] Merge `jni/` and `BaseMPVView.kt` changes into `mpvlibAndroid`.
- [ ] Confirm `MPVNode`, `FastThumbnails`, and `prop*` Flow bindings are preserved in `mpvlibAndroid`.
- [ ] Update `cacert.pem` in `mpvlibAndroid/app/src/main/assets/`.
- [ ] Build new `.aar` (via Docker or GitHub Actions release).
- [ ] Copy `.aar` to `mpvium/app/libs/` and update `mpvium/app/build.gradle.kts`.
- [ ] Port any desired player options or gesture handling from `MPVActivity.kt` into `PlayerViewModel.kt`.
- [ ] Verify build in `mpvium`:
  ```bash
  cd /home/aryan/dev/mpvium
  ./gradlew assembleStandardDebug
  ```
