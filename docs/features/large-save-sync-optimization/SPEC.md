# SPEC: Optimización de sincronización y extracción para partidas de gran tamaño y directorios anidados (Subnautica)

**Estado:** Aprobada <!-- Borrador | En revisión | Aprobada -->

<!-- PARA LA PERSONA
Copia esta plantilla como SPEC.md en una carpeta de la funcionalidad.
Pide al agente que la complete contigo usando MOBILE_GUIDELINES.md.
SPEC.md define qué debe cumplirse; PLAN.md desarrolla cómo implementarlo;
TASKS.md organiza los pasos de ejecución.
-->

<!-- PARA EL AGENTE
- Lee las instrucciones del proyecto y MOBILE_GUIDELINES.md. Inspecciona el
  repositorio para comprobar el comportamiento actual. Si falta la guía, pide su ubicación.
- Completa esta spec con la persona: investiga lo comprobable y consulta las
  decisiones pendientes. Haz pocas preguntas por vez y actualiza las respuestas.
- No inventes requisitos ni exclusiones. Distingue propuestas de decisiones
  confirmadas y marca como PENDIENTE lo que aún no esté resuelto.
- Aplica las consideraciones mobile relevantes sin ampliar el alcance automáticamente.
- No incluyas diseño de clases, tablas, componentes, archivos o algoritmos:
  esos detalles pertenecen a PLAN.md. Sí registra restricciones explícitas del pedido.
- Mantén el documento breve y proporcional a la funcionalidad. Conserva los comentarios.
- Un documento completo no está aprobado automáticamente. Solicita aprobación
  antes de marcarlo como Aprobada. No implementes durante esta etapa.
-->

## Qué construimos y para quién

Una optimización del motor de extracción, escaneo y reconciliación de partidas guardadas en Android Storage Access Framework (SAF) para usuarios de dispositivos móviles y consolas (como Samsung Galaxy Z Fold 8 y AYN Odin 2 Portal). Permite sincronizar y descargar con éxito partidas complejas compuestas por árboles de directorios profundos y cientos/miles de archivos (como *Subnautica* y *Subnautica Below Zero*), evitando bucles de sincronización fallida ("0 UP / 2 DOWN") y bloqueos de IPC Binder.

## Situación actual

Actualmente, juegos estándar con pocos archivos planos en la raíz se sincronizan correctamente. Sin embargo, al sincronizar partidas complejas con cientos o miles de archivos anidados (ej. `slot0000/CellsCache/*.bin` de Subnautica):
1. La descompresión de archivos en SAF realiza consultas repetitivas de búsqueda de directorio y borrado previo archivo por archivo, desencadenando cientos de miles de operaciones IPC sobre el Binder de Android, lo que genera saturación de `CursorWindow`, tiempos de espera excesivos o fallos de escritura silenciosos en One UI.
2. Si el proveedor SAF (como el `DocumentsProvider` de Eden en Android 14+) no expone marcas de tiempo de modificación de archivo (`COLUMN_LAST_MODIFIED = 0`), y la extracción previa quedó incompleta o con hash discrepante, el comparador asume perpetuamente que la nube es más reciente (`DOWNLOAD_FROM_CLOUD`).
3. El usuario pulsa *"Sincronizar ahora"*, la operación no logra estabilizarse en estado sincronizado y la interfaz vuelve a mostrar *"Sincronizar ahora (0 UP / 2 DOWN)"* de forma indefinida.

## Dentro del alcance

