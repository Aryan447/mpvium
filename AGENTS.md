# AGENTS.md

## Project overview

`mpvEx` (mpvExtended) is an Android video player fork based on `mpv-android` and
libmpv. It uses Kotlin, Jetpack Compose, Material 3, Room, Koin, and native
components for media playback and media analysis.

The application package and namespace are:

- Application ID: `app.aryan447.mpvex`
- Android namespace: `app.aryan447.mpvex`
- Minimum SDK: 26
- Target/compile SDK: 36

The project is still under development. Do not describe it as bug-free or
production-perfect without running the relevant tests and build checks.

## Repository layout

- `app/` — main Android application source, resources, and native playback code.
- `app/src/main/java/app/aryan447/mpvex/` — Kotlin application source.
- `app/src/main/java/app/aryan447/mpvex/ui/mediainfo/` — MediaInfo UI/integration.
- `gradle/libs.versions.toml` — dependency and plugin versions.
- `.github/workflows/` — CI, preview, release, and deployment workflows.
- `fastlane/metadata/` — store metadata and screenshots.
- `website/` — project website; it is separate from the Android build.

## MediaInfo dependency

The app consumes the separately maintained Android library repository:

`https://github.com/aryan447/mediainfoAndroid`

The dependency is served by JitPack and is declared in
`gradle/libs.versions.toml`:

```toml
mediainfo-lib = "com.github.aryan447:mediainfoAndroid:v1.0.0-fix"
```

The library repository must contain the matching Git tag `v1.0.0-fix`, and its
JitPack build must succeed before changing or testing this dependency.

The library's public Kotlin package is `net.mediaarea.mediainfo.lib`. Do not
rename that package merely to change the GitHub owner or repository identity.

## Build and verification

Use the Gradle wrapper; do not require a global Gradle installation:

```bash
bash ./gradlew assembleStandardDebug
```

Useful variants:

```bash
bash ./gradlew assembleStandardRelease
bash ./gradlew assemblePlaystoreRelease
bash ./gradlew assembleFdroidRelease
bash ./gradlew assembleStandardPreview
```

The project documents JDK 17. The MediaInfo library currently targets Java/Kotlin
21, so verify the actual CI toolchain if compilation fails locally.

Before committing, run:

```bash
git diff --check
```

Build artifacts are written below `app/build/` and should not be committed.

## GitHub Actions

`.github/workflows/build.yml` builds the standard release APK on pull requests
and pushes to `main`, then uploads unsigned ABI-specific and universal APKs as
workflow artifacts.

Release and preview workflows may require repository signing secrets. Never add
keystores, passwords, tokens, or other credentials to the repository.

## Change guidelines

1. Keep changes focused and preserve existing user-facing behavior unless the
   task explicitly requests a behavior change.
2. Search the repository with `rg` before renaming identifiers or dependencies.
3. Prefer existing project patterns and components over introducing new ones.
4. Do not modify generated files or build outputs unless specifically required.
5. Be careful with native C/C++ and JNI changes; verify all supported ABIs.
6. When changing dependencies, update the version catalog and verify repository
   resolution through JitPack or CI.
7. Do not claim that the app is perfect. Report exactly which checks passed,
   failed, or could not run.

## Development without local installation

GitHub Actions is the preferred fallback when the local machine lacks Android
SDK, NDK, CMake, or network access. Push the project and inspect the workflow
under the repository's **Actions** tab. Download generated APKs from the
workflow's **Artifacts** section.

