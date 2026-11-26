import type { SiteTranslations } from "../types";

export const zhTw: SiteTranslations = {
  meta: {
    title: "mpvium — 合理的預設值，專業級掌控",
    description:
      "mpvium — 合理的預設值，專業級掌控。專為日常觀影精心調校，同時為專業使用者提供全面自訂能力。基於 mpv 的 Android 影片播放器。",
    featuresTitle: "功能 — mpvium",
    featuresDescription:
      "mpvium 的全部能力：基於 mpv 的播放引擎、Material 3 介面、PiP、背景音訊、網路串流、字幕與專業級設定。",
    doctrineTitle: "設計準則 — mpvium",
    doctrineDescription:
      "mpvium 遵循的開發準則：合理的預設值、專業級掌控、播放保真度、隱私保護、高效率以及對變更的坦誠。",
    screenshotsTitle: "螢幕截圖 — mpvium",
    screenshotsDescription:
      "mpvium 實機展示：首頁媒體庫、影片網格、詳情頁以及基於 mpv 的播放器。",
    changelogTitle: "更新日誌 — mpvium",
    changelogDescription:
      "mpvium 最新動態：直接從專案更新日誌產生的最新使用者向變更。",
    downloadTitle: "下載 — mpvium",
    downloadDescription:
      "下載 mpvium APK：適用於各種 CPU 架構的穩定版與預覽版建置。",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "找不到頁面。",
  },
  nav: {
    features: "功能",
    doctrine: "準則",
    screenshots: "截圖",
    changelog: "更新日誌",
    download: "下載",
    openMenu: "開啟選單",
    closeMenu: "關閉選單",
    languageLabel: "語言",
  },
  hero: {
    badge: "免費開源 · GPL-3.0-or-later · 基於 mpv",
    tagline: "合理的預設值，專業級掌控。",
    subtitle:
      "專為日常觀影精心調校，同時為專業使用者提供全面自訂能力 — 無需折騰設定檔即可享受 mpv 的強大實力。Material 3 介面、背景播放、子母畫面。",
    downloadStable: "下載穩定版 (arm64-v8a)",
    checkingPreview: "正在檢查預覽版…",
    resolvingVersions: "正在取得最新版本…",
    downloadStableTag: "下載穩定版 {tag} (arm64-v8a)",
    downloadPreviewTag: "下載預覽版 {tag} (arm64-v8a)",
    noPreview: "暫無預覽版",
    latestMeta: "最新：{parts}",
    releasesUnavailable: "發佈版本可在 GitHub 取得。",
    logoAlt: "mpvium 標誌",
    screenshotAlt: "mpvium 播放器截圖",
  },
  why: {
    heading: "為什麼選擇 mpvium",
    introPrefix: "大多數 mpv 前端止步於播放引擎，大多數影片播放器止步於媒體庫。",
    introHighlight: "mpvium 則是兩者的交集",
    introSuffix:
      "— libmpv 播放能力、真正的影集與電影媒體庫，以及觸控優先的操作體驗，同時不犧牲專業設定。",
    col1Eyebrow: "許多 mpv 分支",
    col1Title: "引擎優先",
    col1Body: "擁有強大的播放與設定自由度，但媒體庫與觸控流程全靠你自己折騰。",
    col1Bullets: [
      "完整的 mpv 播放引擎",
      "mpv.conf、Lua 腳本與設定",
      "極簡、以檔案瀏覽為主的介面",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "開箱即得，兩者兼備",
    col2Body: "同樣的 libmpv 引擎，包裹在完整的媒體庫與專為觸控設計的播放器之中。",
    col2Bullets: [
      "自動整理的影集與電影媒體庫",
      "Material 3 Expressive，16 款主題",
      "mpv.conf、Lua 與多種解碼模式",
      "跳過片頭、詞句解釋、線上字幕",
    ],
    col3Eyebrow: "許多影片播放器",
    col3Title: "媒體庫優先",
    col3Body: "熟悉的媒體庫與控制介面，但播放引擎與微調能力大多被固定死。",
    col3Bullets: [
      "打磨精緻的媒體庫與播放體驗",
      "觸控友善的介面",
      "預設的引擎與編解碼器控制",
    ],
    engineHeading: "引擎的差異",
    engineCompare: "在 mpv.rocks 查看畫質對比 ↗",
    engineBody:
      "播放透過 libmpv 執行，與 mpv 使用相同的渲染引擎 — 而非普通的 Android 解碼器。",
    engineBenefits: [
      {
        title: "還原真實色彩",
        description:
          "尊重片源色彩中繼資料，並為你的螢幕進行 HDR 色調映射 — 不做虛假的過飽和、對比度拉高或銳化。",
      },
      {
        title: "內建去色帶",
        description: "CPU 或 GPU 去色帶可平滑天空、霧氣與暗部場景中的階梯狀色帶。",
      },
      {
        title: "經得起考驗的細節",
        description:
          "高品質縮放、色度升採樣與去交錯處理，讓邊緣與動態畫面保持清晰乾淨。",
      },
      {
        title: "忠實呈現字幕效果",
        description:
          "ASS/SSA 字型、顏色、定位與特效均按製作方原意精確渲染。",
      },
    ],
  },
  home: {
    featuresHeading: "功能",
    featuresSubtitle: "你需要的一切近在手邊，沒有任何多餘干擾。",
    exploreFeatures: "探索全部功能 →",
    shotsHeading: "螢幕截圖",
    shotsSubtitle: "媒體庫、瀏覽、播放 — 預設深色主題。",
    viewGallery: "查看圖庫 →",
    themesHeading: "主題",
    themesSubtitle: "應用程式內的每一款主題 — 全部 16 款。任選其一，整個頁面會像應用程式一樣即時換膚。",
    ctaHeading: "準備好觀影了嗎",
    ctaBody:
      "從 GitHub Releases 取得最新 APK，或在 Discord、Telegram 和 Reddit 社群關注開發動態。",
    ctaGet: "取得 mpvium",
    ctaDiscord: "加入 Discord",
    ctaTelegram: "加入 Telegram",
    ctaReddit: "加入 Reddit",
  },
  featuresPage: {
    heading: "功能",
    subtitle: "開箱即享合理預設值，需要時隨時掌控專業級設定。",
    andMore: "更多特性",
    items: [
      {
        title: "自動分類媒體庫",
        description:
          "檔案自動歸類為影集與電影 — 內建海報牆、接下來觀看與最近新增欄位。",
      },
      {
        title: "自動跳過片頭與前情提要",
        description:
          "播放器內一鍵跳過按鈕，搭配離線快取為你自動跳過片頭。",
      },
      {
        title: "詞句解釋按鈕",
        description: "點擊對話中的任意單字即可查詢詞典釋義與維基百科說明。",
      },
      {
        title: "媒體庫統計洞察",
        description:
          "收藏規模、儲存空間、總時長、觀看進度與每週活躍度盡在一個儀表板。",
      },
      {
        title: "現代 Material 3 介面",
        description: "富有表現力的動效、變形控制項以及支援淺色與深色模式的 16 款主題。",
      },
      {
        title: "字幕與線上搜尋",
        description:
          "支援外掛字幕軌、OpenSubtitles 與可選的 SubSource 搜尋、樣式自訂及雙指縮放調整大小。",
      },
      {
        title: "進階設定",
        description: "完整支援 mpv.conf、input.conf 與 Lua 腳本。",
      },
      {
        title: "子母畫面 (PiP)",
        description: "多工處理時也能繼續觀看。",
      },
      {
        title: "背景播放",
        description: "適合音樂與 Podcast 的純音訊模式。",
      },
      {
        title: "網路串流",
        description: "支援開啟 URL 以及 SMB、FTP 和 WebDAV。",
      },
      {
        title: "檔案管理",
        description: "資料夾與樹狀檢視、播放清單、歷史紀錄、斷點續播。",
      },
      {
        title: "注重隱私且無廣告",
        description: "無追蹤器、無廣告、無多餘權限。",
      },
    ],
    moreItems: [
      "透過 libmpv 實現硬體加速的高畫質播放",
      "手勢控制、畫面縮放與螢幕方向控制",
      "支援章節、播放清單、播放歷史與斷點續播",
      "支援資料夾與樹狀檢視的本地媒體瀏覽",
      "自訂 mpv 設定、腳本與進階播放選項",
      "媒體資訊與中繼資料快取",
    ],
  },
  screenshotsPage: {
    heading: "螢幕截圖",
    subtitle: "媒體庫、瀏覽、播放 — 預設深色主題。",
    morePrefix: "想查看所有主題和淺色模式？完整合集位於程式碼儲存庫的",
    moreLink: "docs/screenshots",
    moreSuffix: "目錄中。",
    shots: [
      { alt: "mpvium 播放器", title: "播放器", caption: "mpv 的強大實力，觸控友善的操作。" },
      {
        alt: "帶跳過片頭按鈕的 mpvium 播放器",
        title: "跳過片頭",
        caption: "一鍵跳過片頭與前情提要。",
      },
      { alt: "mpvium 首頁媒體庫", title: "首頁", caption: "繼續觀看與瀏覽。" },
      { alt: "mpvium 影片網格", title: "全部影片", caption: "影集與電影同屏網格展示。" },
      { alt: "mpvium 媒體庫", title: "媒體庫", caption: "影集、電影與資料夾。" },
      { alt: "mpvium 電影詳情", title: "詳情", caption: "電影展示檢視。" },
      { alt: "mpvium 歡迎引導", title: "歡迎", caption: "首次啟動引導流程。" },
    ],
  },
  downloadPage: {
    heading: "下載",
    subtitle:
      "免費開源，無廣告或追蹤器。下載按鈕預設指向最新的 arm64-v8a APK；當存在預覽版發佈時，預覽版按鈕會自動啟用。",
    tableHeading: "APK 下載",
    colApk: "APK",
    colDevices: "適用裝置",
    apks: [
      { file: "universal", devices: "絕大多數裝置；安裝檔體積較大" },
      { file: "arm64-v8a", devices: "現代 64 位元 ARM 裝置；推薦絕大多數手機使用" },
      { file: "armeabi-v7a", devices: "較舊的 32 位元 ARM 裝置" },
      { file: "x86", devices: "32 位元 Intel/AMD 裝置" },
      { file: "x86_64", devices: "64 位元 Intel/AMD 裝置" },
    ],
    footerPrefix: "預覽版用於測試，可能包含尚未完成的功能。校驗和（Checksum）公佈在每個",
    footerRelease: "發佈頁面",
    footerMiddle: "中。更傾向於從原始碼建置？請參閱",
    footerReadme: "README",
    footerSuffix: "。",
  },
  doctrinePage: {
    generatedFrom: "產生自",
    readFile: "閱讀原檔案 ↗",
    linkTo: "連結至",
  },
  changelogPage: {
    heading: "更新日誌",
    subtitlePrefix: "包含校驗和的完整發佈說明位於",
    subtitleLink: "發佈頁面",
    subtitleSuffix: "。",
    empty: "暫無更新日誌條目。",
  },
  notFoundPage: {
    title: "這一捲膠片遺失了。",
    body: "你造訪的頁面不存在。",
    backHome: "返回首頁",
  },
  footer: {
    releases: "發佈版本",
    doctrine: "設計準則",
    abiNote: "穩定版與預覽版 APK 均為 arm64-v8a。其他 ABI 請前往發佈頁面下載。",
  },
};
