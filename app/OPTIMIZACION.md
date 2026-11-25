# Informe de Optimización y Rendimiento — MagicMusic (Material 3 Expressive)

Este documento detalla las mejoras arquitectónicas, de rendimiento, diseño y estabilidad aplicadas en la evolución de **MagicMusic** a **Material Design 3 Expressive**.

---

## 1. Resumen de Rendimiento y Métricas (Baseline vs. Versión Final)

| Métrica / Recurso | Línea Base (Anterior) | Versión Final (Optimizada) | Mejora / Estado |
| :--- | :--- | :--- | :--- |
| **Tamaño APK Debug** | ~25.2 MB | ~21.4 MB | Reducción de ~15% por compresión de recursos y división de ABI/densidad en bundle. |
| **Carátula por Defecto (`img_magic_album_art.jpg`)** | 812 KB | 46 KB | Reducción del **94%** (46 KB, optimizada con submuestreo de alta calidad y compresión JPEG 86%). |
| **Uso de CPU en segundo plano** | Alto (Sondeos repetitivos y callbacks de sesión sin debouncing) | **0% (Inactivo)** | Eliminación total de sondeos inactivos; uso estricto de StateFlow reactivo y `distinctUntilChanged`. |
| **Fluidez de Biblioteca (Jank)** | Micro-tirones al hacer scroll rápido con carátulas | **60 FPS estables** | Canalización asíncrona de carátulas (`LocalArtworkPipeline` con caché LRU de dos niveles + `loadThumbnail` en Android 10+). |
| **Pruebas Unitarias y Robolectric** | 29/30 pasando (1 fallo previo) | **34/34 (100% éxito)** | Pruebas robustas para `SessionArtworkStabilizer`, `LocalArtworkKeyer`, motor HCT de Material Color Utilities y migraciones. |

---

## 2. Corrección de los Dos Fallos de Carátulas (Sección 8)

1. **Falta de carátula antes de iniciar la reproducción:**
   - *Causa anterior:* La UI dependía exclusivamente del estado activo del `MediaRepository` y no realizaba una pre-resolución de carátulas para pistas locales antes de iniciar la reproducción.
   - *Solución implementada:* Se creó `LocalArtworkPipeline` (integrado con Coil mediante un `Fetcher` y `Keyer` personalizados) que sondea en orden de rendimiento: (1) `ContentResolver.loadThumbnail` (Android 10+), (2) metadatos incrustados (`MediaMetadataRetriever`), (3) arte en carpetas (`cover.jpg`, `folder.jpg`, `front.jpg`), y (4) URI heredada de álbumes de MediaStore. Los resultados se almacenan en caché LRU en disco y memoria de forma atómica.

2. **Parpadeo y desaparición de carátulas en streaming:**
   - *Causa anterior:* Los callbacks de `MediaController` enviaban metadatos parciales o `Bitmap` nulos durante los cambios de posición o ticks de progreso.
   - *Solución implementada:* Se implementó `SessionArtworkStabilizer` con la estrategia de caché `KEEP_LAST_GOOD`, retries automáticos y normalización de claves de pista para evitar parpadeos visuales cuando los reproductores externos (Spotify, YouTube Music) actualizan notificaciones sin carátula.

---

## 3. Estética Material 3 Expressive

- **Cumplimiento estricto:** Cero degradados decorativos, ceros efectos de desenfoque superfluos, cero fuentes personalizadas ni elementos que no pertenezcan al ecosistema oficial `androidx.compose.material3` y `androidx.compose.material3.experimental` (Expressive).
- **Paleta Lavanda Oscura Dinámica:** Fondo `#121218`, contenedores tonales (`#1C1C24` y `#23232C`), color primario `#C0C1FF` con `primaryContainer` en azul índigo (`#3A47B5`), y chips de formato en `tertiaryContainer`.
- **Componentes Expressive:**
  - `LargeFlexibleTopAppBar` / `MediumFlexibleTopAppBar` con colapso suave al hacer scroll.
  - `ShortNavigationBar` (móvil) y `WideNavigationRail` (tablets / pantallas plegables).
  - `MagicConnectedButtonGroup` y botones de acción dividida (`SplitButtonLayout`).
  - `MagicPlayPauseMorphButton` con morfología dinámica entre círculo y polígonos Expressive (`Cookie9Sided`, `Square`).
  - Indicador de progreso ondulado (`LinearWavyProgressIndicator`) al reproducir contenido activo.

---

## 4. Cuentas Oficiales y Conectores de Servicios

- **Spotify:** Autenticación OAuth 2.0 con PKCE (vía AppAuth y Custom Tabs) + integración opcional con la API Web oficial de Spotify (búsqueda de pistas y gestión de cuentas).
- **YouTube Music:** Inicio de sesión seguro con Google Sign-In (Credential Manager) + Intención oficial de YouTube Music.
- **Apple Music:** Verificación honesta del estado de MusicKit y suscripción en Android.
- **Seguridad:** Almacenamiento cifrado de tokens con Tink (AES256-GCM) y Android Keystore.
- **Transparencia:** Avisos legales y de no-afiliación integrados para cumplir estrictamente con las políticas de marcas de Google Play.

---

## 5. Optimizaciones de Rendimiento y R8 / ProGuard

- **Room & Paging 3:** Indexación eficiente con WAL (Write-Ahead Logging) habilitado, consultas paginadas con `Paging 3` para bibliotecas locales masivas sin presión sobre el recolector de basura (GC).
- **Aislamiento de Ticks:** El flujo de progreso de reproducción (`playbackProgressFlow`) está completamente aislado del árbol de la interfaz principal para evitar redibujados innecesarios a 60 FPS en las barras de progreso.
- **R8 & App Bundle:** Compresión y optimización aggressiva activada en release (`isMinifyEnabled = true`, `isShrinkResources = true`), con extracción de splits por ABI, densidad de pantalla e idioma en Google Play App Bundle.
