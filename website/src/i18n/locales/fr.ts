import type { SiteTranslations } from "../types";

export const fr: SiteTranslations = {
  meta: {
    title: "mpvium — Réglages sensés par défaut, contrôle de niveau pro",
    description:
      "mpvium — réglages sensés par défaut, contrôle de niveau pro. Pensé pour le visionnage quotidien, entièrement personnalisable pour les pros. Lecteur vidéo Android propulsé par mpv.",
    featuresTitle: "Fonctionnalités — mpvium",
    featuresDescription:
      "Tout ce que fait mpvium : lecture propulsée par mpv, interface Material 3, PiP, audio en arrière-plan, streaming réseau, sous-titres et configuration avancée.",
    doctrineTitle: "Doctrine — mpvium",
    doctrineDescription:
      "Les règles sur lesquelles mpvium est bâti : réglages sensés par défaut, contrôle de niveau pro, fidélité de lecture, confidentialité, efficacité et transparence sur les changements.",
    screenshotsTitle: "Captures d’écran — mpvium",
    screenshotsDescription:
      "mpvium en action : bibliothèque d’accueil, grille de films, détails et lecteur propulsé par mpv.",
    changelogTitle: "Journal des modifications — mpvium",
    changelogDescription:
      "Nouveautés de mpvium : les derniers changements visibles par l’utilisateur, générés depuis le journal des modifications du projet.",
    downloadTitle: "Télécharger — mpvium",
    downloadDescription:
      "Téléchargez les APK de mpvium : versions stables et préversions pour chaque architecture CPU.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Page introuvable.",
  },
  nav: {
    features: "Fonctionnalités",
    doctrine: "Doctrine",
    screenshots: "Captures",
    changelog: "Nouveautés",
    download: "Télécharger",
    openMenu: "Ouvrir le menu",
    closeMenu: "Fermer le menu",
    languageLabel: "Langue",
  },
  hero: {
    badge: "Gratuit et open source · GPL-3.0-or-later · propulsé par mpv",
    tagline: "Réglages sensés par défaut, contrôle de niveau pro.",
    subtitle:
      "Pensé pour le visionnage quotidien, entièrement personnalisable pour les pros — la puissance de mpv sans la corvée des fichiers de config. Interface Material 3, lecture en arrière-plan, image dans l’image.",
    downloadStable: "Télécharger la stable (arm64-v8a)",
    checkingPreview: "Vérification de la préversion…",
    resolvingVersions: "Récupération des dernières versions…",
    downloadStableTag: "Télécharger la stable {tag} (arm64-v8a)",
    downloadPreviewTag: "Télécharger la préversion {tag} (arm64-v8a)",
    noPreview: "Aucune préversion",
    latestMeta: "Dernière : {parts}",
    releasesUnavailable: "Versions disponibles sur GitHub.",
    logoAlt: "Logo mpvium",
    screenshotAlt: "Capture d’écran du lecteur mpvium",
  },
  why: {
    heading: "Pourquoi mpvium",
    introPrefix:
      "La plupart des interfaces pour mpv s’arrêtent au moteur. La plupart des lecteurs vidéo s’arrêtent à la bibliothèque.",
    introHighlight: "mpvium fait le lien",
    introSuffix:
      "— lecture libmpv, une véritable bibliothèque Séries & Films et des contrôles pensés pour le tactile, sans sacrifier les réglages avancés.",
    col1Eyebrow: "Beaucoup de forks mpv",
    col1Title: "Le moteur d’abord",
    col1Body:
      "Puissance de lecture et liberté de configuration, mais la bibliothèque et l’ergonomie tactile restent à votre charge.",
    col1Bullets: [
      "Moteur de lecture mpv complet",
      "mpv.conf, scripts Lua et configuration",
      "Interface minimaliste centrée sur l’explorateur de fichiers",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Les deux, par défaut",
    col2Body:
      "Le même moteur libmpv, habillé d’une bibliothèque complète et d’un lecteur conçu pour le tactile.",
    col2Bullets: [
      "Bibliothèque Séries & Films automatique",
      "Material 3 Expressive, 16 thèmes",
      "mpv.conf, Lua et modes de décodage",
      "Saut d’intro, Expliquer, sous-titres en ligne",
    ],
    col3Eyebrow: "Beaucoup de lecteurs vidéo",
    col3Title: "La bibliothèque d’abord",
    col3Body:
      "Une bibliothèque et des contrôles familiers, mais le moteur et les réglages fins sont généralement figés.",
    col3Bullets: [
      "Bibliothèque et lecture soignées",
      "Interface adaptée au tactile",
      "Moteur et contrôles de codecs prédéfinis",
    ],
    engineHeading: "La différence du moteur",
    engineCompare: "Comparaisons visuelles sur mpv.rocks ↗",
    engineBody:
      "La lecture passe par libmpv, le même moteur de rendu que mpv — pas un décodeur Android générique.",
    engineBenefits: [
      {
        title: "Des couleurs fidèles",
        description:
          "Les métadonnées de couleur source sont respectées, avec mappage tonal HDR adapté à votre écran — sans saturation, contraste ni netteté artificiels.",
      },
      {
        title: "Débande intégrée",
        description:
          "Le debanding CPU ou GPU lisse les dégradés en escalier dans les ciels, le brouillard et les scènes sombres.",
      },
      {
        title: "Des détails préservés",
        description:
          "Mise à l’échelle de haute qualité, suréchantillonnage chromatique et désentrelacement gardent les contours et les mouvements nets.",
      },
      {
        title: "Sous-titres respectés",
        description:
          "Les polices, couleurs, positionnements et effets ASS/SSA s’affichent exactement comme prévu à la création.",
      },
    ],
  },
  home: {
    featuresHeading: "Fonctionnalités",
    featuresSubtitle: "Tout ce qu’il faut d’emblée, rien qui vous encombre.",
    exploreFeatures: "Explorer toutes les fonctionnalités →",
    shotsHeading: "Captures d’écran",
    shotsSubtitle: "Bibliothèque, navigation, lecture — thème sombre par défaut.",
    viewGallery: "Voir la galerie →",
    themesHeading: "Thèmes",
    themesSubtitle:
      "Tous les thèmes de l’application — les 16. Choisissez-en un : toute cette page change d’apparence comme l’app.",
    ctaHeading: "Prêt à regarder",
    ctaBody:
      "Récupérez le dernier APK sur GitHub Releases, ou suivez le développement sur les communautés Discord, Telegram et Reddit.",
    ctaGet: "Obtenir mpvium",
    ctaDiscord: "Rejoindre Discord",
    ctaTelegram: "Rejoindre Telegram",
    ctaReddit: "Rejoindre Reddit",
  },
  featuresPage: {
    heading: "Fonctionnalités",
    subtitle: "Des réglages sensés par défaut, un contrôle de niveau pro quand vous le souhaitez.",
    andMore: "Et plus encore",
    items: [
      {
        title: "Bibliothèque auto-catégorisée",
        description:
          "Vos fichiers triés automatiquement en Séries & Films — avec affiches, rangées À suivre et Ajouts récents incluses.",
      },
      {
        title: "Saut automatique d’intro et de résumé",
        description:
          "Boutons de saut en un geste dans le lecteur, plus un cache hors ligne qui passe les intros pour vous.",
      },
      {
        title: "Bouton Expliquer",
        description:
          "Touchez n’importe quel mot dans les dialogues pour consulter le dictionnaire et les explications Wikipédia.",
      },
      {
        title: "Statistiques de la bibliothèque",
        description:
          "Taille de la collection, stockage, durée, progression de visionnage et activité hebdomadaire dans un seul tableau de bord.",
      },
      {
        title: "Interface moderne Material 3",
        description:
          "Mouvements expressifs, contrôles adaptatifs et 16 thèmes avec modes clair et sombre.",
      },
      {
        title: "Sous-titres et recherche en ligne",
        description:
          "Pistes externes, recherche OpenSubtitles et SubSource en option, personnalisation du style et pincement pour redimensionner.",
      },
      {
        title: "Configuration avancée",
        description: "Prise en charge complète de mpv.conf, input.conf et des scripts Lua.",
      },
      {
        title: "Image dans l’image",
        description: "Continuez à regarder tout en faisant autre chose.",
      },
      {
        title: "Lecture en arrière-plan",
        description: "Mode audio uniquement pour la musique et les podcasts.",
      },
      {
        title: "Streaming réseau",
        description: "Ouvrez des URL ainsi que SMB, FTP et WebDAV.",
      },
      {
        title: "Gestion des fichiers",
        description: "Vues par dossiers et arborescence, listes de lecture, historique et reprise.",
      },
      {
        title: "Privé et sans publicité",
        description: "Aucun traceur, aucune publicité, aucune permission superflue.",
      },
    ],
    moreItems: [
      "Lecture haute qualité accélérée matériellement via libmpv",
      "Contrôles gestuels, zoom et contrôle de l’orientation de l’écran",
      "Chapitres, listes de lecture, historique de lecture et reprise",
      "Navigation dans les médias locaux avec vues par dossiers et arborescence",
      "Configuration mpv personnalisée, scripts et options de lecture avancées",
      "Informations multimédias et mise en cache des métadonnées",
    ],
  },
  screenshotsPage: {
    heading: "Captures d’écran",
    subtitle: "Bibliothèque, navigation, lecture — thème sombre par défaut.",
    morePrefix: "Vous cherchez tous les thèmes et le mode clair ? La collection complète se trouve dans",
    moreLink: "docs/screenshots",
    moreSuffix: "du dépôt.",
    shots: [
      {
        alt: "Lecteur mpvium",
        title: "Lecteur",
        caption: "Puissance de mpv, contrôles adaptés au tactile.",
      },
      {
        alt: "Lecteur mpvium avec saut d’intro",
        title: "Sauter l’intro",
        caption: "Un geste pour passer les intros et résumés.",
      },
      {
        alt: "Bibliothèque d’accueil mpvium",
        title: "Accueil",
        caption: "Reprendre la lecture et parcourir.",
      },
      {
        alt: "Grille des titres mpvium",
        title: "Titres",
        caption: "Séries et films dans une seule grille.",
      },
      {
        alt: "Bibliothèque mpvium",
        title: "Bibliothèque",
        caption: "Séries, films et dossiers.",
      },
      {
        alt: "Détails du film mpvium",
        title: "Détails",
        caption: "Vue détaillée du film.",
      },
      {
        alt: "Visite de bienvenue mpvium",
        title: "Bienvenue",
        caption: "Visite guidée au premier lancement.",
      },
    ],
  },
  downloadPage: {
    heading: "Télécharger",
    subtitle:
      "Gratuit et open source, sans publicité ni traceur. Les boutons de téléchargement pointent vers les derniers APK arm64-v8a ; la préversion est activée lorsqu’une préversion existe.",
    tableHeading: "Téléchargements APK",
    colApk: "APK",
    colDevices: "Appareils",
    apks: [
      { file: "universal", devices: "La plupart des appareils ; téléchargement plus volumineux" },
      {
        file: "arm64-v8a",
        devices: "Appareils ARM 64 bits modernes ; recommandé pour la plupart des téléphones",
      },
      { file: "armeabi-v7a", devices: "Anciens appareils ARM 32 bits" },
      { file: "x86", devices: "Appareils Intel/AMD 32 bits" },
      { file: "x86_64", devices: "Appareils Intel/AMD 64 bits" },
    ],
    footerPrefix:
      "Les préversions sont destinées aux tests et peuvent contenir des fonctionnalités inachevées. Les sommes de contrôle sont publiées sur chaque",
    footerRelease: "version",
    footerMiddle: ". Vous préférez compiler depuis les sources ? Consultez le",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Généré depuis",
    readFile: "Lire le fichier ↗",
    linkTo: "Lien vers",
  },
  changelogPage: {
    heading: "Nouveautés",
    subtitlePrefix: "Les notes de version complètes avec sommes de contrôle se trouvent sur la",
    subtitleLink: "page des versions",
    subtitleSuffix: ".",
    empty: "Aucune entrée dans le journal des modifications pour le moment.",
  },
  notFoundPage: {
    title: "Cette bobine est introuvable.",
    body: "La page que vous recherchez n’existe pas.",
    backHome: "Retour à l’accueil",
  },
  footer: {
    releases: "Versions",
    doctrine: "Doctrine",
    abiNote: "Les APK stables et préversions sont en arm64-v8a. Autres ABI sur la page des versions.",
  },
};
