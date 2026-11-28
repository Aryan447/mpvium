import { DEFAULT_LOCALE, type LocaleCode } from "./locales";
import type { SiteTranslations } from "./types";
import { ar } from "./locales/ar";
import { de } from "./locales/de";
import { en } from "./locales/en";
import { es } from "./locales/es";
import { fr } from "./locales/fr";
import { hi } from "./locales/hi";
import { id } from "./locales/id";
import { it } from "./locales/it";
import { ja } from "./locales/ja";
import { ko } from "./locales/ko";
import { pl } from "./locales/pl";
import { ptBr } from "./locales/pt-br";
import { ru } from "./locales/ru";
import { tr } from "./locales/tr";
import { uk } from "./locales/uk";
import { vi } from "./locales/vi";
import { zhCn } from "./locales/zh-cn";
import { zhTw } from "./locales/zh-tw";

export const TRANSLATIONS: Record<LocaleCode, SiteTranslations> = {
  en,
  es,
  fr,
  de,
  "pt-br": ptBr,
  it,
  ru,
  uk,
  pl,
  tr,
  ar,
  hi,
  id,
  vi,
  "zh-cn": zhCn,
  "zh-tw": zhTw,
  ja,
  ko,
};

export function getTranslations(locale: LocaleCode = DEFAULT_LOCALE): SiteTranslations {
  return TRANSLATIONS[locale] ?? en;
}
