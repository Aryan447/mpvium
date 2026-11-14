# Shared composables

Paths are relative to `app/src/main/java/app/aryan447/mpvium/`. Check this list before writing
a new component; if something is genuinely missing, put it in the same package family as its
neighbours.

## `presentation/components/`

| Composable | File | Use |
| --- | --- | --- |
| `ConfirmDialog` | `ConfirmDialog.kt` | Confirmation dialog; `BasicAlertDialog`, `shapes.extraLarge`, LongPress haptic on confirm |
| `ExpandableCard` | `ExpandableCard.kt` | Card with rotating chevron + `animateContentSize`, glass-aware |
| `ExposedTextDropDownMenu` | `ExposedTextDropDownMenu.kt` | Read-only text field + exposed dropdown |
| `ExpressiveChoiceRow` | `ExpressiveChoiceRow.kt` | Connected toggle group; `SegmentTick` haptic; `equalWidth = true` for 2–4 short labels |
| `OutlinedNumericChooser` | `OutlinedNumericChooser.kt` | −/+ numeric field with range validation |
| `RepeatingIconButton` | `RepeatingIconButton.kt` | Press-and-hold auto-repeat button |
| `SliderItem`, `VerticalSliderItem` | `SliderItem.kt` | Labelled horizontal/vertical sliders, `TextHandleMove` haptic |
| `TintedSliderItem` | `TintedSliderItem.kt` | Slider tinted from a base color |
| `PlayerSheet` | `PlayerSheet.kt` | Player-only bottom sheet (anchored draggable). Use `ModalBottomSheet` elsewhere |
| `PullRefreshBox` | `pullrefresh/PullRefreshBox.kt` | Pull-to-refresh wrapper with glass-aware indicator |
| `LeftSideOvalShape`, `RightSideOvalShape` | `OvalBox.kt` | Double-tap seek ovals |

## `ui/preferences/`

| Composable | File | Use |
| --- | --- | --- |
| `PreferenceCard` | `CardPreferences.kt` | Settings group card (28.dp, tonal, hairline border, 0 elevation) |
| `PreferenceDivider` | `CardPreferences.kt` | Row divider (0.75.dp, 20.dp start inset) |
| `PreferenceSectionHeader` | `CardPreferences.kt` | Uppercase eyebrow header with optional count pill |
| `SettingsTopBar` | `CardPreferences.kt` | Standard settings top bar |
| `SettingsFlexibleTopBar` | `CardPreferences.kt` | Collapsing hero bar for the dashboard |
| `PreferenceIconBox`, `PreferenceRowIcon` | `CardPreferences.kt` | Tonal icon medallion; `PreferenceRowIcon` is required in `icon = { }` slots |
| `HapticSwitchPreference` | `CardPreferences.kt` | Switch row with LongPress haptic |
| `SettingsPreferenceRow` | `CardPreferences.kt` | Navigation row with chevron bubble |
| `SettingsCategoryPill` | `CardPreferences.kt` | Small uppercase pill |
| `SettingsSearchResultRow` | `CardPreferences.kt` | Search result card |
| `ExpressiveListPreference` | `ExpressiveListPreference.kt` | Single-choice row that opens `ExpressiveChoiceSheet` (radio list) |
| `ThemePicker`, `ThemePreviewCard` | `components/ThemePicker.kt`, `components/ThemePreviewCard.kt` | Theme chooser with mini previews |
| `PlayerButtonChip` | `components/PlayerButtonChip.kt` | Player button editor chip |

## `ui/browser/`

| Composable | File | Use |
| --- | --- | --- |
| `BrowserTopBar` | `components/BrowserTopBar.kt` | Normal and selection top bar; glass and bottom-rounded when scrolled |
| `BrowserBottomBar` | `components/FloatingBottomBar.kt` | Floating selection action bar (copy/move/rename/delete) |
| `BrowserStateMessage`, `EmptyState`, `LoadingState`, `PermissionDeniedState` | `states/` | Standard empty/loading/error states |
| `VideoCard`, `FolderCard`, `PlaylistCard`, `M3UVideoCard`, `Network*Card` | `cards/` | List/grid cards with press-morph |
| `RenameDialog`, `DeleteConfirmationDialog`, `SortDialog`, `AddToPlaylistDialog`, `FilePickerDialog`, `FolderPickerDialog`, `MarkAsDialog`, `CopyPasteDialog` | `dialogs/` | Standard dialogs |
| `PlaylistActionSheet`, `PlayLinkSheet` | `sheets/` | `ModalBottomSheet` actions |
| `FabScrollHelper.trackScrollForFabVisibility` | `fab/FabScrollHelper.kt` | Hide FABs on scroll |

## `ui/streaming/components/`

`StreamingHeroBanner`, `MoviePosterCard`, `SeriesPosterCard`, `ContinueWatchingRow`,
`ContinueWatchingCard`, `LibraryFilterBar`, `StreamingImage` — use these for Home, Movies, and
Shows surfaces.

## `ui/theme/`

| Symbol | File | Notes |
| --- | --- | --- |
| `MpviumTheme`, `DarkMode`, `LocalAppTheme` | `Theme.kt` | Single theme entry point; `MotionScheme.expressive()` is set here |
| `AppTheme` | `AppTheme.kt` | 17 themes plus generated light/dark/AMOLED schemes |
| `Spacing`, `LocalSpacing`, `MaterialTheme.spacing` | `Spacing.kt` | The only spacing tokens |
| `AppTypography` | `Type.kt` | Stock M3 typography |
| Glass kit | `Glass.kt` | `LocalGlass`, `glassChrome`, `glassSheen`, `glassRimStroke`, `glassCardColors`, `glassSheetContainerColor`, `glassMenuContainerColor` |
| `Modifier.themeGlow()` | `AmbientGlow.kt` | Top-edge accent bloom painted by the root layout |
| Cinema extras | `CinemaFilmReel.kt` | Cinema-theme-only decorations |
