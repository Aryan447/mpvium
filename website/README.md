# mpvium website

Astro + Tailwind site for mpvium, deployed free on Cloudflare Pages as a
static site (`https://mpvium.pages.dev`).

## Develop

Requires Bun 1.x (pinned via `bun --version`; Node 20+ also works, see `.nvmrc`).

```bash
cd website
bun install
bun run dev
```

## Build

```bash
bun run build    # outputs to website/dist/
bun run preview  # preview the production build
```

## Deploy on Cloudflare Pages (free)

1. Dashboard → Pages → Connect the `mpvium` GitHub repo.
2. Name the project `mpvium` → live at `https://mpvium.pages.dev`.
3. Build settings: **Root directory** `website`, Framework preset `Astro`,
   build command `bun run build` (fallback: `npm run build` — `package.json`
   is manager-agnostic), output directory `dist`, env var `NODE_VERSION=22`.
4. Deploy. Output is fully static (no Functions), so it fits the free tier:
   unlimited bandwidth, 500 builds/month. Every pull request gets a free
   preview deployment.
5. Optional: set `site:` in `astro.config.mjs` to your production URL
   (e.g. `https://mpvium.pages.dev`) for correct canonical/OG URLs.

## Notes

- The legacy GitHub Pages site has been removed. `docs/` now only holds the
  README assets and the full screenshot collection.
- Screenshots are a curated subset in `public/screenshots/`. The full set lives
  in the repo's `docs/screenshots/` — copy files over and extend
  `src/data/screenshots.ts` to grow the gallery.
- `src/pages/changelog.astro` renders the root `CHANGELOG.md` at build time via
  `src/data/changelog.ts`. Add changes to `CHANGELOG.md` only; the site picks
  them up on the next deployment.
- Theme preference persists in `localStorage` (`mpvium-theme`).
