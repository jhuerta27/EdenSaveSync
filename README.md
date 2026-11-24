# Eden Save Sync 🔄🎮

**Sincroniza automáticamente tus partidas de Eden y Peanuts Native entre todos tus dispositivos Android usando tu propio Google Drive.**

Empieza una partida en tu teléfono, tablet o consola portátil Android —ya sea en **Eden** o en **Peanuts Native**— y continúala exactamente donde la dejaste en otro dispositivo o emulador — **sin cables, sin PC, sin root y sin servidores externos**.

> Compatible con cualquier teléfono, plegable, tablet o consola portátil Android (con soporte completo para pantalla táctil y mando/gamepad).

## 📸 Capturas de pantalla

| Pantalla principal | Sincronización en 1 toque | Ajustes y automatización |
| :---: | :---: | :---: |
| ![Pantalla principal](docs/screenshots/home.png) | ![Sincronización](docs/screenshots/sync.png) | ![Ajustes](docs/screenshots/settings.png) |

## 🚀 Cómo empezar (en 3 pasos)

### 1. Descarga e instala la app
1. Ve a la sección [**Releases**](https://github.com/jhuerta27/EdenSaveSync/releases) de este repositorio y descarga el último archivo `.apk`.
2. Ábrelo en tu dispositivo Android para instalarlo *(requiere Android 8.0 o superior y tener **Eden**, **Peanuts Native** o ambos instalados)*.

### 2. Vincula tus carpetas de partidas locales
Puedes vincular **solo Eden**, **solo Peanuts Native** o **ambos a la vez** (necesitas al menos uno vinculado):

#### Carpeta de Eden
1. En la columna **LOCAL**, toca en **«Eden»**.
2. En el selector de archivos de Android:
   - **En Android 14, 15 o 16 (recomendado)**: abre el menú lateral (`☰`), elige **Eden**, entra en `nand → user → save` y pulsa **«Usar esta carpeta»**.
   - **En consolas Android o rutas personalizadas**: también puedes seleccionar directamente `Android/data/dev.eden.eden_emulator/files/nand/user/save`.

#### Carpeta de Peanuts Native *(requiere Shizuku en Android 11+)*
Peanuts Native (`dev.nxnative.android.peanuts`) guarda las partidas en `Android/data/dev.nxnative.android.peanuts/files/saves/` y actualmente no expone un proveedor de documentos (`DocumentsProvider`). Como Android 11+ restringe el acceso a `Android/data/` desde el selector de archivos, **es necesario usar [Shizuku](https://shizuku.rikka.app/)** en teléfonos Android 11–16 (como Samsung Galaxy):

1. Instala la app **Shizuku** e iníciala desde el propio móvil usando **Depuración inalámbrica** *(sin root ni PC)*.
2. En **Eden Save Sync**, toca en **«Peanuts Native»** dentro de la columna **LOCAL**.
3. Pulsa **«Conectar con Shizuku»** y acepta el permiso en el diálogo de Shizuku. La app quedará vinculada automáticamente como **«Vinculada (Shizuku)»**.
4. *(Alternativa)* En consolas Android sin restricción en `Android/data` o si usas una carpeta personalizada, puedes utilizar el botón **«Abrir carpeta saves o personalizada»** (selector SAF).

> **Nota:** Si reinicias el dispositivo, recuerda volver a iniciar Shizuku mediante Depuración inalámbrica antes de sincronizar partidas de Peanuts Native.

### 3. Conecta tu Google Drive y sincroniza
1. En la columna **REMOTO**, toca en **«Google Drive»** y pulsa **«Iniciar sesión con Google»**.
2. Elige tu cuenta de Google en el navegador y acepta el permiso. Volverás automáticamente a la app.
3. Pulsa **«Sincronizar ahora»** para subir o descargar todas tus partidas en un solo toque.

Repite estos 3 pasos en tu otro dispositivo Android con la misma cuenta de Google y tus partidas quedarán siempre sincronizadas.

> 💡 **Recomendación para la sincronización automática en segundo plano:** en los ajustes de Android (*Ajustes → Aplicaciones → Eden Save Sync → Batería*), selecciona el modo **Sin restricciones** (*Unrestricted*). Así evitarás que el sistema (especialmente One UI en Samsung u otros modos de ahorro de batería) retrase o suspenda las sincronizaciones automáticas en segundo plano.

## ✨ Qué hace por ti

- **☁️ Tu nube, tus partidas**: Guarda tus partidas comprimidas en una carpeta llamada `EdenSaveSync` dentro de **tu propio Google Drive**.
- **🔁 Sincronización unificada entre Eden y Peanuts Native**:
  - **Al subir**: detecta automáticamente la partida más reciente de cada juego (hayas jugado en Eden o en Peanuts Native), la sube a Google Drive y, si tienes ambos vinculados, la replica automáticamente en la otra carpeta local.
  - **Al descargar y restaurar backups**: escribe la partida en todas las carpetas locales vinculadas (Eden y/o Peanuts Native) para mantenerlas siempre igualadas.
- **🧠 Detecta automáticamente qué partida es más reciente**: Compara el contenido real (huella SHA-256) y la fecha de guardado para decirte qué conviene subir y qué conviene descargar.
- **🔀 Compatible aunque cada dispositivo tenga un perfil distinto**: Cada instalación de Eden crea un identificador de perfil diferente; la app los adapta automáticamente al sincronizar para que el emulador reconozca tus partidas sin hacer nada manual.
- **🛡️ Copias de seguridad automáticas antes de sobrescribir**: Antes de reemplazar una partida en tu dispositivo, guarda automáticamente un respaldo local (conserva los últimos 5 por juego) que puedes restaurar en 1 toque si lo necesitas.
- **⏰ Sincronización automática en segundo plano**: Desde **Ajustes (⚙️)** puedes activar el modo **Automático** (comprueba cambios en segundo plano aproximadamente cada **3 minutos** incluso con la app cerrada, además de al bloquear, desbloquear o volver a la app) o el modo **Por hora** (una vez al día a la hora elegida), con notificación opcional del resultado. Para máxima fiabilidad, se recomienda configurar la batería de la app como **Sin restricciones** en los ajustes de Android.
- **🎮 Preparada para mando y pantalla táctil**: Navega cómodamente tanto con el dedo como con el D-pad / joysticks de tu consola portátil.
- **📋 Nombres de juegos automáticos**: Reconoce los títulos automáticamente y además te permite renombrar cualquier juego a tu gusto.

## 🔒 Privacidad y seguridad

- **Cada usuario usa su propia cuenta de Google Drive**: Nadie más tiene acceso a tus partidas ni hay ningún servidor intermedio.
- **Permiso mínimo (`drive.file`)**: La app solo puede ver y gestionar los archivos que ella misma crea dentro de la carpeta `EdenSaveSync`; no puede leer ni tocar ningún otro archivo de tu Google Drive.
- **Acceso Shizuku acotado**: Cuando se usa Shizuku para Peanuts Native, el servicio únicamente accede a la carpeta de guardados `Android/data/dev.nxnative.android.peanuts/files/saves/`.
- Consulta todos los detalles en nuestra [Política de Privacidad](PRIVACY.md).

## 🛠️ Catálogo de juegos

La app incluye un catálogo offline empaquetado en [`app/src/main/assets/titles.json`](app/src/main/assets/titles.json) y se actualiza automáticamente desde este mismo archivo en el repositorio de GitHub para reconocer nuevos Title IDs sin tener que actualizar el APK.

## ⚠️ Aviso Legal

**Eden Save Sync** es una herramienta comunitaria de código abierto sin ninguna afiliación, patrocinio ni respaldo por parte de **Nintendo Co., Ltd.**, del equipo de desarrollo del emulador **Eden** ni de **Peanuts Native**.

Este proyecto **no incluye, aloja ni distribuye ROMs, juegos, firmware ni claves criptográficas**. Su único propósito es facilitar la copia de seguridad y sincronización de archivos de partidas guardadas propiedad del usuario entre sus propios dispositivos.

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
