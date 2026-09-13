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
    src: "/screenshots/mpvium-dark-default-home.png",
    alt: "mpvium home library",
    title: "Home",
    caption: "Continue watching & browse.",
  },
  {
    src: "/screenshots/mpvium-dark-default-movies.png",
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
    src: "/screenshots/mpvium-movie-showcase.png",
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
