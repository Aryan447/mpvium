import { defineCollection } from "astro:content";
import { glob } from "astro/loaders";

// DOCTRINE.md lives at the repository root so the app, the README, and the site
// all quote the same file. The Doctrine page renders this collection.
const doctrine = defineCollection({
  loader: glob({ base: "../", pattern: "DOCTRINE.md", id: "doctrine" }),
});

export const collections = { doctrine };
