import changelogMarkdown from "../../../CHANGELOG.md?raw";

export interface InlinePart {
  /** Rendered inside a `<code>` element instead of plain text. */
  code: boolean;
  text: string;
}

export interface ChangelogGroup {
  heading: string;
  items: InlinePart[][];
}

export interface ChangelogEntry {
  version: string;
  groups: ChangelogGroup[];
}

interface RawGroup {
  heading: string;
  items: string[];
}

/**
 * Splits a changelog line into plain-text and `code` segments so inline
 * backticks survive without pulling in a Markdown renderer.
 */
function inlineParts(value: string): InlinePart[] {
  return value
    .split(/(`[^`]+`)/g)
    .filter(Boolean)
    .map((part) =>
      part.length > 2 && part.startsWith("`") && part.endsWith("`")
        ? { code: true, text: part.slice(1, -1) }
        : { code: false, text: part },
    );
}

function normalizeVersion(value: string): string {
  const version = value.trim();
  return version.startsWith("V") ? `v${version.slice(1)}` : version;
}

/**
 * Parses the root CHANGELOG.md. The repository changelog is the source of
 * truth, so the site never needs its own copy of the release notes:
 * `## version`, `### Added|Changed|Fixed`, and `- item` lines are enough.
 */
function parseChangelog(markdown: string): ChangelogEntry[] {
  const entries: { version: string; groups: RawGroup[] }[] = [];
  let entry: (typeof entries)[number] | undefined;
  let group: RawGroup | undefined;

  for (const rawLine of markdown.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line) continue;

    const version = line.match(/^##\s+(.+)$/);
    if (version) {
      entry = { version: normalizeVersion(version[1]), groups: [] };
      entries.push(entry);
      group = undefined;
      continue;
    }

    const heading = line.match(/^###\s+(.+)$/);
    if (heading && entry) {
      group = { heading: heading[1].trim(), items: [] };
      entry.groups.push(group);
      continue;
    }

    const item = line.match(/^[-*]\s+(.+)$/);
    if (item && group) {
      group.items.push(item[1].trim());
      continue;
    }

    // Ignore the document title and horizontal rules.
    if (line.startsWith("#") || /^[-*_]{3,}$/.test(line)) continue;

    // Re-join bullet text that was wrapped across multiple lines.
    if (group && group.items.length > 0) {
      group.items[group.items.length - 1] += ` ${line}`;
    }
  }

  return entries
    .map(({ version, groups }) => ({
      version,
      groups: groups
        .filter((g) => g.items.length > 0)
        .map((g) => ({ heading: g.heading, items: g.items.map(inlineParts) })),
    }))
    .filter((e) => e.groups.length > 0);
}

export const changelog: ChangelogEntry[] = parseChangelog(changelogMarkdown);
