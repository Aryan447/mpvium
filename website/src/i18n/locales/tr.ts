import type { SiteTranslations } from "../types";

export const tr: SiteTranslations = {
  meta: {
    title: "mpvium — Akıllı varsayılanlar, profesyonel düzeyde kontrol",
    description:
      "mpvium — akıllı varsayılanlar, profesyonel düzeyde kontrol. Günlük izleme için özenle tasarlanmış, profesyoneller için tamamen özelleştirilebilir. mpv tabanlı Android video oynatıcı.",
    featuresTitle: "Özellikler — mpvium",
    featuresDescription:
      "mpvium'un sunduğu her şey: mpv tabanlı oynatma, Material 3 arayüzü, PiP, arka planda ses, ağ akışı, altyazılar ve profesyonel düzeyde yapılandırma.",
    doctrineTitle: "Doktrin — mpvium",
    doctrineDescription:
      "mpvium'un üzerine kurulduğu ilkeler: akıllı varsayılanlar, profesyonel düzeyde kontrol, oynatma sadakati, gizlilik, verimlilik ve değişikliklerde şeffaflık.",
    screenshotsTitle: "Ekran Görüntüleri — mpvium",
    screenshotsDescription:
      "mpvium iş başında: ana kütüphane, filmler ızgarası, ayrıntılar ve mpv tabanlı oynatıcı.",
    changelogTitle: "Değişiklik Günlüğü — mpvium",
    changelogDescription:
      "mpvium'da yenilikler: proje değişiklik günlüğünden oluşturulan en son kullanıcı odaklı değişiklikler.",
    downloadTitle: "İndir — mpvium",
    downloadDescription:
      "mpvium APK'larını indirin: her CPU mimarisi için kararlı ve önizleme sürümleri.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Sayfa bulunamadı.",
  },
  nav: {
    features: "Özellikler",
    doctrine: "Doktrin",
    screenshots: "Ekran Görüntüleri",
    changelog: "Değişiklik Günlüğü",
    download: "İndir",
    openMenu: "Menüyü aç",
    closeMenu: "Menüyü kapat",
    languageLabel: "Dil",
  },
  hero: {
    badge: "Ücretsiz ve açık kaynak · GPL-3.0-or-later · mpv tabanlı",
    tagline: "Akıllı varsayılanlar, profesyonel düzeyde kontrol.",
    subtitle:
      "Günlük izleme için özenle tasarlanmış, profesyoneller için tamamen özelleştirilebilir — yapılandırma dosyası zahmeti olmadan mpv gücü. Material 3 arayüzü, arka planda oynatma, pencere içinde pencere.",
    downloadStable: "Kararlı Sürümü İndir (arm64-v8a)",
    checkingPreview: "Önizleme kontrol ediliyor…",
    resolvingVersions: "En son sürümler alınıyor…",
    downloadStableTag: "Kararlı {tag} Sürümünü İndir (arm64-v8a)",
    downloadPreviewTag: "Önizleme {tag} Sürümünü İndir (arm64-v8a)",
    noPreview: "Önizleme sürümü yok",
    latestMeta: "En son: {parts}",
    releasesUnavailable: "Sürümler GitHub'da mevcuttur.",
    logoAlt: "mpvium logosu",
    screenshotAlt: "mpvium oynatıcı ekran görüntüsü",
  },
  why: {
    heading: "Neden mpvium",
    introPrefix: "Çoğu mpv arayüzü motorda durur. Çoğu video oynatıcı kütüphanede durur.",
    introHighlight: "mpvium ikisinin kesişimidir",
    introSuffix:
      "— libmpv oynatma motoru, gerçek bir Dizi ve Film kütüphanesi ve dokunmatik öncelikli kontroller; üstelik profesyonel ayarlardan ödün vermeden.",
    col1Eyebrow: "Birçok mpv çatalı",
    col1Title: "Önce motor",
    col1Body: "Oynatma gücü ve yapılandırma özgürlüğü sunar, ancak kütüphane ve dokunmatik akış size bırakılır.",
    col1Bullets: [
      "Tam mpv oynatma motoru",
      "mpv.conf, Lua betikleri ve yapılandırma",
      "Minimal, dosya gezgini odaklı arayüz",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Varsayılan olarak ikisi birden",
    col2Body: "Aynı libmpv motoru, eksiksiz bir kütüphane ve dokunmatik kullanım için tasarlanmış bir oynatıcıyla bir arada.",
    col2Bullets: [
      "Otomatik Dizi ve Film kütüphanesi",
      "Material 3 Expressive, 16 tema",
      "mpv.conf, Lua ve kod çözücü modları",
      "Giriş atlama, Açıkla, çevrimiçi altyazılar",
    ],
    col3Eyebrow: "Birçok video oynatıcı",
    col3Title: "Önce kütüphane",
    col3Body: "Tanıdık bir kütüphane ve kontroller sunar, ancak motor ve ince ayarlar çoğunlukla sabittir.",
    col3Bullets: [
      "Cilalı kütüphane ve oynatma",
      "Dokunmatik dostu arayüz",
      "Önceden ayarlanmış motor ve kodek kontrolleri",
    ],
    engineHeading: "Motor farkı",
    engineCompare: "mpv.rocks üzerinde görsel karşılaştırmalar ↗",
    engineBody:
      "Oynatma, sıradan bir Android kod çözücü yerine mpv ile aynı işleme motoru olan libmpv üzerinden çalışır.",
    engineBenefits: [
      {
        title: "Olduğu gibi renkler",
        description:
          "Kaynak renk meta verilerine sadık kalınır ve ekranınız için HDR ton eşleme uygulanır — yapay doygunluk, kontrast veya keskinleştirme yok.",
      },
      {
        title: "Dahili şeritlenme giderme",
        description: "CPU veya GPU şeritlenme giderme; gökyüzü, sis ve karanlık sahnelerdeki basamaklı geçişleri yumuşatır.",
      },
      {
        title: "Bozulmayan ayrıntılar",
        description:
          "Yüksek kaliteli ölçekleme, kroma yukarı örnekleme ve taramalı görüntü çözme, kenarları ve hareketi net tutar.",
      },
      {
        title: "Hazırlandığı gibi altyazılar",
        description:
          "ASS/SSA yazı tipleri, renkler, konumlandırma ve efektler yayında amaçlandığı şekilde işlenir.",
      },
    ],
  },
  home: {
    featuresHeading: "Özellikler",
    featuresSubtitle: "İhtiyacınız olan her şey elinizin altında, yolunuza çıkan hiçbir şey yok.",
    exploreFeatures: "Tüm özellikleri keşfet →",
    shotsHeading: "Ekran Görüntüleri",
    shotsSubtitle: "Kütüphane, göz atma, oynatma — varsayılan koyu tema.",
    viewGallery: "Galeriyi görüntüle →",
    themesHeading: "Temalar",
    themesSubtitle: "Uygulamadaki tüm temalar — 16'sı da burada. Birini seçin; bu sayfa tıpkı uygulama gibi yeniden giydirilsin.",
    ctaHeading: "İzlemeye hazır mısınız",
    ctaBody:
      "En son APK'yı GitHub Sürümler'den indirin veya geliştirme sürecini Discord, Telegram ve Reddit topluluklarından takip edin.",
    ctaGet: "mpvium'u Edin",
    ctaDiscord: "Discord'a Katıl",
    ctaTelegram: "Telegram'a Katıl",
    ctaReddit: "Reddit'e Katıl",
  },
  featuresPage: {
    heading: "Özellikler",
    subtitle: "Ön planda akıllı varsayılanlar, istediğinizde profesyonel düzeyde kontrol.",
    andMore: "Ve daha fazlası",
    items: [
      {
        title: "Otomatik Kategorize Edilen Kütüphane",
        description:
          "Dosyalarınız otomatik olarak Dizi ve Filmler şeklinde sıralanır — afişler, Sıradaki ve Son Eklenenler rafları dahil.",
      },
      {
        title: "Otomatik Giriş ve Özet Atlama",
        description:
          "Oynatıcıda tek dokunuşla atlama düğmeleri ve girişleri sizin yerinize atlayan çevrimdışı önbellek.",
      },
      {
        title: "Açıkla Düğmesi",
        description: "Sözlük aramaları ve Wikipedia açıklamaları için diyalogdaki herhangi bir sözcüğe dokunun.",
      },
      {
        title: "Kütüphane İstatistikleri",
        description:
          "Koleksiyon boyutu, depolama, toplam süre, izleme ilerlemesi ve haftalık etkinlik tek bir panoda.",
      },
      {
        title: "Modern Material 3 Arayüzü",
        description: "Akıcı hareketler, dönüşen kontroller ve açık/koyu modlu 16 tema.",
      },
      {
        title: "Altyazılar ve Çevrimiçi Arama",
        description:
          "Harici parçalar, OpenSubtitles ve isteğe bağlı SubSource araması, biçimlendirme ve parmakla yakınlaştırarak boyutlandırma.",
      },
      {
        title: "Gelişmiş Yapılandırma",
        description: "Tam mpv.conf, input.conf ve Lua betik desteği.",
      },
      {
        title: "Pencere İçinde Pencere (PiP)",
        description: "Başka işlerle uğraşırken izlemeye devam edin.",
      },
      {
        title: "Arka Planda Oynatma",
        description: "Müzik ve podcast'ler için yalnızca ses modu.",
      },
      {
        title: "Ağ Akışı",
        description: "URL'lerin yanı sıra SMB, FTP ve WebDAV desteği.",
      },
      {
        title: "Dosya Yönetimi",
        description: "Klasör ve ağaç görünümleri, oynatma listeleri, geçmiş ve kaldığı yerden devam.",
      },
      {
        title: "Gizlilik Odaklı ve Reklamsız",
        description: "İzleyici yok, reklam yok, gereksiz izinler yok.",
      },
    ],
    moreItems: [
      "libmpv üzerinden donanım hızlandırmalı, yüksek kaliteli oynatma",
      "Hareket kontrolleri, yakınlaştırma ve ekran yönü kontrolleri",
      "Bölümler, oynatma listeleri, izleme geçmişi ve kaldığı yerden devam desteği",
      "Klasör ve ağaç görünümleriyle yerel medyaya göz atma",
      "Özel mpv yapılandırması, betikler ve gelişmiş oynatma seçenekleri",
      "Medya bilgisi ve meta veri önbellekleme",
    ],
  },
  screenshotsPage: {
    heading: "Ekran Görüntüleri",
    subtitle: "Kütüphane, göz atma, oynatma — varsayılan koyu tema.",
    morePrefix: "Tüm temaları ve açık modu mu arıyorsunuz? Tam koleksiyon depodaki",
    moreLink: "docs/screenshots",
    moreSuffix: "klasöründe bulunuyor.",
    shots: [
      { alt: "mpvium oynatıcı", title: "Oynatıcı", caption: "mpv gücü, dokunmatik dostu kontroller." },
      {
        alt: "giriş atlama özellikli mpvium oynatıcı",
        title: "Girişi atla",
        caption: "Girişleri ve özetleri tek dokunuşla geçin.",
      },
      { alt: "mpvium ana kütüphane", title: "Ana Sayfa", caption: "İzlemeye devam et ve göz at." },
      { alt: "mpvium başlıklar ızgarası", title: "Başlıklar", caption: "Diziler ve filmler tek ızgarada." },
      { alt: "mpvium kütüphane", title: "Kütüphane", caption: "Diziler, filmler ve klasörler." },
      { alt: "mpvium film ayrıntıları", title: "Ayrıntılar", caption: "Film vitrin görünümü." },
      { alt: "mpvium karşılama turu", title: "Hoş Geldiniz", caption: "İlk kurulum rehber turu." },
    ],
  },
  downloadPage: {
    heading: "İndir",
    subtitle:
      "Ücretsiz ve açık kaynaklı, reklam veya izleyici yok. İndirme düğmeleri en son arm64-v8a APK'larına yönlendirir; bir önizleme sürümü mevcut olduğunda önizleme etkinleştirilir.",
    tableHeading: "APK indirmeleri",
    colApk: "APK",
    colDevices: "Cihazlar",
    apks: [
      { file: "universal", devices: "Çoğu cihaz; daha büyük indirme boyutu" },
      { file: "arm64-v8a", devices: "Modern 64 bit ARM cihazlar; çoğu telefon için önerilir" },
      { file: "armeabi-v7a", devices: "Daha eski 32 bit ARM cihazlar" },
      { file: "x86", devices: "32 bit Intel/AMD cihazlar" },
      { file: "x86_64", devices: "64 bit Intel/AMD cihazlar" },
    ],
    footerPrefix: "Önizleme sürümleri test amaçlıdır ve tamamlanmamış özellikler içerebilir. Sağlama toplamları her",
    footerRelease: "sürüm",
    footerMiddle: "sayfasında yayınlanır. Kaynaktan derlemeyi mi tercih ediyorsunuz?",
    footerReadme: "README",
    footerSuffix: "dosyasına göz atın.",
  },
  doctrinePage: {
    generatedFrom: "Kaynak dosya:",
    readFile: "Dosyayı oku ↗",
    linkTo: "Bağlantı:",
  },
  changelogPage: {
    heading: "Yenilikler",
    subtitlePrefix: "Sağlama toplamlarıyla birlikte tam sürüm notları",
    subtitleLink: "sürümler sayfasında",
    subtitleSuffix: "yer almaktadır.",
    empty: "Henüz değişiklik günlüğü girdisi yok.",
  },
  notFoundPage: {
    title: "Bu makara kayıp.",
    body: "Aradığınız sayfa mevcut değil.",
    backHome: "Ana sayfaya dön",
  },
  footer: {
    releases: "Sürümler",
    doctrine: "Doktrin",
    abiNote: "Kararlı ve önizleme APK'ları arm64-v8a mimarisi içindir. Diğer ABI'ler sürümler sayfasındadır.",
  },
};
