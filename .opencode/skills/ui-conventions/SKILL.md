---
name: mpvium UI & theming
description: Add or change Compose UI in mpvium using its existing Material 3 Expressive design system. Use for any screen, dialog, sheet, setting, card, animation, haptic, inset, RTL, string, or theme change, or when asked to make new UI match the rest of the app.
---

# UI & theming conventions

The app already has one design system. Reuse it; never introduce a parallel set of colors,
spacing, radii, or components. When in doubt, open the nearest existing screen and match it.

## Tokens

- `MpviumTheme` is applied once per Activity. Never re-provide `MaterialTheme` deeper.
- **Colors**: `MaterialTheme.colorScheme.*`. No hex literals or `Color.White.copy(alpha=…)` in
  screens.
- **Spacing**: `MaterialTheme.spacing.*` (import `app.aryan447.mpvium.ui.theme.spacing`):
  `extraSmall` 4, `smaller` 8, `small` 12, `medium` 16, `large` 24, `larger` 32,
  `extraLarge` 48, `largest` 64. Screen horizontal padding is 16.dp.
- **Shapes**: `MaterialTheme.shapes.*`; in settings use `SettingsCardShape` (28.dp),
  `SettingsTileShape` (24→32.dp pressed), `SettingsIconBoxCorner` (18.dp) from
  `ui/preferences/CardPreferences.kt`.
- **Type**: stock `AppTypography`. Monospace only for timecodes and MediaInfo values.
- **Theme state**: `LocalAppTheme.current` (15 themes), `LocalGlass.current` (Glass theme),
  `LocalSpacing`. Never key app colors off `isSystemInDarkTheme()` — it disagrees with the
  user's Light/Dark/System and AMOLED choice.

Radii in use — pick the closest existing value, don't invent one: 28 cards/dialogs, 32 pills
and connected button groups, 24 settings tiles, 20 glass cards and top-bar bottom corners, 16
glass sheets, 14 posters, 12 thumbnails, 8 chips, 4 badges, `CircleShape` for icon bubbles.

## Reuse before writing

`references/components.md` indexes every shared composable with its file. Highlights:

- Settings: `PreferenceCard` + `PreferenceSectionHeader` + `PreferenceDivider`; rows via
  `HapticSwitchPreference`, `ExpressiveListPreference`, `SettingsPreferenceRow`; icons via
  `PreferenceRowIcon`.
- Choices: `ExpressiveChoiceRow` (connected toggle group). No dropdowns for 2–4 short options.
- Dialogs: `ConfirmDialog`. Sheets: `ModalBottomSheet` for app screens, `PlayerSheet` only in
  the player.
- States: `BrowserStateMessage`, `EmptyState`, `LoadingState`, `PermissionDeniedState` — never
  hand-roll an empty or loading view.

A new setting or screen is not finished until it is registered:

1. `SearchablePreferences.allPreferences` (`ui/preferences/SearchablePreference.kt:25`).
2. `rememberDashboardSections()` (`ui/preferences/PreferencesScreen.kt:219`) when it needs a
   dashboard entry.

## Motion

- Every duration and curve comes from `MaterialTheme.motionScheme`: `defaultSpatialSpec()`,
  `defaultEffectsSpec()`, `fastSpatialSpec()`, `fastEffectsSpec()`. No `tween()`/`spring()` in
  new UI.
- Navigation is fade-through + scale: enter `fadeIn(defaultEffects) + scaleIn(0.94,
  defaultSpatial)`, exit `fadeOut(fastEffects) + scaleOut(1.04, fastSpatial)`.
- Signature press-morph: scale 0.965–0.98, corners grow (24→32 tile, 18→26 icon medallion),
  `indication = null`, applied via `graphicsLayer` inside `clip(shape)`.
- Expand/collapse: `animateContentSize()` plus a 180° chevron rotation.

## Feedback (haptics)

`LocalHapticFeedback.current` directly — there is no wrapper.

| Interaction | Type |
| --- | --- |
| Confirm, navigate, switch, tab change, theme pick | `LongPress` |
| Slider drag / scrub | `TextHandleMove` |
| Selection in a connected choice group | `SegmentTick` |

Fire on the state-changing click only, and stay silent when the value is already selected.

## Insets and background

- Screens use `Scaffold(containerColor = Color.Transparent)` and consume `padding`; the theme
  glow paints the background. Never paint a screen background yourself.
- Floating surfaces outside the Scaffold: `windowInsetsPadding(WindowInsets.systemBars)`, plus
  `LocalNavigationBarHeight` when a FAB or bar can collide with the nav bar.
- Player overlays: `WindowInsets.statusBars` on top, `navigationBars.asPaddingValues()` at the
  bottom; sheets pad `Top + Horizontal`, then `imePadding()`.
- Never hardcode status/nav bar heights.

## RTL

- Use `start`/`end`, `Arrangement.Start/End`; no `left`/`right` padding.
- Directional icons: `Icons.AutoMirrored.*` (`ArrowBack`, `ArrowForwardIos`, `Sort`, `List`,
  `PlaylistAdd`, `DriveFileMove`, `ViewQuilt`).
- The player is deliberately LTR-locked (`PlayerControls.kt`). Do not "fix" it.

## Strings

- All user-facing text **and** `contentDescription` go to
  `app/src/main/res/values/strings.xml`, prefixed `pref_`, `player_`, `theme_`, `generic_`.
- URLs, brand names, and format templates: `translatable="false"`.
- Only `en-US` ships. Do not add a locale directory without a complete translation.
- Older screens still hold hardcoded literals (e.g. `FloatingBottomBar`). Don't copy them;
  convert the ones you touch.

## Screen skeleton

```kotlin
@Serializable
object ExampleScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val prefs = koinInject<ExamplePreferences>()
    val vm: ExampleViewModel = viewModel(factory = ExampleViewModel.factory(context.applicationContext as Application))
    val state by vm.state.collectAsState()

    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar(...) }) { padding ->
      LazyColumn(Modifier.fillMaxSize().padding(padding)) { /* … */ }
    }
  }
}
```

- Routes are `@Serializable object`/`data class` implementing `Screen`; push with
  `LocalBackStack.current.add(...)`.
- ViewModels are `AndroidViewModel + KoinComponent` with `by inject()` and
  `companion object { fun factory(application) }`. Do not introduce `koinViewModel`.
- Preferences write synchronously (`prefs.x.set(value)`) and read with the project's
  `collectAsState()` extension from `preferences/preference/Preference.kt`.
- Private helpers in the same file are named `XxxContent`, `XxxRow`, `XxxCard`.

## Checklist

- [ ] Only existing tokens, radii, and components used
- [ ] Light + dark + AMOLED + one non-default theme; Glass too if shared chrome changed
- [ ] Phone and wide/tablet layout, RTL, gesture and 3-button navigation
- [ ] Text in resources, no new hardcoded `contentDescription`
- [ ] Setting registered in `allPreferences` (+ dashboard section if needed)
- [ ] `CHANGELOG.md` entry
