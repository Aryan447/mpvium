import type { SiteTranslations } from "../types";

export const zhCn: SiteTranslations = {
  meta: {
    title: "mpvium — 合理的默认设置，专业级掌控",
    description:
      "mpvium — 合理的默认设置，专业级掌控。专为日常观影精心调校，同时为专业用户提供全面自定义能力。基于 mpv 的 Android 视频播放器。",
    featuresTitle: "功能 — mpvium",
    featuresDescription:
      "mpvium 的全部能力：基于 mpv 的播放引擎、Material 3 界面、PiP、后台音频、网络串流、字幕与专业级配置。",
    doctrineTitle: "设计准则 — mpvium",
    doctrineDescription:
      "mpvium 遵循的开发准则：合理的默认设置、专业级掌控、播放保真度、隐私保护、高效率以及对变更的坦诚。",
    screenshotsTitle: "屏幕截图 — mpvium",
    screenshotsDescription:
      "mpvium 实机展示：主页媒体库、影片网格、详情页以及基于 mpv 的播放器。",
    changelogTitle: "更新日志 — mpvium",
    changelogDescription:
      "mpvium 最新动态：直接从项目更新日志生成的最新面向用户的变更。",
    downloadTitle: "下载 — mpvium",
    downloadDescription:
      "下载 mpvium APK：适用于各种 CPU 架构的稳定版与预览版构建。",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "未找到页面。",
  },
  nav: {
    features: "功能",
    doctrine: "准则",
    screenshots: "截图",
    changelog: "更新日志",
    download: "下载",
    openMenu: "打开菜单",
    closeMenu: "关闭菜单",
    languageLabel: "语言",
  },
  hero: {
    badge: "免费开源 · GPL-3.0-or-later · 基于 mpv",
    tagline: "合理的默认设置，专业级掌控。",
    subtitle:
      "专为日常观影精心调校，同时为专业用户提供全面自定义能力 — 无需折腾配置文件即可享受 mpv 的强大实力。Material 3 界面、后台播放、画中画。",
    downloadStable: "下载稳定版 (arm64-v8a)",
    checkingPreview: "正在检查预览版…",
    resolvingVersions: "正在获取最新版本…",
    downloadStableTag: "下载稳定版 {tag} (arm64-v8a)",
    downloadPreviewTag: "下载预览版 {tag} (arm64-v8a)",
    noPreview: "暂无预览版",
    latestMeta: "最新：{parts}",
    releasesUnavailable: "发布版本可在 GitHub 获取。",
    logoAlt: "mpvium 徽标",
    screenshotAlt: "mpvium 播放器截图",
  },
  why: {
    heading: "为什么选择 mpvium",
    introPrefix: "大多数 mpv 前端止步于播放引擎，大多数视频播放器止步于媒体库。",
    introHighlight: "mpvium 则是两者的交集",
    introSuffix:
      "— libmpv 播放能力、真正的剧集与电影媒体库，以及触控优先的操作体验，同时不牺牲专业设置。",
    col1Eyebrow: "许多 mpv 分支",
    col1Title: "引擎优先",
    col1Body: "拥有强大的播放与配置自由度，但媒体库与触控流程全靠你自己折腾。",
    col1Bullets: [
      "完整的 mpv 播放引擎",
      "mpv.conf、Lua 脚本与配置",
      "极简、以文件浏览为主的界面",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "开箱即得，两者兼备",
    col2Body: "同样的 libmpv 引擎，包裹在完整的媒体库与专为触控设计的播放器之中。",
    col2Bullets: [
      "自动整理的剧集与电影媒体库",
      "Material 3 Expressive，16 款主题",
      "mpv.conf、Lua 与多种解码模式",
      "跳过片头、词句解释、在线字幕",
    ],
    col3Eyebrow: "许多视频播放器",
    col3Title: "媒体库优先",
    col3Body: "熟悉的媒体库与控制界面，但播放引擎与微调能力大多被固定死。",
    col3Bullets: [
      "打磨精致的媒体库与播放体验",
      "触控友好的界面",
      "预设的引擎与编解码器控制",
    ],
    engineHeading: "引擎的差异",
    engineCompare: "在 mpv.rocks 查看画质对比 ↗",
    engineBody:
      "播放通过 libmpv 运行，与 mpv 使用相同的渲染引擎 — 而非普通的 Android 解码器。",
    engineBenefits: [
      {
        title: "还原真实色彩",
        description:
          "尊重片源色彩元数据，并为你的屏幕进行 HDR 色调映射 — 不做虚假的过饱和、对比度拉高或锐化。",
      },
      {
        title: "内置去色带",
        description: "CPU 或 GPU 去色带可平滑天空、雾气与暗部场景中的阶梯状色带。",
      },
      {
        title: "经得起检验的细节",
        description:
          "高质量缩放、色度升采样与去隔行处理，让边缘与动态画面保持清晰干净。",
      },
      {
        title: "忠实呈现字幕效果",
        description:
          "ASS/SSA 字体、颜色、定位与特效均按制作方原意精确渲染。",
      },
    ],
  },
  home: {
    featuresHeading: "功能",
    featuresSubtitle: "你需要的一切近在手边，没有任何多余干扰。",
    exploreFeatures: "探索全部功能 →",
    shotsHeading: "屏幕截图",
    shotsSubtitle: "媒体库、浏览、播放 — 默认深色主题。",
    viewGallery: "查看图库 →",
    themesHeading: "主题",
    themesSubtitle: "应用内的每一款主题 — 全部 16 款。任选其一，整个页面会像应用一样实时换肤。",
    ctaHeading: "准备好观影了吗",
    ctaBody:
      "从 GitHub Releases 获取最新 APK，或在 Discord、Telegram 和 Reddit 社区关注开发动态。",
    ctaGet: "获取 mpvium",
    ctaDiscord: "加入 Discord",
    ctaTelegram: "加入 Telegram",
    ctaReddit: "加入 Reddit",
  },
  featuresPage: {
    heading: "功能",
    subtitle: "开箱即享合理默认设置，需要时随时掌控专业级配置。",
    andMore: "更多特性",
    items: [
      {
        title: "自动分类媒体库",
        description:
          "文件自动归类为剧集与电影 — 内置海报墙、接下来观看与最近添加栏目。",
      },
      {
        title: "自动跳过片头与前情提要",
        description:
          "播放器内一键跳过按钮，配合离线缓存为你自动跳过片头。",
      },
      {
        title: "词句解释按钮",
        description: "点击对话中的任意单词即可查询词典释义与维基百科说明。",
      },
      {
        title: "媒体库统计洞察",
        description:
          "收藏规模、存储占用、总时长、观看进度与每周活跃度尽在一个仪表盘。",
      },
      {
        title: "现代 Material 3 界面",
        description: "富有表现力的动效、变形控件以及支持浅色与深色模式的 16 款主题。",
      },
      {
        title: "字幕与在线搜索",
        description:
          "支持外挂字幕轨、OpenSubtitles 与可选的 SubSource 搜索、样式自定义及双指缩放调整大小。",
      },
      {
        title: "高级配置",
        description: "完整支持 mpv.conf、input.conf 与 Lua 脚本。",
      },
      {
        title: "画中画 (PiP)",
        description: "多任务处理时也能继续观看。",
      },
      {
        title: "后台播放",
        description: "适合音乐与播客的纯音频模式。",
      },
      {
        title: "网络串流",
        description: "支持打开 URL 以及 SMB、FTP 和 WebDAV。",
      },
      {
        title: "文件管理",
        description: "文件夹与树形视图、播放列表、历史记录、断点续播。",
      },
      {
        title: "注重隐私且无广告",
        description: "无跟踪器、无广告、无多余权限。",
      },
    ],
    moreItems: [
      "通过 libmpv 实现硬件加速的高画质播放",
      "手势控制、画面缩放与屏幕方向控制",
      "支持章节、播放列表、播放历史与断点续播",
      "支持文件夹与树形视图的本地媒体浏览",
      "自定义 mpv 配置、脚本与高级播放选项",
      "媒体信息与元数据缓存",
    ],
  },
  screenshotsPage: {
    heading: "屏幕截图",
    subtitle: "媒体库、浏览、播放 — 默认深色主题。",
    morePrefix: "想查看所有主题和浅色模式？完整合集位于代码仓库的",
    moreLink: "docs/screenshots",
    moreSuffix: "目录中。",
    shots: [
      { alt: "mpvium 播放器", title: "播放器", caption: "mpv 的强大实力，触控友好的操作。" },
      {
        alt: "带跳过片头按钮的 mpvium 播放器",
        title: "跳过片头",
        caption: "一键跳过片头与前情提要。",
      },
      { alt: "mpvium 主页媒体库", title: "主页", caption: "继续观看与浏览。" },
      { alt: "mpvium 影片网格", title: "全部影片", caption: "剧集与电影同屏网格展示。" },
      { alt: "mpvium 媒体库", title: "媒体库", caption: "剧集、电影与文件夹。" },
      { alt: "mpvium 电影详情", title: "详情", caption: "电影展示视图。" },
      { alt: "mpvium 欢迎引导", title: "欢迎", caption: "首次启动引导流程。" },
    ],
  },
  downloadPage: {
    heading: "下载",
    subtitle:
      "免费开源，无广告或跟踪器。下载按钮默认指向最新的 arm64-v8a APK；当存在预览版发布时，预览版按钮会自动启用。",
    tableHeading: "APK 下载",
    colApk: "APK",
    colDevices: "适用设备",
    apks: [
      { file: "universal", devices: "绝大多数设备；安装包体积较大" },
      { file: "arm64-v8a", devices: "现代 64 位 ARM 设备；推荐绝大多数手机使用" },
      { file: "armeabi-v7a", devices: "较旧的 32 位 ARM 设备" },
      { file: "x86", devices: "32 位 Intel/AMD 设备" },
      { file: "x86_64", devices: "64 位 Intel/AMD 设备" },
    ],
    footerPrefix: "预览版用于测试，可能包含尚未完成的功能。校验和（Checksum）公布在每个",
    footerRelease: "发布页面",
    footerMiddle: "中。更倾向于从源码构建？请参阅",
    footerReadme: "README",
    footerSuffix: "。",
  },
  doctrinePage: {
    generatedFrom: "生成自",
    readFile: "阅读原文件 ↗",
    linkTo: "链接至",
  },
  changelogPage: {
    heading: "更新日志",
    subtitlePrefix: "包含校验和的完整发布说明位于",
    subtitleLink: "发布页面",
    subtitleSuffix: "。",
    empty: "暂无更新日志条目。",
  },
  notFoundPage: {
    title: "这一卷胶片丢失了。",
    body: "你访问的页面不存在。",
    backHome: "返回首页",
  },
  footer: {
    releases: "发布版本",
    doctrine: "设计准则",
    abiNote: "稳定版与预览版 APK 均为 arm64-v8a。其他 ABI 请前往发布页面下载。",
  },
};
