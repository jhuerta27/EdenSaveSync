# SPEC: Sincronización de Partidas de Eden (Google Drive)

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

Una solución nativa de Android que permite a los usuarios del emulador Eden (Nintendo Switch) sincronizar sus partidas guardadas entre múltiples dispositivos Android (por ejemplo, teléfonos plegables como Samsung Galaxy Z Fold 8 y consolas portátiles como AYN Odin 2 Portal) a través de Google Drive, de forma transparente, segura y con resolución inteligente de conflictos.

## Situación actual

La aplicación base contaba con una implementación monolítica inicial en paquetes planos (`ui`, `model`, `cloud`, `storage`). Para cumplir con las directrices de `AGENTS.md` y `docs/`, se requiere adaptar la arquitectura completa a Clean Architecture (separando `domain`, `data`, `presentation`, `core` y `ui/theme`), desacoplando modelos de dominio de frameworks de Android, integrando casos de uso, repositorios, inyección de dependencias modular y componentes de UI testeables.

## Dentro del alcance

- **RF-01:** Acceso persistente al almacenamiento de partidas de Eden mediante Storage Access Framework (SAF) y DocumentsProvider de Eden (`dev.eden.eden_emulator`).
- **RF-02:** Normalización automática de perfiles de usuario de Switch (mapeo por Title ID hexadecimal de 16 caracteres, abstrayendo los UUIDs de perfil locales de cada instalación).
- **RF-03:** Verificación de contenido por hash SHA-256 y detección precisa de fecha de guardado real para evitar falsos positivos provocados por la extracción SAF.
- **RF-04:** Integración y autenticación con Google Drive REST API v3 mediante OAuth 2.0 PKCE, loopback local y clonado de sesión entre dispositivos mediante código de emparejamiento.
- **RF-05:** Sincronización inteligente en 1 toque (`syncAllRecommended`) y operaciones manuales individuales (subir, descargar).
- **RF-06:** Copias de seguridad automáticas locales rotativas (últimas 5 versiones en ZIP) antes de cualquier sobreescritura de partidas, con posibilidad de restauración inmediata.
- **RF-07:** Interfaz de usuario adaptada con tema oscuro AMOLED (Material 3), optimizada para pantallas plegables y consolas portátiles panorámicas con soporte de ciclo de vida (`collectAsStateWithLifecycle`).

## Fuera de alcance

- Sincronización P2P directa sin conexión a Internet (ambos dispositivos desconectados de la red).
- Modificación directa de binarios de juegos o dumps de ROMs (solo se gestionan partidas guardadas).
- Sincronización con otros proveedores en la nube distintos de Google Drive en esta versión.

## Flujo de usuario

1. El usuario abre la app y vincula la carpeta de guardados de Eden usando el selector SAF (DocumentsProvider o almacenamiento accesible).
2. El usuario conecta su cuenta de Google Drive mediante inicio de sesión web OAuth o importando el código de emparejamiento de otro dispositivo.
3. La app escanea el catálogo local de partidas y lo compara con el almacenamiento en la nube, mostrando el estado de cada juego (subida recomendada, descarga recomendada, sincronizado).
4. El usuario pulsa "Sincronizar todo" (o acciones individuales por juego). La app respalda localmente cualquier archivo a sobrescribir y realiza la subida/descarga.
5. Si ocurre cualquier imprevisto, el usuario puede restaurar la copia de seguridad previa desde la tarjeta del juego.

## Datos y reglas de negocio

- Cada juego de Nintendo Switch se identifica unívocamente por su `Title ID` de 16 caracteres hexadecimales (ej. `0100F2C0115B6000`).
- Las partidas se empaquetan en archivos ZIP nombrados `<TITLE_ID>.zip` en Google Drive con metadatos asociados (`titleId`, `gameName`, `contentHash`, `lastModifiedMs`, `sourceDeviceName`, `uploadedAtMs`).
- Si el hash SHA-256 coincide entre local y remoto, la recomendación es `IN_SYNC`.
- Si los hashes difieren, se compara la fecha efectiva de modificación: si local supera a remoto por más de 2000 ms, se recomienda `UPLOAD_TO_CLOUD`; de lo contrario, `DOWNLOAD_FROM_CLOUD`.
- Antes de sobrescribir una partida local, se guarda un ZIP en `backups/<TITLE_ID>/backup_<timestamp>.zip` conservando un máximo de 5 respaldos.

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Indicador visual de progreso y deshabilitación de acciones conflictivas concurrentes para evitar duplicados. |
| Sin datos | Mensaje explicativo guiando al usuario a vincular la carpeta de Eden o conectar Google Drive. |
| Entrada inválida | Validación inline al introducir nombres de dispositivo, códigos de emparejamiento o tokens manuales. |
| Error o espera excesiva | Notificación en banner informativo descartable con descripción del error sin interrumpir el estado de la app. |
| Sin conexión o conexión interrumpida | Se mantiene la visualización del estado local en caché; las operaciones en la nube informan fallo sin corromper archivos locales. |
| Cancelar o volver atrás | Cancelación ordenada de operaciones de red sin tragar `CancellationException`. |
| Pasar a segundo plano y regresar | `collectAsStateWithLifecycle` detiene la recolección activa en pausa y en `onResume` se refresca el estado si hubo cambios. |
| Recrear la pantalla | Estado de UI preservado mediante `StateFlow` en ViewModel. |
| Reabrir después de terminarse el proceso | Preferencias persistentes restauradas (carpeta seleccionada, credenciales, historial de sync). |
| Otros puntos aplicables de la guía | UI responsive para formatos panorámicos de consolas y paneles desplegables/multiventana de dispositivos plegables. |

