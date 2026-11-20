const fs = require('fs');
const path = require('path');
const sharp = require('../../website/node_modules/sharp');

const rootDir = path.resolve(__dirname, '../../');
const sourceDir = path.resolve(__dirname, 'source');

// Read source assets and convert to base64
const iconPngB64 = fs.readFileSync(path.join(rootDir, 'docs/icon.png')).toString('base64');
const playerJpgB64 = fs.readFileSync(path.join(rootDir, 'docs/screenshots/mpvium-player.jpg')).toString('base64');
const homeJpgB64 = fs.readFileSync(path.join(rootDir, 'docs/screenshots/mpvium-dark-default-home.jpg')).toString('base64');

// Common icons (mini vector paths)
const icons = {
  play: '<polygon points="5,3 15,9 5,15" fill="#00D2FF" />',
  ui: '<rect x="3" y="3" width="14" height="12" rx="2" fill="none" stroke="#E8B5EF" stroke-width="1.6"/><line x1="3" y1="7" x2="17" y2="7" stroke="#E8B5EF" stroke-width="1.6"/>',
  stream: '<circle cx="10" cy="10" r="2" fill="#70D7EB"/><path d="M7 7a4.2 4.2 0 0 0 0 6M13 7a4.2 4.2 0 0 1 0 6M4.5 4.5a7.8 7.8 0 0 0 0 11M15.5 4.5a7.8 7.8 0 0 1 0 11" fill="none" stroke="#70D7EB" stroke-width="1.5" stroke-linecap="round"/>',
  config: '<path d="M4 6h12M4 14h12" stroke="#FFB690" stroke-width="1.6" stroke-linecap="round"/><circle cx="8" cy="6" r="2" fill="#FFB690"/><circle cx="12" cy="14" r="2" fill="#FFB690"/>',
  shield: '<path d="M10 3L4 6v4c0 4.5 2.5 7.5 6 9 3.5-1.5 6-4.5 6-9V6l-6-3z" fill="none" stroke="#9BD79C" stroke-width="1.6"/><path d="M8 10l1.8 1.8 3.2-3.6" fill="none" stroke="#9BD79C" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>'
};

