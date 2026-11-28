import type { SiteTranslations } from "../types";

export const de: SiteTranslations = {
  meta: {
    title: "mpvium — Sinnvolle Standards, Profi-Kontrolle",
    description:
      "mpvium — sinnvolle Standards, Profi-Kontrolle. Meinungsstark für den Alltag, vollständig anpassbar für Profis. Android-Videoplayer auf Basis von mpv.",
    featuresTitle: "Funktionen — mpvium",
    featuresDescription:
      "Alles, was mpvium kann: Wiedergabe mit mpv, Material 3-Oberfläche, PiP, Hintergrund-Audio, Netzwerk-Streaming, Untertitel und Profi-Konfiguration.",
    doctrineTitle: "Doktrin — mpvium",
    doctrineDescription:
      "Die Regeln, nach denen mpvium entwickelt wird: sinnvolle Standards, Profi-Kontrolle, Wiedergabetreue, Datenschutz, Effizienz und Ehrlichkeit über Änderungen.",
    screenshotsTitle: "Screenshots — mpvium",
    screenshotsDescription:
      "mpvium in Aktion: Start-Mediathek, Filmraster, Detailansicht und der auf mpv basierende Player.",
    changelogTitle: "Änderungsprotokoll — mpvium",
    changelogDescription:
      "Was es Neues in mpvium gibt: die neuesten nutzerorientierten Änderungen, generiert aus dem Änderungsprotokoll des Projekts.",
    downloadTitle: "Download — mpvium",
    downloadDescription:
      "mpvium-APKs herunterladen: Stable- und Preview-Builds für jede CPU-Architektur.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Seite nicht gefunden.",
  },
  nav: {
    features: "Funktionen",
    doctrine: "Doktrin",
    screenshots: "Screenshots",
    changelog: "Änderungen",
    download: "Download",
    openMenu: "Menü öffnen",
    closeMenu: "Menü schließen",
    languageLabel: "Sprache",
  },
  hero: {
    badge: "Kostenlos & Open Source · GPL-3.0-or-later · powered by mpv",
    tagline: "Sinnvolle Standards, Profi-Kontrolle.",
    subtitle:
      "Meinungsstark für den Alltag, vollständig anpassbar für Profis — die volle Kraft von mpv ohne Konfigurationsdatei-Hausaufgaben. Material 3-Oberfläche, Hintergrundwiedergabe, Bild-in-Bild.",
    downloadStable: "Stable herunterladen (arm64-v8a)",
    checkingPreview: "Preview wird geprüft…",
    resolvingVersions: "Aktuelle Versionen werden ermittelt…",
    downloadStableTag: "Stable {tag} herunterladen (arm64-v8a)",
    downloadPreviewTag: "Preview {tag} herunterladen (arm64-v8a)",
    noPreview: "Kein Preview-Build",
    latestMeta: "Aktuell: {parts}",
    releasesUnavailable: "Releases auf GitHub verfügbar.",
    logoAlt: "mpvium-Logo",
    screenshotAlt: "Screenshot des mpvium-Players",
  },
  why: {
    heading: "Warum mpvium",
    introPrefix:
      "Die meisten mpv-Oberflächen hören bei der Engine auf. Die meisten Videoplayer hören bei der Mediathek auf.",
    introHighlight: "mpvium ist die Schnittmenge",
    introSuffix:
      "— libmpv-Wiedergabe, eine echte Serien- & Film-Mediathek und touch-optimierte Steuerung, ohne auf Profi-Einstellungen zu verzichten.",
    col1Eyebrow: "Viele mpv-Forks",
    col1Title: "Engine zuerst",
    col1Body:
      "Wiedergabe-Power und Konfigurationsfreiheit, aber Mediathek und Touch-Bedienung bleiben dir überlassen.",
    col1Bullets: [
      "Vollständige mpv-Wiedergabe-Engine",
      "mpv.conf, Lua-Skripte und Konfiguration",
      "Minimalistische, dateibrowser-basierte Oberfläche",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Beides, standardmäßig",
    col2Body:
      "Dieselbe libmpv-Engine, verpackt in eine vollständige Mediathek und einen für Touch entwickelten Player.",
    col2Bullets: [
      "Automatische Serien- & Film-Mediathek",
      "Material 3 Expressive, 16 Designs",
      "mpv.conf, Lua und Decoder-Modi",
      "Intro überspringen, Erklären, Online-Untertitel",
    ],
    col3Eyebrow: "Viele Videoplayer",
    col3Title: "Mediathek zuerst",
    col3Body:
      "Eine vertraute Mediathek und Steuerung, aber Engine und Feintuning sind meist fest vorgegeben.",
    col3Bullets: [
      "Ausgereifte Mediathek und Wiedergabe",
      "Touch-freundliche Oberfläche",
      "Vorgegebene Engine- und Codec-Steuerung",
    ],
    engineHeading: "Der Unterschied bei der Engine",
    engineCompare: "Visuelle Vergleiche auf mpv.rocks ↗",
    engineBody:
      "Die Wiedergabe läuft über libmpv, dieselbe Rendering-Engine wie mpv — kein generischer Android-Decoder.",
    engineBenefits: [
      {
        title: "Farben wie beabsichtigt",
        description:
          "Quell-Farbmetadaten werden respektiert, mit HDR-Tone-Mapping für dein Display — ohne künstliche Sättigung, Kontrast oder Nachschärfung.",
      },
      {
        title: "Integriertes Debanding",
        description:
          "CPU- oder GPU-Debanding glättet abgestufte Farbverläufe in Himmeln, Nebel und dunklen Szenen.",
      },
      {
        title: "Details, die standhalten",
        description:
          "Hochwertiges Skalieren, Chroma-Upsampling und Deinterlacing halten Kanten und Bewegungen sauber.",
      },
      {
        title: "Untertitel wie erstellt",
        description:
          "ASS/SSA-Schriftarten, Farben, Positionierung und Effekte werden genau so gerendert, wie es das Release vorsieht.",
      },
    ],
  },
  home: {
    featuresHeading: "Funktionen",
    featuresSubtitle: "Alles, was du direkt brauchst — nichts, was im Weg steht.",
    exploreFeatures: "Alle Funktionen entdecken →",
    shotsHeading: "Screenshots",
    shotsSubtitle: "Mediathek, Durchsuchen, Wiedergabe — dunkles Standard-Design.",
    viewGallery: "Galerie ansehen →",
    themesHeading: "Designs",
    themesSubtitle:
      "Jedes Design der App — alle 16. Wähle eines aus: Diese ganze Seite passt sich wie die App an.",
    ctaHeading: "Bereit zum Anschauen",
    ctaBody:
      "Hol dir die neueste APK von GitHub Releases oder verfolge die Entwicklung in den Communities auf Discord, Telegram und Reddit.",
    ctaGet: "mpvium holen",
    ctaDiscord: "Discord beitreten",
    ctaTelegram: "Telegram beitreten",
    ctaReddit: "Reddit beitreten",
  },
  featuresPage: {
    heading: "Funktionen",
    subtitle: "Sinnvolle Standards von Anfang an, Profi-Kontrolle, wann immer du sie willst.",
    andMore: "Und mehr",
    items: [
      {
        title: "Automatisch kategorisierte Mediathek",
        description:
          "Deine Dateien werden automatisch in Serien & Filme sortiert — inklusive Postern sowie den Reihen „Als Nächstes“ und „Kürzlich hinzugefügt“.",
      },
      {
        title: "Intro & Rückblick automatisch überspringen",
        description:
          "Überspringen-Buttons mit einem Tipp im Player sowie ein Offline-Cache, der Intros automatisch für dich überspringt.",
      },
      {
        title: "Erklären-Button",
        description:
          "Tippe auf ein beliebiges Wort im Dialog für Wörterbuch-Abfragen und Wikipedia-Erklärungen.",
      },
      {
        title: "Mediathek-Einblicke",
        description:
          "Sammlungsgröße, Speicherplatz, Laufzeit, Wiedergabefortschritt und wöchentliche Aktivität in einem Dashboard.",
      },
      {
        title: "Moderne Material 3-Oberfläche",
        description:
          "Ausdrucksstarke Animationen, wandelbare Bedienelemente und 16 Designs mit hellem und dunklem Modus.",
      },
      {
        title: "Untertitel & Online-Suche",
        description:
          "Externe Spuren, OpenSubtitles und optionale SubSource-Suche, Stil-Anpassung und Zoom per Fingergeste.",
      },
      {
        title: "Erweiterte Konfiguration",
        description: "Volle Unterstützung für mpv.conf, input.conf und Lua-Skripte.",
      },
      {
        title: "Bild-in-Bild",
        description: "Schau weiter, während du andere Aufgaben erledigst.",
      },
      {
        title: "Hintergrundwiedergabe",
        description: "Reiner Audio-Modus für Musik und Podcasts.",
      },
      {
        title: "Netzwerk-Streaming",
        description: "URLs öffnen sowie SMB, FTP und WebDAV.",
      },
      {
        title: "Dateiverwaltung",
        description: "Ordner- & Baumansichten, Wiedergabelisten, Verlauf, Fortsetzen.",
      },
      {
        title: "Privat & werbefrei",
        description: "Keine Tracker, keine Werbung, keine unnötigen Berechtigungen.",
      },
    ],
    moreItems: [
      "Hardwarebeschleunigte, hochwertige Wiedergabe über libmpv",
      "Gestensteuerung, Zoom und Steuerung der Bildschirmausrichtung",
      "Kapitel, Wiedergabelisten, Wiedergabeverlauf und Fortsetzen",
      "Lokales Durchsuchen von Medien mit Ordner- und Baumansichten",
      "Eigene mpv-Konfiguration, Skripte und erweiterte Wiedergabeoptionen",
      "Medieninformationen und Metadaten-Caching",
    ],
  },
  screenshotsPage: {
    heading: "Screenshots",
    subtitle: "Mediathek, Durchsuchen, Wiedergabe — dunkles Standard-Design.",
    morePrefix: "Suchst du alle Designs und den hellen Modus? Die vollständige Sammlung liegt unter",
    moreLink: "docs/screenshots",
    moreSuffix: "im Repository.",
    shots: [
      {
        alt: "mpvium-Player",
        title: "Player",
        caption: "mpv-Power, touch-freundliche Steuerung.",
      },
      {
        alt: "mpvium-Player mit Intro überspringen",
        title: "Intro überspringen",
        caption: "Mit einem Tipp an Intros und Rückblicken vorbei.",
      },
      {
        alt: "mpvium-Startmediathek",
        title: "Start",
        caption: "Weiterschauen & durchsuchen.",
      },
      {
        alt: "mpvium-Titelraster",
        title: "Titel",
        caption: "Serien und Filme in einem Raster.",
      },
      {
        alt: "mpvium-Mediathek",
        title: "Mediathek",
        caption: "Serien, Filme & Ordner.",
      },
      {
        alt: "mpvium-Filmdetails",
        title: "Details",
        caption: "Film-Detailansicht.",
      },
      {
        alt: "mpvium-Willkommenstour",
        title: "Willkommen",
        caption: "Einführungstour beim ersten Start.",
      },
    ],
  },
  downloadPage: {
    heading: "Download",
    subtitle:
      "Kostenlos und Open Source, ohne Werbung oder Tracker. Die Download-Buttons verweisen auf die neuesten arm64-v8a-APKs; Preview ist aktiviert, wenn ein Preview-Release existiert.",
    tableHeading: "APK-Downloads",
    colApk: "APK",
    colDevices: "Geräte",
    apks: [
      { file: "universal", devices: "Die meisten Geräte; größerer Download" },
      {
        file: "arm64-v8a",
        devices: "Moderne 64-Bit-ARM-Geräte; empfohlen für die meisten Smartphones",
      },
      { file: "armeabi-v7a", devices: "Ältere 32-Bit-ARM-Geräte" },
      { file: "x86", devices: "32-Bit-Intel/AMD-Geräte" },
      { file: "x86_64", devices: "64-Bit-Intel/AMD-Geräte" },
    ],
    footerPrefix:
      "Preview-Builds sind zum Testen gedacht und können unfertige Funktionen enthalten. Prüfsummen werden bei jedem",
    footerRelease: "Release",
    footerMiddle: " veröffentlicht. Lieber selbst kompilieren? Sieh dir die",
    footerReadme: "README",
    footerSuffix: " an.",
  },
  doctrinePage: {
    generatedFrom: "Generiert aus",
    readFile: "Datei lesen ↗",
    linkTo: "Link zu",
  },
  changelogPage: {
    heading: "Was ist neu",
    subtitlePrefix: "Vollständige Release-Notes mit Prüfsummen findest du auf der",
    subtitleLink: "Releases-Seite",
    subtitleSuffix: ".",
    empty: "Noch keine Einträge im Änderungsprotokoll.",
  },
  notFoundPage: {
    title: "Diese Filmrolle fehlt.",
    body: "Die gesuchte Seite existiert nicht.",
    backHome: "Zurück zur Startseite",
  },
  footer: {
    releases: "Releases",
    doctrine: "Doktrin",
    abiNote: "Stable- und Preview-APKs sind arm64-v8a. Weitere ABIs auf der Releases-Seite.",
  },
};
