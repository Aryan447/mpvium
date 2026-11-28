export const DEFAULT_LOCALE = "en" as const;

export const LOCALES = [
  { code: "en", htmlLang: "en", nativeName: "English", dir: "ltr" },
  { code: "es", htmlLang: "es", nativeName: "Español", dir: "ltr" },
  { code: "fr", htmlLang: "fr", nativeName: "Français", dir: "ltr" },
  { code: "de", htmlLang: "de", nativeName: "Deutsch", dir: "ltr" },
  { code: "pt-br", htmlLang: "pt-BR", nativeName: "Português (Brasil)", dir: "ltr" },
  { code: "it", htmlLang: "it", nativeName: "Italiano", dir: "ltr" },
  { code: "ru", htmlLang: "ru", nativeName: "Русский", dir: "ltr" },
  { code: "uk", htmlLang: "uk", nativeName: "Українська", dir: "ltr" },
  { code: "pl", htmlLang: "pl", nativeName: "Polski", dir: "ltr" },
  { code: "tr", htmlLang: "tr", nativeName: "Türkçe", dir: "ltr" },
  { code: "ar", htmlLang: "ar", nativeName: "العربية", dir: "rtl" },
  { code: "hi", htmlLang: "hi", nativeName: "हिन्दी", dir: "ltr" },
  { code: "id", htmlLang: "id", nativeName: "Bahasa Indonesia", dir: "ltr" },
  { code: "vi", htmlLang: "vi", nativeName: "Tiếng Việt", dir: "ltr" },
  { code: "zh-cn", htmlLang: "zh-CN", nativeName: "简体中文", dir: "ltr" },
  { code: "zh-tw", htmlLang: "zh-TW", nativeName: "繁體中文", dir: "ltr" },
  { code: "ja", htmlLang: "ja", nativeName: "日本語", dir: "ltr" },
  { code: "ko", htmlLang: "ko", nativeName: "한국어", dir: "ltr" },
] as const;

export type LocaleCode = (typeof LOCALES)[number]["code"];

export const NON_DEFAULT_LOCALES = LOCALES.filter((l) => l.code !== DEFAULT_LOCALE);

export function getLocaleInfo(code?: string) {
  return LOCALES.find((l) => l.code === code) ?? LOCALES[0];
}

export function localePath(locale: LocaleCode, path: string): string {
  const normalized = path.startsWith("/") ? path : `/${path}`;
  if (locale === DEFAULT_LOCALE) return normalized;
  return normalized === "/" ? `/${locale}` : `/${locale}${normalized}`;
}
