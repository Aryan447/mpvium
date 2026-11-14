---
name: mpvium build & verify
description: Build, test, and verify mpvium changes locally or in CI. Use before claiming work is done, when a Gradle task fails, when choosing which variant or flag to run, or when you need to know whether a local build is even allowed.
---

# Build & verify

Never say "this should build". Report exactly what ran, what passed, what failed, and what
could not run.

## Hard rules

1. **Do not install anything outside the flake.** JDK 17, Android SDK 37, build-tools 36.0.0,
   and git come from `flake.nix` via `nix develop`. Do not install a separate JDK, SDK, NDK,
   or Gradle distribution without explicit user approval. `nix develop`/`nix run` fetching
   their pinned closure is the approved path.
2. Check the environment before running Gradle:

   ```bash
   nix --version
   nix develop --command java -version   # expect 17
   nix develop --command bash -c 'echo "$ANDROID_HOME"'
   ```

   If Nix is missing, do **not** run `./gradlew` "just to see". Push a branch and let CI build
   it, or ask the user to install Nix.
3. `git diff --check` before every commit.
4. Never commit `local.properties`, `app/build/`, `.gradle/`, `.direnv/`, `PLAN.md`, `TODO.md`
   (ignored on purpose).

## Toolchain (trust the build files and flake, not prose)

| Fact | Value | Source |
| --- | --- | --- |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | `app/build.gradle.kts:16-21` |
| Java + Kotlin target | 17 | `app/build.gradle.kts:109-112,173` |
| CI JDK | 17 from nixpkgs | `flake.nix` |
| Android SDK | platform 37.0, build-tools 36.0.0 | `flake.nix` |
| AGP / Kotlin | 9.5.1 / 2.4.21 | `gradle/libs.versions.toml:1,5` |
| Flavor | `standard` only (playstore commented out) | `app/build.gradle.kts:35-49` |
| Build types | `debug`, `release`, `preview` | `app/build.gradle.kts:74-107` |
| ABIs | armeabi-v7a, arm64-v8a, x86, x86_64 + universal | `app/build.gradle.kts:56-72` |

When `compileSdk` or AGP changes, bump `androidSdkFor` in `flake.nix` in the same change and
commit `flake.lock`. A stale SDK shows up as "Failed to find target with hash string
android-37" or a missing build-tools revision.

`versionName` carries the git commit count, and `BuildConfig.GIT_SHA` / `GIT_COUNT` shell out
to `git`, so builds need a real repo (shallow clones degrade to `0`).

## Commands

| Goal | Command |
| --- | --- |
| Enter toolchain | `nix develop` |
| Fast debug build | `nix run .#debug` |
| Unit tests | `nix run .#test` |
| Release APKs, all ABIs, minified | `nix run .#build` |
| Preview APKs | `nix run .#preview` |
| App bundle | `nix run .#bundle` |
| CI-equivalent | `nix run .#ci` |
| Inside the shell | `bash ./gradlew assembleStandardDebug` |

Flags that change what you get:

- `-PciFastBuild` — arm64-v8a + universal only, R8/shrinking off. Artifacts are larger; never
  measure APK size from them.
- `-PbundleBuild` — disables ABI splits (AGP cannot build an AAB with them).
- `-PciDebugSigning` — signs release with the debug key; CI only.

## CI

Every build workflow installs Nix, restores the Nix store, then runs Gradle through
`nix develop`. `build.yml` runs on pull requests and pushes to `main` with the CI-equivalent
command, then uploads `app-standard-release-apks-ci-signed-<sha>` and the AAB. Inspect it with:

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
- **Signing** uses `$ANDROID_BUILD_TOOLS/apksigner` from the dev shell. Do not hardcode
  `build-tools/34.0.0` again.

## Definition of done

- [ ] `git diff --check` clean
- [ ] Relevant Gradle task run through Nix, or the CI run linked with its result
- [ ] `nix flake check` passes when `flake.nix` changed
- [ ] If the build could not run locally, say so and rely on CI
- [ ] `CHANGELOG.md` / `UNRELEASED.md` entry for user-facing changes
- [ ] Device/emulator check when the change affects playback or UI
