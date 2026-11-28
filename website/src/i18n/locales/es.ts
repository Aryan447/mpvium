import type { SiteTranslations } from "../types";

export const es: SiteTranslations = {
  meta: {
    title: "mpvium — Valores predeterminados sensatos, control profesional",
    description:
      "mpvium — valores predeterminados sensatos, control profesional. Pensado para ver contenido todos los días, totalmente personalizable para expertos. Reproductor de video para Android impulsado por mpv.",
    featuresTitle: "Funciones — mpvium",
    featuresDescription:
      "Todo lo que hace mpvium: reproducción con mpv, interfaz Material 3, PiP, audio en segundo plano, streaming en red, subtítulos y configuración profesional.",
    doctrineTitle: "Doctrina — mpvium",
    doctrineDescription:
      "Las reglas sobre las que se construye mpvium: valores predeterminados sensatos, control profesional, fidelidad de reproducción, privacidad, eficiencia y honestidad sobre los cambios.",
    screenshotsTitle: "Capturas de pantalla — mpvium",
    screenshotsDescription:
      "mpvium en acción: biblioteca de inicio, cuadrícula de películas, detalles y el reproductor impulsado por mpv.",
    changelogTitle: "Registro de cambios — mpvium",
    changelogDescription:
      "Novedades en mpvium: los últimos cambios visibles para el usuario, generados a partir del registro de cambios del proyecto.",
    downloadTitle: "Descargar — mpvium",
    downloadDescription:
      "Descarga los APK de mpvium: compilaciones estables y preliminares para cada arquitectura de CPU.",
    notFoundTitle: "404 — mpvium",
    notFoundDescription: "Página no encontrada.",
  },
  nav: {
    features: "Funciones",
    doctrine: "Doctrina",
    screenshots: "Capturas",
    changelog: "Cambios",
    download: "Descargar",
    openMenu: "Abrir menú",
    closeMenu: "Cerrar menú",
    languageLabel: "Idioma",
  },
  hero: {
    badge: "Gratis y de código abierto · GPL-3.0-or-later · impulsado por mpv",
    tagline: "Valores predeterminados sensatos, control profesional.",
    subtitle:
      "Pensado para ver contenido todos los días, totalmente personalizable para expertos — toda la potencia de mpv sin la tarea de editar archivos de configuración. Interfaz Material 3, reproducción en segundo plano, imagen en imagen.",
    downloadStable: "Descargar estable (arm64-v8a)",
    checkingPreview: "Comprobando versión preliminar…",
    resolvingVersions: "Resolviendo últimas versiones…",
    downloadStableTag: "Descargar estable {tag} (arm64-v8a)",
    downloadPreviewTag: "Descargar preliminar {tag} (arm64-v8a)",
    noPreview: "Sin compilación preliminar",
    latestMeta: "Última: {parts}",
    releasesUnavailable: "Lanzamientos disponibles en GitHub.",
    logoAlt: "Logotipo de mpvium",
    screenshotAlt: "Captura de pantalla del reproductor mpvium",
  },
  why: {
    heading: "Por qué mpvium",
    introPrefix:
      "La mayoría de las interfaces para mpv se quedan en el motor. La mayoría de los reproductores de video se quedan en la biblioteca.",
    introHighlight: "mpvium es el punto de encuentro",
    introSuffix:
      "— reproducción con libmpv, una biblioteca real de Series y Películas, y controles pensados para pantalla táctil, sin renunciar a los ajustes avanzados.",
    col1Eyebrow: "Muchos forks de mpv",
    col1Title: "Primero el motor",
    col1Body:
      "Potencia de reproducción y libertad de configuración, pero la biblioteca y el flujo táctil quedan por tu cuenta.",
    col1Bullets: [
      "Motor de reproducción mpv completo",
      "mpv.conf, scripts Lua y configuración",
      "Interfaz minimalista centrada en el explorador de archivos",
    ],
    col2Eyebrow: "mpvium",
    col2Title: "Ambos, por defecto",
    col2Body:
      "El mismo motor libmpv, envuelto en una biblioteca completa y un reproductor diseñado para el tacto.",
    col2Bullets: [
      "Biblioteca automática de Series y Películas",
      "Material 3 Expressive, 16 temas",
      "mpv.conf, Lua y modos de decodificación",
      "Saltar intro, Explicar, subtítulos en línea",
    ],
    col3Eyebrow: "Muchos reproductores de video",
    col3Title: "Primero la biblioteca",
    col3Body:
      "Una biblioteca y controles familiares, pero el motor y los ajustes finos suelen ser fijos.",
    col3Bullets: [
      "Biblioteca y reproducción pulidas",
      "Interfaz táctil amigable",
      "Motor y controles de códec predefinidos",
    ],
    engineHeading: "La diferencia del motor",
    engineCompare: "Comparaciones visuales en mpv.rocks ↗",
    engineBody:
      "La reproducción pasa por libmpv, el mismo motor de renderizado que mpv — no un decodificador genérico de Android.",
    engineBenefits: [
      {
        title: "Colores tal como se concibieron",
        description:
          "Se respetan los metadatos de color de origen, con mapeo de tonos HDR para tu pantalla — sin saturación, contraste ni nitidez artificiales.",
      },
      {
        title: "Eliminación de bandas integrada",
        description:
          "El debanding por CPU o GPU suaviza los degradados escalonados en cielos, niebla y escenas oscuras.",
      },
      {
        title: "Detalle que se mantiene",
        description:
          "Escalado de alta calidad, sobremuestreo de croma y desentrelazado mantienen los bordes y el movimiento nítidos.",
      },
      {
        title: "Subtítulos tal como se crearon",
        description:
          "Las fuentes, colores, posición y efectos ASS/SSA se renderizan exactamente como el lanzamiento pretendía.",
      },
    ],
  },
  home: {
    featuresHeading: "Funciones",
    featuresSubtitle: "Todo lo que necesitas desde el principio, sin estorbos.",
    exploreFeatures: "Explorar todas las funciones →",
    shotsHeading: "Capturas de pantalla",
    shotsSubtitle: "Biblioteca, exploración, reproducción — tema oscuro predeterminado.",
    viewGallery: "Ver galería →",
    themesHeading: "Temas",
    themesSubtitle:
      "Todos los temas de la aplicación — los 16. Elige uno: toda esta página cambia de aspecto como la app.",
    ctaHeading: "Listo para ver",
    ctaBody:
      "Consigue el último APK en GitHub Releases, o sigue el desarrollo en las comunidades de Discord, Telegram y Reddit.",
    ctaGet: "Obtener mpvium",
    ctaDiscord: "Unirse a Discord",
    ctaTelegram: "Unirse a Telegram",
    ctaReddit: "Unirse a Reddit",
  },
  featuresPage: {
    heading: "Funciones",
    subtitle: "Valores predeterminados sensatos desde el principio, control profesional cuando lo quieras.",
    andMore: "Y mucho más",
    items: [
      {
        title: "Biblioteca autocategorizada",
        description:
          "Tus archivos organizados en Series y Películas automáticamente — incluye pósteres y filas de A continuación y Añadido recientemente.",
      },
      {
        title: "Salto automático de intro y resumen",
        description:
          "Botones de salto con un toque en el reproductor, más una caché sin conexión que salta las intros por ti.",
      },
      {
        title: "Botón Explicar",
        description:
          "Toca cualquier palabra del diálogo para consultar diccionarios y explicaciones de Wikipedia.",
      },
      {
        title: "Estadísticas de la biblioteca",
        description:
          "Tamaño de la colección, almacenamiento, duración, progreso de visualización y actividad semanal en un solo panel.",
      },
      {
        title: "Interfaz moderna Material 3",
        description:
          "Animaciones expresivas, controles dinámicos y 16 temas con modos claro y oscuro.",
      },
      {
        title: "Subtítulos y búsqueda en línea",
        description:
          "Pistas externas, búsqueda en OpenSubtitles y SubSource opcional, estilos y pellizco para cambiar el tamaño.",
      },
      {
        title: "Configuración avanzada",
        description: "Soporte completo para mpv.conf, input.conf y scripts Lua.",
      },
      {
        title: "Imagen en imagen",
        description: "Sigue viendo mientras realizas otras tareas.",
      },
      {
        title: "Reproducción en segundo plano",
        description: "Modo solo audio para música y pódcasts.",
      },
      {
        title: "Streaming en red",
        description: "Abre URL además de SMB, FTP y WebDAV.",
      },
      {
        title: "Gestión de archivos",
        description: "Vistas de carpetas y árbol, listas de reproducción, historial y reanudación.",
      },
      {
        title: "Privado y sin anuncios",
        description: "Sin rastreadores, sin anuncios, sin permisos innecesarios.",
      },
    ],
    moreItems: [
      "Reproducción de alta calidad acelerada por hardware mediante libmpv",
      "Controles gestuales, zoom y controles de orientación de pantalla",
      "Capítulos, listas de reproducción, historial de reproducción y reanudación",
      "Exploración de medios locales con vistas de carpetas y árbol",
      "Configuración personalizada de mpv, scripts y opciones avanzadas de reproducción",
      "Información multimedia y almacenamiento en caché de metadatos",
    ],
  },
  screenshotsPage: {
    heading: "Capturas de pantalla",
    subtitle: "Biblioteca, exploración, reproducción — tema oscuro predeterminado.",
    morePrefix: "¿Buscas todos los temas y el modo claro? La colección completa está en",
    moreLink: "docs/screenshots",
    moreSuffix: "en el repositorio.",
    shots: [
      {
        alt: "Reproductor mpvium",
        title: "Reproductor",
        caption: "Potencia de mpv, controles táctiles cómodos.",
      },
      {
        alt: "Reproductor mpvium con saltar intro",
        title: "Saltar intro",
        caption: "Un toque para saltar intros y resúmenes.",
      },
      {
        alt: "Biblioteca de inicio de mpvium",
        title: "Inicio",
        caption: "Seguir viendo y explorar.",
      },
      {
        alt: "Cuadrícula de títulos de mpvium",
        title: "Títulos",
        caption: "Series y películas en una sola cuadrícula.",
      },
      {
        alt: "Biblioteca de mpvium",
        title: "Biblioteca",
        caption: "Series, películas y carpetas.",
      },
      {
        alt: "Detalles de película en mpvium",
        title: "Detalles",
        caption: "Vista de ficha de película.",
      },
      {
        alt: "Recorrido de bienvenida de mpvium",
        title: "Bienvenida",
        caption: "Recorrido inicial de configuración.",
      },
    ],
  },
  downloadPage: {
    heading: "Descargar",
    subtitle:
      "Gratis y de código abierto, sin anuncios ni rastreadores. Los botones de descarga apuntan a los últimos APK arm64-v8a; la versión preliminar se habilita cuando existe un lanzamiento preliminar.",
    tableHeading: "Descargas APK",
    colApk: "APK",
    colDevices: "Dispositivos",
    apks: [
      { file: "universal", devices: "La mayoría de los dispositivos; descarga más pesada" },
      {
        file: "arm64-v8a",
        devices: "Dispositivos ARM modernos de 64 bits; recomendado para la mayoría de los teléfonos",
      },
      { file: "armeabi-v7a", devices: "Dispositivos ARM antiguos de 32 bits" },
      { file: "x86", devices: "Dispositivos Intel/AMD de 32 bits" },
      { file: "x86_64", devices: "Dispositivos Intel/AMD de 64 bits" },
    ],
    footerPrefix:
      "Las compilaciones preliminares están pensadas para pruebas y pueden contener funciones sin terminar. Las sumas de verificación se publican en cada",
    footerRelease: "lanzamiento",
    footerMiddle: ". ¿Prefieres compilar desde el código fuente? Consulta el",
    footerReadme: "README",
    footerSuffix: ".",
  },
  doctrinePage: {
    generatedFrom: "Generado a partir de",
    readFile: "Leer el archivo ↗",
    linkTo: "Enlace a",
  },
  changelogPage: {
    heading: "Novedades",
    subtitlePrefix: "Las notas de lanzamiento completas con sumas de verificación están en la",
    subtitleLink: "página de lanzamientos",
    subtitleSuffix: ".",
    empty: "Aún no hay entradas en el registro de cambios.",
  },
  notFoundPage: {
    title: "Falta este carrete.",
    body: "La página que buscas no existe.",
    backHome: "Volver al inicio",
  },
  footer: {
    releases: "Lanzamientos",
    doctrine: "Doctrina",
    abiNote: "Los APK estables y preliminares son arm64-v8a. Otras ABI en la página de lanzamientos.",
  },
};
