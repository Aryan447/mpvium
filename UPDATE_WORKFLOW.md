# mpv-android Ecosystem Update Workflow

This document describes the complete workflow for keeping the local
`mpvlibAndroid` library synchronized with upstream `mpv-android`, while
preserving `mpvlibAndroid`'s custom functionality and updating the downstream
`mpvium` application.

---

# 1. Repository Architecture

The projects are separate Git repositories located next to each other:

```text
/home/aryan/dev/
├── mpv-android/
├── mpvlibAndroid/
├── mpvium/
└── mediainfoAndroid/
```

The dependency flow is:

```text
                       ┌──────────────────────────────┐
                       │         mpv-android          │
                       │       Upstream project       │
                       │                              │
                       │ • build/toolchain scripts    │
                       │ • libmpv integration         │
                       │ • JNI bridge                 │
                       │ • BaseMPVView                │
                       │ • full Android player app    │
                       └──────────────┬───────────────┘
                                      │
                         selective upstream sync
                                      │
                                      ▼
                       ┌──────────────────────────────┐
                       │       mpvlibAndroid          │
                       │      Custom Android lib      │
                       │                              │
                       │ • standalone AAR             │
                       │ • MPVNode                    │
                       │ • FastThumbnails             │
                       │ • Kotlin Flow/StateFlow      │
                       │ • selected mpv-android code  │
                       └──────────────┬───────────────┘
                                      │
                              generated AAR
                                      │
                                      ▼
                       ┌──────────────────────────────┐
                       │           mpvium             │
                       │     Modern Compose app       │
                       │                              │
                       │ • Jetpack Compose            │
                       │ • PlayerViewModel            │
                       │ • application/UI logic       │
                       └──────────────────────────────┘

                       mediainfoAndroid
                              │
                              └── independent dependency
```

## Important

`mpvlibAndroid` is **not a full fork of `mpv-android`**.

It is a selectively maintained library derived from parts of
`mpv-android`.

Therefore:

```text
mpv-android  !=  mpvlibAndroid
```

and the correct synchronization strategy is:

```text
inspect → select → merge → preserve custom code → build → release
```

not:

```text
git merge upstream/master
```

---

# 2. What Each Repository Contains

## `mpv-android`

The upstream repository contains the complete Android player:

```text
mpv-android/
└── app/src/main/
    ├── java/
    │   └── is/xyz/mpv/
    │       ├── filepicker/
    │       └── mpv/
    │           ├── MPVActivity.kt
    │           ├── MainActivity.kt
    │           ├── MPVLib.kt
    │           ├── BaseMPVView.kt
    │           ├── MPVView.kt
    │           ├── TouchGestures.kt
    │           ├── preferences/
    │           └── ...
    ├── jni/
    ├── assets/
    └── res/
```

It also contains the upstream native build system:

```text
mpv-android/buildscripts/
```

---

## `mpvlibAndroid`

The library intentionally contains a much smaller subset:

```text
mpvlibAndroid/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── cacert.pem
│   │   ├── java/is/xyz/mpv/
│   │   │   ├── BaseMPVView.kt
│   │   │   ├── FastThumbnails.kt
│   │   │   ├── KeyMapping.kt
│   │   │   ├── MPVLib.kt
│   │   │   ├── MPVNode.kt
│   │   │   └── Utils.kt
│   │   └── jni/
│   │       ├── Android.mk
│   │       ├── Application.mk
│   │       ├── event.cpp
│   │       ├── event.h
│   │       ├── globals.h
│   │       ├── jni_utils.cpp
│   │       ├── jni_utils.h
│   │       ├── log.cpp
│   │       ├── log.h
│   │       ├── main.cpp
│   │       ├── node.cpp
│   │       ├── node.h
│   │       ├── property.cpp
│   │       ├── render.cpp
│   │       └── thumbnail.cpp
│   └── build.gradle
├── buildscripts/
├── gradle/
├── scripts/
└── ...
```

The missing upstream application files are intentional.

---

# 3. Sync Classification

Every upstream change should be classified before being copied.

## Category A — Native Toolchain / Buildscripts

Upstream:

```text
mpv-android/buildscripts/
```

Destination:

```text
mpvlibAndroid/buildscripts/
```

Examples:

```text
buildall.sh
include/depinfo.sh
include/download-deps.sh
scripts/ffmpeg.sh
scripts/mpv.sh
scripts/mpv-android.sh
```

These generally need to track upstream closely, but local modifications must
still be preserved where `mpvlibAndroid` differs.

---

## Category B — JNI / Native Player Layer

Upstream:

```text
mpv-android/app/src/main/jni/
```

Destination:

```text
mpvlibAndroid/app/src/main/jni/
```

Relevant upstream files include:

```text
event.cpp
event.h
jni_utils.cpp
jni_utils.h
log.cpp
log.h
main.cpp
property.cpp
render.cpp
```

These should be inspected and selectively synchronized.

### Never blindly overwrite

```text
node.cpp
node.h
thumbnail.cpp
```

These contain custom `mpvlibAndroid` functionality.

---

## Category C — Kotlin MPV Layer

Relevant upstream files:

```text
BaseMPVView.kt
MPVLib.kt
Utils.kt
KeyMapping.kt
```

Destination:

```text
mpvlibAndroid/app/src/main/java/is/xyz/mpv/
```

These files may contain both upstream code and local modifications.

They should normally be **manually merged**, not blindly replaced.

---

## Category D — Assets

Relevant upstream assets:

```text
mpv-android/app/src/main/assets/cacert.pem
```

Destination:

```text
mpvlibAndroid/app/src/main/assets/cacert.pem
```

If upstream changes the asset, update the library version after checking that
there are no intentional local modifications.

---

## Category E — Application/UI Features

Examples:

```text
MPVActivity.kt
MainActivity.kt
MainScreenFragment.kt
MPVView.kt
TouchGestures.kt
BackgroundPlaybackService.kt
preferences/
filepicker/
res/
```

These generally **do not belong in `mpvlibAndroid`**.

Because `mpvium` has its own Compose-based player architecture, application-level
features should normally be ported directly into `mpvium`.

---

# 4. Custom `mpvlibAndroid` Code

The following files are specifically part of the library's custom layer.

## Kotlin

```text
app/src/main/java/is/xyz/mpv/FastThumbnails.kt
app/src/main/java/is/xyz/mpv/MPVNode.kt
```

Also preserve custom changes inside:

```text
BaseMPVView.kt
MPVLib.kt
Utils.kt
```

Especially the reactive API:

```text
propInt
propDouble
propString
logFlow
eventFlow
```

---

## JNI

Preserve:

```text
app/src/main/jni/node.cpp
app/src/main/jni/node.h
app/src/main/jni/thumbnail.cpp
```

Also ensure `Android.mk` still compiles the custom native sources and links
the required libraries.

At minimum, verify the custom sources and required libav libraries are still
present.

---

# 5. Initial Upstream Remote Setup

Run these commands from `mpvlibAndroid`.

```bash
cd /home/aryan/dev/mpvlibAndroid
```

Check the working tree:

```bash
git status
```

It should be clean before starting a synchronization.

Check remotes:

```bash
git remote -v
```

Add upstream if it does not already exist:

```bash
git remote add upstream https://github.com/mpv-android/mpv-android.git
```

Verify:

```bash
git remote -v
```

Expected:

```text
origin
upstream
```

Do this only once.

---

# 6. Keep Both Repositories Side-by-Side

The recommended local structure is:

```text
/home/aryan/dev/
├── mpv-android/
└── mpvlibAndroid/
```

This makes direct comparisons easy.

From `mpvlibAndroid`:

```bash
../mpv-android
```

can be used to inspect the upstream working tree.

For example:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/BaseMPVView.kt \
    app/src/main/java/is/xyz/mpv/BaseMPVView.kt
