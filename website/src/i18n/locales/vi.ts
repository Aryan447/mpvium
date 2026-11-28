import type { SiteTranslations } from "../types";

export const vi: SiteTranslations = {
  meta: {
    title: "mpvium — Mặc định hợp lý, tùy chỉnh chuyên sâu",
    description:
      "mpvium — mặc định hợp lý, tùy chỉnh chuyên sâu. Tối ưu cho việc xem hằng ngày, tùy biến toàn diện cho người dùng chuyên nghiệp. Trình phát video Android sử dụng nhân mpv.",
    featuresTitle: "Tính năng — mpvium",
    featuresDescription:
      "Mọi thứ mpvium làm được: phát lại bằng nhân mpv, giao diện Material 3, PiP, phát nền, phát trực tuyến qua mạng, phụ đề và cấu hình chuyên sâu.",
    doctrineTitle: "Nguyên tắc — mpvium",
    doctrineDescription:
      "Những nguyên tắc xây dựng mpvium: mặc định hợp lý, tùy chỉnh chuyên sâu, độ trung thực khi phát, quyền riêng tư, hiệu quả và minh bạch về các thay đổi.",
    screenshotsTitle: "Ảnh chụp màn hình — mpvium",
    screenshotsDescription:
      "mpvium trong thực tế: thư viện trang chủ, lưới phim, chi tiết và trình phát dùng nhân mpv.",
    changelogTitle: "Nhật ký thay đổi — mpvium",
    changelogDescription:
      "Có gì mới trong mpvium: những thay đổi mới nhất dành cho người dùng, được tạo từ nhật ký thay đổi của dự án.",
    downloadTitle: "Tải xuống — mpvium",
    downloadDescription:
      "Tải xuống APK mpvium: các bản phát hành ổn định và bản xem trước cho mọi kiến trúc CPU.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Không tìm thấy trang.",
  },
  nav: {
    features: "Tính năng",
    doctrine: "Nguyên tắc",
    screenshots: "Ảnh chụp",
    changelog: "Nhật ký",
    download: "Tải xuống",
    openMenu: "Mở menu",
    closeMenu: "Đóng menu",
    languageLabel: "Ngôn ngữ",
  },
  hero: {
    badge: "Miễn phí & mã nguồn mở · GPL-3.0-or-later · dùng nhân mpv",
    tagline: "Mặc định hợp lý, tùy chỉnh chuyên sâu.",
    subtitle:
      "Tối ưu cho việc xem hằng ngày, tùy biến toàn diện cho người dùng chuyên nghiệp — sức mạnh của mpv mà không cần loay hoay với tệp cấu hình. Giao diện Material 3, phát nền, hình trong hình.",
    downloadStable: "Tải bản ổn định (arm64-v8a)",
    checkingPreview: "Đang kiểm tra bản xem trước…",
    resolvingVersions: "Đang lấy phiên bản mới nhất…",
    downloadStableTag: "Tải bản ổn định {tag} (arm64-v8a)",
    downloadPreviewTag: "Tải bản xem trước {tag} (arm64-v8a)",
    noPreview: "Không có bản xem trước",
    latestMeta: "Mới nhất: {parts}",
    releasesUnavailable: "Các bản phát hành có trên GitHub.",
    logoAlt: "Logo mpvium",
    screenshotAlt: "Ảnh chụp màn hình trình phát mpvium",
  },
  why: {
    heading: "Tại sao chọn mpvium",
    introPrefix: "Hầu hết các giao diện mpv chỉ dừng ở nhân phát. Hầu hết trình phát video chỉ dừng ở thư viện.",
    introHighlight: "mpvium là điểm giao thoa",
    introSuffix:
      "— phát lại bằng libmpv, thư viện Phim bộ & Phim lẻ thực thụ và điều khiển ưu tiên cảm ứng, mà không bỏ đi các thiết lập chuyên sâu.",
    col1Eyebrow: "Nhiều bản fork mpv",
    col1Title: "Ưu tiên nhân phát",
    col1Body: "Sức mạnh phát lại và tự do cấu hình, nhưng phần thư viện và thao tác cảm ứng để mặc bạn tự lo.",
    col1Bullets: [
      "Đầy đủ nhân phát mpv",
      "mpv.conf, tập lệnh Lua và cấu hình",
      "Giao diện tối giản, ưu tiên duyệt tệp",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Cả hai, ngay từ mặc định",
    col2Body: "Cùng một nhân libmpv, được bọc trong một thư viện hoàn chỉnh và trình phát thiết kế cho cảm ứng.",
    col2Bullets: [
      "Thư viện Phim bộ & Phim lẻ tự động",
      "Material 3 Expressive, 16 chủ đề",
      "mpv.conf, Lua và các chế độ giải mã",
      "Bỏ qua phần mở đầu, Giải thích, phụ đề trực tuyến",
    ],
    col3Eyebrow: "Nhiều trình phát video",
    col3Title: "Ưu tiên thư viện",
    col3Body: "Thư viện và điều khiển quen thuộc, nhưng nhân phát và tinh chỉnh hầu như bị cố định.",
    col3Bullets: [
      "Thư viện và phát lại trau chuốt",
      "Giao diện thân thiện cảm ứng",
      "Nhân phát và điều khiển codec cài sẵn",
    ],
    engineHeading: "Sự khác biệt của nhân phát",
    engineCompare: "So sánh trực quan tại mpv.rocks ↗",
    engineBody:
      "Quá trình phát chạy qua libmpv, cùng nhân kết xuất với mpv — không phải bộ giải mã Android thông thường.",
    engineBenefits: [
      {
        title: "Màu sắc đúng nguyên bản",
        description:
          "Siêu dữ liệu màu nguồn được tôn trọng, với ánh xạ tông HDR phù hợp màn hình của bạn — không tăng bão hòa, tương phản hay làm nét giả tạo.",
      },
      {
        title: "Tích hợp khử dải màu",
        description: "Khử dải màu bằng CPU hoặc GPU làm mượt các vùng chuyển sắc bậc thang trên bầu trời, sương mù và cảnh tối.",
      },
      {
        title: "Chi tiết sắc nét bền vững",
        description:
          "Phóng đại chất lượng cao, lấy mẫu tăng cường sắc độ và khử xen kẽ giữ cho đường nét và chuyển động sạch sẽ.",
      },
      {
        title: "Phụ đề đúng như tác giả thiết kế",
        description:
          "Phông chữ, màu sắc, vị trí và hiệu ứng ASS/SSA hiển thị đúng như bản phát hành mong muốn.",
      },
    ],
  },
  home: {
    featuresHeading: "Tính năng",
    featuresSubtitle: "Mọi thứ bạn cần ngay trước mắt, không gì cản trở.",
    exploreFeatures: "Khám phá tất cả tính năng →",
    shotsHeading: "Ảnh chụp màn hình",
    shotsSubtitle: "Thư viện, duyệt nội dung, phát lại — chủ đề tối mặc định.",
    viewGallery: "Xem thư viện ảnh →",
    themesHeading: "Chủ đề",
    themesSubtitle: "Mọi chủ đề trong ứng dụng — tất cả 16 chủ đề. Chọn một chủ đề — cả trang này sẽ đổi giao diện giống như ứng dụng.",
    ctaHeading: "Sẵn sàng thưởng thức",
    ctaBody:
      "Tải APK mới nhất từ GitHub Releases, hoặc theo dõi quá trình phát triển trong cộng đồng Discord, Telegram và Reddit.",
    ctaGet: "Tải mpvium",
    ctaDiscord: "Tham gia Discord",
    ctaTelegram: "Tham gia Telegram",
    ctaReddit: "Tham gia Reddit",
  },
  featuresPage: {
    heading: "Tính năng",
    subtitle: "Mặc định hợp lý ngay từ đầu, tùy chỉnh chuyên sâu khi bạn cần.",
    andMore: "Và hơn thế nữa",
    items: [
      {
        title: "Thư viện tự động phân loại",
        description:
          "Tệp của bạn được tự động sắp xếp thành Phim bộ & Phim lẻ — kèm áp phích, hàng Xem tiếp và Mới thêm.",
      },
      {
        title: "Tự động bỏ qua Mở đầu & Tóm tắt",
        description:
          "Nút bỏ qua một chạm trong trình phát, cùng bộ nhớ đệm ngoại tuyến tự động bỏ qua phần mở đầu cho bạn.",
      },
      {
        title: "Nút Giải thích",
        description: "Chạm vào bất kỳ từ nào trong lời thoại để tra từ điển và xem giải thích từ Wikipedia.",
      },
      {
        title: "Thống kê thư viện",
        description:
          "Quy mô bộ sưu tập, dung lượng lưu trữ, thời lượng, tiến độ xem và hoạt động hằng tuần trong một bảng điều khiển.",
      },
      {
        title: "Giao diện Material 3 hiện đại",
        description: "Chuyển động biểu cảm, nút điều khiển biến hình và 16 chủ đề với chế độ sáng & tối.",
      },
      {
        title: "Phụ đề & Tìm kiếm trực tuyến",
        description:
          "Rãnh phụ đề ngoài, tìm kiếm OpenSubtitles và tùy chọn SubSource, tùy chỉnh kiểu dáng và chụm để đổi cỡ.",
      },
      {
        title: "Cấu hình nâng cao",
        description: "Hỗ trợ đầy đủ mpv.conf, input.conf và tập lệnh Lua.",
      },
      {
        title: "Hình trong hình (PiP)",
        description: "Tiếp tục xem trong khi làm việc khác.",
      },
      {
        title: "Phát nền",
        description: "Chế độ chỉ phát âm thanh cho nhạc và podcast.",
      },
      {
        title: "Phát trực tuyến qua mạng",
        description: "Mở URL cùng với SMB, FTP và WebDAV.",
      },
      {
        title: "Quản lý tệp",
        description: "Chế độ xem thư mục & dạng cây, danh sách phát, lịch sử, phát tiếp.",
      },
      {
        title: "Riêng tư & Không quảng cáo",
        description: "Không trình theo dõi, không quảng cáo, không quyền truy cập thừa thãi.",
      },
    ],
    moreItems: [
      "Phát lại chất lượng cao tăng tốc phần cứng qua libmpv",
      "Điều khiển cử chỉ, thu phóng và xoay hướng màn hình",
      "Hỗ trợ chương, danh sách phát, lịch sử phát và phát tiếp",
      "Duyệt phương tiện cục bộ với chế độ xem thư mục và dạng cây",
      "Cấu hình mpv tùy chỉnh, tập lệnh và các tùy chọn phát lại nâng cao",
      "Thông tin phương tiện và lưu đệm siêu dữ liệu",
    ],
  },
  screenshotsPage: {
    heading: "Ảnh chụp màn hình",
    subtitle: "Thư viện, duyệt nội dung, phát lại — chủ đề tối mặc định.",
    morePrefix: "Đang tìm tất cả chủ đề và chế độ sáng? Bộ sưu tập đầy đủ nằm tại",
    moreLink: "docs/screenshots",
    moreSuffix: "trong kho mã nguồn.",
    shots: [
      { alt: "Trình phát mpvium", title: "Trình phát", caption: "Sức mạnh mpv, điều khiển thân thiện cảm ứng." },
      {
        alt: "Trình phát mpvium với nút bỏ qua mở đầu",
        title: "Bỏ qua mở đầu",
        caption: "Một chạm để vượt qua phần mở đầu và tóm tắt.",
      },
      { alt: "Thư viện trang chủ mpvium", title: "Trang chủ", caption: "Xem tiếp & duyệt nội dung." },
      { alt: "Lưới tiêu đề mpvium", title: "Tiêu đề", caption: "Phim bộ và phim lẻ trong một lưới." },
      { alt: "Thư viện mpvium", title: "Thư viện", caption: "Phim bộ, phim lẻ & thư mục." },
      { alt: "Chi tiết phim mpvium", title: "Chi tiết", caption: "Giao diện giới thiệu phim." },
      { alt: "Hướng dẫn chào mừng mpvium", title: "Chào mừng", caption: "Hướng dẫn làm quen lần đầu." },
    ],
  },
  downloadPage: {
    heading: "Tải xuống",
    subtitle:
      "Miễn phí và mã nguồn mở, không quảng cáo hay trình theo dõi. Các nút tải xuống trỏ tới APK arm64-v8a mới nhất; nút bản xem trước được bật khi có bản phát hành xem trước.",
    tableHeading: "Tải xuống APK",
    colApk: "APK",
    colDevices: "Thiết bị",
    apks: [
      { file: "universal", devices: "Hầu hết thiết bị; tệp tải xuống lớn hơn" },
      { file: "arm64-v8a", devices: "Thiết bị ARM 64-bit hiện đại; khuyến nghị cho hầu hết điện thoại" },
      { file: "armeabi-v7a", devices: "Thiết bị ARM 32-bit cũ hơn" },
      { file: "x86", devices: "Thiết bị Intel/AMD 32-bit" },
      { file: "x86_64", devices: "Thiết bị Intel/AMD 64-bit" },
    ],
    footerPrefix: "Các bản xem trước dành cho việc thử nghiệm và có thể chứa tính năng chưa hoàn thiện. Mã kiểm tra (checksum) được công bố trên mỗi",
    footerRelease: "bản phát hành",
    footerMiddle: ". Muốn tự biên dịch từ mã nguồn? Xem tệp",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Được tạo từ",
    readFile: "Đọc tệp ↗",
    linkTo: "Liên kết tới",
  },
  changelogPage: {
    heading: "Có gì mới",
    subtitlePrefix: "Ghi chú phát hành đầy đủ kèm mã kiểm tra có trên",
    subtitleLink: "trang phát hành",
    subtitleSuffix: ".",
    empty: "Chưa có mục nhật ký thay đổi nào.",
  },
  notFoundPage: {
    title: "Cuộn phim này bị thất lạc.",
    body: "Trang bạn đang tìm kiếm không tồn tại.",
    backHome: "Về trang chủ",
  },
  footer: {
    releases: "Phát hành",
    doctrine: "Nguyên tắc",
    abiNote: "APK bản ổn định và xem trước đều là arm64-v8a. Các ABI khác có trên trang phát hành.",
  },
};
