# The mpvium doctrine

mpvium exists because watching video should be good before you change a setting, and
still yours after you change all of them. These are the rules the app is built
against — the standard a change has to meet before it ships.

## Sane defaults are the product

The first launch is the product, not a demo. A new install opens on a library that
already reads Shows, Movies, and folders, and a player that looks right without
touching a setting. Every default is chosen the way most people would choose it if
they knew the options existed.

A default that needs explaining is the wrong default.

## Pro control is never taken away

Sane defaults only earn the name when nothing is locked behind them. mpv.conf, Lua
scripts, decoder modes, audio and subtitle tracks, and the full player control set
stay one trip to Settings away. Automation is a starting point that can be turned
off, never a replacement for the manual.

## Playback fidelity beats enhancement

mpvium renders with libmpv and trusts the file. Source color metadata is respected,
HDR is tone-mapped for your screen, debanding and scaling work on the picture instead
of inventing one, and subtitles keep the fonts, colors, and positioning they were
authored with. Enhancement never substitutes for correctness.

## Detail is not decoration

Spacing, corner radii, elevation, motion, haptics, and typography come from the
design system, not from guesswork. Screens respect status, navigation, and gesture
insets, mirror correctly in RTL, and are checked in both light and dark themes. A
near-duplicate value is a bug, and "close enough" is not a review pass.

## Beauty is a requirement

This is an app people open in the dark, for hours, when they are not working. It
should look the part. Layouts are composed rather than assembled, motion explains
what changed instead of decorating the screen, haptics confirm what a finger cannot
see, and a theme recolors the whole app rather than tinting it. A settings page is
not a debug console, and an empty state is not an apology.

"Works" is not the bar; "looks inevitable" is. Beauty never buys frames, battery,
or clarity — it is how the app earns being opened again.

## Efficiency is a feature

Every release is judged on battery and on time to first frame. Playback, scanning,
and the browsing interface avoid work they do not need, and a change that trades
power for polish has to say so out loud. On a phone that is warm in the hand,
restraint is the feature.

## One app, every screen

Phones and tablets are the same product. Layouts, the player, the seekbar, and the
sheets use the room a large screen gives them instead of stretching phone UI across
it. Small screens are the constraint, not the only target.

## Boring integrity

mpvium ships without advertisements. It is GPL-3.0-or-later, built on mpv, FFmpeg,
and mpv-android, and it says so. Preview builds are labeled preview, because
presenting unfinished work as finished is a lie to the people testing it.

## Privacy by subtraction

There is no account to create, no analytics SDK, no advertising library, and no
cloud library holding a copy of your files. The network is used for what you asked
for — network playback, online subtitles, an update check — and local media,
playback progress, and history stay on the device.

Privacy is mostly what an app declines to collect. Permissions are requested when
the feature that needs them is first used, never gathered up front "just in case",
and a feature that cannot justify a permission ships without it.

## Measure, then claim

"This is faster" is not a changelog entry. Battery, load time, file size, and frame
timing get a number, and the number comes from a measurement rather than a feeling.
When the number is not there yet, we describe what changed and leave the claim out.