- **RF-01:** Extracción optimizada de archivos ZIP con estructura jerárquica a través de Storage Access Framework, eliminando la resolución redundante de carpetas intermedias y evitando búsquedas innecesarias en directorios limpios recién creados.
- **RF-02:** Verificación atómica y robusta del estado posdescarga: asegurar que los metadatos de sincronización (`SyncedRecord`: hash de contenido, marca temporal y conteo) queden registrados y reconciliados tras una importación exitosa, evitando falsos positivos de "Nube más reciente".
- **RF-03:** Resiliencia y soporte para proveedores SAF sin marca de tiempo de archivo (`COLUMN_LAST_MODIFIED` nula o igual a cero), recurriendo a los metadatos sincronizados o al timestamp de extracción para evitar que una partida local quede anclada en timestamp 0.
- **RF-04:** Manejo adecuado de tipos MIME en la creación de archivos anidados (ej. `.bin`, `.json`, `.jpg`) para evitar que el sistema operativo o el proveedor modifique extensiones de archivo o genere duplicados `(1)`.

## Fuera de alcance

- Modificaciones en el emulador Eden o en su `DocumentsProvider` externo.
- Cambios en el protocolo de Google Drive REST API v3 o en el formato de archivo remoto `.zip`.
- Soporte para resolución manual de conflictos archivo por archivo (la unidad de sincronización se mantiene a nivel de partida/TitleID completo).

## Flujo de usuario

1. El usuario abre Eden Save Sync en su dispositivo (ej. Samsung Galaxy Z Fold 8) teniendo partidas en la nube de juegos complejos (ej. *Subnautica* y *Subnautica Below Zero* subidas desde una Odin 2 Portal).
2. La pantalla principal detecta las partidas remotas y, si no existen localmente o están desactualizadas, las marca como recomendadas para descargar (botón *"Sincronizar ahora (0 UP / 2 DOWN)"*).
3. El usuario pulsa *"Sincronizar ahora"* (o el botón individual de descarga en la tarjeta del juego).
4. La aplicación descarga el archivo temporal, extrae de manera fluida y eficiente todos los directorios y archivos sin saturar Binder ni abortar por timeout, y computa el estado local final.
5. Al finalizar el escaneo posdescarga, las tarjetas de dichos juegos pasan al estado verde **"Al día"** (`IN_SYNC`), y el botón principal de sincronización se actualiza a *"Todo sincronizado"* sin solicitar descargas repetidas.

## Datos y reglas de negocio

- **Unidad de guardado:** Todo el contenido bajo el directorio del Title ID (16 caracteres hexadecimales) representa la partida íntegra, incluyendo subdirectorios y ficheros con cualquier profundidad.
- **Reconciliación de estado:**
  - Si el hash SHA-256 local coincide con el hash del archivo en la nube, el juego está en estado `IN_SYNC`.
  - Si una partida se acaba de descargar con éxito desde la nube y el contenido extraído coincide en estructura/hash, se actualiza el registro local sincronizado (`SyncedRecord`) con la marca de tiempo remota para blindar la coherencia ante proveedores SAF que no persistan `lastModified`.
  - Una partida recién importada no debe considerarse obsoleta (`DOWNLOAD_FROM_CLOUD`) de inmediato tras completarse su descarga.
- **Seguridad previa a la sobrescritura:** Antes de limpiar y sobreescribir el directorio local de guardado, se conserva la copia de seguridad comprimida previa (hasta 5 copias rotatorias locales).

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Muestra el progreso de sincronización indicando el juego actual y desactiva pulsaciones repetidas en el botón de sincronización. La operación corre en un hilo secundario sin congelar la interfaz. |
| Sin datos | Si no existen partidas remotas ni locales, muestra la vista vacía informativa. |
| Entrada inválida | Si el ZIP descargado está corrupto o truncado, se aborta la importación, se preserva el backup previo y se reporta el error sin dejar la carpeta en estado corrupto. |
| Error o espera excesiva | Si un archivo individual falla al escribirse por timeout o desconexión del proveedor SAF, se cancela la operación de forma limpia, reportando un aviso al usuario sin colapsar el proceso ni la interfaz. |
| Sin conexión o conexión interrumpida | Se cancela la descarga activa con mensaje de error claro en el banner y se permite reintentar una vez restablecida la red. |
| Cancelar o volver atrás | Las tareas asíncronas respetan `CancellationException` de Kotlin Coroutines y no dejan archivos temporales huérfanos en `cacheDir`. |
| Pasar a segundo plano y regresar | La sincronización en curso continúa sin reiniciarse ni perderse; al regresar al primer plano, la UI refleja el estado actualizado. |
| Recrear la pantalla | El progreso y el estado de la sincronización se mantienen en el `StateFlow` del ViewModel ante rotaciones o pliegue/despliegue del Galaxy Z Fold 8. |
| Reabrir después de terminarse el proceso | La información de partidas ya sincronizadas persiste en `AppPreferences` (`synced_records_json`), manteniendo el estado consistente. |
| Otros puntos aplicables de la guía | Rendimiento y recursos: el uso de memoria durante la extracción e inspección se mantiene acotado con búferes fijos (16 KB) y sin instanciación masiva de cursores de Binder. |

