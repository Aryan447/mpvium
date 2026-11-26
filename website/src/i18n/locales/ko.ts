import type { SiteTranslations } from "../types";

export const ko: SiteTranslations = {
  meta: {
    title: "mpvium — 합리적인 기본값, 전문가 수준의 제어",
    description:
      "mpvium — 합리적인 기본값, 전문가 수준의 제어. 일상적인 시청에 최적화되어 있으며, 전문가를 위한 완벽한 커스터마이징을 지원합니다. mpv 기반 Android 비디오 플레이어.",
    featuresTitle: "기능 — mpvium",
    featuresDescription:
      "mpvium의 모든 기능: mpv 기반 재생, Material 3 UI, PiP, 백그라운드 오디오, 네트워크 스트리밍, 자막 및 전문가 수준의 설정.",
    doctrineTitle: "원칙 — mpvium",
    doctrineDescription:
      "mpvium의 개발 원칙: 합리적인 기본값, 전문가 수준의 제어, 재생 충실도, 프라이버시, 효율성, 그리고 변경 사항에 대한 정직함.",
    screenshotsTitle: "스크린샷 — mpvium",
    screenshotsDescription:
      "mpvium 실제 화면: 홈 라이브러리, 영화 그리드, 상세 정보, 그리고 mpv 기반 플레이어.",
    changelogTitle: "변경 기록 — mpvium",
    changelogDescription:
      "mpvium의 새로운 소식: 프로젝트 변경 기록에서 생성된 최신 사용자 중심 변경 사항.",
    downloadTitle: "다운로드 — mpvium",
    downloadDescription:
      "mpvium APK 다운로드: 모든 CPU 아키텍처용 안정 버전 및 프리뷰 빌드.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "페이지를 찾을 수 없습니다.",
  },
  nav: {
    features: "기능",
    doctrine: "원칙",
    screenshots: "스크린샷",
    changelog: "변경 기록",
    download: "다운로드",
    openMenu: "메뉴 열기",
    closeMenu: "메뉴 닫기",
    languageLabel: "언어",
  },
  hero: {
    badge: "무료 & 오픈 소스 · GPL-3.0-or-later · mpv 기반",
    tagline: "합리적인 기본값, 전문가 수준의 제어.",
    subtitle:
      "일상적인 시청에 최적화되어 있으며, 전문가를 위한 완벽한 커스터마이징을 지원합니다 — 설정 파일과 씨름하지 않고도 mpv의 강력한 성능을 누려보세요. Material 3 인터페이스, 백그라운드 재생, 화면 속 화면.",
    downloadStable: "안정 버전 다운로드 (arm64-v8a)",
    checkingPreview: "프리뷰 확인 중…",
    resolvingVersions: "최신 버전 확인 중…",
    downloadStableTag: "안정 버전 {tag} 다운로드 (arm64-v8a)",
    downloadPreviewTag: "프리뷰 {tag} 다운로드 (arm64-v8a)",
    noPreview: "프리뷰 빌드 없음",
    latestMeta: "최신: {parts}",
    releasesUnavailable: "릴리스는 GitHub에서 확인할 수 있습니다.",
    logoAlt: "mpvium 로고",
    screenshotAlt: "mpvium 플레이어 스크린샷",
  },
  why: {
    heading: "왜 mpvium인가",
    introPrefix: "대부분의 mpv 프론트엔드는 엔진에서 멈추고, 대부분의 비디오 플레이어는 라이브러리에서 멈춥니다.",
    introHighlight: "mpvium은 그 교집합입니다",
    introSuffix:
      "— libmpv 재생, 진짜 시리즈 & 영화 라이브러리, 터치 중심 컨트롤을 전문가용 설정을 포기하지 않고 모두 제공합니다.",
    col1Eyebrow: "많은 mpv 포크",
    col1Title: "엔진 우선",
    col1Body: "강력한 재생 성능과 설정의 자유를 주지만, 라이브러리와 터치 흐름은 사용자의 몫으로 남겨둡니다.",
    col1Bullets: [
      "완전한 mpv 재생 엔진",
      "mpv.conf, Lua 스크립트 및 설정",
      "파일 브라우저 중심의 미니멀한 인터페이스",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "기본값부터, 둘 다",
    col2Body: "동일한 libmpv 엔진을 완성도 높은 라이브러리와 터치에 맞게 설계된 플레이어로 감쌌습니다.",
    col2Bullets: [
      "자동 정리되는 시리즈 & 영화 라이브러리",
      "Material 3 Expressive, 16가지 테마",
      "mpv.conf, Lua 및 디코더 모드",
      "오프닝 건너뛰기, 단어 설명, 온라인 자막",
    ],
    col3Eyebrow: "많은 비디오 플레이어",
    col3Title: "라이브러리 우선",
    col3Body: "익숙한 라이브러리와 조작감을 제공하지만, 엔진과 세부 튜닝은 대부분 고정되어 있습니다.",
    col3Bullets: [
      "다듬어진 라이브러리와 재생 경험",
      "터치 친화적인 인터페이스",
      "고정된 엔진 및 코덱 제어",
    ],
    engineHeading: "엔진의 차이",
    engineCompare: "mpv.rocks에서 화질 비교 보기 ↗",
    engineBody:
      "일반적인 Android 디코더가 아닌, mpv와 동일한 렌더링 엔진인 libmpv를 통해 재생됩니다.",
    engineBenefits: [
      {
        title: "의도된 그대로의 색감",
        description:
          "원본 색상 메타데이터를 존중하며 화면에 맞는 HDR 톤 매핑을 적용합니다 — 인위적인 채도, 대비, 샤프닝을 더하지 않습니다.",
      },
      {
        title: "내장 디밴딩",
        description: "CPU 또는 GPU 디밴딩이 하늘, 안개, 어두운 장면의 계단 현상을 부드럽게 보정합니다.",
      },
      {
        title: "무너지지 않는 디테일",
        description:
          "고품질 스케일링, 크로마 업샘플링 및 디인터레이싱으로 윤곽과 움직임을 깔끔하게 유지합니다.",
      },
      {
        title: "제작자 의도대로의 자막",
        description:
          "ASS/SSA 글꼴, 색상, 위치 및 효과를 릴리스 의도 그대로 렌더링합니다.",
      },
    ],
  },
  home: {
    featuresHeading: "기능",
    featuresSubtitle: "필요한 모든 것을 한눈에, 방해되는 요소는 없이.",
    exploreFeatures: "모든 기능 살펴보기 →",
    shotsHeading: "스크린샷",
    shotsSubtitle: "라이브러리, 탐색, 재생 — 기본 다크 테마.",
    viewGallery: "갤러리 보기 →",
    themesHeading: "테마",
    themesSubtitle: "앱에 포함된 16가지 모든 테마. 하나를 고르면 이 페이지 전체가 앱처럼 즉시 바뀝니다.",
    ctaHeading: "감상할 준비가 되셨나요",
    ctaBody:
      "GitHub Releases에서 최신 APK를 받거나 Discord, Telegram, Reddit 커뮤니티에서 개발 소식을 팔로우하세요.",
    ctaGet: "mpvium 받기",
    ctaDiscord: "Discord 참여",
    ctaTelegram: "Telegram 참여",
    ctaReddit: "Reddit 참여",
  },
  featuresPage: {
    heading: "기능",
    subtitle: "처음에는 합리적인 기본값으로, 원할 때는 언제든 전문가 수준의 제어를.",
    andMore: "그 외 더 많은 기능",
    items: [
      {
        title: "자동 분류 라이브러리",
        description:
          "파일을 시리즈와 영화로 자동 분류 — 포스터, 다음 시청 및 최근 추가 레일 포함.",
      },
      {
        title: "오프닝 & 줄거리 자동 건너뛰기",
        description:
          "플레이어 내 원탭 건너뛰기 버튼과 오프라인 캐시로 오프닝을 자동으로 건너뜁니다.",
      },
      {
        title: "단어 설명 버튼",
        description: "대사의 단어를 탭하여 사전 검색과 Wikipedia 설명을 바로 확인하세요.",
      },
      {
        title: "라이브러리 인사이트",
        description:
          "컬렉션 크기, 저장 공간, 총 재생 시간, 시청 진행률, 주간 활동을 하나의 대시보드에서 확인하세요.",
      },
      {
        title: "모던 Material 3 UI",
        description: "표현력 있는 모션, 모핑 컨트롤, 라이트 & 다크 모드를 지원하는 16가지 테마.",
      },
      {
        title: "자막 & 온라인 검색",
        description:
          "외부 자막 트랙, OpenSubtitles 및 선택적 SubSource 검색, 스타일링, 핀치로 크기 조절.",
      },
      {
        title: "고급 설정",
        description: "mpv.conf, input.conf 및 Lua 스크립트 완벽 지원.",
      },
      {
        title: "화면 속 화면 (PiP)",
        description: "멀티태스킹 중에도 계속 시청하세요.",
      },
      {
        title: "백그라운드 재생",
        description: "음악과 팟캐스트를 위한 오디오 전용 모드.",
      },
      {
        title: "네트워크 스트리밍",
        description: "URL 열기 및 SMB, FTP, WebDAV 지원.",
      },
      {
        title: "파일 관리",
        description: "폴더 & 트리 보기, 재생 목록, 시청 기록, 이어보기.",
      },
      {
        title: "프라이버시 보호 & 광고 없음",
        description: "트래커 없음, 광고 없음, 불필요한 권한 없음.",
      },
    ],
    moreItems: [
      "libmpv를 통한 하드웨어 가속 고화질 재생",
      "제스처 제어, 화면 확대/축소 및 화면 방향 제어",
      "챕터, 재생 목록, 재생 기록 및 이어보기 지원",
      "폴더 및 트리 보기를 지원하는 로컬 미디어 탐색",
      "사용자 지정 mpv 설정, 스크립트 및 고급 재생 옵션",
      "미디어 정보 및 메타데이터 캐싱",
    ],
  },
  screenshotsPage: {
    heading: "스크린샷",
    subtitle: "라이브러리, 탐색, 재생 — 기본 다크 테마.",
    morePrefix: "모든 테마와 라이트 모드를 찾고 계신가요? 전체 컬렉션은 저장소의",
    moreLink: "docs/screenshots",
    moreSuffix: "에 있습니다.",
    shots: [
      { alt: "mpvium 플레이어", title: "플레이어", caption: "mpv의 강력한 성능과 터치 친화적인 컨트롤." },
      {
        alt: "오프닝 건너뛰기가 있는 mpvium 플레이어",
        title: "오프닝 건너뛰기",
        caption: "오프닝과 줄거리를 한 번의 탭으로 건너뛰세요.",
      },
      { alt: "mpvium 홈 라이브러리", title: "홈", caption: "이어서 시청 및 탐색." },
      { alt: "mpvium 타이틀 그리드", title: "타이틀", caption: "시리즈와 영화를 하나의 그리드에서." },
      { alt: "mpvium 라이브러리", title: "라이브러리", caption: "시리즈, 영화 및 폴더." },
      { alt: "mpvium 영화 상세 정보", title: "상세 정보", caption: "영화 쇼케이스 보기." },
      { alt: "mpvium 환영 투어", title: "환영", caption: "첫 실행 온보딩 투어." },
    ],
  },
  downloadPage: {
    heading: "다운로드",
    subtitle:
      "무료 오픈 소스이며 광고나 트래커가 없습니다. 다운로드 버튼은 최신 arm64-v8a APK로 연결되며, 프리뷰 릴리스가 있을 때 프리뷰 버튼이 활성화됩니다.",
    tableHeading: "APK 다운로드",
    colApk: "APK",
    colDevices: "지원 기기",
    apks: [
      { file: "universal", devices: "대부분의 기기; 다운로드 용량이 더 큼" },
      { file: "arm64-v8a", devices: "최신 64비트 ARM 기기; 대부분의 스마트폰에 권장" },
      { file: "armeabi-v7a", devices: "구형 32비트 ARM 기기" },
      { file: "x86", devices: "32비트 Intel/AMD 기기" },
      { file: "x86_64", devices: "64비트 Intel/AMD 기기" },
    ],
    footerPrefix: "프리뷰 빌드는 테스트용이며 미완성 기능이 포함될 수 있습니다. 체크섬은 각",
    footerRelease: "릴리스",
    footerMiddle: "페이지에 게시됩니다. 소스에서 직접 빌드하려면",
    footerReadme: "README",
    footerSuffix: "를 참고하세요.",
  },
  doctrinePage: {
    generatedFrom: "원본 파일:",
    readFile: "파일 읽기 ↗",
    linkTo: "링크:",
  },
  changelogPage: {
    heading: "새로운 소식",
    subtitlePrefix: "체크섬이 포함된 전체 릴리스 노트는",
    subtitleLink: "릴리스 페이지",
    subtitleSuffix: "에서 확인할 수 있습니다.",
    empty: "아직 변경 기록 항목이 없습니다.",
  },
  notFoundPage: {
    title: "이 필름 릴을 찾을 수 없습니다.",
    body: "찾고 계신 페이지가 존재하지 않습니다.",
    backHome: "홈으로 돌아가기",
  },
  footer: {
    releases: "릴리스",
    doctrine: "원칙",
    abiNote: "안정 버전 및 프리뷰 APK는 arm64-v8a입니다. 다른 ABI는 릴리스 페이지에 있습니다.",
  },
};
