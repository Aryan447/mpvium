import { defineConfig } from "astro/config";
import tailwindcss from "@tailwindcss/vite";

// Static output: free-tier friendly on Cloudflare Pages, no adapter needed.
// After deploying, set your production URL below for correct
// canonical URLs, sitemap, and social cards.
// site: "https://mpvium.pages.dev",
export default defineConfig({
  i18n: {
    defaultLocale: "en",
    locales: [
      "en",
      "es",
      "fr",
      "de",
      "pt-br",
      "it",
      "ru",
      "uk",
      "pl",
      "tr",
      "ar",
      "hi",
      "id",
      "vi",
      "zh-cn",
      "zh-tw",
      "ja",
      "ko",
    ],
    routing: {
      prefixDefaultLocale: false,
    },
  },
  vite: {
    plugins: [tailwindcss()],
  },
});