```

This is useful even though the repositories are independent.

---

# 7. Start an Update

Always start from a clean `master`.

```bash
cd /home/aryan/dev/mpvlibAndroid
git switch master
git status
```

Update the local library repository:

```bash
git pull origin master
```

Fetch upstream without changing the working tree:

```bash
git fetch upstream
```

Check the latest upstream commits:

```bash
git log --oneline upstream/master -20
```

---

# 8. Create a Synchronization Branch

Do not perform synchronization directly on `master`.

```bash
git switch -c sync-upstream
```

If you already created the branch:

```bash
git switch sync-upstream
```

---

# 9. Find the Previous Upstream Sync Point

This is important because the two repositories have different histories.

Do not assume that:

```bash
HEAD..upstream/master
```

means "changes since the last synchronization".

Find previous synchronization commits:

```bash
git log --all --oneline --grep='Sync mpv-android'
```

Example:

```text
91a2c4f Sync mpv-android 1234567
```

The upstream SHA recorded in the synchronization commit is the previous
upstream baseline.

For a new project where no sync commit exists yet, establish the baseline
manually by inspecting the existing library code against the current upstream
version.

---

# 10. Inspect Upstream Changes

Assuming:

```text
LAST_SYNC_SHA=123456789abcdef
```

Inspect buildscript changes:

```bash
git diff LAST_SYNC_SHA..upstream/master -- buildscripts/
```

Inspect JNI:

```bash
git diff LAST_SYNC_SHA..upstream/master -- app/src/main/jni/
```

Inspect Kotlin:

```bash
git diff LAST_SYNC_SHA..upstream/master -- \
    app/src/main/java/is/xyz/mpv/
```

Inspect assets:

```bash
git diff LAST_SYNC_SHA..upstream/master -- \
    app/src/main/assets/
```

Check the statistics first:

```bash
git diff --stat LAST_SYNC_SHA..upstream/master -- buildscripts/
git diff --stat LAST_SYNC_SHA..upstream/master -- app/src/main/jni/
git diff --stat LAST_SYNC_SHA..upstream/master -- app/src/main/java/is/xyz/mpv/
git diff --stat LAST_SYNC_SHA..upstream/master -- app/src/main/assets/
```

---

# 11. Inspect Individual Upstream Commits

First list commits:

```bash
git log --oneline LAST_SYNC_SHA..upstream/master
```

Inspect an individual commit:

```bash
git show <commit>
```

Only inspect relevant files:

```bash
git show <commit> -- buildscripts/
git show <commit> -- app/src/main/jni/
git show <commit> -- app/src/main/java/is/xyz/mpv/
```

This is often easier than trying to understand one enormous combined diff.

---

# 12. Use Neovim for the Actual Review

Open the library:

```bash
cd /home/aryan/dev/mpvlibAndroid
nvim .
```

With `vim-fugitive`:

```vim
:Git
```

Useful commands:

```vim
:Gdiffsplit
```

If `diffview.nvim` is installed:

```vim
:DiffviewOpen
```

For a specific upstream file, use:

```bash
git show upstream/master:app/src/main/java/is/xyz/mpv/BaseMPVView.kt
```

Or compare the actual separate repositories:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/BaseMPVView.kt \
    app/src/main/java/is/xyz/mpv/BaseMPVView.kt
```

---

# 13. Apply Buildscript Changes

Inspect:

```bash
git diff LAST_SYNC_SHA..upstream/master -- buildscripts/
```

If an upstream file has no local modifications, it can be replaced with the
upstream version.

For example:

```bash
git restore --source=upstream/master -- buildscripts/buildall.sh
```

For an entire buildscript directory:

```bash
git restore --source=upstream/master -- buildscripts/
```

Only use the directory-wide command after confirming that there are no
intentional `mpvlibAndroid` changes in the directory.

If a file has custom changes, manually merge it instead.

---

# 14. Apply JNI Changes

Inspect:

```bash
git diff LAST_SYNC_SHA..upstream/master -- app/src/main/jni/
```

Typical upstream files that can be updated:

```text
event.cpp
event.h
jni_utils.cpp
jni_utils.h
log.cpp
log.h
main.cpp
property.cpp
render.cpp
```

If appropriate, restore individual files:

```bash
git restore --source=upstream/master -- \
    app/src/main/jni/event.cpp \
    app/src/main/jni/event.h \
    app/src/main/jni/jni_utils.cpp \
    app/src/main/jni/jni_utils.h \
    app/src/main/jni/log.cpp \
    app/src/main/jni/log.h \
    app/src/main/jni/main.cpp \
    app/src/main/jni/property.cpp \
    app/src/main/jni/render.cpp
```

