export interface SiteTranslations {
  meta: {
    title: string;
    description: string;
    featuresTitle: string;
    featuresDescription: string;
    doctrineTitle: string;
    doctrineDescription: string;
    screenshotsTitle: string;
    screenshotsDescription: string;
    changelogTitle: string;
    changelogDescription: string;
    downloadTitle: string;
    downloadDescription: string;
    notFoundTitle: string;
    notFoundDescription: string;
  };
  nav: {
    features: string;
    doctrine: string;
    screenshots: string;
    changelog: string;
    download: string;
    openMenu: string;
    closeMenu: string;
    languageLabel: string;
  };
  hero: {
    badge: string;
    tagline: string;
    subtitle: string;
    downloadStable: string;
    checkingPreview: string;
    resolvingVersions: string;
    downloadStableTag: string;
    downloadPreviewTag: string;
    noPreview: string;
    latestMeta: string;
    releasesUnavailable: string;
    logoAlt: string;
    screenshotAlt: string;
  };
  why: {
    heading: string;
    introPrefix: string;
    introHighlight: string;
    introSuffix: string;
    col1Eyebrow: string;
    col1Title: string;
    col1Body: string;
    col1Bullets: [string, string, string];
    col2Eyebrow: string;
    col2Title: string;
    col2Body: string;
    col2Bullets: [string, string, string, string];
    col3Eyebrow: string;
    col3Title: string;
    col3Body: string;
    col3Bullets: [string, string, string];
    engineHeading: string;
    engineCompare: string;
    engineBody: string;
    engineBenefits: Array<{ title: string; description: string }>;
  };
  home: {
    featuresHeading: string;
    featuresSubtitle: string;
    exploreFeatures: string;
    shotsHeading: string;
    shotsSubtitle: string;
    viewGallery: string;
    themesHeading: string;
    themesSubtitle: string;
    ctaHeading: string;
    ctaBody: string;
    ctaGet: string;
    ctaDiscord: string;
    ctaTelegram: string;
    ctaReddit: string;
  };
  featuresPage: {
    heading: string;
    subtitle: string;
    andMore: string;
    items: Array<{ title: string; description: string }>;
    moreItems: string[];
  };
  screenshotsPage: {
    heading: string;
    subtitle: string;
    morePrefix: string;
    moreLink: string;
    moreSuffix: string;
    shots: Array<{ alt: string; title: string; caption: string }>;
  };
  downloadPage: {
    heading: string;
    subtitle: string;
    tableHeading: string;
    colApk: string;
    colDevices: string;
    apks: Array<{ file: string; devices: string }>;
    footerPrefix: string;
    footerRelease: string;
    footerMiddle: string;
    footerReadme: string;
    footerSuffix: string;
  };
  doctrinePage: {
    generatedFrom: string;
    readFile: string;
    linkTo: string;
  };
  changelogPage: {
    heading: string;
    subtitlePrefix: string;
    subtitleLink: string;
    subtitleSuffix: string;
    empty: string;
  };
  notFoundPage: {
    title: string;
    body: string;
    backHome: string;
  };
  footer: {
    releases: string;
    doctrine: string;
    abiNote: string;
  };
}
