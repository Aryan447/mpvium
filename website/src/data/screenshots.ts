export interface Shot {
  src: string;
  alt: string;
  title: string;
  caption: string;
  /** Landscape shots span the full grid row instead of one column. */
  wide?: boolean;
}

/**
 * Curated set shipped with the site. The full collection lives in
 * `docs/screenshots/` in the repo — copy more files into
 * `website/public/screenshots/` and extend this list to grow the gallery.
 */
export const shots: Shot[] = [
  {
    src: "/screenshots/mpvium-player.jpg",
    alt: "mpvium player",
    title: "Player",
    caption: "mpv power, touch-friendly controls.",
    wide: true,
  },
  {
    src: "/screenshots/mpvium-player-skip-intro.jpg",
    alt: "mpvium player with skip intro",
    title: "Skip intro",
    caption: "One tap past intros and recaps.",
    wide: true,
  },
  {
    src: "/screenshots/mpvium-dark-default-home.jpg",
    alt: "mpvium home library",
    title: "Home",
    caption: "Continue watching & browse.",
  },
  {
    src: "/screenshots/mpvium-dark-default-movies.jpg",
    alt: "mpvium movies grid",
    title: "Movies",
    caption: "Poster grid with durations.",
  },
  {
    src: "/screenshots/mpvium-dark-default-library.png",
    alt: "mpvium library",
    title: "Library",
    caption: "Shows, movies & folders.",
  },
  {
    src: "/screenshots/mpvium-movie-showcase.jpg",
    alt: "mpvium movie details",
    title: "Details",
    caption: "Movie showcase view.",
  },
  {
    src: "/screenshots/mpvium-welcome.png",
    alt: "mpvium welcome tour",
    title: "Welcome",
    caption: "First-run onboarding tour.",
  },
];