Do **not** do:

```bash
git restore --source=upstream/master -- app/src/main/jni/
```

because that can overwrite:

```text
node.cpp
node.h
thumbnail.cpp
```

---

# 15. Check Custom JNI Functions

If upstream introduces JNI cleanup such as:

```cpp
DeleteLocalRef(...)
```

inspect whether the same cleanup is needed in custom functions such as:

```text
commandNode
grabThumbnailFast
```

Do not blindly copy the upstream implementation if it does not contain the
custom functionality.

The goal is:

```text
upstream safety/fixes
        +
mpvlibAndroid custom functionality
```

---

# 16. Verify Android.mk

Compare:

```bash
diff -u \
    ../mpv-android/app/src/main/jni/Android.mk \
    app/src/main/jni/Android.mk
```

Then inspect:

```bash
nvim app/src/main/jni/Android.mk
```

Verify that the custom native files are still included:

```text
node.cpp
thumbnail.cpp
```

Also verify that the required native dependencies remain configured, including
the relevant libav libraries:

```text
avformat
avutil
swscale
avcodec
```

Do not replace `Android.mk` blindly.

---

# 17. Apply Kotlin Changes

The main files are:

```text
BaseMPVView.kt
MPVLib.kt
Utils.kt
KeyMapping.kt
```

Compare them directly:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/BaseMPVView.kt \
    app/src/main/java/is/xyz/mpv/BaseMPVView.kt
```

Repeat for the other files:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/MPVLib.kt \
    app/src/main/java/is/xyz/mpv/MPVLib.kt

diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/Utils.kt \
    app/src/main/java/is/xyz/mpv/Utils.kt

diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/KeyMapping.kt \
    app/src/main/java/is/xyz/mpv/KeyMapping.kt
```

---

# 18. BaseMPVView.kt

`BaseMPVView.kt` is upstream-derived but may contain library-specific changes.

Therefore:

```text
upstream BaseMPVView
        +
local mpvlibAndroid changes
```

must be combined.

Do not blindly overwrite the file.

Review:

```bash
git diff -- app/src/main/java/is/xyz/mpv/BaseMPVView.kt
```

---

# 19. MPVLib.kt

`MPVLib.kt` is especially important because it contains the library API.

Preserve the custom reactive functionality:

```text
propInt
propDouble
propString
logFlow
eventFlow
```

When upstream changes `MPVLib.kt`, manually merge the changes.

Review:

```bash
git diff -- app/src/main/java/is/xyz/mpv/MPVLib.kt
```

---

# 20. Utils.kt

Compare:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/Utils.kt \
    app/src/main/java/is/xyz/mpv/Utils.kt
```

Merge upstream fixes where applicable while preserving library-specific
changes.

---

# 21. KeyMapping.kt

Check:

```bash
diff -u \
    ../mpv-android/app/src/main/java/is/xyz/mpv/KeyMapping.kt \
    app/src/main/java/is/xyz/mpv/KeyMapping.kt
```

If the file has no custom modifications and the upstream change is relevant,
it can generally be synchronized directly.

---

# 22. Preserve Custom Kotlin Files

Never replace:

```text
app/src/main/java/is/xyz/mpv/FastThumbnails.kt
app/src/main/java/is/xyz/mpv/MPVNode.kt
```

with upstream files.

These are library-specific.

After synchronization:

```bash
test -f app/src/main/java/is/xyz/mpv/FastThumbnails.kt
test -f app/src/main/java/is/xyz/mpv/MPVNode.kt
```

---

# 23. Update `cacert.pem`

Compare:

```bash
diff -u \
    ../mpv-android/app/src/main/assets/cacert.pem \
    app/src/main/assets/cacert.pem
```

If upstream changed the certificate bundle and there are no intentional local
changes:

```bash
git restore --source=upstream/master -- \
    app/src/main/assets/cacert.pem
