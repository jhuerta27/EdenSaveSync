# TASKS: Optimización de sincronización y extracción para partidas de gran tamaño y directorios anidados (Subnautica)

**Plan de referencia:** [docs/features/large-save-sync-optimization/PLAN.md](PLAN.md)
**Spec de referencia:** [docs/features/large-save-sync-optimization/SPEC.md](SPEC.md)

---

### Tareas de implementación

- [x] **TASK-01: Optimizar la descompresión SAF en `EdenStorageManager.kt`**
  - **Objetivo:** Refactorizar `importTitleSaveFromZip` implementando `dirCache: MutableMap<String, DocumentFile>`, eliminación de búsquedas O(N²) redundantes (`findFile` archivo a archivo) y resolución de tipos MIME mediante `MimeTypeMap`.
  - **Alcance:** [`app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt).
  - **Dependencias:** Ninguna.
  - **Criterios resueltos:** CA-01, CA-02 (RF-01, RF-04).
  - **Método de validación:** Inspección de código, compilación limpia y pruebas de extracción de estructuras jerárquicas sin errores.

- [x] **TASK-02: Robustecer la reconciliación y persistencia de sincronización en `SaveSyncRepositoryImpl.kt`**
  - **Objetivo:** Ajustar `downloadSingleGame` para persistir los metadatos de sincronización con marcas de tiempo fiables y actualizar `mergeLocalAndRemote` para evitar bucles de "Nube más reciente (0 UP / 2 DOWN)" cuando el proveedor SAF entrega `lastModifiedMs = 0L` (Samsung One UI con Eden DocumentsProvider).
  - **Alcance:** [`app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt) y [`app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveReconciler.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveReconciler.kt).
  - **Dependencias:** TASK-01.
  - **Criterios resueltos:** CA-03, CA-04 (RF-02, RF-03).
  - **Método de validación:** Pruebas unitarias de reconciliación cubriendo casos de timestamp 0, coincidencia de hash y detección de modificaciones locales.

- [x] **TASK-03: Implementar pruebas unitarias para almacenamiento y reconciliación**
  - **Objetivo:** Crear/ampliar suites de tests unitarios verificando los criterios de aceptación CA-01 a CA-04.
  - **Alcance:** [`app/src/test/java/com/jotasoft/edensavesync/SaveReconcilerTest.kt`](../../../app/src/test/java/com/jotasoft/edensavesync/SaveReconcilerTest.kt), [`app/src/test/java/com/jotasoft/edensavesync/LargeSaveExtractionTest.kt`](../../../app/src/test/java/com/jotasoft/edensavesync/LargeSaveExtractionTest.kt).
  - **Dependencias:** TASK-01, TASK-02.
  - **Criterios resueltos:** CA-01, CA-02, CA-03, CA-04.
  - **Método de validación:** Ejecución de `./gradlew :app:testDebugUnitTest`.

- [x] **TASK-04: Verificación global de calidad y compilación**
  - **Objetivo:** Comprobar compilación de depuración, tests JVM y Android Lint sin regresiones.
  - **Alcance:** Todo el proyecto.
  - **Dependencias:** TASK-01, TASK-02, TASK-03.
  - **Criterios resueltos:** Todos.
  - **Método de validación:** `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`.

- [x] **TASK-05: Protección contra partidas vacías y limpieza en Google Drive**
  - **Objetivo:** Prevenir la subida o importación de archivos ZIP vacíos (<= 22 bytes), ignorar `.yuzu_save_size` como archivo de guardado real cuando la carpeta carece de partidas, seleccionar el perfil con mayor cantidad de archivos en la exportación y eliminar los 2 archivos vacíos en Google Drive.
  - **Alcance:** [`app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/storage/EdenStorageManager.kt), [`app/src/main/java/com/jotasoft/edensavesync/data/cloud/GoogleDriveSyncClient.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/cloud/GoogleDriveSyncClient.kt), [`app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt`](../../../app/src/main/java/com/jotasoft/edensavesync/data/repository/SaveSyncRepositoryImpl.kt).
  - **Dependencias:** TASK-01, TASK-02.
  - **Criterios resueltos:** RF-01, RF-02, CA-03.
  - **Método de validación:** Eliminación verificada en Drive con la API v3, instalación en Galaxy Z Fold 8 y Odin 2 Portal comprobando estado "Todo al día" con 52 partidas activas.

---

### Registro de evidencias y validación

| Tarea | Estado | Evidencia |
| --- | --- | --- |
| TASK-01 | Completada | Implementada caché de directorios `dirCache` en `importTitleSaveFromZip`, eliminación de `findFile` redundante y resolución MIME. Código compilando limpiamente. |
| TASK-02 | Completada | Creado `SaveReconciler.kt` y actualizada la delegación en `SaveSyncRepositoryImpl.kt` y `downloadSingleGame` con persistencia atómica de `syncedRecord`. |
| TASK-03 | Completada | Pruebas unitarias creadas en `SaveReconcilerTest` y `LargeSaveExtractionTest` cubriendo CA-01 a CA-04. Ejecución exitosa con `./gradlew :app:testDebugUnitTest`. |
| TASK-04 | Completada | Verificados `:app:assembleDebug` (código 0), `:app:testDebugUnitTest` (código 0, todos los tests pasados) y `:app:lintDebug` (código 0, 0 errores). |
| TASK-05 | Completada | Eliminados `0100429011144000.zip` y `010014C011146000.zip` (22 bytes) en Drive. Añadidas protecciones contra subida/listado de zips vacíos. Probado en Fold 8 y Odin 2: ambos reportan "Todo al día (52 partidas)". |
