---
name: mpvium quantified commits & PRs
description: Write mpvium commit messages and pull request descriptions that lead with the measured outcome (faster/smaller/fewer by N%) instead of the mechanism. Use whenever committing, opening or editing a PR, drafting release notes for a change, or rewriting a vague commit subject.
---

# Quantified commits & PRs

The subject says **what got better and by how much**. The body says **how**.

```text
BAD    perf(server): negotiate permessage-deflate on the websocket
GOOD   perf(server): cut websocket frame size by 70%+ with gzipping
```

The bad one describes the patch. The good one tells a reader whether to care — and the number
is checkable. Aim for that every time, including PR titles.

## Rules

1. **Every number in the subject must be backed in the body** with its baseline, method, and
   conditions. No orphan percentages.
2. **Never invent a number.** If you did not measure it, don't claim it: write `Unmeasured` in
   the body and keep the subject qualitative, or measure first.
3. **Never borrow a number** from another device, build type, file set, or commit.
4. Subject: `type(scope): outcome`, lowercase after the colon, imperative, no trailing period,
   ≤ 72 characters, number placed early.
5. One number beats three — report the metric the user actually feels.
6. A bound is fine only when it is true: `70%+` means every run beat 70%.
7. A structural change without a metric still needs something checkable: a count, a removed
   duplicate, a fixed wrong behavior. Never a fake percentage.

## Types and scopes

Types: `feat`, `fix`, `perf`, `refactor`, `build`, `chore`, `docs`, `test`, `revert`.

Scopes already used here: `player`, `playback`, `streaming`, `library`, `movies`, `series`,
`folders`, `browser`, `preferences`, `appearance`, `mediainfo`, `onboarding`, `widget`,
`update`, `deps`, `build`, `website`, `app`. Reuse an existing scope before adding one.

## Body template

```text
<one sentence: what users saw / what was wrong>

<one sentence: what changed and why this approach>

Impact
- <metric>: <before> -> <after> (<±%>)
- <metric>: <before> -> <after> (<±%>)

Method
- <device / ABI / Android version>, <build variant>, <n> runs, <median|mean>
- <command or tool that produced the numbers>

Verified
- <gradle task, CI run link, manual steps>
```

Drop the whole `Impact` block when you have no metric. `Method: unmeasured` is honest and
allowed for structural work.

## Measuring on this project

| Claim | How to get a real number |
| --- | --- |
| Startup to first frame | `adb shell am start -W -n app.aryan447.mpvium.debug/.MainActivity`, plus logcat timestamps around `MPVView` init |
| Player frame drops / jank | `adb shell dumpsys gfxinfo app.aryan447.mpvium framestats`, or `dumpsys SurfaceFlinger --latency` |
| Scroll jank (Home, lists) | `dumpsys gfxinfo` before/after; state that no macrobenchmark exists yet |
| APK size | universal APK from `assembleStandardRelease` — never from `-PciFastBuild`, which skips R8 |
| Memory | `adb shell dumpsys meminfo app.aryan447.mpvium` after a fixed script |
| Poster / metadata latency | logcat timings around the repository call |
| Folder scan time | same folder, same file set, cache state stated explicitly |
| CI wall time | `gh run view <id> --json createdAt,updatedAt` |

Number rules:

- Baseline and comparison use the same device, build type, and inputs.
- State `n` and whether it is median or mean. One run is "1 run", never "typical".
- Percentage: `(before - after) / before`, rounded to the nearest whole number, never rounded
  up to look better.
- Cold vs warm cache is part of the claim — say which.

## Examples

| BAD | GOOD |
| --- | --- |
| `perf(server): negotiate permessage-deflate on the websocket` | `perf(server): cut websocket frame size by 70%+ with gzipping` |
| `perf(player): switch to gpu-next` | `perf(player): cut dropped frames on 4K HEVC from 8.4% to 0.3%` |
| `fix(player): refactor gesture handler` | `fix(player): stop swipe-seek overshoot from ±9 min to ±7 s/cm` |
| `feat(streaming): cache posters` | `feat(streaming): show cached posters in 12 ms instead of 380 ms` |
| `perf: update mpv` | `perf(playback): cut 1080p H.264 software-decode CPU from 62% to 38%` |
| `fix(ui): improve insets` | `fix(ui): stop the nav bar overlapping the player sheet on gesture-nav` |
| `chore: clean up` | `refactor(preferences): remove 240 lines of duplicate picker sheets` |

The last two have no percentage because there was nothing to measure — they still name a
concrete, checkable outcome.

## Pull requests

- Title: same rule as a subject — best quantified outcome, ≤ 72 characters.
- Body order: `Summary` → `Before / After` → `How measured` → `Verification` → `Changelog` →
  `Risks`.
- Link the CI run instead of writing "CI passes".
- Name the ABI/device and themes you actually checked.

```markdown
## Summary
- Swipe seeking now maps finger travel to time instead of a fixed step.
- Seek error dropped from ±9 min to ±7 s/cm on a 10 cm swipe.
- Configurable 1–10 s/cm, full swipe capped at 3 min or the whole video.

## Before / After
| Metric | Before | After |
| --- | --- | --- |
| Seek error, 10 cm swipe | ±9:00 | ±1:10 |
| New settings rows | 0 | 2 |

## How measured
Pixel 8 (arm64, Android 15), debug build, 5 swipes each, median. Frame timings from
`adb shell dumpsys gfxinfo app.aryan447.mpvium framestats`.

## Verification
- [ ] `git diff --check`
- [ ] CI build green: <run link>
- [ ] Device: arm64, gesture nav, light + dark + AMOLED
- [ ] `CHANGELOG.md` Unreleased entry

## Risks
Swipe mapping changed; the old fixed 3-minute behavior is gone for updating users.
```

## Special cases

- **Dependabot / merges**: leave generated subjects alone
  (`chore(deps): bump okhttp from 5.5.0 to 5.5.1`). Don't rewrite them into claims.
- **Release commits**: `chore(release): v1.4.0`, body = the `CHANGELOG.md` section.
- **Reverts**: `revert: <original subject>` plus the reason; don't restate the old claims.
- **Security fixes**: state the outcome, not the exploit path, and avoid numbers that help an
  attacker.
- **Unrelated changes**: split them. One claim per commit keeps the number honest.

## Check before committing

```bash
sh .opencode/skills/commit-messages/scripts/check-commit-msg.sh "perf(player): cut ..."
```

The script flags perf subjects without a number, improvement claims without evidence, bad
types, over-long subjects, and trailing periods. Optional `commit-msg` hook:

```bash
ln -sf ../../.opencode/skills/commit-messages/scripts/check-commit-msg.sh .git/hooks/commit-msg
```
