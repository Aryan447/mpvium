import type { SiteTranslations } from "../types";

export const ja: SiteTranslations = {
  meta: {
    title: "mpvium — 賢明なデフォルト、プロレベルの制御",
    description:
      "mpvium — 賢明なデフォルト、プロレベルの制御。毎日の視聴に最適化され、上級者向けには自在なカスタマイズが可能。mpv 駆動の Android ビデオプレーヤー。",
    featuresTitle: "機能 — mpvium",
    featuresDescription:
      "mpvium のすべて：mpv 駆動の再生、Material 3 UI、PiP、バックグラウンド音声再生、ネットワークストリーミング、字幕、そしてプロレベルの設定。",
    doctrineTitle: "ドクトリン — mpvium",
    doctrineDescription:
      "mpvium の開発指針：賢明なデフォルト、プロレベルの制御、再生忠実度、プライバシー、効率性、そして変更内容の透明性。",
    screenshotsTitle: "スクリーンショット — mpvium",
    screenshotsDescription:
      "mpvium の実際の画面：ホームライブラリ、映画グリッド、詳細画面、そして mpv 駆動のプレーヤー。",
    changelogTitle: "更新履歴 — mpvium",
    changelogDescription:
      "mpvium の最新情報：プロジェクトの変更履歴から生成された、ユーザー向けの最新変更点。",
    downloadTitle: "ダウンロード — mpvium",
    downloadDescription:
      "mpvium APK をダウンロード：あらゆる CPU アーキテクチャ向けの安定版およびプレビュー版ビルド。",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "ページが見つかりません。",
  },
  nav: {
    features: "機能",
    doctrine: "ドクトリン",
    screenshots: "スクリーンショット",
    changelog: "更新履歴",
    download: "ダウンロード",
    openMenu: "メニューを開く",
    closeMenu: "メニューを閉じる",
    languageLabel: "言語",
  },
  hero: {
    badge: "無料 & オープンソース · GPL-3.0-or-later · mpv 搭載",
    tagline: "賢明なデフォルト、プロレベルの制御。",
    subtitle:
      "毎日の視聴に最適化され、上級者向けには自在なカスタマイズが可能 — 設定ファイルと格闘せずに mpv の力を手に入れられます。Material 3 インターフェース、バックグラウンド再生、ピクチャーインピクチャー対応。",
    downloadStable: "安定版をダウンロード (arm64-v8a)",
    checkingPreview: "プレビュー版を確認中…",
    resolvingVersions: "最新バージョンを取得中…",
    downloadStableTag: "安定版 {tag} をダウンロード (arm64-v8a)",
    downloadPreviewTag: "プレビュー版 {tag} をダウンロード (arm64-v8a)",
    noPreview: "プレビュービルドなし",
    latestMeta: "最新: {parts}",
    releasesUnavailable: "リリースは GitHub で公開されています。",
    logoAlt: "mpvium ロゴ",
    screenshotAlt: "mpvium プレーヤーのスクリーンショット",
  },
  why: {
    heading: "mpvium を選ぶ理由",
    introPrefix: "多くの mpv フロントエンドはエンジン止まり。多くの動画プレーヤーはライブラリ止まり。",
    introHighlight: "mpvium はその両方を兼ね備えています",
    introSuffix:
      "— libmpv による再生、本格的な番組＆映画ライブラリ、そしてタッチ操作優先の UI を、プロ向け設定を犠牲にすることなく実現しました。",
    col1Eyebrow: "多くの mpv フォーク",
    col1Title: "エンジン優先",
    col1Body: "強力な再生機能と設定の自由度はあるものの、ライブラリやタッチ操作の流れはユーザー任せです。",
    col1Bullets: [
      "フル機能の mpv 再生エンジン",
      "mpv.conf、Lua スクリプトと設定",
      "ファイルブラウザ中心のミニマルな UI",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "最初から、その両方を",
    col2Body: "同じ libmpv エンジンを、充実したライブラリとタッチ向けに設計されたプレーヤーで包み込みました。",
    col2Bullets: [
      "自動整理される番組＆映画ライブラリ",
      "Material 3 Expressive、16 種類のテーマ",
      "mpv.conf、Lua、デコーダーモード",
      "イントロスキップ、解説機能、オンライン字幕",
    ],
    col3Eyebrow: "多くの動画プレーヤー",
    col3Title: "ライブラリ優先",
    col3Body: "見慣れたライブラリと操作性を備えていますが、エンジンや細かな調整はほぼ固定されています。",
    col3Bullets: [
      "洗練されたライブラリと再生体験",
      "タッチ操作に適したインターフェース",
      "固定されたエンジンとコーデック制御",
    ],
    engineHeading: "エンジンの違い",
    engineCompare: "mpv.rocks で画質を比較 ↗",
    engineBody:
      "再生は一般的な Android デコーダーではなく、mpv と同じレンダリングエンジンである libmpv を通じて行われます。",
    engineBenefits: [
      {
        title: "意図された通りの色彩",
        description:
          "ソースのカラーメタデータを尊重し、お使いの画面に合わせて HDR トーンマッピングを実行 — 不自然な彩度・コントラスト強調やシャープネス補正は行いません。",
      },
      {
        title: "バンディング除去を内蔵",
        description: "CPU または GPU によるバンディング除去が、空や霧、暗いシーンの階調飛びを滑らかにします。",
      },
      {
        title: "崩れないディテール",
        description:
          "高品質なスケーリング、クロマアップサンプリング、インターレース解除により、輪郭と動きをクリアに保ちます。",
      },
      {
        title: "制作者の意図通りの字幕",
        description:
          "ASS/SSA のフォント、色、配置、エフェクトをリリースの意図通りに描画します。",
      },
    ],
  },
  home: {
    featuresHeading: "機能",
    featuresSubtitle: "必要なものはすべて手元に、邪魔なものは一切なし。",
    exploreFeatures: "すべての機能を見る →",
    shotsHeading: "スクリーンショット",
    shotsSubtitle: "ライブラリ、ブラウジング、再生 — デフォルトのダークテーマ。",
    viewGallery: "ギャラリーを見る →",
    themesHeading: "テーマ",
    themesSubtitle: "アプリ内の全 16 テーマ。1 つ選ぶと、このページ全体がアプリのように着せ替わります。",
    ctaHeading: "さあ、視聴を始めよう",
    ctaBody:
      "GitHub Releases から最新 APK を入手するか、Discord・Telegram・Reddit コミュニティで開発状況をフォローしてください。",
    ctaGet: "mpvium を入手",
    ctaDiscord: "Discord に参加",
    ctaTelegram: "Telegram に参加",
    ctaReddit: "Reddit に参加",
  },
  featuresPage: {
    heading: "機能",
    subtitle: "最初は賢明なデフォルトで、必要なときはいつでもプロレベルの制御を。",
    andMore: "さらに充実の機能",
    items: [
      {
        title: "自動分類ライブラリ",
        description:
          "ファイルを番組と映画に自動分類 — ポスター、「次へ」「最近追加」レールも完備。",
      },
      {
        title: "イントロ＆あらすじの自動スキップ",
        description:
          "プレーヤー内のワンタップスキップボタンに加え、オフラインキャッシュでイントロを自動スキップします。",
      },
      {
        title: "解説ボタン",
        description: "セリフ内の任意の単語をタップして、辞書検索や Wikipedia の解説を表示します。",
      },
      {
        title: "ライブラリインサイト",
        description:
          "コレクション数、ストレージ使用量、再生時間、視聴進捗、週間アクティビティを 1 つのダッシュボードに。",
      },
      {
        title: "モダンな Material 3 UI",
        description: "表現力豊かなモーション、モーフィングするコントロール、ライト＆ダーク対応の 16 テーマ。",
      },
      {
        title: "字幕＆オンライン検索",
        description:
          "外部字幕トラック、OpenSubtitles およびオプションの SubSource 検索、スタイル設定、ピンチでサイズ変更。",
      },
      {
        title: "高度な設定",
        description: "mpv.conf、input.conf、Lua スクリプトに完全対応。",
      },
      {
        title: "ピクチャーインピクチャー",
        description: "マルチタスクをしながら視聴を続けられます。",
      },
      {
        title: "バックグラウンド再生",
        description: "音楽やポッドキャスト向けの音声のみモード。",
      },
      {
        title: "ネットワークストリーミング",
        description: "URL に加え SMB、FTP、WebDAV に対応。",
      },
      {
        title: "ファイル管理",
        description: "フォルダ＆ツリー表示、プレイリスト、履歴、レジューム再生。",
      },
      {
        title: "プライバシー重視＆広告なし",
        description: "トラッカーなし、広告なし、不要な権限なし。",
      },
    ],
    moreItems: [
      "libmpv によるハードウェアアクセラレーション対応の高品質再生",
      "ジェスチャー操作、ズーム、画面の向き制御",
      "チャプター、プレイリスト、再生履歴、レジューム再生に対応",
      "フォルダ＆ツリー表示によるローカルメディアのブラウジング",
      "カスタム mpv 設定、スクリプト、高度な再生オプション",
      "メディア情報とメタデータのキャッシュ",
    ],
  },
  screenshotsPage: {
    heading: "スクリーンショット",
    subtitle: "ライブラリ、ブラウジング、再生 — デフォルトのダークテーマ。",
    morePrefix: "すべてのテーマやライトモードをお探しですか？全コレクションはリポジトリの",
    moreLink: "docs/screenshots",
    moreSuffix: "にあります。",
    shots: [
      { alt: "mpvium プレーヤー", title: "プレーヤー", caption: "mpv のパワーとタッチ操作しやすいコントロール。" },
      {
        alt: "イントロスキップ付き mpvium プレーヤー",
        title: "イントロスキップ",
        caption: "イントロやあらすじをワンタップでスキップ。",
      },
      { alt: "mpvium ホームライブラリ", title: "ホーム", caption: "続きから再生＆ブラウズ。" },
      { alt: "mpvium タイトルグリッド", title: "タイトル", caption: "番組と映画を 1 つのグリッドに。" },
      { alt: "mpvium ライブラリ", title: "ライブラリ", caption: "番組、映画＆フォルダ。" },
      { alt: "mpvium 映画詳細", title: "詳細", caption: "映画ショーケースビュー。" },
      { alt: "mpvium ウェルカムツアー", title: "ようこそ", caption: "初回起動時のオンボーディングツアー。" },
    ],
  },
  downloadPage: {
    heading: "ダウンロード",
    subtitle:
      "無料・オープンソースで、広告やトラッカーはありません。ダウンロードボタンは最新の arm64-v8a APK にリンクされます。プレビュー版が存在する場合はプレビューボタンが有効になります。",
    tableHeading: "APK ダウンロード",
    colApk: "APK",
    colDevices: "対象デバイス",
    apks: [
      { file: "universal", devices: "ほとんどのデバイス向け；ファイルサイズが大きくなります" },
      { file: "arm64-v8a", devices: "最新の 64-bit ARM デバイス；ほとんどのスマートフォンで推奨" },
      { file: "armeabi-v7a", devices: "古い 32-bit ARM デバイス" },
      { file: "x86", devices: "32-bit Intel/AMD デバイス" },
      { file: "x86_64", devices: "64-bit Intel/AMD デバイス" },
    ],
    footerPrefix: "プレビュービルドはテスト用であり、未完成の機能が含まれる場合があります。チェックサムは各",
    footerRelease: "リリース",
    footerMiddle: "ページで公開されています。ソースからビルドしたい場合は",
    footerReadme: "README",
    footerSuffix: "をご覧ください。",
  },
  doctrinePage: {
    generatedFrom: "生成元:",
    readFile: "ファイルを読む ↗",
    linkTo: "リンク:",
  },
  changelogPage: {
    heading: "最新情報",
    subtitlePrefix: "チェックサムを含む完全なリリースノートは",
    subtitleLink: "リリースページ",
    subtitleSuffix: "をご覧ください。",
    empty: "更新履歴のエントリはまだありません。",
  },
  notFoundPage: {
    title: "このフィルムリールは見つかりません。",
    body: "お探しのページは存在しません。",
    backHome: "ホームに戻る",
  },
  footer: {
    releases: "リリース",
    doctrine: "ドクトリン",
    abiNote: "安定版・プレビュー版 APK は arm64-v8a です。その他の ABI はリリースページにあります。",
  },
};
