---
name: mpvium release & changelog
description: Prepare an mpvium release or preview — CHANGELOG entries, version bump, tag naming, fastlane metadata, and the release workflows. Use when adding a changelog entry, cutting a version, tagging a preview or stable release, editing release notes, or changing anything under .github/workflows.
---

# Release & changelog

`CHANGELOG.md` is the single source of truth for release notes, and it is written for users,
not for developers.

## Changelog rules

```markdown
## Unreleased

### Added

- …

### Changed

- …

### Fixed

- …
```

- Start every user-visible change under `Unreleased`, grouped by `Added` / `Changed` /
  `Fixed` (plus `Removed` when something is gone). Drop empty groups.
- One line per change, in the user's words: what they now see or can do. No file names, class
  names, "refactored", or commit hashes.
- Keep the existing voice: sentence case, full stop at the end, feature prefix where it helps
  (`Player:`, `Appearance:`, `Movies:`).
- Internal-only work (CI, agent skills, invisible refactors) does not belong here.
- Do not bump `versionCode`/`versionName` for ordinary changes.

Cutting a version:

1. Rename `## Unreleased` to `## V1.4.0` (existing style keeps the `V`) and add a fresh empty
   `## Unreleased` above it.
2. Bump `versionCode` and `versionName` together in `app/build.gradle.kts` `defaultConfig`.
3. Tag `v1.4.0` only after CI on `main` is green. Preview tags are `v1.4.0-preview1` (or
   `v1.4.0.preview1`); the workflows match `v*`, and `release.yml` skips any tag containing
   `preview`.
4. Paste the version section into the GitHub release body; checksums are appended
   automatically.

## Version state

| Field | Where |
| --- | --- |
| `versionCode` / `versionName` | `app/build.gradle.kts` `defaultConfig` |
| Debug/preview suffixes | git commit count, added by the build types |
| ABI version-code offset | base×10 + 1..4 (`armeabi-v7a`…`x86_64`), universal = base×10 |

## Workflows

| Workflow | Trigger | Result |
| --- | --- | --- |
| `build.yml` | PR + push to `main` | CI-signed release APKs (arm64 + universal) and the AAB as artifacts |
| `release.yml` | tag `v*` without `preview` | all 5 APKs, signed with repo secrets, draft GitHub release |
| `pre-release.yml` | tag `v*-preview*` or `v*.preview*` | 5 preview APKs, published prerelease |
| `preview.yml` | manual dispatch | preview APKs as workflow artifacts only |
| `telegram-release.yml` | release published | posts the universal APK plus notes to Telegram |

Every release path needs exactly these artifact names, which must match the ABI splits in
`app/build.gradle.kts`: `universal`, `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. Renaming an
ABI breaks the copy step.

Never add keystores, passwords, or tokens to the repo. Signing comes from the
`SIGNING_KEYSTORE`, `SIGNING_KEY_ALIAS`, `SIGNING_STORE_PASSWORD`, and `KEY_PASSWORD` secrets.

## Fastlane

Store metadata lives in `fastlane/metadata/android/en-US/`: `title.txt`,
`short_description.txt`, `full_description.txt`, `changelogs/<versionCode>.txt`, `images/`.
Changelog files are named by **versionCode**, so each release needs
`changelogs/<new versionCode>.txt` with a short user-facing summary (under 500 characters, not
a copy of the whole `CHANGELOG.md` section).

Only `en-US` exists. Do not add locales without complete translations.

## Checklist

- [ ] `Unreleased` entry in the right group and voice
- [ ] Version bumped in `app/build.gradle.kts` (stable releases)
- [ ] Fastlane changelog for the new `versionCode`
- [ ] `git diff --check` clean
- [ ] CI on `main` green before tagging
- [ ] Tag pushed and draft release body checked against `CHANGELOG.md`