```

---

# 24. Do Not Import the Upstream Application

The upstream project has many files that are intentionally absent from
`mpvlibAndroid`.

Do not copy:

```text
MPVActivity.kt
MainActivity.kt
MainScreenFragment.kt
MPVView.kt
TouchGestures.kt
BackgroundPlaybackService.kt
FilePickerActivity.kt
MPVDocumentPickerFragment.java
MPVFilePickerFragment.java
preferences/
filepicker/
res/
debug/
```

Also do not copy the entire:

```text
app/src/main/
```

directory.

`mpvlibAndroid` is a library, not the complete player application.

---

# 25. Check the Resulting Diff

After applying changes:

```bash
git status
```

Changed files:

```bash
git diff --name-status
```

Statistics:

```bash
git diff --stat
```

Complete diff:

```bash
git diff
```

Whitespace/errors:

```bash
git diff --check
```

---

# 26. Verify Custom Code

Run:

```bash
test -f app/src/main/java/is/xyz/mpv/FastThumbnails.kt
test -f app/src/main/java/is/xyz/mpv/MPVNode.kt
test -f app/src/main/jni/node.cpp
test -f app/src/main/jni/node.h
test -f app/src/main/jni/thumbnail.cpp
```

Then verify the files manually:

```bash
git diff -- \
    app/src/main/java/is/xyz/mpv/FastThumbnails.kt \
    app/src/main/java/is/xyz/mpv/MPVNode.kt \
    app/src/main/jni/node.cpp \
    app/src/main/jni/node.h \
    app/src/main/jni/thumbnail.cpp
```

These files should only appear in the diff if you intentionally modified them.

---

# 27. Verify Build Scripts

Check the important scripts:

```bash
bash -n buildscripts/buildall.sh
bash -n buildscripts/docker-build.sh
```

Check all shell scripts:

```bash
find buildscripts -type f -name '*.sh' -print0 |
while IFS= read -r -d '' file; do
    bash -n "$file" || exit 1
done
```

---

# 28. Build `mpvlibAndroid`

First try:

```bash
cd /home/aryan/dev/mpvlibAndroid
./gradlew assembleRelease
```

For the native Docker build:

```bash
./buildscripts/docker-build.sh
```

Expected AAR:

```text
app/build/outputs/aar/app-release.aar
```

Check:

```bash
ls -lh app/build/outputs/aar/
```

The build must succeed before publishing the new library version.

---

# 29. Final Pre-Commit Review

Check:

```bash
git status
git diff --check
git diff --stat
```

Then stage only the reviewed synchronization files.

Prefer:

```bash
git add buildscripts/
git add app/src/main/jni/<reviewed-files>
git add app/src/main/java/is/xyz/mpv/<reviewed-files>
git add app/src/main/assets/cacert.pem
```

Avoid blindly using:

```bash
git add -A
```

because build output or unrelated changes can accidentally enter the commit.

---

# 30. Review the Staged Diff

Before committing:

```bash
git diff --cached --stat
```

Then:

```bash
git diff --cached
```

Verify:

```text
[ ] upstream build changes are correct
[ ] JNI changes are correct
[ ] BaseMPVView changes are correct
[ ] MPVLib changes preserve Flow/StateFlow
[ ] custom MPVNode code remains
[ ] custom FastThumbnails code remains
[ ] node.cpp remains
[ ] node.h remains
[ ] thumbnail.cpp remains
[ ] Android.mk still includes custom sources
[ ] no upstream-only UI was imported
[ ] no generated build files were staged
```

---

# 31. Commit the Synchronization

Record the exact upstream commit SHA.

Get it with:

```bash
git rev-parse upstream/master
```

For example:

```text
a1b2c3d4e5f6...
```

Commit:

```bash
git commit -m "Sync mpv-android a1b2c3d"
```

Using the upstream SHA makes the next synchronization much easier.

Find previous sync commits later:

```bash
git log --all --oneline --grep='Sync mpv-android'
```

---

# 32. Push the Synchronization Branch

```bash
git push -u origin sync-upstream
```

Review the GitHub branch/PR before merging.

After review, merge it into `master`.

---

# 33. Return to Master

After merging:

```bash
git switch master
git pull origin master
```

Delete the local sync branch:

```bash
git branch -d sync-upstream
```

---

# 34. Release a New `mpvlibAndroid` AAR

After the synchronization is merged, decide on the new library version.

Example:

```text
v0.0.2
```

Create the tag:

```bash
git tag v0.0.2
```

Push:

```bash
git push origin master --tags
```

If the repository's GitHub Actions release workflow is configured, the tag can
trigger the release build.

The release artifact is:

```text
mpv-android-lib-v0.0.2.aar
```

---

# 35. Local Docker AAR Build

If building locally:

```bash
cd /home/aryan/dev/mpvlibAndroid
./buildscripts/docker-build.sh
```

The resulting AAR should be:

```text
app/build/outputs/aar/app-release.aar
```

Copy it to the `mpvium` project:

```bash
cp app/build/outputs/aar/app-release.aar \
    /home/aryan/dev/mpvium/app/libs/mpv-android-lib-v0.0.2.aar