**Puntos de la guía no aplicables y motivo:** Notificaciones push en segundo plano no aplican en esta fase al ser una sincronización manual/on-demand guiada por el usuario.

## Restricciones del pedido

- Módulo único: `:app`.
- Paquete raíz: `com.jotasoft.edensavesync`.
- Arquitectura Limpia (Clean Architecture) con flujo unidireccional de datos (UDF).
- Capa de dominio pura Kotlin sin dependencias del SDK de Android.
- Inyección de dependencias modular y desacoplada en `core/di/`.
- Material 3 con paleta optimizada para pantallas OLED/gaming en `ui/theme/`.

## Criterios de aceptación

- **CA-01 · RF-01:** Dado que el usuario selecciona una carpeta de Eden mediante SAF, cuando la app procesa el URI, entonces adquiere permisos persistentes de lectura y escritura y detecta las partidas locales.
- **CA-02 · RF-02:** Dado que dos dispositivos poseen UUIDs de perfil de Switch distintos, cuando sincronizan un mismo Title ID, entonces la partida se extrae en el perfil local activo sin crear perfiles ajenos.
- **CA-03 · RF-03:** Dado que una partida se descarga de Google Drive, cuando se extrae en el almacenamiento local, entonces se conserva su hash y referencia de sincronización sin marcar falsamente la copia local como más nueva.
- **CA-04 · RF-04:** Dado que se conecta Google Drive por OAuth o emparejamiento, cuando se consulta la nube, entonces se listan los archivos remotos con sus metadatos.
- **CA-05 · RF-05:** Dado que existen partidas desactualizadas, cuando el usuario ejecuta la sincronización, entonces se transfieren las partidas correspondientes y se actualiza el resumen visual.
- **CA-06 · RF-06:** Dado que se va a sobrescribir una partida local existente, cuando se descarga la versión remota, entonces se genera previamente un archivo de respaldo en almacenamiento privado y se limita a 5 copias.
- **CA-07 · RF-07:** Dado un cambio de configuración o rotación de pantalla en consola o plegable, cuando la pantalla se recrea, entonces el estado de la UI y los diálogos se preservan de forma reactiva.

## Cómo se comprueba el comportamiento

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Verificar persistencia de URI y lectura de estructura de carpetas en tests unitarios y SAF. | URI persistido y partidas mapeadas correctamente. |
| CA-02 | Comprobar normalización de Title ID con perfil local simulado. | Archivos dirigidos a la carpeta del perfil activo sin mezclar UUIDs. |
| CA-03 | Comparar cálculo de hash SHA-256 y regla de desempate temporal. | Hash idéntico devuelve `IN_SYNC`; diferencia de fecha mayor a 2s orienta recomendación. |
| CA-04 | Ejecutar parsing de metadatos remotos y deserialización de código de emparejamiento. | Credenciales y propiedades cargadas íntegramente. |
| CA-05 | Ejecutar caso de uso de sincronización con partidas simuladas. | Subidas y descargas recomendadas ejecutadas y resumidas en `SyncSummaryResult`. |
| CA-06 | Simular sobreescritura con múltiples descargas. | Se generan copias de seguridad ZIP en almacenamiento privado rotando a un máximo de 5. |
| CA-07 | Compilar con `:app:assembleDebug`, `:app:testDebugUnitTest` y `:app:lintDebug`. | Compilación exitosa, pruebas en verde y sin violaciones de arquitectura o Compose. |

## Decisiones pendientes

- Ninguna.