// -------------------------------------------------------------
// DESKTOP BANNER (2560 x 512 px)
// -------------------------------------------------------------
function generateDesktopSvg() {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 2560 512" width="2560" height="512">
  <defs>
    <!-- Background Gradients -->
    <linearGradient id="desktop_bg" x1="0" y1="0" x2="2560" y2="512" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#12091F" />
      <stop offset="30%" stop-color="#0B0613" />
      <stop offset="65%" stop-color="#06040A" />
      <stop offset="100%" stop-color="#030206" />
    </linearGradient>

    <radialGradient id="glow_brand" cx="500" cy="240" r="480" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#8A2BE2" stop-opacity="0.20" />
      <stop offset="50%" stop-color="#8A2BE2" stop-opacity="0.06" />
      <stop offset="100%" stop-color="#8A2BE2" stop-opacity="0" />
    </radialGradient>

    <radialGradient id="glow_devices" cx="1900" cy="256" r="600" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#6B1FB2" stop-opacity="0.25" />
      <stop offset="45%" stop-color="#00D2FF" stop-opacity="0.08" />
      <stop offset="100%" stop-color="#000000" stop-opacity="0" />
    </radialGradient>

    <!-- Top Filmstrip Accent Gradient -->
    <linearGradient id="top_border_grad" x1="0" y1="0" x2="2560" y2="0" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#FF416C" stop-opacity="0" />
      <stop offset="15%" stop-color="#FF416C" stop-opacity="0.45" />
      <stop offset="50%" stop-color="#8A2BE2" stop-opacity="0.75" />
      <stop offset="85%" stop-color="#00D2FF" stop-opacity="0.5" />
      <stop offset="100%" stop-color="#00D2FF" stop-opacity="0" />
    </linearGradient>

    <!-- Wordmark Gradient: Matches the official play glyph streaming colors -->
    <linearGradient id="wordmark_grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#FFA8BC" />
      <stop offset="45%" stop-color="#D0BCFF" />
      <stop offset="100%" stop-color="#70D7EB" />
    </linearGradient>

    <!-- Glass Glare Reflection -->
    <linearGradient id="glass_glare" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.12" />
      <stop offset="35%" stop-color="#FFFFFF" stop-opacity="0.03" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
    </linearGradient>

    <!-- Device Rim Light -->
    <linearGradient id="device_rim" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.30" />
      <stop offset="35%" stop-color="#8A2BE2" stop-opacity="0.40" />
      <stop offset="70%" stop-color="#00D2FF" stop-opacity="0.25" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.10" />
    </linearGradient>

    <!-- Clip Paths -->
    <clipPath id="squircle_icon_clip">
      <rect x="180" y="120" width="96" height="96" rx="24" ry="24" />
    </clipPath>

    <clipPath id="portrait_phone_screen">
      <rect x="1272" y="44" width="202" height="424" rx="20" ry="20" />
    </clipPath>

    <clipPath id="landscape_phone_screen">
      <rect x="1466" y="59" width="868" height="394" rx="18" ry="18" />
    </clipPath>

    <!-- Shadows -->
    <filter id="shadow_deep" x="-10%" y="-10%" width="125%" height="130%">
      <feDropShadow dx="0" dy="24" stdDeviation="30" flood-color="#000000" flood-opacity="0.85" />
      <feDropShadow dx="0" dy="6" stdDeviation="10" flood-color="#000000" flood-opacity="0.40" />
    </filter>

    <filter id="shadow_icon" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="12" stdDeviation="16" flood-color="#000000" flood-opacity="0.7" />
      <feDropShadow dx="0" dy="0" stdDeviation="22" flood-color="#8A2BE2" flood-opacity="0.35" />
    </filter>

    <!-- Subtle Film Perforation Pattern -->
    <pattern id="film_perfs" width="20" height="8" patternUnits="userSpaceOnUse">
      <rect x="4" y="2" width="12" height="4" rx="1" fill="#06040A" opacity="0.6"/>
    </pattern>
  </defs>

  <!-- Base Dark Canvas -->
  <rect width="2560" height="512" fill="url(#desktop_bg)" />

  <!-- Atmospheric Glows -->
  <circle cx="500" cy="240" r="480" fill="url(#glow_brand)" />
  <circle cx="1900" cy="256" r="600" fill="url(#glow_devices)" />

  <!-- Subtle Cinematic Filmstrip Edge (Top) -->
  <rect x="0" y="0" width="2560" height="8" fill="#181126" />
  <rect x="0" y="0" width="2560" height="8" fill="url(#film_perfs)" />
  <line x1="0" y1="8" x2="2560" y2="8" stroke="url(#top_border_grad)" stroke-width="1.5" />

  <!-- Subtle Bottom Edge Horizon -->
  <line x1="0" y1="511" x2="2560" y2="511" stroke="rgba(255, 255, 255, 0.06)" stroke-width="1" />

  <!-- Subtle Technical Coordinate Watermark (Left Safe Margin) -->
  <text x="180" y="488" font-family="system-ui, -apple-system, sans-serif" font-size="11" font-weight="600" letter-spacing="1.5" fill="#4E445B">MPVIUM // ANDROID MEDIA PLAYER</text>
  <text x="500" y="488" font-family="system-ui, -apple-system, sans-serif" font-size="11" font-weight="600" letter-spacing="1.5" fill="#3D3647">GPL-3.0-OR-LATER</text>

  <!-- ========================================================= -->
  <!-- RIGHT SIDE: APPLICATION INTERFACE SHOWCASE                -->
  <!-- ========================================================= -->
  <g id="device_showcase">
    <!-- DEVICE 1: PORTRAIT PHONE (Browsing / Movies & Library) -->
    <!-- Placed behind on the left of the player to show the auto-categorized poster library -->
    <g id="portrait_device" transform="translate(0, 0)">
      <!-- Back Shadow -->
      <rect x="1266" y="38" width="214" height="436" rx="24" ry="24" fill="#000000" opacity="0.6" filter="url(#shadow_deep)" />
      <!-- Outer Chassis Frame -->
      <rect x="1266" y="38" width="214" height="436" rx="24" ry="24" fill="#15121B" stroke="url(#device_rim)" stroke-width="1.5" />
      <!-- Screen Display -->
      <image href="data:image/jpeg;base64,${homeJpgB64}" x="1272" y="44" width="202" height="424" preserveAspectRatio="xMidYMid slice" clip-path="url(#portrait_phone_screen)" />
      <!-- Glass Glare Highlight -->
      <path d="M 1272,44 L 1400,44 L 1340,468 L 1272,468 Z" fill="url(#glass_glare)" clip-path="url(#portrait_phone_screen)" />
      <!-- Display Border Stroke -->
      <rect x="1272" y="44" width="202" height="424" rx="20" ry="20" fill="none" stroke="rgba(0, 0, 0, 0.4)" stroke-width="1" />
    </g>

    <!-- DEVICE 2: LANDSCAPE HERO PHONE (Video Playback in Action) -->
    <!-- Centerpiece showing the actual player with controls and skip intro -->
    <g id="landscape_device">
      <!-- Deep Elevation Shadow -->
      <rect x="1458" y="51" width="884" height="410" rx="22" ry="22" fill="#000000" filter="url(#shadow_deep)" />
      <!-- Premium Titanium Outer Rim -->
      <rect x="1458" y="51" width="884" height="410" rx="22" ry="22" fill="#16131D" stroke="url(#device_rim)" stroke-width="1.8" />
      <!-- Display Bezel Cushion -->
      <rect x="1464" y="57" width="872" height="398" rx="20" ry="20" fill="#09080D" />
      <!-- Actual Video Player Screenshot -->
      <image href="data:image/jpeg;base64,${playerJpgB64}" x="1466" y="59" width="868" height="394" preserveAspectRatio="xMidYMid slice" clip-path="url(#landscape_phone_screen)" />
      <!-- Subtle Front Camera Cutout (Top Edge Center) -->
      <circle cx="1900" cy="54" r="3.2" fill="#07060A" />
      <circle cx="1900" cy="54" r="1.5" fill="#151A2E" />
      <!-- Diagonal Glass Reflection Sweep -->
      <path d="M 1466,59 L 1820,59 L 1620,453 L 1466,453 Z" fill="url(#glass_glare)" clip-path="url(#landscape_phone_screen)" />
      <!-- Inner Bezel Stroke -->
      <rect x="1466" y="59" width="868" height="394" rx="18" ry="18" fill="none" stroke="rgba(0, 0, 0, 0.4)" stroke-width="1" />
    </g>
  </g>

  <!-- ========================================================= -->
  <!-- LEFT SIDE: BRAND IDENTITY & TECHNICAL PROPOSITION         -->
  <!-- ========================================================= -->
  <g id="brand_content">
    <!-- Top Pill: Category & Engine Badge -->
    <g id="category_badge">
      <rect x="180" y="68" width="345" height="30" rx="15" fill="rgba(138, 43, 226, 0.14)" stroke="rgba(138, 43, 226, 0.38)" stroke-width="1" />
      <!-- Live Indicator Dot -->
      <circle cx="200" cy="83" r="3.5" fill="#00D2FF" />
      <circle cx="200" cy="83" r="7" fill="none" stroke="#00D2FF" stroke-opacity="0.3" stroke-width="1.2" />
      <text x="216" y="87" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="11.5" font-weight="700" letter-spacing="1.5" fill="#E0D4FC">FREE &amp; OPEN SOURCE · POWERED BY LIBMPV</text>
    </g>

    <!-- Official Brand Lockup: Official Squircle Icon + Bold Wordmark -->
    <g id="brand_lockup">
      <!-- Official App Icon with Squircle Mask and Shadow -->
      <g filter="url(#shadow_icon)">
        <image href="data:image/png;base64,${iconPngB64}" x="180" y="120" width="96" height="96" clip-path="url(#squircle_icon_clip)" preserveAspectRatio="xMidYMid slice" />
        <rect x="180" y="120" width="96" height="96" rx="24" ry="24" fill="none" stroke="rgba(255, 255, 255, 0.18)" stroke-width="1.5" />
      </g>

      <!-- Brand Wordmark -->
      <text x="298" y="194" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="76" font-weight="900" letter-spacing="-2.5px" fill="#FFFFFF">mpv<tspan fill="url(#wordmark_grad)">ium</tspan></text>

      <!-- Subtitle Version / Platform Badge -->
      <rect x="618" y="150" width="86" height="24" rx="12" fill="rgba(255, 255, 255, 0.06)" stroke="rgba(255, 255, 255, 0.12)" stroke-width="1" />
      <text x="661" y="166" text-anchor="middle" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="10.5" font-weight="700" letter-spacing="1" fill="#A8ADB9">v1.5 · APP</text>
    </g>

    <!-- Tagline: "Sane defaults, pro-level control." -->
    <text x="180" y="264" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="30" font-weight="700" letter-spacing="-0.5px" fill="#F2ECF3">
      Sane defaults, <tspan fill="#D0BCFF">pro-level control.</tspan>
    </text>

    <!-- Subtitle / Brand Descriptor -->
    <text x="180" y="302" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="16.5" font-weight="400" fill="#A8ADB9">
      The modern Android media player. Material 3 design, background playback, and full mpv power.
    </text>

    <!-- Restrained Technical Feature Pills (5 Key Pillars) -->
    <g id="feature_pills" transform="translate(180, 356)">
      <!-- Pill 1: Powerful Playback (libmpv) -->
      <g transform="translate(0, 0)">
        <rect width="174" height="36" rx="10" fill="rgba(255, 255, 255, 0.04)" stroke="rgba(138, 43, 226, 0.32)" stroke-width="1" />
        <g transform="translate(14, 9)">${icons.play}</g>
        <text x="38" y="23" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="600" fill="#DDD5E8">Libmpv Playback</text>
      </g>

      <!-- Pill 2: Modern Material 3 UI -->
      <g transform="translate(186, 0)">
        <rect width="154" height="36" rx="10" fill="rgba(255, 255, 255, 0.04)" stroke="rgba(138, 43, 226, 0.32)" stroke-width="1" />
        <g transform="translate(14, 9)">${icons.ui}</g>
        <text x="38" y="23" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="600" fill="#DDD5E8">Material 3 UI</text>
      </g>

      <!-- Pill 3: Network Streaming -->
      <g transform="translate(352, 0)">
        <rect width="190" height="36" rx="10" fill="rgba(255, 255, 255, 0.04)" stroke="rgba(138, 43, 226, 0.32)" stroke-width="1" />
        <g transform="translate(14, 9)">${icons.stream}</g>
        <text x="38" y="23" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="600" fill="#DDD5E8">Network Streaming</text>
      </g>

      <!-- Pill 4: Full mpv.conf & Lua -->
      <g transform="translate(554, 0)">
        <rect width="180" height="36" rx="10" fill="rgba(255, 255, 255, 0.04)" stroke="rgba(138, 43, 226, 0.32)" stroke-width="1" />
        <g transform="translate(14, 9)">${icons.config}</g>
        <text x="38" y="23" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="600" fill="#DDD5E8">mpv.conf &amp; Lua</text>
      </g>

      <!-- Pill 5: No Ads, No Trackers -->
      <g transform="translate(746, 0)">
        <rect width="206" height="36" rx="10" fill="rgba(255, 255, 255, 0.04)" stroke="rgba(138, 43, 226, 0.32)" stroke-width="1" />
        <g transform="translate(14, 9)">${icons.shield}</g>
        <text x="38" y="23" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="600" fill="#DDD5E8">No Ads · No Trackers</text>
      </g>
    </g>
  </g>
</svg>`;
}

// -------------------------------------------------------------
// MOBILE BANNER (1600 x 480 px)
// -------------------------------------------------------------
function generateMobileSvg() {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 480" width="1600" height="480">
  <defs>
    <!-- Background Gradients -->
    <linearGradient id="mobile_bg" x1="0" y1="0" x2="1600" y2="480" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#140A24" />
      <stop offset="35%" stop-color="#0C0715" />
      <stop offset="70%" stop-color="#06040A" />
      <stop offset="100%" stop-color="#030206" />
    </linearGradient>

    <radialGradient id="mobile_glow_brand" cx="340" cy="220" r="420" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#8A2BE2" stop-opacity="0.22" />
      <stop offset="50%" stop-color="#8A2BE2" stop-opacity="0.06" />
      <stop offset="100%" stop-color="#8A2BE2" stop-opacity="0" />
    </radialGradient>

    <radialGradient id="mobile_glow_device" cx="1240" cy="240" r="480" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#6B1FB2" stop-opacity="0.25" />
      <stop offset="45%" stop-color="#00D2FF" stop-opacity="0.09" />
      <stop offset="100%" stop-color="#000000" stop-opacity="0" />
    </radialGradient>

    <linearGradient id="mobile_top_border" x1="0" y1="0" x2="1600" y2="0" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#FF416C" stop-opacity="0" />
      <stop offset="20%" stop-color="#FF416C" stop-opacity="0.45" />
      <stop offset="50%" stop-color="#8A2BE2" stop-opacity="0.75" />
      <stop offset="80%" stop-color="#00D2FF" stop-opacity="0.5" />
      <stop offset="100%" stop-color="#00D2FF" stop-opacity="0" />
    </linearGradient>

    <linearGradient id="mobile_wordmark_grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#FFA8BC" />
      <stop offset="45%" stop-color="#D0BCFF" />
      <stop offset="100%" stop-color="#70D7EB" />
    </linearGradient>

    <linearGradient id="mobile_glass_glare" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.12" />
      <stop offset="35%" stop-color="#FFFFFF" stop-opacity="0.03" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
    </linearGradient>

    <linearGradient id="mobile_device_rim" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.32" />
      <stop offset="35%" stop-color="#8A2BE2" stop-opacity="0.42" />
      <stop offset="70%" stop-color="#00D2FF" stop-opacity="0.28" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.12" />
    </linearGradient>

    <!-- Clip Paths -->
    <clipPath id="mobile_icon_clip">
      <rect x="90" y="106" width="84" height="84" rx="22" ry="22" />
    </clipPath>

    <clipPath id="mobile_player_screen">
      <rect x="866" y="94" width="636" height="292" rx="16" ry="16" />
    </clipPath>

    <!-- Shadows -->
    <filter id="mobile_shadow_deep" x="-10%" y="-10%" width="125%" height="130%">
      <feDropShadow dx="0" dy="20" stdDeviation="24" flood-color="#000000" flood-opacity="0.85" />
      <feDropShadow dx="0" dy="4" stdDeviation="8" flood-color="#000000" flood-opacity="0.40" />
    </filter>

    <filter id="mobile_shadow_icon" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#000000" flood-opacity="0.7" />
      <feDropShadow dx="0" dy="0" stdDeviation="18" flood-color="#8A2BE2" flood-opacity="0.35" />
    </filter>

    <pattern id="mobile_film_perfs" width="18" height="6" patternUnits="userSpaceOnUse">
      <rect x="3" y="1" width="12" height="4" rx="1" fill="#06040A" opacity="0.6"/>
    </pattern>
  </defs>

  <!-- Base Dark Canvas -->
  <rect width="1600" height="480" fill="url(#mobile_bg)" />

  <!-- Atmospheric Glows -->
  <circle cx="340" cy="220" r="420" fill="url(#mobile_glow_brand)" />
  <circle cx="1240" cy="240" r="480" fill="url(#mobile_glow_device)" />

  <!-- Cinematic Top Accent -->
  <rect x="0" y="0" width="1600" height="6" fill="#181126" />
  <rect x="0" y="0" width="1600" height="6" fill="url(#mobile_film_perfs)" />
  <line x1="0" y1="6" x2="1600" y2="6" stroke="url(#mobile_top_border)" stroke-width="1.5" />

  <!-- Bottom Horizon -->
  <line x1="0" y1="479" x2="1600" y2="479" stroke="rgba(255, 255, 255, 0.05)" stroke-width="1" />

  <!-- ========================================================= -->
  <!-- RIGHT SIDE: HERO PLAYER DISPLAY (PROPORTIONED FOR MOBILE) -->
  <!-- Balanced margins: 90px left, 92px right                   -->
  <!-- ========================================================= -->
  <g id="mobile_device_hero">
    <!-- Drop Shadow -->
    <rect x="860" y="88" width="648" height="304" rx="20" ry="20" fill="#000000" filter="url(#mobile_shadow_deep)" />
    <!-- Outer Titanium Bezel -->
    <rect x="860" y="88" width="648" height="304" rx="20" ry="20" fill="#16131D" stroke="url(#mobile_device_rim)" stroke-width="1.6" />
    <!-- Display Cushion -->
    <rect x="864" y="92" width="640" height="296" rx="18" ry="18" fill="#09080D" />
    <!-- Screenshot: Landscape Video Player with Controls -->
    <image href="data:image/jpeg;base64,${playerJpgB64}" x="866" y="94" width="636" height="292" preserveAspectRatio="xMidYMid slice" clip-path="url(#mobile_player_screen)" />
    <!-- Front Camera Cutout -->
    <circle cx="1184" cy="91" r="2.8" fill="#07060A" />
    <circle cx="1184" cy="91" r="1.3" fill="#151A2E" />
    <!-- Diagonal Glass Glare Reflection -->
    <path d="M 866,94 L 1144,94 L 984,386 L 866,386 Z" fill="url(#mobile_glass_glare)" clip-path="url(#mobile_player_screen)" />
    <!-- Inner Screen Stroke -->
    <rect x="866" y="94" width="636" height="292" rx="16" ry="16" fill="none" stroke="rgba(0, 0, 0, 0.4)" stroke-width="1" />
  </g>

  <!-- ========================================================= -->
  <!-- LEFT SIDE: MOBILE-OPTIMIZED BRAND LOCKUP & SAFE CONTENT   -->
  <!-- Notice: Placed comfortably away from lower-left avatar    -->
  <!-- ========================================================= -->
  <g id="mobile_brand_content">
    <!-- Top Pill: Category -->
    <g id="mobile_category_badge">
      <rect x="90" y="58" width="300" height="28" rx="14" fill="rgba(138, 43, 226, 0.16)" stroke="rgba(138, 43, 226, 0.40)" stroke-width="1" />
      <circle cx="108" cy="72" r="3.2" fill="#00D2FF" />
      <circle cx="108" cy="72" r="6" fill="none" stroke="#00D2FF" stroke-opacity="0.3" stroke-width="1.2" />
      <text x="124" y="76" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="11" font-weight="700" letter-spacing="1.4" fill="#E0D4FC">LIBMPV · ANDROID MEDIA PLAYER</text>
    </g>

    <!-- Brand Lockup: Official App Icon + High Legibility Wordmark -->
    <g id="mobile_lockup">
      <g filter="url(#mobile_shadow_icon)">
        <image href="data:image/png;base64,${iconPngB64}" x="90" y="106" width="84" height="84" clip-path="url(#mobile_icon_clip)" preserveAspectRatio="xMidYMid slice" />
        <rect x="90" y="106" width="84" height="84" rx="22" ry="22" fill="none" stroke="rgba(255, 255, 255, 0.20)" stroke-width="1.5" />
      </g>

      <!-- Wordmark -->
      <text x="194" y="172" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="64" font-weight="900" letter-spacing="-2px" fill="#FFFFFF">mpv<tspan fill="url(#mobile_wordmark_grad)">ium</tspan></text>
      
      <!-- Sub-badge -->
      <rect x="472" y="132" width="76" height="22" rx="11" fill="rgba(255, 255, 255, 0.06)" stroke="rgba(255, 255, 255, 0.12)" stroke-width="1" />
      <text x="510" y="147" text-anchor="middle" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="10" font-weight="700" letter-spacing="1" fill="#A8ADB9">v1.5</text>
    </g>

    <!-- Tagline: Large & High Contrast -->
    <text x="90" y="232" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="25" font-weight="700" letter-spacing="-0.4px" fill="#F2ECF3">
      Sane defaults, <tspan fill="#D0BCFF">pro-level control.</tspan>
    </text>

    <!-- Clean Subtitle Descriptor -->
    <text x="90" y="268" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="15" font-weight="500" fill="#A8ADB9">
      Free &amp; open source · Material 3 design · Ad-free
    </text>

    <!-- Restrained High-Legibility Badges (Safely above avatar collision zone) -->
    <g id="mobile_feature_pills" transform="translate(90, 312)">
      <!-- Pill 1 -->
      <g transform="translate(0, 0)">
        <rect width="148" height="34" rx="9" fill="rgba(255, 255, 255, 0.05)" stroke="rgba(138, 43, 226, 0.35)" stroke-width="1" />
        <g transform="translate(12, 8)">${icons.play}</g>
        <text x="36" y="22" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="12.5" font-weight="600" fill="#DDD5E8">Libmpv Core</text>
      </g>

      <!-- Pill 2 -->
      <g transform="translate(160, 0)">
        <rect width="168" height="34" rx="9" fill="rgba(255, 255, 255, 0.05)" stroke="rgba(138, 43, 226, 0.35)" stroke-width="1" />
        <g transform="translate(12, 8)">${icons.config}</g>
        <text x="36" y="22" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="12.5" font-weight="600" fill="#DDD5E8">Full mpv.conf</text>
      </g>

      <!-- Pill 3 -->
      <g transform="translate(340, 0)">
        <rect width="144" height="34" rx="9" fill="rgba(255, 255, 255, 0.05)" stroke="rgba(138, 43, 226, 0.35)" stroke-width="1" />
        <g transform="translate(12, 8)">${icons.shield}</g>
        <text x="36" y="22" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="12.5" font-weight="600" fill="#DDD5E8">100% Private</text>
      </g>
    </g>
  </g>
</svg>`;
}

