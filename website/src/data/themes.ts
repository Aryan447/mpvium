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
  { id: "glass", name: "Glass", primary: "#F2F2F2", bg: "#000000" },
  { id: "forest", name: "Forest", primary: "#9BD79C", bg: "#0C1510" },
  { id: "rosegold", name: "Rose Gold", primary: "#F5B4C1", bg: "#1D1114" },
  { id: "violet", name: "Violet", primary: "#C9B0FF", bg: "#150E27" },
  { id: "sapphire", name: "Sapphire", primary: "#AEC6FF", bg: "#0A1122" },
  { id: "sunset", name: "Sunset", primary: "#FFB690", bg: "#1E0E05" },
  { id: "ocean", name: "Ocean", primary: "#70D7EB", bg: "#071416" },
  { id: "gruvbox", name: "Gruvbox", primary: "#FE8019", bg: "#282828" },
  { id: "kanagawa", name: "Kanagawa", primary: "#7E9CD8", bg: "#1F1F28" },
  { id: "doom", name: "Doom", primary: "#C678DD", bg: "#282C34" },
  { id: "rosepine", name: "Rosé Pine", primary: "#EBBCBA", bg: "#191724" },
];

export const defaultTheme = "cinema";
