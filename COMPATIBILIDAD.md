# Matriz de Compatibilidad de MagicMusic

**Versión mínima (`minSdk`):** Android 8.0 (API 26)
**Versión objetivo (`targetSdk` / `compileSdk`):** Android 16 (API 36)

> **Justificación de `minSdk = 26`:** Cubre >96 % de los dispositivos Android activos y garantiza canales de notificación nativos (`NotificationChannel`), `startForegroundService`, `AppWidgetManager.requestPinAppWidget`, `NotificationListenerService.requestRebind` y estabilidad en `TileService` sin parches frágiles para Android 7.

---

## 1. Compatibilidad por versión de Android

| Capacidad | API mínima | Comportamiento en versiones anteriores (fallback) |
|---|---|---|
| Controles multimedia universales (`MediaSessionManager` + `NotificationListenerService`) | API 26+ | Funciona en todas las versiones soportadas (con reconexión automática vía `requestRebind`). |
| Fijar widget desde la app (`requestPinAppWidget`) | API 26+ | Si el launcher no soporta `isRequestPinAppWidgetSupported`, se muestran instrucciones para añadirlo manualmente desde el escritorio. |
| Panel en pantalla de bloqueo (`showWhenLocked` / `turnScreenOn`) | API 27+ (XML + Activity flags) | En API 26 se usan los flags heredados de ventana (`FLAG_SHOW_WHEN_LOCKED` / `FLAG_TURN_SCREEN_ON`). |
| Subtítulo en Quick Settings Tiles (`Tile.subtitle`) | API 29+ (`Q`) | En API 26–28 se muestra el título principal en `Tile.label` y la descripción accesible en `contentDescription`. |
| Color dinámico Material You (`dynamicColorScheme`) | API 31+ (`S`) | Paleta tonal estática Material 3 completa generada a partir de color semilla. |
| Desenfoque GPU (`RenderEffect`) en UI | API 31+ (`S`) | Superficie translúcida con gradiente y borde de contraste sin coste de CPU. |
| Añadir Quick Settings Tile con un toque (`StatusBarManager.requestAddTileService`) | API 33+ (`TIRAMISU`) | Guía visual paso a paso para arrastrar el tile desde *Ajustes Rápidos > Editar*. |
| Permiso en tiempo de ejecución `POST_NOTIFICATIONS` | API 33+ (`TIRAMISU`) | No requerido por el sistema en API 26–32 (concedido en instalación). |
| Retroceso predictivo (`enableOnBackInvokedCallback`) | API 33+ (activo por defecto en API 35+) | Animación estándar de retroceso en API 26–32. |
| Declaración estricta `foregroundServiceType="mediaPlayback"` | API 34+ (`UPSIDE_DOWN_CAKE`) | `startForeground(...)` estándar sin parámetro de tipo en API 26–28; `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` en API 29+. |
| Vistas previas generadas de widgets (`generatedPreviewLayout`) | API 35+ (`VANILLA_ICE_CREAM`) | Previsualización estática con `initialLayout` / `previewImage` en API 26–34. |
| Controles multimedia Android 16 en bloqueo y Live Updates | API 36 (`BAKLAVA`) | `Notification.MediaStyle` estándar con `PlaybackState.CustomAction` en API 26–35. |

---

## 2. Compatibilidad por fabricante (`OemCompat`)

MagicMusic **no utiliza APIs privadas ni específicas de ningún fabricante**. El módulo `OemCompat` detecta `Build.MANUFACTURER` / `Build.BRAND` únicamente para mostrar guías de ajuste de batería y enlaces seguros (con `resolveActivity` + `try/catch`) a pantallas estándar del sistema:

- **Samsung (One UI):** *Batería > Límites de uso en segundo plano > Aplicaciones nunca inactivas* + *Optimización de batería: No restringido*.
- **Xiaomi / Redmi / POCO (HyperOS / MIUI):** *Ajustes > Aplicaciones > MagicMusic > Ahorro de batería: Sin restricciones* + *Inicio automático* + candado en recientes.
- **OPPO / Realme / OnePlus (ColorOS / OxygenOS):** *Información de app > Uso de batería > Permitir actividad en segundo plano* e *Inicio automático*.
- **Honor / Huawei:** *Ajustes > Batería > Inicio de aplicaciones > MagicMusic > Gestionar manualmente* (activar *Inicio automático*, *Inicio secundario* y *Ejecutar en segundo plano*).
- **vivo / iQOO (Funtouch OS / OriginOS):** *Uso de batería en segundo plano > Permitir consumo elevado* + *Inicio automático*.
- **Motorola / Google Pixel / Sony / Nothing:** *Información de app > Batería de la app > Sin restricciones*.

---

## 3. Política de privacidad y uso de permisos

- `BIND_NOTIFICATION_LISTENER_SERVICE`: Se utiliza **exclusivamente** para leer metadatos de reproducción (`MediaMetadata`, `PlaybackState`) y enviar comandos de transporte (`play`, `pause`, `next`, `previous`, `seekTo`) al reproductor activo. No lee mensajes, correos ni notificaciones personales y ningún dato sale del dispositivo.
- `<queries>` declarativas en `AndroidManifest.xml`: Descubre reproductores compatibles sin solicitar `QUERY_ALL_PACKAGES`.
