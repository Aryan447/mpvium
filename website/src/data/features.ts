export interface Feature {
  title: string;
  description: string;
  /** Inner SVG markup, rendered with `set:html`. */
  icon: string;
}

export const features: Feature[] = [
  {
    title: "Modern Material 3 UI",
    description: "Expressive light & dark themes, intuitive browsing.",
    icon: '<rect x="3" y="5" width="18" height="14" rx="3"/><path d="M10 9.5v5l4.5-2.5z"/>',
  },
  {
    title: "Advanced Config",
    description: "Full mpv.conf, input.conf and Lua script support.",
    icon: '<path d="M4 8h16M4 16h16"/><circle cx="9" cy="8" r="2"/><circle cx="15" cy="16" r="2"/>',
  },
  {
    title: "Picture-in-Picture",
    description: "Keep watching while you multitask.",
    icon: '<rect x="3" y="4" width="18" height="12" rx="2"/><rect x="13" y="16" width="8" height="5" rx="1"/>',
  },
  {
    title: "Background Playback",
    description: "Audio-only mode for music and podcasts.",
    icon: '<path d="M3 11l9-7 9 7"/><path d="M6 10v9h12v-9"/>',
  },
  {
    title: "Network Streaming",
    description: "Open URLs plus SMB, FTP and WebDAV.",
    icon: '<circle cx="12" cy="12" r="2"/><path d="M8.5 8.5a5 5 0 000 7M15.5 8.5a5 5 0 010 7M5.5 5.5a9 9 0 000 13M18.5 5.5a9 9 0 010 13"/>',
  },
  {
    title: "Subtitles & Audio",
    description: "External tracks, online search, fine-tuned delay.",
    icon: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M7 12h4M7 15h7"/>',
  },
  {
    title: "File Management",
    description: "Folder & tree views, playlists, history, resume.",
    icon: '<path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>',
  },
  {
    title: "Private & Ad-Free",
    description: "No trackers, no ads, no unnecessary permissions.",
    icon: '<rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 018 0v3"/>',
  },
];

/** Extra capabilities from the README, shown on /features. */
export const moreFeatures: string[] = [
  "Hardware-accelerated, high-quality playback through libmpv",
  "Gesture controls, zoom, and screen orientation controls",
  "Chapters, playlists, playback history, and resume support",
  "Local media browsing with folder and tree views",
  "Custom mpv configuration, scripts, and advanced playback options",
  "Media information and metadata caching",
];