async function run() {
  console.log('Generating Desktop Banner (2560 x 512)...');
  const desktopSvg = generateDesktopSvg();
  const desktopSvgPath = path.join(sourceDir, 'desktop-banner.svg');
  const desktopPngPath = path.resolve(__dirname, 'desktop-banner.png');
  fs.writeFileSync(desktopSvgPath, desktopSvg);

  await sharp(Buffer.from(desktopSvg))
    .png({ quality: 100, compressionLevel: 9 })
    .toFile(desktopPngPath);
  console.log('✓ Desktop banner exported:', desktopPngPath);

  console.log('Generating Mobile Banner (1600 x 480)...');
  const mobileSvg = generateMobileSvg();
  const mobileSvgPath = path.join(sourceDir, 'mobile-banner.svg');
  const mobilePngPath = path.resolve(__dirname, 'mobile-banner.png');
  fs.writeFileSync(mobileSvgPath, mobileSvg);

  await sharp(Buffer.from(mobileSvg))
    .png({ quality: 100, compressionLevel: 9 })
    .toFile(mobilePngPath);
  console.log('✓ Mobile banner exported:', mobilePngPath);

  console.log('All banners successfully generated!');
}

run().catch(err => {
  console.error('Error generating banners:', err);
  process.exit(1);
});
