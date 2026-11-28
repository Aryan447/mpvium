import type { SiteTranslations } from "../types";

export const it: SiteTranslations = {
  meta: {
    title: "mpvium — Impostazioni predefinite sensate, controllo da pro",
    description:
      "mpvium — impostazioni predefinite sensate, controllo da pro. Ottimizzato per la visione quotidiana, completamente personalizzabile per i professionisti. Lettore video Android basato su mpv.",
    featuresTitle: "Funzionalità — mpvium",
    featuresDescription:
      "Tutto ciò che fa mpvium: riproduzione basata su mpv, interfaccia Material 3, PiP, audio in background, streaming di rete, sottotitoli e configurazione avanzata.",
    doctrineTitle: "Dottrina — mpvium",
    doctrineDescription:
      "Le regole su cui è costruito mpvium: impostazioni predefinite sensate, controllo da pro, fedeltà di riproduzione, privacy, efficienza e trasparenza sulle novità.",
    screenshotsTitle: "Screenshot — mpvium",
    screenshotsDescription:
      "mpvium in azione: libreria home, griglia film, dettagli e il lettore basato su mpv.",
    changelogTitle: "Changelog — mpvium",
    changelogDescription:
      "Novità in mpvium: le ultime modifiche per gli utenti, generate dal changelog del progetto.",
    downloadTitle: "Download — mpvium",
    downloadDescription:
      "Scarica gli APK di mpvium: build stabili e preview per ogni architettura CPU.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Pagina non trovata.",
  },
  nav: {
    features: "Funzionalità",
    doctrine: "Dottrina",
    screenshots: "Screenshot",
    changelog: "Changelog",
    download: "Download",
    openMenu: "Apri menu",
    closeMenu: "Chiudi menu",
    languageLabel: "Lingua",
  },
  hero: {
    badge: "Gratuito e open source · GPL-3.0-or-later · basato su mpv",
    tagline: "Impostazioni predefinite sensate, controllo da pro.",
    subtitle:
      "Ottimizzato per la visione quotidiana, completamente personalizzabile per i professionisti — tutta la potenza di mpv senza dover studiare i file di configurazione. Interfaccia Material 3, riproduzione in background, picture-in-picture.",
    downloadStable: "Scarica Stabile (arm64-v8a)",
    checkingPreview: "Verifica preview…",
    resolvingVersions: "Ricerca ultime versioni…",
    downloadStableTag: "Scarica Stabile {tag} (arm64-v8a)",
    downloadPreviewTag: "Scarica Preview {tag} (arm64-v8a)",
    noPreview: "Nessuna build preview",
    latestMeta: "Ultime: {parts}",
    releasesUnavailable: "Release disponibili su GitHub.",
    logoAlt: "Logo di mpvium",
    screenshotAlt: "Screenshot del lettore mpvium",
  },
  why: {
    heading: "Perché mpvium",
    introPrefix: "Molti frontend per mpv si fermano al motore. Molti lettori video si fermano alla libreria.",
    introHighlight: "mpvium è il punto d'incontro",
    introSuffix:
      "— riproduzione libmpv, una vera libreria Serie TV e Film e controlli pensati per il touch, senza rinunciare alle impostazioni avanzate.",
    col1Eyebrow: "Molti fork di mpv",
    col1Title: "Prima il motore",
    col1Body: "Potenza di riproduzione e libertà di configurazione, ma libreria e flusso touch sono lasciati a te.",
    col1Bullets: [
      "Motore di riproduzione mpv completo",
      "mpv.conf, script Lua e configurazione",
      "Interfaccia minimale basata su file browser",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Entrambi, di serie",
    col2Body: "Lo stesso motore libmpv, racchiuso in una libreria completa e un lettore progettato per il touch.",
    col2Bullets: [
      "Libreria automatica Serie TV e Film",
      "Material 3 Expressive, 16 temi",
      "mpv.conf, Lua e modalità decoder",
      "Salta intro, Spiega, sottotitoli online",
    ],
    col3Eyebrow: "Molti lettori video",
    col3Title: "Prima la libreria",
    col3Body: "Libreria e controlli familiari, ma il motore e la regolazione fine sono per lo più bloccati.",
    col3Bullets: [
      "Libreria e riproduzione curate",
      "Interfaccia ottimizzata per il touch",
      "Controlli di motore e codec preimpostati",
    ],
    engineHeading: "La differenza del motore",
    engineCompare: "Confronti visivi su mpv.rocks ↗",
    engineBody:
      "La riproduzione passa attraverso libmpv, lo stesso motore di rendering di mpv — non un generico decoder Android.",
    engineBenefits: [
      {
        title: "Colori fedeli all'originale",
        description:
          "I metadati colore della sorgente vengono rispettati, con tone mapping HDR per il tuo schermo — nessuna saturazione, contrasto o nitidezza artificiale.",
      },
      {
        title: "Debanding integrato",
        description: "Il debanding CPU o GPU attenua le bande nei gradienti di cieli, nebbia e scene scure.",
      },
      {
        title: "Dettagli che reggono",
        description:
          "Scaling ad alta qualità, sovracampionamento cromatico e deinterlacciamento mantengono bordi e movimento puliti.",
      },
      {
        title: "Sottotitoli come concepiti",
        description:
          "Font, colori, posizionamento ed effetti ASS/SSA vengono renderizzati esattamente come previsto dalla release.",
      },
    ],
  },
  home: {
    featuresHeading: "Funzionalità",
    featuresSubtitle: "Tutto ciò che ti serve subito, niente che ti intralci.",
    exploreFeatures: "Esplora tutte le funzionalità →",
    shotsHeading: "Screenshot",
    shotsSubtitle: "Libreria, navigazione, riproduzione — tema scuro predefinito.",
    viewGallery: "Vedi galleria →",
    themesHeading: "Temi",
    themesSubtitle: "Ogni tema dell'app — tutti e 16. Scegline uno — l'intera pagina cambia stile come l'app.",
    ctaHeading: "Pronto a guardare",
    ctaBody:
      "Scarica l'ultimo APK dalle Release di GitHub oppure segui lo sviluppo nelle community Discord, Telegram e Reddit.",
    ctaGet: "Ottieni mpvium",
    ctaDiscord: "Unisciti a Discord",
    ctaTelegram: "Unisciti a Telegram",
    ctaReddit: "Unisciti a Reddit",
  },
  featuresPage: {
    heading: "Funzionalità",
    subtitle: "Impostazioni predefinite sensate da subito, controllo da pro quando lo desideri.",
    andMore: "E altro ancora",
    items: [
      {
        title: "Libreria auto-categorizzata",
        description:
          "I tuoi file ordinati automaticamente in Serie TV e Film — con locandine e sezioni Prossimi e Aggiunti di recente.",
      },
      {
        title: "Salta intro e riepilogo automatico",
        description:
          "Pulsanti di salto con un tocco nel lettore, più una cache offline che salta le sigle automaticamente.",
      },
      {
        title: "Pulsante Spiega",
        description: "Tocca qualsiasi parola nei dialoghi per consultare il dizionario e le spiegazioni di Wikipedia.",
      },
      {
        title: "Statistiche della libreria",
        description:
          "Dimensione della raccolta, spazio occupato, durata, avanzamento della visione e attività settimanale in un'unica dashboard.",
      },
      {
        title: "Interfaccia Material 3 moderna",
        description: "Animazioni espressive, controlli adattivi e 16 temi con modalità chiara e scura.",
      },
      {
        title: "Sottotitoli e ricerca online",
        description:
          "Tracce esterne, ricerca su OpenSubtitles e opzionale su SubSource, stile personalizzabile e pizzico per ridimensionare.",
      },
      {
        title: "Configurazione avanzata",
        description: "Supporto completo per mpv.conf, input.conf e script Lua.",
      },
      {
        title: "Picture-in-Picture",
        description: "Continua a guardare mentre usi altre app.",
      },
      {
        title: "Riproduzione in background",
        description: "Modalità solo audio per musica e podcast.",
      },
      {
        title: "Streaming di rete",
        description: "Apri URL e connessioni SMB, FTP e WebDAV.",
      },
      {
        title: "Gestione file",
        description: "Vista cartelle e ad albero, playlist, cronologia e ripresa.",
      },
      {
        title: "Privato e senza pubblicità",
        description: "Nessun tracker, nessuna pubblicità, nessun permesso superfluo.",
      },
    ],
    moreItems: [
      "Riproduzione accelerata via hardware ad alta qualità tramite libmpv",
      "Controlli gestuali, zoom e gestione dell'orientamento dello schermo",
      "Capitoli, playlist, cronologia di riproduzione e ripresa automatica",
      "Navigazione dei file locali con vista cartelle e ad albero",
      "Configurazione mpv personalizzata, script e opzioni di riproduzione avanzate",
      "Informazioni multimediali e cache dei metadati",
    ],
  },
  screenshotsPage: {
    heading: "Screenshot",
    subtitle: "Libreria, navigazione, riproduzione — tema scuro predefinito.",
    morePrefix: "Cerchi tutti i temi e la modalità chiara? La raccolta completa si trova in",
    moreLink: "docs/screenshots",
    moreSuffix: "nel repository.",
    shots: [
      { alt: "Lettore mpvium", title: "Lettore", caption: "Potenza mpv, controlli ottimizzati per il touch." },
      {
        alt: "Lettore mpvium con salta intro",
        title: "Salta intro",
        caption: "Un tocco per saltare sigle e riepiloghi.",
      },
      { alt: "Libreria home di mpvium", title: "Home", caption: "Continua a guardare ed esplora." },
      { alt: "Griglia titoli di mpvium", title: "Titoli", caption: "Serie TV e film in un'unica griglia." },
      { alt: "Libreria di mpvium", title: "Libreria", caption: "Serie TV, film e cartelle." },
      { alt: "Dettagli film di mpvium", title: "Dettagli", caption: "Vista vetrina del film." },
      { alt: "Tour di benvenuto di mpvium", title: "Benvenuto", caption: "Tour introduttivo al primo avvio." },
    ],
  },
  downloadPage: {
    heading: "Download",
    subtitle:
      "Gratuito e open source, senza pubblicità né tracker. I pulsanti di download puntano agli ultimi APK arm64-v8a; la preview è abilitata quando esiste una release preview.",
    tableHeading: "Download APK",
    colApk: "APK",
    colDevices: "Dispositivi",
    apks: [
      { file: "universal", devices: "Maggior parte dei dispositivi; download più pesante" },
      { file: "arm64-v8a", devices: "Dispositivi ARM 64-bit moderni; consigliato per la maggior parte dei telefoni" },
      { file: "armeabi-v7a", devices: "Dispositivi ARM 32-bit meno recenti" },
      { file: "x86", devices: "Dispositivi Intel/AMD 32-bit" },
      { file: "x86_64", devices: "Dispositivi Intel/AMD 64-bit" },
    ],
    footerPrefix: "Le build preview sono pensate per i test e possono contenere funzioni incomplete. I checksum sono pubblicati su ogni",
    footerRelease: "release",
    footerMiddle: ". Preferisci compilare dai sorgenti? Consulta il",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Generato da",
    readFile: "Leggi il file ↗",
    linkTo: "Collegamento a",
  },
  changelogPage: {
    heading: "Novità",
    subtitlePrefix: "Le note di rilascio complete con i checksum sono nella",
    subtitleLink: "pagina delle release",
    subtitleSuffix: ".",
    empty: "Nessuna voce nel changelog per ora.",
  },
  notFoundPage: {
    title: "Questa bobina è mancante.",
    body: "La pagina che stai cercando non esiste.",
    backHome: "Torna alla home",
  },
  footer: {
    releases: "Release",
    doctrine: "Dottrina",
    abiNote: "Gli APK stabili e preview sono arm64-v8a. Altre ABI nella pagina delle release.",
  },
};