**Puntos de la guía no aplicables y motivo:** No aplica gestión de formularios complejos ni autenticación biométrica en esta funcionalidad (se enfoca exclusivamente en la capa de sincronización y almacenamiento SAF).

## Restricciones del pedido

- Mantener estricta compatibilidad con Storage Access Framework (SAF) tanto para `ExternalStorageProvider` como para el `DocumentsProvider` de Eden (`dev.eden.eden_emulator.documents`).
- No utilizar comandos nativos de Linux o acceso directo por ruta de archivo (`/storage/emulated/0/...`), preservando el aislamiento de Android 14+ / One UI.
- No alterar la retrocompatibilidad con las partidas ya subidas a Google Drive por otros dispositivos.

## Criterios de aceptación

- **CA-01 · RF-01:** Dado un archivo ZIP de guardado con más de 500 archivos y múltiples niveles de subdirectorios (ej. Subnautica), cuando se ejecuta la importación local (`importTitleSaveFromZip`), entonces todos los directorios y archivos se extraen íntegramente sin bloqueos por saturación de Binder y en una sola pasada $O(N)$.
- **CA-02 · RF-01, RF-04:** Dado un archivo con extensiones mixtas (`.json`, `.bin`, `.jpg`), cuando se extrae mediante SAF, entonces se crea con su nombre exacto sin sufijos no deseados ni extensiones duplicadas.
- **CA-03 · RF-02, RF-03:** Dado un dispositivo donde el proveedor SAF devuelve `0` como fecha de modificación de archivos locales (como Samsung One UI con Eden DocumentsProvider), cuando se descarga y extrae una partida desde la nube, entonces la aplicación registra el estado sincronizado y el juego pasa a estado `IN_SYNC` ("Al día") sin volver a mostrar "Nube más reciente (0 UP / 2 DOWN)".
- **CA-04 · RF-01, RF-02:** Dado que la partida de Subnautica está sincronizada (`IN_SYNC`), cuando se refresca la pantalla o se reinicia la aplicación, entonces el juego permanece en estado `IN_SYNC` a menos que se detecte una modificación posterior real.

## Cómo se comprueba el comportamiento

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Test unitario / de integración simulando descompresión de ZIP con estructura anidada y múltiples archivos. | El método de extracción completa el 100% de los archivos y directorios sin excepciones. |
| CA-02 | Verificación de nombres de archivos extraídos con distintas extensiones comunes en partidas de Switch. | Todos los nombres y rutas coinciden exactamente con la entrada del archivo ZIP. |
| CA-03 | Simulación de reconciliación en `SaveSyncRepositoryImpl` con partida recién descargada cuyo `lastModifiedMs` SAF local sea `0` o no fiable. | La recomendación resultante es `SyncRecommendation.IN_SYNC`. |
| CA-04 | Ejecución de `scanGames` tras sincronizar. | El item conserva `SyncRecommendation.IN_SYNC` y no solicita nuevas descargas. |

## Decisiones pendientes

- Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
