import type { SiteTranslations } from "../types";

export const pl: SiteTranslations = {
  meta: {
    title: "mpvium — Rozsądne ustawienia domyślne, profesjonalna kontrola",
    description:
      "mpvium — rozsądne ustawienia domyślne, profesjonalna kontrola. Dopracowany do codziennego oglądania, w pełni konfigurowalny dla zaawansowanych. Odtwarzacz wideo dla Androida oparty na mpv.",
    featuresTitle: "Funkcje — mpvium",
    featuresDescription:
      "Wszystko, co oferuje mpvium: odtwarzanie oparte na mpv, interfejs Material 3, PiP, dźwięk w tle, streaming sieciowy, napisy i zaawansowana konfiguracja.",
    doctrineTitle: "Doktryna — mpvium",
    doctrineDescription:
      "Zasady, na których zbudowano mpvium: rozsądne ustawienia domyślne, profesjonalna kontrola, wierność odtwarzania, prywatność, wydajność i uczciwość w opisie zmian.",
    screenshotsTitle: "Zrzuty ekranu — mpvium",
    screenshotsDescription:
      "mpvium w akcji: biblioteka główna, siatka filmów, szczegóły oraz odtwarzacz oparty na mpv.",
    changelogTitle: "Lista zmian — mpvium",
    changelogDescription:
      "Co nowego w mpvium: najnowsze zmiany widoczne dla użytkownika, generowane z dziennika zmian projektu.",
    downloadTitle: "Pobierz — mpvium",
    downloadDescription:
      "Pobierz pliki APK mpvium: stabilne i testowe wydania dla każdej architektury CPU.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Nie znaleziono strony.",
  },
  nav: {
    features: "Funkcje",
    doctrine: "Doktryna",
    screenshots: "Zrzuty ekranu",
    changelog: "Lista zmian",
    download: "Pobierz",
    openMenu: "Otwórz menu",
    closeMenu: "Zamknij menu",
    languageLabel: "Język",
  },
  hero: {
    badge: "Darmowy i open source · GPL-3.0-or-later · napędzany przez mpv",
    tagline: "Rozsądne ustawienia domyślne, profesjonalna kontrola.",
    subtitle:
      "Dopracowany do codziennego oglądania, w pełni konfigurowalny dla zaawansowanych — moc mpv bez żmudnej edycji plików konfiguracyjnych. Interfejs Material 3, odtwarzanie w tle, obraz w obrazie.",
    downloadStable: "Pobierz wersję stabilną (arm64-v8a)",
    checkingPreview: "Sprawdzanie wersji preview…",
    resolvingVersions: "Pobieranie najnowszych wersji…",
    downloadStableTag: "Pobierz wersję stabilną {tag} (arm64-v8a)",
    downloadPreviewTag: "Pobierz wersję preview {tag} (arm64-v8a)",
    noPreview: "Brak kompilacji preview",
    latestMeta: "Najnowsze: {parts}",
    releasesUnavailable: "Wydania są dostępne na GitHub.",
    logoAlt: "Logo mpvium",
    screenshotAlt: "Zrzut ekranu odtwarzacza mpvium",
  },
  why: {
    heading: "Dlaczego mpvium",
    introPrefix: "Większość nakładek na mpv kończy się na silniku. Większość odtwarzaczy wideo kończy się na bibliotece.",
    introHighlight: "mpvium łączy oba światy",
    introSuffix:
      "— odtwarzanie przez libmpv, prawdziwa biblioteka Seriali i Filmów oraz obsługa dotykiem bez rezygnacji z zaawansowanych ustawień.",
    col1Eyebrow: "Wiele forków mpv",
    col1Title: "Najpierw silnik",
    col1Body: "Duża moc odtwarzania i swoboda konfiguracji, ale biblioteka i obsługa dotykowa pozostają po Twojej stronie.",
    col1Bullets: [
      "Pełny silnik odtwarzania mpv",
      "mpv.conf, skrypty Lua i konfiguracja",
      "Minimalistyczny interfejs oparty na przeglądarce plików",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Jedno i drugie domyślnie",
    col2Body: "Ten sam silnik libmpv zamknięty w kompletnej bibliotece i odtwarzaczu zaprojektowanym pod dotyk.",
    col2Bullets: [
      "Automatyczna biblioteka Seriali i Filmów",
      "Material 3 Expressive, 16 motywów",
      "mpv.conf, Lua i tryby dekodera",
      "Pomijanie czołówki, Wyjaśnij, napisy online",
    ],
    col3Eyebrow: "Wiele odtwarzaczy wideo",
    col3Title: "Najpierw biblioteka",
    col3Body: "Znajoma biblioteka i sterowanie, ale silnik oraz precyzyjne dostrajanie są w większości zablokowane.",
    col3Bullets: [
      "Dopracowana biblioteka i odtwarzanie",
      "Interfejs przyjazny dotykowi",
      "Z góry ustalone opcje silnika i kodeków",
    ],
    engineHeading: "Różnica w silniku",
    engineCompare: "Porównania wizualne na mpv.rocks ↗",
    engineBody:
      "Odtwarzanie działa przez libmpv — ten sam silnik renderujący co w mpv, a nie zwykły dekoder Androida.",
    engineBenefits: [
      {
        title: "Kolory zgodnie z zamysłem",
        description:
          "Metadane kolorów źródła są respektowane wraz z mapowaniem tonów HDR dla Twojego ekranu — bez sztucznego nasycenia, kontrastu czy wyostrzania.",
      },
      {
        title: "Wbudowany debanding",
        description: "Debanding CPU lub GPU wygładza schodkowe przejścia tonalne na niebie, we mgle i w ciemnych scenach.",
      },
      {
        title: "Detale, które się bronią",
        description:
          "Wysokiej jakości skalowanie, nadpróbkowanie chrominancji i usuwanie przeplotu utrzymują czyste krawędzie i płynny ruch.",
      },
      {
        title: "Napisy takie, jak stworzono",
        description:
          "Czcionki, kolory, pozycjonowanie i efekty ASS/SSA są renderowane dokładnie tak, jak przewidziano w wydaniu.",
      },
    ],
  },
  home: {
    featuresHeading: "Funkcje",
    featuresSubtitle: "Wszystko, czego potrzebujesz pod ręką — nic nie wchodzi w drogę.",
    exploreFeatures: "Poznaj wszystkie funkcje →",
    shotsHeading: "Zrzuty ekranu",
    shotsSubtitle: "Biblioteka, przeglądanie, odtwarzanie — domyślny ciemny motyw.",
    viewGallery: "Zobacz galerię →",
    themesHeading: "Motywy",
    themesSubtitle: "Każdy motyw z aplikacji — wszystkie 16. Wybierz jeden, a cała strona zmieni wygląd tak jak aplikacja.",
    ctaHeading: "Gotowy do oglądania",
    ctaBody:
      "Pobierz najnowszy plik APK z wydań GitHub lub śledź rozwój w społecznościach na Discord, Telegram i Reddit.",
    ctaGet: "Pobierz mpvium",
    ctaDiscord: "Dołącz do Discorda",
    ctaTelegram: "Dołącz do Telegrama",
    ctaReddit: "Dołącz do Reddita",
  },
  featuresPage: {
    heading: "Funkcje",
    subtitle: "Rozsądne ustawienia na start, profesjonalna kontrola, gdy jej potrzebujesz.",
    andMore: "I więcej",
    items: [
      {
        title: "Automatycznie kategoryzowana biblioteka",
        description:
          "Twoje pliki automatycznie dzielą się na Seriale i Filmy — z plakatami oraz sekcjami Następne i Ostatnio dodane.",
      },
      {
        title: "Automatyczne pomijanie czołówki i podsumowań",
        description:
          "Przyciski pomijania jednym dotknięciem w odtwarzaczu oraz pamięć podręczna offline, która automatycznie pomija czołówki.",
      },
      {
        title: "Przycisk Wyjaśnij",
        description: "Dotknij dowolnego słowa w dialogu, aby sprawdzić słownik i wyjaśnienia z Wikipedii.",
      },
      {
        title: "Statystyki biblioteki",
        description:
          "Rozmiar kolekcji, zajęte miejsce, czas trwania, postęp oglądania i tygodniowa aktywność na jednym ekranie.",
      },
      {
        title: "Nowoczesny interfejs Material 3",
        description: "Wyraziste animacje, płynnie zmieniające się kontrolki oraz 16 motywów w trybie jasnym i ciemnym.",
      },
      {
        title: "Napisy i wyszukiwanie online",
        description:
          "Zewnętrzne ścieżki, wyszukiwanie w OpenSubtitles i opcjonalnie SubSource, stylowanie oraz zmiana rozmiaru uszczypnięciem.",
      },
      {
        title: "Zaawansowana konfiguracja",
        description: "Pełna obsługa mpv.conf, input.conf oraz skryptów Lua.",
      },
      {
        title: "Obraz w obrazie",
        description: "Oglądaj dalej, robiąc inne rzeczy.",
      },
      {
        title: "Odtwarzanie w tle",
        description: "Tryb samego dźwięku do muzyki i podcastów.",
      },
      {
        title: "Streaming sieciowy",
        description: "Otwieranie adresów URL oraz SMB, FTP i WebDAV.",
      },
      {
        title: "Zarządzanie plikami",
        description: "Widok folderów i drzewa katalogów, playlisty, historia, wznawianie.",
      },
      {
        title: "Prywatny i bez reklam",
        description: "Żadnych trackerów, żadnych reklam, żadnych zbędnych uprawnień.",
      },
    ],
    moreItems: [
      "Wysokiej jakości odtwarzanie akcelerowane sprzętowo przez libmpv",
      "Gesty sterowania, powiększanie i kontrola orientacji ekranu",
      "Rozdziały, playlisty, historia odtwarzania i wznawianie",
      "Przeglądanie lokalnych multimediów w widoku folderów i drzewa",
      "Własna konfiguracja mpv, skrypty i zaawansowane opcje odtwarzania",
      "Informacje o multimediach i buforowanie metadanych",
    ],
  },
  screenshotsPage: {
    heading: "Zrzuty ekranu",
    subtitle: "Biblioteka, przeglądanie, odtwarzanie — domyślny ciemny motyw.",
    morePrefix: "Szukasz wszystkich motywów i trybu jasnego? Pełna kolekcja znajduje się w",
    moreLink: "docs/screenshots",
    moreSuffix: "w repozytorium.",
    shots: [
      { alt: "Odtwarzacz mpvium", title: "Odtwarzacz", caption: "Moc mpv, sterowanie przyjazne dotykowi." },
      {
        alt: "Odtwarzacz mpvium z pomijaniem czołówki",
        title: "Pomiń czołówkę",
        caption: "Jedno dotknięcie omija czołówki i podsumowania.",
      },
      { alt: "Biblioteka główna mpvium", title: "Główna", caption: "Wznów oglądanie i przeglądaj." },
      { alt: "Siatka tytułów mpvium", title: "Tytuły", caption: "Seriale i filmy w jednej siatce." },
      { alt: "Biblioteka mpvium", title: "Biblioteka", caption: "Seriale, filmy i foldery." },
      { alt: "Szczegóły filmu w mpvium", title: "Szczegóły", caption: "Widok prezentacji filmu." },
      { alt: "Ekran powitalny mpvium", title: "Powitanie", caption: "Przewodnik przy pierwszym uruchomieniu." },
    ],
  },
  downloadPage: {
    heading: "Pobierz",
    subtitle:
      "Darmowy i open source, bez reklam ani trackerów. Przyciski pobierania prowadzą do najnowszych plików APK arm64-v8a; wersja preview jest dostępna, gdy istnieje wydanie preview.",
    tableHeading: "Pliki APK do pobrania",
    colApk: "APK",
    colDevices: "Urządzenia",
    apks: [
      { file: "universal", devices: "Większość urządzeń; większy plik do pobrania" },
      { file: "arm64-v8a", devices: "Nowoczesne 64-bitowe urządzenia ARM; zalecane dla większości telefonów" },
      { file: "armeabi-v7a", devices: "Starsze 32-bitowe urządzenia ARM" },
      { file: "x86", devices: "32-bitowe urządzenia Intel/AMD" },
      { file: "x86_64", devices: "64-bitowe urządzenia Intel/AMD" },
    ],
    footerPrefix: "Kompilacje preview służą do testów i mogą zawierać nieukończone funkcje. Sumy kontrolne są publikowane przy każdym",
    footerRelease: "wydaniu",
    footerMiddle: ". Wolisz zbudować ze źródeł? Zobacz",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Wygenerowano z",
    readFile: "Czytaj plik ↗",
    linkTo: "Odnośnik do",
  },
  changelogPage: {
    heading: "Co nowego",
    subtitlePrefix: "Pełne informacje o wydaniach wraz z sumami kontrolnymi znajdują się na",
    subtitleLink: "stronie wydań",
    subtitleSuffix: ".",
    empty: "Brak wpisów w dzienniku zmian.",
  },
  notFoundPage: {
    title: "Ta szpula zaginęła.",
    body: "Strona, której szukasz, nie istnieje.",
    backHome: "Wróć na stronę główną",
  },
  footer: {
    releases: "Wydania",
    doctrine: "Doktryna",
    abiNote: "Stabilne i preview pliki APK są w wersji arm64-v8a. Inne ABI na stronie wydań.",
  },
};
