# PLAN: Optimización de sincronización y extracción para partidas de gran tamaño y directorios anidados (Subnautica)

**SPEC de referencia:** [docs/features/large-save-sync-optimization/SPEC.md](SPEC.md)
**Versión de la spec revisada:** Versión 1.0 (Aprobada)
**Estado:** Aprobado <!-- Borrador | En revisión | Aprobado -->

<!-- PARA LA PERSONA
Copia esta plantilla como PLAN.md junto a la SPEC.md aprobada.
Este documento define la solución técnica. Una vez revisado, el agente puede
derivar TASKS.md con tareas, dependencias y comprobaciones.
-->

<!-- PARA EL AGENTE
- Lee la SPEC.md aprobada, las instrucciones del proyecto y MOBILE_GUIDELINES.md.
  Si falta un documento necesario o la spec no está aprobada, indícalo antes de avanzar.
- Inspecciona el repositorio. Referencia rutas verificadas y distingue las nuevas propuestas.
- Propón una solución proporcional al alcance y coherente con el proyecto.
  Reutiliza lo existente y justifica nuevas dependencias o cambios de arquitectura.
- Distingue hechos, decisiones confirmadas y propuestas. Consulta las decisiones
  no resueltas; haz pocas preguntas por vez y actualiza el plan con las respuestas.
- Referencia los requisitos y criterios por su ID, sin copiar toda la spec.
- Si una decisión cambia el comportamiento o alcance, vuelve a la spec y solicita
  confirmación. No resuelvas una duda de producto mediante una suposición técnica.
- Conserva estos comentarios. No implementes durante la planificación.
- Solicita aprobación antes de marcar el plan como Aprobado. La autorización
  para implementar debe ser explícita; no se deduce del estado de los documentos.
-->

## Contexto técnico verificado

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| `EdenStorageManager` | [`data/storage/EdenStorageManager.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt) | Coordina las operaciones SAF (`DocumentFile`), empaquetado ZIP, extracción y cálculo de estadísticas/hash de partidas. |
| `SaveSyncRepositoryImpl` | [`data/repository/SaveSyncRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt) | Reconciliación de partidas locales y remotas (`mergeLocalAndRemote`) y ejecución de subida/descarga individual y masiva. |
| `AppPreferences` | [`data/storage/AppPreferences.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/AppPreferences.kt) | Persistencia de preferencias y registros de sincronización atómicos (`synced_records_json`). |
| `EdenStorageRepositoryImpl` | [`data/repository/EdenStorageRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/EdenStorageRepositoryImpl.kt) | Implementación de repositorio de almacenamiento que delega en `EdenStorageManager`. |

**Convenciones y patrón de referencia:** Arquitectura limpia con Unidirectional Data Flow (UDF), Hilt para inyección de dependencias, Kotlin Coroutines en `Dispatchers.IO` para I/O y SAF, y funciones de expresión única para métodos cortos según `AGENTS.md` y `GENERIC_RULES.md`.

## Solución propuesta

La solución ataca directamente los dos cuellos de botella identificados:

1. **Extracción $O(N)$ en [`EdenStorageManager.importTitleSaveFromZip`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt#L208):**
   - Introducir una caché en memoria `dirCache: MutableMap<String, DocumentFile>` asociando la ruta relativa normalizada (ej. `"slot0000/CellsCache"`) directamente a su `DocumentFile`.
   - Crear carpetas jerárquicamente solo cuando un prefijo de ruta no esté en la caché, garantizando que cada directorio se cree como máximo una sola vez.
   - Dado que el directorio de la partida se elimina y recrea limpio antes de extraer (`newTitleDir`), eliminar la llamada $O(N)$ `parentDir.findFile(fileName)?.delete()`.
   - Utilizar el tipo MIME correspondiente a la extensión del archivo (`MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)`) con fallback a `"application/octet-stream"`, evitando que SAF renombre extensiones o cree duplicados `(1)`.
   - Con este cambio, las ~500.000 operaciones de Binder se reducen a exactamente $N$ llamadas de creación/escritura y un número mínimo de creaciones de carpetas, completando la extracción de Subnautica en pocos segundos en lugar de minutos.

2. **Reconciliación y protección ante `COLUMN_LAST_MODIFIED = 0` en [`SaveSyncRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt#L166):**
   - En `downloadSingleGame`: registrar en `prefs.recordSyncedTitle(...)` el hash efectivo y la marca de tiempo remota atómica (`remote.lastModifiedMs`).
   - En `mergeLocalAndRemote`:
     - Si `local.contentHash == remote.contentHash`, el estado es `IN_SYNC`.
     - Si `syncedRecord != null` y `syncedRecord.contentHash == local.contentHash`: significa que los archivos locales no han cambiado desde la última sincronización. Solo se recomendará `DOWNLOAD_FROM_CLOUD` si el archivo remoto en Google Drive fue modificado con posterioridad (`remote.lastModifiedMs > syncedRecord.remoteTimestampMs + 2_000L`). De lo contrario, permanece en `IN_SYNC`.
     - Si los hashes difieren y el hash local no coincide con el registro sincronizado previo (el usuario ha jugado localmente):
       - Si `local.lastModifiedMs > 0L`: se compara normalmente contra `remote.lastModifiedMs`.
       - Si `local.lastModifiedMs == 0L` (proveedor Eden en One UI): si la nube no ha cambiado (`remote.lastModifiedMs <= syncedRecord.remoteTimestampMs + 2_000L`), el cambio es necesariamente local, por lo que se recomienda `UPLOAD_TO_CLOUD` en lugar de asumir erróneamente que la nube es más reciente.

## Módulos y componentes afectados

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| `:app` | Existente | Módulo único de la aplicación. | Ninguna nueva dependencia. |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `data/storage/EdenStorageManager.kt` | Modificar | Optimizar `importTitleSaveFromZip` con `dirCache`, resolución jerárquica $O(1)$, eliminación de búsquedas O(N²) y mapeo MIME. | [RF-01], [RF-04] |
| `data/repository/SaveSyncRepositoryImpl.kt` | Modificar | Robustecer `downloadSingleGame` y `mergeLocalAndRemote` para contemplar invariantes de `syncedRecord` y proveedores con timestamp cero. | [RF-02], [RF-03] |
| `app/src/test/java/.../SaveSyncRepositoryTest.kt` | Crear / Ampliar | Tests unitarios para reconciliación de partidas con timestamp local 0L, partidas complejas y detección de cambios locales vs remotos. | [CA-03], [CA-04] |
| `app/src/test/java/.../EdenStorageManagerTest.kt` | Crear / Ampliar | Tests unitarios para validación de lógica de descompresión y nombres de archivo MIME. | [CA-01], [CA-02] |

## Datos y contratos

- **Modelos y contratos de entrada y salida:** No se modifican los contratos de dominio (`LocalSaveGame`, `RemoteSaveGame`, `GameSyncItem`, `SyncRecommendation`, `SyncedRecord`).
- **Identificadores, relaciones y restricciones:** Identificación de partidas basada en `Title ID` de 16 caracteres hexadecimales.
- **Origen de los datos mostrados y transformaciones:** Datos locales escaneados vía SAF y remotos listados vía Google Drive REST API.
- **Persistencia, consultas y actualizaciones:** Persistencia de `SyncedRecord` en SharedPreferences (`synced_records_json`).
- **Convivencia entre datos locales y remotos:** Coexistencia consistente incluso cuando el proveedor local no soporte modificación de timestamps.
- **Compatibilidad y migraciones de datos existentes:** Totalmente compatible hacia atrás con registros existentes.

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:** La interfaz reacciona a través de `EdenSyncUiState` (`isSyncing`, `syncProgressMessage`, `games`).
- **Conservación y restauración del estado:** Se mantiene en `StateFlow` dentro de `EdenSyncViewModel`.
- **Ejecución, concurrencia y cancelación de operaciones:** Descompresión e I/O en `Dispatchers.IO`. Cancelaciones propagan `CancellationException`.
- **Errores, reintentos y prevención de duplicados:** Si la descompresión falla en algún archivo, se captura, se elimina el archivo temporal y se emite error en `Result.failure`, manteniendo a salvo la copia de seguridad previa.
- **Otras consideraciones mobile aplicables y su solución:** Sin fugas de memoria o de `CursorWindow` gracias a la eliminación de búsquedas repetitivas por Binder.

## Dependencias y configuración

- No se requieren nuevas librerías externas. Se utilizan APIs estándar de Android SDK (`MimeTypeMap`, `DocumentsContract`, `DocumentFile`) y Kotlin Standard Library.

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Test unitario JVM simulando extracción de ZIP estructurado con carpetas anidadas y múltiples ficheros. | `EdenStorageManagerTest` (JVM local). | Test en verde verificando creación de estructura sin búsquedas cuadráticas. |
| CA-02 | Test unitario verificando detección de tipo MIME según extensión (`json` -> `application/json`, etc.). | `EdenStorageManagerTest` (JVM local). | Mapeo verificado de extensiones y nombres preservados. |
| CA-03 | Test unitario de `mergeLocalAndRemote` en `SaveSyncRepositoryTest` con `local.lastModifiedMs = 0L` tras sincronización previa. | `SaveSyncRepositoryTest` (JVM local). | La recomendación es `SyncRecommendation.IN_SYNC`. |
| CA-04 | Test unitario de escaneo y persistencia verificando estabilidad de `IN_SYNC`. | `SaveSyncRepositoryTest` (JVM local). | La recomendación no cambia a `DOWNLOAD_FROM_CLOUD` sin cambios remotos. |

**Comprobaciones de regresión:** Ejecutar `:app:testDebugUnitTest`, `:app:assembleDebug`, y `:app:lintDebug`.

**Comandos verificados para compilar y ejecutar tests:**
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

**Pruebas en dispositivo, emulador o simulador:** Verificación en Galaxy Z Fold 8 / Odin 2 Portal de la sincronización de Subnautica pasando a estado "Al día" sin bucle de 0 UP / 2 DOWN.

**Limitaciones del entorno:** La interacción física con el `DocumentsProvider` de Eden en One UI depende del hardware del usuario; se valida exhaustivamente la lógica en JVM y con builds libres de errores.

## Orden de implementación

1. **Etapa 1 (Optimización de descompresión y SAF):** Refactorizar `importTitleSaveFromZip` en [`EdenStorageManager.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt) implementando `dirCache` y MIME types adecuados. Comprobación: compilación y pruebas unitarias de almacenamiento.
2. **Etapa 2 (Reconciliación y protección de timestamps 0L):** Actualizar `downloadSingleGame` y `mergeLocalAndRemote` en [`SaveSyncRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt). Comprobación: pruebas unitarias de reconciliación en JVM.
3. **Etapa 3 (Validación y control de calidad):** Ejecutar `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug` y `./gradlew :app:assembleDebug` registrando evidencias.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:** 
  - *Riesgo:* Que `createFile` falle si un archivo ya existía. *Medida:* Al inicio de la importación, el directorio del Title ID se limpia por completo de forma garantizada antes de crear `newTitleDir`.
  - *Riesgo:* Que un archivo no tenga extensión. *Medida:* Fallback seguro a `"application/octet-stream"`.
- **Decisiones pendientes:** Ninguna.
- **Uso de subagentes:** No se requieren subagentes para esta tarea (los cambios se concentran en dos archivos clave de datos y sus pruebas unitarias asociadas).

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->
