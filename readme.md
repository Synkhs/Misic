<h1 align="center">MagicMusic</h1>

<p align="center">
  <strong>Toda tu música, un solo control.</strong>
</p>

<p align="center">
  <img src="assets/cover.png" alt="Portada de MagicMusic: control unificado de música, widgets configurables, pantalla de bloqueo, Ajustes Rápidos y reproductor local Hi-Res para Android 8.0+" width="100%" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 o superior" />
  <img src="https://img.shields.io/badge/Precio-Gratis-CDA9FF?style=for-the-badge" alt="Precio: Gratis" />
  <img src="https://img.shields.io/badge/Actualizado-24%20de%20noviembre%20de%202026-68459E?style=for-the-badge" alt="Última actualización: 24 de noviembre de 2026" />
  <img src="https://img.shields.io/badge/Privacidad-100%25%20En%20el%20dispositivo-2A1A4A?style=for-the-badge" alt="Privacidad local en el dispositivo" />
</p>

<p align="center">
  <a href="https://github.com/Synkhs/Misic">
    <img src="https://img.shields.io/github/stars/Synkhs/Misic?style=social" alt="Estrellas en GitHub" />
  </a>
</p>

---

## 📌 Índice

* [Título e imagen de portada](#magicmusic)
* [Insignias](#magicmusic)
* [Índice](#-índice)
* [Descripción del proyecto](#-descripción-del-proyecto)
* [Estado del proyecto](#-estado-del-proyecto)
* [Demostración de funciones y aplicación](#-demostración-de-funciones-y-aplicación)
* [Acceso al proyecto](#-acceso-al-proyecto)
* [Abre y ejecuta el proyecto](#%EF%B8%8F-abre-y-ejecuta-el-proyecto)
* [Tecnologías utilizadas](#-tecnologías-utilizadas)
* [Personas contribuyentes](#-personas-contribuyentes)
* [Personas desarrolladoras del proyecto](#-personas-desarrolladoras-del-proyecto)
* [Licencia](#-licencia)

---

## 📝 Descripción del proyecto

**MagicMusic** es una aplicación gratuita para **Android 8.0 o superior** que unifica el control de toda tu música en una sola app, con widgets personalizables, panel en pantalla de bloqueo y mosaicos en Ajustes Rápidos para cualquier teléfono Android, sin importar la marca del fabricante.

Funciona de manera universal con **Spotify**, **YouTube Music**, **Amazon Music**, **Apple Music**, **Deezer**, **TIDAL** y cualquier reproductor compatible con la API estándar **MediaSession** de Android. Además, incorpora su propio **reproductor de música local en alta resolución (Hi-Res)**.

### Pilares de privacidad

* **Todo ocurre en el dispositivo:** la detección de sesiones multimedia, la biblioteca local y los ajustes de widgets se procesan íntegramente dentro de tu teléfono.
* **Uso estricto del acceso a notificaciones:** este permiso se utiliza únicamente para leer los datos de reproducción activa (*título, artista, carátula y estado*) y enviar comandos básicos de control. MagicMusic no lee mensajes ni otras notificaciones.
* **Sin cuentas ni servidores:** no requiere registro ni inicio de sesión, y no existen servidores externos que almacenen tu información.

---

## 🚧 Estado del proyecto

<h4 align="center">
  :sparkles: Aplicación disponible y en evolución continua :sparkles:
</h4>

Las funciones principales de control multimedia, estudio de widgets, pantalla de bloqueo, Ajustes Rápidos y motor local Hi-Res ya están disponibles.

### Hoja de ruta (Próximamente)

Las siguientes características están planificadas y **aún no se encuentran disponibles**:

* **Próximamente:** Automatizaciones (acciones automáticas al conectar auriculares Bluetooth o iniciar un reproductor).
* **Próximamente:** Atajos de la app desde el icono en el lanzador.
* **Próximamente:** Historial ampliado de reproducción local.
* **Próximamente:** Tarjeta compartible con carátula y datos de la pista.
* **Próximamente:** Copia de seguridad completa de ajustes y plantillas.

---

## 🔨 Demostración de funciones y aplicación

### Funcionalidades disponibles

* `Integración universal`: detecta automáticamente las aplicaciones de música activas, permite cambiar entre ellas, abrir la app de origen y definir prioridades (*reproduciendo primero*, *última activa* o *app preferida*).
* `Estudio de widgets`: formatos Tarjeta (`4×2`), Cápsula (`4×1`), Cuadrado (`2×2`) y Mini (`2×1` / `1×1`); cuatro estilos (*Tonal*, *Superficie*, *Carátula* y *Transparente*); radio de esquinas, opacidad, barra de progreso, insignia del servicio y botones configurables; plantillas exportables en JSON.
* `Pantalla de bloqueo`: panel de control completo con carátula, controles directos de reproducción y opciones de privacidad para controlar qué se muestra con el dispositivo bloqueado.
* `Ajustes Rápidos`: siete mosaicos nativos para **Música**, **Siguiente**, **Anterior**, **Aleatorio**, **Repetir**, **Me gusta** y **Temporizador**.
* `Reproducción completa`: vista **Ahora suena**, **letras**, **cola de reproducción**, **temporizador de apagado** con fundido progresivo de volumen y **selector de salida de audio**.
* `Biblioteca local y Motor Hi-Res`: navegación por listas, álbumes, artistas y carpetas; compatibilidad con `FLAC`, `ALAC`, `WAV`, `OGG`, `OPUS`, `MP3` y `AAC`; reproducción sin pausas (*gapless*), *crossfade*, **ReplayGain** y **ecualizador**.
* `Diseño Material Design 3`: color dinámico del sistema (*Material You*), extracción de paleta desde la carátula y modos **Claro**, **Oscuro** y **AMOLED**.
* `Estado del sistema`: semáforo visual de permisos y guía de optimización de batería adaptada según el fabricante (Samsung, Xiaomi, Oppo, Honor, Motorola, Google Pixel, entre otros).

### Capturas de la aplicación

> *Nota: Para visualizar tus capturas directamente aquí, guarda las imágenes dentro de la carpeta `assets/screens/` de este repositorio con los nombres indicados.*

| Inicio y sesiones activas | Estudio de widgets | Pantalla de bloqueo |
| :---: | :---: | :---: |
| <img src="assets/screens/inicio.png" alt="Pantalla de Inicio con sesiones activas y semáforo de estado" width="240" /> | <img src="assets/screens/estudio-widgets.png" alt="Estudio de widgets Material 3" width="240" /> | <img src="assets/screens/bloqueo.png" alt="Panel de control y privacidad en pantalla de bloqueo" width="240" /> |

| Ajustes Rápidos (7 mosaicos) | Biblioteca local | Ecualizador y Motor Hi-Res |
| :---: | :---: | :---: |
| <img src="assets/screens/ajustes-rapidos.png" alt="Configuración de los 7 mosaicos en Ajustes Rápidos" width="240" /> | <img src="assets/screens/biblioteca.png" alt="Biblioteca local indexada por canciones, listas, artistas y álbumes" width="240" /> | <img src="assets/screens/ecualizador.png" alt="Ecualizador de bandas y mejoras Hi-Res" width="240" /> |

### Compatibilidad por versión de Android

| Función | Android 8.0 – 10 | Android 11 – 12 | Android 13+ |
| :--- | :---: | :---: | :---: |
| Control universal (`MediaSession`) | Disponible | Disponible | Disponible |
| Widgets (`4×2`, `4×1`, `2×2`, Mini) | Disponible | Disponible | Disponible |
| Panel de bloqueo con privacidad | Disponible | Disponible | Disponible |
| 7 mosaicos de Ajustes Rápidos | Disponible | Disponible | Disponible *(requiere permiso de notificaciones)* |
| Reproductor local Hi-Res y ecualizador | Disponible | Disponible | Disponible |
| Color dinámico (*Material You*) | Paleta desde carátula | Sistema en Android 12+ | Disponible |

---

## 📁 Acceso al proyecto

Puedes acceder al código fuente y descargar la aplicación gratuita directamente desde el repositorio oficial en GitHub:

* **Repositorio oficial:** [https://github.com/Synkhs/Misic](https://github.com/Synkhs/Misic)
* **Sección de lanzamientos (Releases):** [https://github.com/Synkhs/Misic/releases](https://github.com/Synkhs/Misic/releases)

---

## 🛠️ Abre y ejecuta el proyecto

### Instalación en tu dispositivo Android

1. Descarga el archivo instalador (`.apk`) desde [https://github.com/Synkhs/Misic](https://github.com/Synkhs/Misic) en un teléfono con **Android 8.0 o superior**.
2. Instala la aplicación y ábrela.
3. Concede el permiso de **Acceso a notificaciones** cuando te lo solicite para habilitar la detección de tus reproductores activos.
4. *(Opcional)* Entra en **Ajustes → Estado del sistema** dentro de MagicMusic para verificar el semáforo de permisos y aplicar la guía de batería de tu fabricante.

### Clonar el repositorio

```bash
git clone https://github.com/Synkhs/Misic.git
cd Misic
```

---

## 💻 Tecnologías utilizadas

* **Android SDK (API 26+ / Android 8.0 o superior):** plataforma base de ejecución compatible con dispositivos de cualquier fabricante.
* **Android `MediaSession` & `NotificationListenerService`:** detección universal de sesiones multimedia activas y envío de controles de reproducción sin depender de APIs privadas de terceros.
* **Material Design 3 (Material You):** interfaz adaptativa con color dinámico del sistema, extracción cromática desde carátulas y soporte nativo para temas Claro, Oscuro y AMOLED.
* **Android AppWidgets (`Glance` / RemoteViews):** renderizado personalizable de widgets en formatos `4×2`, `4×1`, `2×2`, `2×1` y `1×1` con plantillas exportables en JSON.
* **Quick Settings `TileService`:** integración directa con la cortina de Ajustes Rápidos del sistema mediante 7 mosaicos independientes.
* **Motor de audio local Hi-Res:** decodificación y reproducción sin pausas (*gapless*) para `FLAC`, `ALAC`, `WAV`, `OGG`, `OPUS`, `MP3` y `AAC`, con *crossfade*, **ReplayGain** y ecualizador de frecuencias.

---

## 🤝 Personas contribuyentes

Las sugerencias, reportes de errores y mejoras son bienvenidos para seguir haciendo crecer **MagicMusic**:

1. Haz un *Fork* de este repositorio (`https://github.com/Synkhs/Misic`).
2. Crea una rama con tu mejora (`git checkout -b feature/nueva-mejora`).
3. Realiza tus cambios y haz *commit* (`git commit -m "Añade nueva mejora"`).
4. Sube tu rama (`git push origin feature/nueva-mejora`) y abre un **Pull Request**.

También puedes abrir un **Issue** en el repositorio indicando el modelo de tu dispositivo y versión de Android si encuentras algún problema de compatibilidad.

---

## 👨‍💻 Personas desarrolladoras del proyecto

| [<img src="https://github.com/Synkhs.png" width="115" alt="Avatar de Synkhs"><br><sub><b>Synkhs</b></sub>](https://github.com/Synkhs) |
| :---: |

* **Última actualización:** 24 de noviembre de 2026
* **Precio:** Gratis

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **[Licencia del proyecto, por ejemplo: MIT]** (consulta el archivo `LICENSE` en el repositorio para más detalles).

> **Aviso de no afiliación:** MagicMusic es un proyecto independiente desarrollado por **Synkhs**. No está afiliada, asociada ni respaldada por Spotify, YouTube Music, Amazon Music, Apple Music, Deezer, TIDAL, Google, Samsung, Xiaomi, Oppo, Honor, Motorola ni ningún otro servicio o fabricante mencionado. Todas las marcas comerciales pertenecen a sus respectivos propietarios y se citan únicamente con fines descriptivos de compatibilidad técnica.