```

---

# 36. Update `mpvium`

Update:

```text
mpvium/app/build.gradle.kts
```

For example:

```kotlin
implementation(files("libs/mpv-android-lib-v0.0.2.aar"))
```

Remove the old AAR if it is no longer required:

```bash
rm /home/aryan/dev/mpvium/app/libs/mpv-android-lib-v<old-version>.aar
```

Then build:

```bash
cd /home/aryan/dev/mpvium
./gradlew assembleStandardDebug
```

---

# 37. Port Application-Level Features to `mpvium`

After synchronizing the library, inspect upstream application changes that were
not copied into `mpvlibAndroid`.

Look at:

```text
mpv-android/app/src/main/java/is/xyz/mpv/
```

especially:

```text
MPVActivity.kt
KeyMapping.kt
Utils.kt
TouchGestures.kt
```

Determine whether any behavior should be reproduced in `mpvium`.

Typical destinations:

```text
mpvium/.../PlayerViewModel.kt
mpvium/.../MPVView.kt
```

Examples:

### New mpv properties/options

Inspect upstream:

```text
MPVActivity.kt
MPVLib.kt
```

Then add the equivalent functionality to the appropriate `mpvium` layer.

### New key mappings

Inspect:

```text
KeyMapping.kt
```

and update `mpvium` key handling where necessary.

### New formats/protocols

Inspect:

```text
Utils.kt
```

including media extensions and protocol handling where relevant.

### Gestures/UI

Do not copy upstream `TouchGestures.kt` wholesale.

Adapt the behavior to the existing Compose architecture.

---

# 38. What Does Not Require a `mpvlibAndroid` Update?

Not every upstream commit requires synchronization.

For example, if upstream changes only:

```text
MPVActivity.kt
res/
preferences/
filepicker/
fastlane/
docs/
```

and the change has no impact on the reusable MPV library, there may be nothing
to change in `mpvlibAndroid`.

The correct result can simply be:

```text
No mpvlibAndroid changes required.
```

Application-level changes can still be reviewed separately for `mpvium`.

---

# 39. Complete Future Sync Procedure

Use this as the normal repeatable process.

```text
1. cd /home/aryan/dev/mpvlibAndroid

2. git switch master

3. git status

4. git pull origin master

5. git fetch upstream

6. Find the last:
       Sync mpv-android <SHA>
   commit.

7. Create:
       sync-upstream
   branch.

8. Inspect:
       LAST_SYNC_SHA..upstream/master

9. Review:
       buildscripts/
       app/src/main/jni/
       app/src/main/java/is/xyz/mpv/
       app/src/main/assets/

10. Classify every change:
       library change
       custom-library conflict
       application-only change
       irrelevant change

11. Apply only relevant upstream changes.

12. Manually merge:
       BaseMPVView.kt
       MPVLib.kt
       Utils.kt

13. Preserve:
       FastThumbnails.kt
       MPVNode.kt
       node.cpp
       node.h
       thumbnail.cpp
       Flow/StateFlow additions

14. Verify Android.mk.

15. Run:
       git diff --check

16. Build:
       ./gradlew assembleRelease

17. Review:
       git diff

18. Stage only reviewed files.

19. Review:
       git diff --cached

20. Commit:
       Sync mpv-android <UPSTREAM_SHA>

21. Push:
       git push -u origin sync-upstream

22. Review/merge.

23. Tag a new mpvlibAndroid version.

24. Build/publish the AAR.

25. Copy the new AAR to:
       mpvium/app/libs/

