# Official mpvium Reddit Banners

This directory contains the official desktop and mobile community banners designed for **r/Mpvium**, representing the open-source Android media player built on libmpv.

---

## Deliverables & Directory Structure

```text
design/
└── reddit/
    ├── README.md               # Design documentation, safe areas, and build guide
    ├── desktop-banner.png      # 2560 × 512 px PNG (Aspect ratio 5:1)
    ├── mobile-banner.png       # 1600 × 480 px PNG (Aspect ratio 10:3)
    ├── export.js               # Reproducible generator and export script
    └── source/
        ├── desktop-banner.svg  # High-resolution standalone vector source (Desktop)
        └── mobile-banner.svg   # High-resolution standalone vector source (Mobile)
```

---

## 1. Specifications & Dimensions

| Asset | File | Dimensions | Aspect Ratio | Target Display |
| :--- | :--- | :--- | :--- | :--- |
| **Desktop Banner** | `desktop-banner.png` | **2560 × 512 px** | **5:1** | Reddit Web / Desktop (sh.reddit & new reddit) |
| **Mobile Banner** | `mobile-banner.png` | **1600 × 480 px** | **10:3** | Reddit Mobile App (Android & iOS) |

---

## 2. Brand Identity & Visual Direction

Both banners embody mpvium's core doctrine: **"Sane defaults, pro-level control."**

- **Atmosphere:** Deep obsidian and cinematic indigo canvas (`#08060D` to `#140A24`) with subtle radial ambient glow (`#8A2BE2` violet and `#00D2FF` cyan).
- **Cinematic Edge:** A refined film-strip track along the top border with subtle perforation accents and a multi-stop light horizon (`#FF416C` &rarr; `#8A2BE2` &rarr; `#00D2FF`), reflecting media playback DNA.
- **Official Brand Mark:**
  - Official squircle icon with soft drop shadow and violet ambient back-glow.
  - Bold neo-grotesque wordmark: crisp `#FFFFFF` for `mpv` and the signature streaming linear gradient for `ium`.
- **Authenticity:** Built exclusively using real Android screenshots and official project assets. No generic AI art, no fake mockups, no neon clutter.

---

## 3. Dedicated Compositions

The desktop and mobile banners are independently designed for their respective viewports:

### Desktop Banner (2560 × 512 px)
- **Left Column (Identity & Features):**
  - Category pill: `FREE & OPEN SOURCE · POWERED BY LIBMPV` with active indicator.
  - Official mpvium squircle icon + 76px bold wordmark + `v1.5 · APP` edition badge.
  - Tagline: `Sane defaults, pro-level control.` (lavender highlight).
  - Clear sub-descriptor explaining the app's value proposition.
  - 5 technical feature badges: `Libmpv Playback`, `Material 3 UI`, `Network Streaming`, `mpv.conf & Lua`, and `No Ads · No Trackers`.
- **Right Column (Duo Device Showcase):**
  - **Portrait Phone (Background):** Showcases the auto-categorized poster library and browse folders (`mpvium-dark-default-home.jpg`), illustrating **"Sane defaults"**.
  - **Landscape Flagship Phone (Foreground):** Highlights the video player in action (`mpvium-player.jpg`) with progress bar, play/pause controls, PiP, and skip buttons, illustrating **"Pro-level control"**.
  - Titanium bezel frame, precision corner curvature, front camera cutout, glass reflection sweep, and realistic multi-stage drop shadows.

### Mobile Banner (1600 × 480 px)
- **Tailored for Small Screens:** Avoids squishing the desktop layout into mobile proportions.
- **Left Column (High Legibility):**
  - Category badge: `LIBMPV · ANDROID MEDIA PLAYER`.
  - Prominent 84px squircle app icon with 64px bold wordmark and `v1.5` tag.
  - Large 25px tagline for immediate readability at hand-held sizes.
  - 3 high-contrast pill badges: `Libmpv Core`, `Full mpv.conf`, and `100% Private`.
- **Right Column (Hero Player):**
  - A single landscape flagship phone showcasing the video player interface.
  - Balanced side margins (90px left, 92px right) preventing edge clipping.

---

## 4. Reddit Safe Areas & Cropping Considerations

Reddit applies dynamic cropping depending on screen width, client version, and device type:

### Mobile Safe Areas
```text
┌────────────────────────────────────────────────────────────────────────┐
│ [Back]                       TOP SAFE MARGIN (>50px)         [Search] │
│                                                                        │
│   [Badge]                                  ┌───────────────────────┐   │
│   [Icon] mpvium                            │                       │   │
│   Sane defaults, pro-level control.        │   Landscape Player    │   │
│   Free & open source · Material 3          │   Interface Mockup    │   │
│   [Pill 1] [Pill 2] [Pill 3]               │                       │   │
│ ┌───────────────┐                          └───────────────────────┘   │
│ │ Reddit Avatar │           BOTTOM SAFE MARGIN (>35px)                 │
│ │ Overlap Zone  │                                                      │
└─┴───────────────┴──────────────────────────────────────────────────────┘
```
- **Subreddit Avatar Collision:** On Reddit mobile apps, the circular community avatar overlays the bottom-left corner of the banner (approx. `x: 0–220`, `y: 320–480`). All typography, the app icon, and badges are positioned safely above `y = 350`, ensuring zero avatar overlap.
- **Top Bar Controls:** Back and Search buttons overlay the top ~45px. Safe content starts at `y = 58px`.
- **Responsive Width:** Content stays within `x: 90px` to `x: 1508px`, preventing clipping on narrow smartphone screens.

### Desktop Safe Areas
- **Horizontal Centering:** Critical brand lockup and devices are contained within the central `1920px` width (`x: 180px` to `x: 2342px`).
- **Vertical Breathing Room:** Generous `52px` top and `46px` bottom margins allow flexible rendering across laptop screens, high-DPI monitors, and ultra-wide viewports.

---

## 5. Source Assets Used

All graphics reuse existing repository assets:

- **Logo & App Icon:**
  - `docs/icon.png` (512 × 512 px)
  - `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - `app/src/main/res/drawable/ic_launcher_background.xml`
  - `app/src/main/res/drawable/ic_mpvium_mark.xml`
- **Application Screenshots:**
  - `docs/screenshots/mpvium-player.jpg` (2340 × 1080 landscape playback)
  - `docs/screenshots/mpvium-dark-default-home.jpg` (1080 × 2340 portrait library)
- **Theme Palette:**
  - Default / Dynamic theme: `#E8B5EF`, `#D0BCFF`, `#C9B0FF`
  - Streaming mark gradient: `#FF416C` &rarr; `#8A2BE2` &rarr; `#00D2FF`
  - Canvas background: `#08060D`, `#12091F`, `#0B0613`
  - Muted text: `#A8ADB9`, `#DDD5E8`

---

## 6. How to Re-Export

The export pipeline is reproducible and uses the existing `sharp` package in the workspace:

```bash
# Rebuild both SVG sources and export to high-resolution PNGs
node design/reddit/export.js
```

Both `.svg` files in `source/` are self-contained (using embedded high-resolution asset data) and can be opened or edited in Figma, Inkscape, Illustrator, or any standard vector design tool.
