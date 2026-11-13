import changelogMarkdown from "../../../CHANGELOG.md?raw";

export interface InlinePart {
  /** `code` renders in a `<code>` chip, `bold` in a `<strong>`. */
  kind: "text" | "code" | "bold";
  text: string;
}

export interface ChangelogImage {
  src: string;
  alt: string;
  /**
   * Explicit display width for portrait phone shots. Landscape shots omit it
   * so they span the full text column, like the prose around them.
   */
  width?: number;
}

export interface ChangelogGroup {
  /** Empty for the release intro, which has no heading of its own. */
  heading: string;
  paragraphs: InlinePart[][];
  items: InlinePart[][];
  images: ChangelogImage[];
}

export interface ChangelogEntry {
  version: string;
  groups: ChangelogGroup[];
}

function inlineParts(value: string): InlinePart[] {
  return value
    .split(/(`[^`]+`|\*\*[^*]+\*\*)/g)
    .filter(Boolean)
    .map((part) => {
      if (part.length > 2 && part.startsWith("`") && part.endsWith("`")) {
        return { kind: "code", text: part.slice(1, -1) } as const;
      }
      if (part.length > 4 && part.startsWith("**") && part.endsWith("**")) {
        return { kind: "bold", text: part.slice(2, -2) } as const;
      }
      return { kind: "text", text: part } as const;
    });
}

function normalizeVersion(value: string): string {
  return `v${value.trim().replace(/^v/i, "")}`;
}

/**
 * Changelog images are written repo-relative so GitHub renders them:
 * `docs/screenshots/foo.jpg`. The site serves its own copy from
 * `website/public/screenshots/`, so rewrite the prefix here.
 */
function normalizeSrc(src: string): string {
  const value = src.trim();
  if (/^[a-z][a-z0-9+.-]*:/i.test(value) || value.startsWith("//")) return value;
  return `/screenshots/${value.replace(/^\.?\//, "").replace(/^.*docs\/screenshots\//, "")}`;
}

function attr(attributes: string, name: string): string | undefined {
  const match = attributes.match(new RegExp(`${name}\\s*=\\s*("([^"]*)"|'([^']*)')`, "i"));
  return match ? (match[2] ?? match[3]) : undefined;
}

/**
 * Reads a standalone `<img src="docs/screenshots/x.jpg" width="220" alt="…" />`
 * line, or the Markdown equivalent `![alt](docs/screenshots/x.jpg)`.
 */
function imageFrom(line: string): ChangelogImage | undefined {
  const html = line.match(/^<img\b([^>]*?)\/?>$/i);
  const markdown = line.match(/^!\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)$/);

  const src = html ? attr(html[1], "src") : markdown?.[2];
  if (!src) return undefined;

  const alt = html ? attr(html[1], "alt") : markdown?.[1];
  const width = html ? Number.parseInt(attr(html[1], "width") ?? "", 10) : Number.NaN;
  const explicitWidth = Number.isFinite(width) && width > 0 ? width : undefined;

  return {
    src: normalizeSrc(src),
    alt: alt ?? "",
    ...(explicitWidth ? { width: explicitWidth } : {}),
  };
}

const TITLE_LINE = /^#\s+(.+)$/;
const SECTION_LINE = /^##\s+(.+)$/;
const SUBSECTION_LINE = /^###\s+(.+)$/;
const BULLET_LINE = /^[-*]\s+(.+)$/;
const RULE_LINE = /^[-*_]{3,}$/;
const VERSION_TOKEN = /^v?\d+(?:\.\d+)*$/i;

/**
 * Parses the root CHANGELOG.md, which is written as a release announcement:
 *
 * ```markdown
 * # mpvium 1.5.0
 *
 * Intro paragraph, possibly several.
 *
 * ## A headline feature
 *
 * Prose about it, and a screenshot on its own line.
 *
 * <img src="docs/screenshots/x.jpg" width="220" alt="…" />
 *
 * ## More features
 *
 * - **Lead-in** and a line about it.
 * ```
 *
 * The `### Added|Changed|Fixed` + bullets shape still parses, so older
 * versions keep rendering.
 */
function parseChangelog(markdown: string): ChangelogEntry[] {
  const entries: ChangelogEntry[] = [];
  let entry: ChangelogEntry | undefined;
  let group: ChangelogGroup | undefined;
  let paragraph: string[] = [];

  const flushParagraph = () => {
    if (group && paragraph.length > 0) {
      group.paragraphs.push(inlineParts(paragraph.join(" ")));
    }
    paragraph = [];
  };

  const openGroup = (heading: string) => {
    if (!entry) return;
    flushParagraph();
    group = { heading, paragraphs: [], items: [], images: [] };
    entry.groups.push(group);
  };

  for (const rawLine of markdown.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line) {
      flushParagraph();
      continue;
    }
    if (RULE_LINE.test(line)) continue;

    const title = line.match(TITLE_LINE);
    if (title) {
      flushParagraph();
      const lastWord = title[1].trim().split(/\s+/).pop() ?? "";
      if (VERSION_TOKEN.test(lastWord)) {
        entry = { version: normalizeVersion(lastWord), groups: [] };
        entries.push(entry);
        openGroup("");
      } else {
        group = undefined;
      }
      continue;
    }

    const section = line.match(SECTION_LINE);
    if (section) {
      openGroup(section[1].trim());
      continue;
    }

    const subsection = line.match(SUBSECTION_LINE);
    if (subsection) {
      openGroup(subsection[1].trim());
      continue;
    }

    if (line.startsWith("#")) continue;

    const image = imageFrom(line);
    if (image) {
      flushParagraph();
      if (group) group.images.push(image);
      continue;
    }

    const bullet = line.match(BULLET_LINE);
    if (bullet) {
      flushParagraph();
      if (group) group.items.push(inlineParts(bullet[1].trim()));
      continue;
    }

    if (group) paragraph.push(line);
  }

  flushParagraph();

  return entries
    .map(({ version, groups }) => ({
      version,
      groups: groups.filter(
        (g) => g.paragraphs.length > 0 || g.items.length > 0 || g.images.length > 0,
      ),
    }))
    .filter((e) => e.groups.length > 0);
}

export const changelog: ChangelogEntry[] = parseChangelog(changelogMarkdown);