26. Update mpvium's AAR dependency.

27. Port relevant application-level changes into mpvium.

28. Build:
       ./gradlew assembleStandardDebug

29. Verify the application.
```

---

# 40. Quick Command Reference

## Update upstream

```bash
cd /home/aryan/dev/mpvlibAndroid
git fetch upstream
git log --oneline upstream/master -20
```

## Find previous sync

```bash
git log --all --oneline --grep='Sync mpv-android'
```

## Inspect build changes

```bash
git diff LAST_SYNC_SHA..upstream/master -- buildscripts/
```

## Inspect JNI changes

```bash
git diff LAST_SYNC_SHA..upstream/master -- app/src/main/jni/
```

## Inspect Kotlin changes

```bash
git diff LAST_SYNC_SHA..upstream/master -- \
    app/src/main/java/is/xyz/mpv/
```

## Inspect assets

```bash
git diff LAST_SYNC_SHA..upstream/master -- \
    app/src/main/assets/
```

## Direct file comparison

```bash
diff -u ../mpv-android/<file> <file>
```

## Neovim

```bash
nvim .
```

```vim
:Git
:Gdiffsplit
:DiffviewOpen
```

## Validate

```bash
git diff --check
```

## Build

```bash
./gradlew assembleRelease
```

or:

```bash
./buildscripts/docker-build.sh
```

## Review staged changes

```bash
git diff --cached
```

## Commit

```bash
git commit -m "Sync mpv-android <SHA>"
```

## Push

```bash
git push -u origin sync-upstream
```

## Build consumer app

```bash
cd /home/aryan/dev/mpvium
./gradlew assembleStandardDebug
```

---

# 41. Rules to Remember

## Rule 1 — `mpvlibAndroid` is a selective extraction

Do not treat it as a normal fork.

```text
mpv-android
    ↓ selected code
mpvlibAndroid
```

---

## Rule 2 — Never merge upstream wholesale

Do not run:

```bash
git merge upstream/master
```

This can introduce the entire upstream application and conflicting history.

---

## Rule 3 — Never copy the complete `app/src/main`

Do not run:

```bash
cp -r ../mpv-android/app/src/main/* app/src/main/
```

---

## Rule 4 — Never replace the complete JNI directory

Do not run:

```bash
git restore --source=upstream/master -- app/src/main/jni/
```

---

## Rule 5 — Preserve custom library functionality

Always preserve:

```text
FastThumbnails.kt
MPVNode.kt
node.cpp
node.h
thumbnail.cpp
```

and custom Flow/StateFlow API additions.

---

## Rule 6 — Treat mixed files as merge targets

Especially:

```text
BaseMPVView.kt
MPVLib.kt
Utils.kt
```

---

## Rule 7 — Keep UI/application code out of the library

Upstream application functionality normally belongs in:

```text
mpvium
```

not:

```text
mpvlibAndroid
```

---

## Rule 8 — Record the upstream SHA

Every synchronization commit should identify the upstream version:

```text
Sync mpv-android <SHA>
```

This creates a reliable synchronization history.

---

## Rule 9 — Build before releasing

Never publish a new AAR without successfully building:

```bash
./gradlew assembleRelease
```

or the project's Docker build.

---

## Rule 10 — Review before staging

Prefer:

```bash
git diff
git diff --check
git diff --cached
```

over blindly staging everything.

---

# 42. Final Mental Model

The synchronization should always be thought of as a three-layer process:

```text
                 UPSTREAM
              mpv-android
                    │
                    │
          ┌─────────┴─────────┐
          │                   │
    reusable engine       app features
          │                   │
          ▼                   ▼
   mpvlibAndroid            mpvium
          │
          │ AAR
          ▼
       mpvium
```

Therefore:

```text
Upstream native/JNI/library change
            ↓
      mpvlibAndroid
            ↓
          AAR
            ↓
          mpvium
```

while:

```text
Upstream UI/application change
            ↓
          mpvium
```

The core rule is:

> **Synchronize the reusable MPV layer into `mpvlibAndroid`; adapt
> application-level behavior directly in `mpvium`; never overwrite the custom
> library layer with the complete upstream application.**
