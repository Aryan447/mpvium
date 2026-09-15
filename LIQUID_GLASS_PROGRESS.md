# Liquid Glass Progress — resumable tracker

Goal: Optional clear iOS 26-style Liquid Glass theme beside Cinema / Noir, chrome-first.

Decisions (locked):
- Blur core: Haze 1.5.3 (verified latest on Maven Central; monolithic 1.x API). Runner-up true-lens libs (Abdullajon1881 LiquidGlass 1.0.0, Haze 2.x `haze-glass` beta) are NOT on Central — revisit when published.
- Theme model: new `AppTheme.LiquidGlass` entry (auto-appears in picker), not an overlay toggle.
- Coverage: chrome first (bottom nav pill, top bars, floating bar, player panels/chips). Cards/lists later.
- Look: CLEAR (translucent ~35-55%, blur ~20-24dp, subtle tint + rim), not frosted-opaque.

File index:
- `gradle/libs.versions.toml` — version catalog (+haze 1.5.3)
- `app/build.gradle.kts` — app deps (+implementation(libs.haze))
- `app/src/main/java/app/aryan447/mpvium/ui/theme/AppTheme.kt` — LiquidGlass entry + AMOLED guard
- `app/src/main/java/app/aryan447/mpvium/ui/theme/LiquidGlass.kt` — NEW core kit (tokens, LocalLiquidGlass, glassBackdrop/glassChrome/glassHazeStyle)
- `app/src/main/java/app/aryan447/mpvium/ui/theme/Theme.kt` — provide LocalLiquidGlass
- `app/src/main/java/app/aryan447/mpvium/MainActivity.kt` — transparent scrims for glass
- `app/src/main/java/app/aryan447/mpvium/ui/browser/MainScreen.kt` — pill nav glass + content backdrop source
- `app/src/main/java/app/aryan447/mpvium/ui/browser/components/BrowserTopBar.kt` — top bar glass (both modes)
- `app/src/main/java/app/aryan447/mpvium/ui/browser/components/FloatingBottomBar.kt` — selection bar glass
- `app/src/main/java/app/aryan447/mpvium/ui/player/controls/PlayerPanels.kt` — panelCardsColors glass alpha
- `app/src/main/java/app/aryan447/mpvium/ui/player/controls/components/ControlsButton.kt` — clearer chips over video
- `app/src/main/java/app/aryan447/mpvium/ui/preferences/components/ThemePreviewCard.kt` — frosted preview
- `app/src/main/java/app/aryan447/mpvium/ui/preferences/AppearancePreferencesScreen.kt` — AMOLED guard
- `app/src/main/java/app/aryan447/mpvium/ui/preferences/SearchablePreference.kt` — glass keywords
- `app/src/main/res/values/strings.xml` — `theme_liquid_glass` + onboarding copy

Steps:
- [x] Step 0: Create this progress file
- [x] Step 1: Add Haze 1.5.3 dependency (catalog + app build; version verified on Central)
- [x] Step 2: Add AppTheme.LiquidGlass entry + string + preview
- [x] Step 3: Create ui/theme/LiquidGlass.kt core kit
- [x] Step 4: Wire LocalLiquidGlass in Theme.kt + MainActivity scrims
- [x] Step 5a: Nav pill + content backdrop glass (MainScreen — true live blur)
- [x] Step 5b: Top bars glass (BrowserTopBar normal + selection — frost until 5e)
- [x] Step 5c: Floating selection bar glass (FloatingBottomBar — frost until 5e)
- [x] Step 5d: Player glass (panelCardsColors 0.55 + ControlsButton 0.35; native video surface can't be Haze-sampled so frost is correct)
- [x] Step 6: Settings polish (AMOLED disabled for glass, keywords, onboarding)
- [x] Step 7 (local): diff-check PASS, rg audit 7 call sites. CI build + device matrix pending.
- [ ] Step 5e (optional): per-screen HazeState refactor for live top-bar/floating-bar blur
- [ ] Step 8 (later): lens swap (Abdullajon1881 or Haze `haze-glass`) once published to Central

Notes:
- Top-bar / floating-bar `glassHaze` states have no `hazeSource` behind them yet → translucent frost until 5e (each list screen owns a shared HazeState, bar as sibling above provider).
- Haze 2.x (`haze-blur`/`haze-glass`, `GlassStyle.clear`) is beta and `haze-glass` is NOT on Central (verified 2026-09-15). Abdullajon1881 1.0.0 README says Central "configured but not yet released". `LiquidGlass.kt` isolates all glass calls for a later swap.

Verification log:
- 2026-09-15 Steps 1-4: catalog + app build, LiquidGlass theme/AMOLED/preview, core kit, LocalLiquidGlass, scrims.
- 2026-09-15 Step 5a: MainScreen pill transparent + glassChrome; content Box glassBackdrop.
- 2026-09-15 Steps 5b-6: BrowserTopBar, FloatingBottomBar, panelCardsColors, ControlsButton, AMOLED guard, keywords, onboarding. `git diff --check` PASS.
- 2026-09-15 Version/API audit: Central latest Haze = 1.5.3 (1.7.x NOT published; corrected pin). 1.5.3 monolithic API matches `LiquidGlass.kt` (`HazeStyle(backgroundColor, blurRadius)`, `hazeEffect(state, style)`, `hazeSource(state)`, `rememberHazeState()`).
- 2026-09-15 CI fix: merge of main into style/liquid-glass (7aac19d) dropped `haze = "1.5.3"` from `[versions]` while library entry still referenced it. Restored in eedf12d; pushed to PR #56; CI re-run pending.

Resume:
- ▶ NEXT: CI re-run (PR #56 head now at eedf12d with haze version restored); then manual matrix (API 26 fallback, 31-32 blur, 33+ full; light/dark; pill/flat; player; rotation/tablet). Optional 5e afterwards.
