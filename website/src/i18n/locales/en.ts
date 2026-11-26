import type { SiteTranslations } from "../types";

export const en: SiteTranslations = {
  meta: {
    title: "mpvium — Sane defaults, pro-level control",
    description:
      "mpvium — sane defaults, pro-level control. Opinionated for everyday watching, fully customizable for pros. Android video player powered by mpv.",
    featuresTitle: "Features — mpvium",
    featuresDescription:
      "Everything mpvium does: mpv-powered playback, Material 3 UI, PiP, background audio, network streaming, subtitles, and pro-level config.",
    doctrineTitle: "Doctrine — mpvium",
    doctrineDescription:
      "The rules mpvium is built against: sane defaults, pro-level control, playback fidelity, privacy, efficiency, and honesty about what changed.",
    screenshotsTitle: "Screenshots — mpvium",
    screenshotsDescription:
      "mpvium in action: home library, movies grid, details, and the mpv-powered player.",
    changelogTitle: "Changelog — mpvium",
    changelogDescription:
      "What's new in mpvium: the latest user-facing changes, generated from the project changelog.",
    downloadTitle: "Download — mpvium",
    downloadDescription:
      "Download mpvium APKs: stable and preview builds for every CPU architecture.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Page not found.",
  },
  nav: {
    features: "Features",
    doctrine: "Doctrine",
    screenshots: "Screenshots",
    changelog: "Changelog",
    download: "Download",
    openMenu: "Open menu",
    closeMenu: "Close menu",
    languageLabel: "Language",
  },
  hero: {
    badge: "Free & open source · GPL-3.0-or-later · powered by mpv",
    tagline: "Sane defaults, pro-level control.",
    subtitle:
      "Opinionated for everyday watching, fully customizable for pros — mpv power without the config-file homework. Material 3 interface, background playback, picture-in-picture.",
    downloadStable: "Download Stable (arm64-v8a)",
    checkingPreview: "Checking preview…",
    resolvingVersions: "Resolving latest versions…",
    downloadStableTag: "Download Stable {tag} (arm64-v8a)",
    downloadPreviewTag: "Download Preview {tag} (arm64-v8a)",
    noPreview: "No preview build",
    latestMeta: "Latest: {parts}",
    releasesUnavailable: "Releases available on GitHub.",
    logoAlt: "mpvium logo",
    screenshotAlt: "mpvium player screenshot",
  },
  why: {
    heading: "Why mpvium",
    introPrefix: "Most mpv frontends stop at the engine. Most video players stop at the library.",
    introHighlight: "mpvium is the overlap",
    introSuffix:
      "— libmpv playback, a real Shows & Movies library, and touch-first controls, without giving up the pro settings.",
    col1Eyebrow: "Many mpv forks",
    col1Title: "Engine first",
    col1Body: "Playback power and config freedom, but the library and touch flow are left to you.",
    col1Bullets: [
      "Full mpv playback engine",
      "mpv.conf, Lua scripts and config",
      "Minimal, file-browser-first interface",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Both, by default",
    col2Body: "The same libmpv engine, wrapped in a complete library and a player designed for touch.",
    col2Bullets: [
      "Automatic Shows & Movies library",
      "Material 3 Expressive, 16 themes",
      "mpv.conf, Lua and decoder modes",
      "Intro skip, Explain, online subtitles",
    ],
    col3Eyebrow: "Many video players",
    col3Title: "Library first",
    col3Body: "A familiar library and controls, but the engine and fine-tuning are mostly fixed.",
    col3Bullets: [
      "Polished library and playback",
      "Touch-friendly interface",
      "Preset engine and codec controls",
    ],
    engineHeading: "The engine difference",
    engineCompare: "Visual comparisons at mpv.rocks ↗",
    engineBody:
      "Playback runs through libmpv, the same rendering engine as mpv — not a generic Android decoder.",
    engineBenefits: [
      {
        title: "Colors as intended",
        description:
          "Source color metadata is respected, with HDR tone mapping for your screen — no fake saturation, contrast or sharpening.",
      },
      {
        title: "Debanding built in",
        description: "CPU or GPU debanding smooths stepped gradients in skies, fog and dark scenes.",
      },
      {
        title: "Detail that holds up",
        description:
          "High-quality scaling, chroma upsampling and deinterlacing keep edges and motion clean.",
      },
      {
        title: "Subtitles as authored",
        description:
          "ASS/SSA fonts, colors, positioning and effects render the way the release intended.",
      },
    ],
  },
  home: {
    featuresHeading: "Features",
    featuresSubtitle: "Everything you need up front, nothing in your way.",
    exploreFeatures: "Explore all features →",
    shotsHeading: "Screenshots",
    shotsSubtitle: "Library, browsing, playback — dark default theme.",
    viewGallery: "View gallery →",
    themesHeading: "Themes",
    themesSubtitle: "Every theme in the app — all 16. Pick one — this whole page re-skins like the app.",
    ctaHeading: "Ready to watch",
    ctaBody:
      "Grab the latest APK from GitHub Releases, or follow development in the Discord, Telegram, and Reddit communities.",
    ctaGet: "Get mpvium",
    ctaDiscord: "Join Discord",
    ctaTelegram: "Join Telegram",
    ctaReddit: "Join Reddit",
  },
  featuresPage: {
    heading: "Features",
    subtitle: "Sane defaults up front, pro-level control when you want it.",
    andMore: "And more",
    items: [
      {
        title: "Auto-Categorized Library",
        description:
          "Your files sorted into Shows & Movies automatically — posters, Up Next and Recently Added rails included.",
      },
      {
        title: "Auto Skip Intro & Recap",
        description:
          "One-tap skip buttons in the player, plus an offline cache that skips intros for you.",
      },
      {
        title: "Explain Button",
        description: "Tap any word in the dialogue for dictionary lookups and Wikipedia explanations.",
      },
      {
        title: "Library Insights",
        description:
          "Collection size, storage, runtime, watch progress and weekly activity in one dashboard.",
      },
      {
        title: "Modern Material 3 UI",
        description: "Expressive motion, morphing controls, and 16 themes with light & dark modes.",
      },
      {
        title: "Subtitles & Online Search",
        description:
          "External tracks, OpenSubtitles and optional SubSource search, styling, and pinch-to-resize.",
      },
      {
        title: "Advanced Config",
        description: "Full mpv.conf, input.conf and Lua script support.",
      },
      {
        title: "Picture-in-Picture",
        description: "Keep watching while you multitask.",
      },
      {
        title: "Background Playback",
        description: "Audio-only mode for music and podcasts.",
      },
      {
        title: "Network Streaming",
        description: "Open URLs plus SMB, FTP and WebDAV.",
      },
      {
        title: "File Management",
        description: "Folder & tree views, playlists, history, resume.",
      },
      {
        title: "Private & Ad-Free",
        description: "No trackers, no ads, no unnecessary permissions.",
      },
    ],
    moreItems: [
      "Hardware-accelerated, high-quality playback through libmpv",
      "Gesture controls, zoom, and screen orientation controls",
      "Chapters, playlists, playback history, and resume support",
      "Local media browsing with folder and tree views",
      "Custom mpv configuration, scripts, and advanced playback options",
      "Media information and metadata caching",
    ],
  },
  screenshotsPage: {
    heading: "Screenshots",
    subtitle: "Library, browsing, playback — dark default theme.",
    morePrefix: "Looking for every theme and light mode? The full collection lives in",
    moreLink: "docs/screenshots",
    moreSuffix: "in the repo.",
    shots: [
      { alt: "mpvium player", title: "Player", caption: "mpv power, touch-friendly controls." },
      {
        alt: "mpvium player with skip intro",
        title: "Skip intro",
        caption: "One tap past intros and recaps.",
      },
      { alt: "mpvium home library", title: "Home", caption: "Continue watching & browse." },
      { alt: "mpvium titles grid", title: "Titles", caption: "Shows and movies in one grid." },
      { alt: "mpvium library", title: "Library", caption: "Shows, movies & folders." },
      { alt: "mpvium movie details", title: "Details", caption: "Movie showcase view." },
      { alt: "mpvium welcome tour", title: "Welcome", caption: "First-run onboarding tour." },
    ],
  },
  downloadPage: {
    heading: "Download",
    subtitle:
      "Free and open source, no ads or trackers. Download buttons resolve to the latest arm64-v8a APKs; preview is enabled when a preview release exists.",
    tableHeading: "APK downloads",
    colApk: "APK",
    colDevices: "Devices",
    apks: [
      { file: "universal", devices: "Most devices; larger download" },
      { file: "arm64-v8a", devices: "Modern 64-bit ARM devices; recommended for most phones" },
      { file: "armeabi-v7a", devices: "Older 32-bit ARM devices" },
      { file: "x86", devices: "32-bit Intel/AMD devices" },
      { file: "x86_64", devices: "64-bit Intel/AMD devices" },
    ],
    footerPrefix: "Preview builds are intended for testing and may contain unfinished features. Checksums are published on each",
    footerRelease: "release",
    footerMiddle: ". Prefer building from source? See the",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Generated from",
    readFile: "Read the file ↗",
    linkTo: "Link to",
  },
  changelogPage: {
    heading: "What's New",
    subtitlePrefix: "Full release notes with checksums are on the",
    subtitleLink: "releases page",
    subtitleSuffix: ".",
    empty: "No changelog entries yet.",
  },
  notFoundPage: {
    title: "This reel is missing.",
    body: "The page you're looking for doesn't exist.",
    backHome: "Back home",
  },
  footer: {
    releases: "Releases",
    doctrine: "Doctrine",
    abiNote: "Stable and preview APKs are arm64-v8a. Other ABIs on the releases page.",
  },
};
