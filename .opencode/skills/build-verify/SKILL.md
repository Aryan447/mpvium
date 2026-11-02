---
name: mpvium build & verify
description: Build, test, and verify mpvium changes locally or in CI. Use before claiming work is done, when a Gradle task fails, when choosing which variant or flag to run, or when you need to know whether a local build is even allowed.
---

# Build & verify

Never say "this should build". Report exactly what ran, what passed, what failed, and what
could not run.

## Hard rules

1. **Do not download or install anything locally** (SDKs, JDKs, Gradle distributions,
   dependencies, NDK) without explicit user approval — `AGENTS.md` requires this.
2. Check the environment before running Gradle:

   ```bash
   java -version               # expect 17
   echo "$ANDROID_HOME"        # expect a real SDK path
   ls ~/.gradle/wrapper/dists  # expect the wrapper distribution cached
   ```

   If something is missing, do **not** run `./gradlew` "just to see". Push a branch and let CI
   build it.

3. `git diff --check` before every commit.
4. Never commit `local.properties`, `app/build/`, `.gradle/`, `PLAN.md`, `TODO.md` (ignored on
   purpose).

## Toolchain (trust the build files, not prose)

| Fact | Value | Source |
| --- | --- | --- |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | `app/build.gradle.kts:16-21` |
| Java + Kotlin target | 17 | `app/build.gradle.kts:109-112,173` |
| CI JDK | temurin 17 | `.github/workflows/build.yml:24-28` |
| AGP / Kotlin | 9.4.1 / 2.4.21 | `gradle/libs.versions.toml:1,5` |
| Flavor | `standard` only (playstore commented out) | `app/build.gradle.kts:35-49` |
| Build types | `debug`, `release`, `preview` | `app/build.gradle.kts:74-107` |
| ABIs | armeabi-v7a, arm64-v8a, x86, x86_64 + universal | `app/build.gradle.kts:56-72` |

`AGENTS.md` says compileSdk 36; the build says 37. Follow the build file.

`versionName` carries the git commit count, and `BuildConfig.GIT_SHA` / `GIT_COUNT` shell out
to `git`, so builds need a real repo (shallow clones degrade to `0`).

## Commands

| Goal | Command |
| --- | --- |
| Fast debug build | `bash ./gradlew assembleStandardDebug` |
| Unit tests | `bash ./gradlew testStandardDebugUnitTest` |
| Release APKs, all ABIs, minified | `bash ./gradlew assembleStandardRelease` |
| Preview APKs | `bash ./gradlew assembleStandardPreview` |
| App bundle | `bash ./gradlew bundleStandardRelease -PbundleBuild` |
| CI-equivalent | `bash ./gradlew assembleStandardRelease bundleStandardRelease --parallel -x lintVitalStandardRelease -PciDebugSigning -PbundleBuild -PciFastBuild` |

Flags that change what you get:

- `-PciFastBuild` — arm64-v8a + universal only, R8/shrinking off. Artifacts are larger; never
  measure APK size from them.
- `-PbundleBuild` — disables ABI splits (AGP cannot build an AAB with them).
- `-PciDebugSigning` — signs release with the debug key; CI only.

## CI

`build.yml` runs on pull requests and pushes to `main` with the CI-equivalent command, then
uploads `app-standard-release-apks-ci-signed-<sha>` and the AAB. Inspect it with:

```bash
gh run list --workflow build.yml --limit 5
gh run watch <run-id>
gh run view --log-failed <run-id>
```

CI does **not** run `lint`, instrumented tests, or a device, and it skips
`lintVitalStandardRelease`. Do not claim those passed.

## Foot-guns

- **Room**: schemas live in `app/schemas/`. An entity change needs a migration plus the
  regenerated schema JSON committed, or upgrades crash or lose data. Never add
  `fallbackToDestructiveMigration`.
- **ABI names must stay in sync** with `release.yml`, `preview.yml`, and `pre-release.yml`,
  which hardcode `universal arm64-v8a armeabi-v7a x86 x86_64`. Renaming an ABI in Gradle
  breaks releases at artifact-copy time.
- **MediaInfo ships Java 21 bytecode** while the app targets 17. Do not "fix" it by lowering
  targets; verify through CI.
- **Tests**: only `app/src/test/java/.../MovieLibraryClassifierTest.kt` exists (JUnit 4). Put
  new tests beside it, not in `androidTest`.
- A local `assembleStandardRelease` produces unsigned APKs — expected, not a failure.

## Definition of done

- [ ] `git diff --check` clean
- [ ] Relevant Gradle task run, or the CI run linked with its result
- [ ] If the build could not run locally, say so and rely on CI
- [ ] `CHANGELOG.md` → `Unreleased` entry for user-facing changes
- [ ] Device/emulator check when the change affects playback or UI
