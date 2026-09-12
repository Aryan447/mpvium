# Contributing to mpvium

Thanks for your interest in contributing to mpvium! This is a community-driven,
GPL-licensed Android video player built on mpv / libmpv.

Philosophy: **Sane defaults, pro-level control** — opinionated for everyday
watching, fully customizable for pros.

Please read this file, our [Code of Conduct](CODE_OF_CONDUCT.md), and
[SECURITY.md](SECURITY.md) before contributing.

## Quick start

1. Fork the repo and clone your fork.
2. Create a focused branch: `feat/my-feature`, `fix/my-bug`, `docs/my-change`.
3. Build and test, then open a pull request against `main`.

```bash
bash ./gradlew assembleStandardDebug
```

Useful variants:

```bash
bash ./gradlew assembleStandardRelease
bash ./gradlew assemblePlaystoreRelease
bash ./gradlew assembleStandardPreview
```

APKs are written to `app/build/outputs/apk/`. Do not commit build outputs.

### Requirements

- JDK 17 (see `.github/workflows/build.yml` for the CI toolchain)
- Android SDK with compile SDK 36, min SDK 26
- Git

Dependencies are resolved by Gradle. Some are hosted on JitPack
(see `gradle/libs.versions.toml`). First build may download Gradle +
dependencies — this is expected.

Project stack: Kotlin, Jetpack Compose + Material 3, Room, Koin, OkHttp,
Navigation 3, native libmpv playback.

## What to contribute

Good first areas:

- Bug fixes with a clear repro
- Tablet / large-screen layout improvements (current focus)
- Performance + battery efficiency improvements with measured before/after
- Player UX polish (gestures, seekbar, subtitles, chapters)
- File browser, SMB / FTP / WebDAV fixes
- Docs, translations, screenshots, Fastlane metadata

Check [`CHANGELOG.md`](CHANGELOG.md), [`PLAN.md`](PLAN.md), and open issues
before starting large work. For large features, open an issue / discussion
first to align on design.

## Issues

Before opening an issue:

- Search existing issues (open + closed).
- Use the Bug report or Feature request template.
- Include: device model, Android version, app version (or commit SHA),
  APK flavor (`standard` / `playstore`, `arm64-v8a` / `universal` etc.),
  steps to reproduce, expected vs actual, logs / screenshots.

Do not include keystores, passwords, tokens, or private media links.

## Pull requests

Keep PRs small and focused. One feature / fix per PR.

PR checklist:

- [ ] Descriptive title (`feat(player): ...`, `fix(browser): ...`, `docs: ...`)
- [ ] Linked issue (`Fixes #123` if applicable)
- [ ] What changed + why, screenshots / video for UI changes
- [ ] Tested on: device/emulator, Android version, build variant
- [ ] `git diff --check` clean (no whitespace errors)
- [ ] No generated files or `app/build/` outputs committed
- [ ] CHANGELOG `Unreleased` entry added for user-facing changes
- [ ] GPL compliance preserved (see below)

CI builds `assembleStandardRelease` on pull requests and uploads APK
artifacts. Ensure CI passes. If CI fails on native / NDK steps, note it in
the PR — maintainers can help.

### Code style

- Match existing spacing, padding, corner radii, elevation, colors,
  typography, and iconography. Reuse shared components, dimens, shapes,
  and string resources — do not introduce near-duplicates.
- Verify alignment, insets (status / navigation / gesture areas),
  RTL mirroring, and light/dark themes for UI changes.
- Kotlin + Compose conventions already in the tree win over personal style.
- Run `git diff --check` before committing.

### Native / JNI changes

Be extra careful with C/C++ and JNI under `app/src/main/jni/` (if touched):

- Verify all supported ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`).
- Explain testing per-ABI in the PR.
- Do not bump NDK / CMake versions incidentally.

### Dependencies

- Update `gradle/libs.versions.toml`, not ad-hoc `build.gradle.kts` versions.
- Verify JitPack / Maven resolution via CI. In particular
  `mediainfo-lib = "com.github.aryan447:mediainfoAndroid:v1.0.0-fix"`
  requires the matching Git tag + successful JitPack build upstream.

## Licensing — important (GPL)

mpvium as a whole is distributed under
[GPL-3.0-or-later](LICENSE). It links GPL components (mpv, mpv-android,
FFmpeg); see [NOTICE](NOTICE) for attribution.

By contributing you agree your contributions will be licensed under the same
license (GPL-3.0-or-later) and that you have the right to contribute them
(your own work, or compatible-licensed work with attribution preserved).

- Do not introduce proprietary / incompatible-licensed code.
- Preserve copyright + license headers and `NOTICE` attributions.
- Never commit keystores, passwords, tokens, or signing secrets.
  Release signing uses GitHub Actions secrets — see `AGENTS.md`.

## Flavors

- `standard` — full-featured build with update support
- `playstore` — Google Play-compatible build with restricted storage behavior

If a bug is flavor-specific, state which flavor you tested.

## Communication

- Join our [Telegram community](https://t.me/mpvium) for tester discussion,
  preview builds, help, and feature ideas.
- Formal bug reports and feature requests still belong on GitHub Issues
  (see [Issues](#issues)) so they stay tracked.
- Be kind and constructive per our Code of Conduct.
- Reviewers may ask for smaller scope or screenshots — this is normal.
- If your PR goes stale, a gentle ping after 7 days is fine.

Thank you for helping make mpvium better!
