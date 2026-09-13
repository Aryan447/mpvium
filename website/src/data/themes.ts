export interface Theme {
  id: string;
  name: string;
  primary: string;
  bg: string;
}

export const themes: Theme[] = [
  { id: "default", name: "Default", primary: "#E8B5EF", bg: "#161217" },
  { id: "dynamic", name: "Dynamic", primary: "#D0BCFF", bg: "#1C1B1F" },
  { id: "cinema", name: "Cinema", primary: "#FFD27A", bg: "#0F0607" },
  { id: "noir", name: "Noir Cinema", primary: "#E6E1D5", bg: "#0A0B0D" },
];

export const defaultTheme = "cinema";
