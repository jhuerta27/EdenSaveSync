# PLAN: Sincronización de Partidas de Eden (Google Drive)

**SPEC de referencia:** docs/features/save-sync/SPEC.md
**Versión de la spec revisada:** commit actual / SPEC.md aprobado
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
| `SyncModels.kt` | `app/src/main/java/com/jotasoft/edensavesync/model/SyncModels.kt` | Modelos planos actuales; deben desacoplarse a `domain/model/` y `data/device/`. |
| `SwitchTitleCatalog.kt` | `app/src/main/java/com/jotasoft/edensavesync/model/SwitchTitleCatalog.kt` | Catálogo de juegos Switch; migra a `data/catalog/SwitchTitleCatalog.kt`. |
| `AppPreferences.kt` | `app/src/main/java/com/jotasoft/edensavesync/storage/AppPreferences.kt` | Preferencias de almacenamiento; migra a `data/storage/AppPreferences.kt`. |
| `EdenStorageManager.kt` | `app/src/main/java/com/jotasoft/edensavesync/storage/EdenStorageManager.kt` | Integración SAF y ZIPs; migra a `data/storage/EdenStorageManager.kt`. |
| `GoogleDriveSyncClient.kt` | `app/src/main/java/com/jotasoft/edensavesync/cloud/GoogleDriveSyncClient.kt` | Cliente REST y OAuth de Drive; migra a `data/cloud/GoogleDriveSyncClient.kt`. |
| `EdenSyncViewModel.kt` | `app/src/main/java/com/jotasoft/edensavesync/ui/EdenSyncViewModel.kt` | ViewModel monolítico actual; se refactoriza para consumir casos de uso en `presentation/sync/`. |
| `EdenSyncScreen.kt` | `app/src/main/java/com/jotasoft/edensavesync/ui/EdenSyncScreen.kt` | Pantalla Compose monolítica; se separa en componentes modulares en `presentation/sync/components/`. |

**Convenciones y patrón de referencia:** Clean Architecture con flujo unidireccional de datos (UDF), Hilt/Inyección de dependencias modular (`core/di/`), navegación desacoplada (`core/navigation/`), y Compose Material 3 con tema AMOLED en `ui/theme/`.

## Solución propuesta

Reorganizar el proyecto dividiendo estrictamente las responsabilidades según la especificación de `AGENTS.md`:
1. **Dominio puro (`domain/`)**: Libre de frameworks Android. Contiene entidades inmutables (`LocalSaveGame`, `RemoteSaveGame`, `GameSyncItem`, `SyncRecommendation`, `SyncSummaryResult`), contratos de repositorio (`SaveSyncRepository`, `EdenStorageRepository`, `CloudSyncRepository`, `AuthRepository`) y casos de uso orientados a acciones atómicas (`ScanGamesUseCase`, `SyncAllRecommendedUseCase`, `UploadSaveGameUseCase`, `DownloadSaveGameUseCase`, `RestoreBackupUseCase`, `PairingCodeUseCase`).
2. **Datos (`data/`)**: Implementa los contratos de repositorio coordinando SAF (`EdenStorageManager`), Drive v3 (`GoogleDriveSyncClient`), catálogo y preferencias. Modela DTOs en `data/cloud/model/` y mappeo a dominio en `data/mapper/`.
3. **Núcleo (`core/`)**: Módulo de inyección de dependencias (`core/di/`) y rutas de navegación (`core/navigation/`).
4. **Presentación (`presentation/`)**: ViewModel consumiendo casos de uso y emitiendo `StateFlow<EdenSyncUiState>`. Composables modularizados separando la pantalla contenedora con ciclo de vida (`collectAsStateWithLifecycle`) y componentes individuales (`GameItemCard`, `SyncSummaryBanner`, `DriveConfigDialog`, etc.).
5. **Tema (`ui/theme/`)**: Extracción de paleta de colores y tipografía Material 3 en archivos dedicados.

## Módulos y componentes afectados

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| `:app` | Existente | Módulo único de la aplicación. Reorganización interna de paquetes bajo `com.jotasoft.edensavesync`. | Dependencias de AndroidX, Compose, Coroutines y `javax.inject`. |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `domain/model/*` | Crear | Modelos puros de dominio sin dependencias de Android SDK. | RF-02, RF-03, RF-05 |
| `domain/repository/*` | Crear | Interfaces/contratos de repositorio para almacenamiento, nube, autenticación y orquestación. | RF-01 a RF-06 |
| `domain/usecase/*` | Crear | Casos de uso especializados para escaneo, sync, subida, bajada y restauración. | RF-01 a RF-06 |
| `data/cloud/model/*` | Crear | DTOs de Drive y OAuth. | RF-04 |
| `data/mapper/*` | Crear | Extensiones de transformación DTO a entidades de dominio. | RF-04 |
| `data/device/DeviceDetector.kt` | Crear | Detección de dispositivos (Odin 2, Fold 8) utilizando `android.os.Build` aislado en capa de datos. | RF-07 |
| `data/repository/*Impl.kt` | Crear | Implementación de contratos de repositorio. | RF-01 a RF-06 |
| `core/navigation/*` | Crear | Rutas tipadas y orquestación de navegación. | RF-07 |
| `core/di/*` | Crear | Módulos y contenedor de inyección de dependencias (`DataModule`, `UseCaseModule`, `AppContainer`). | RF-01 a RF-07 |
| `ui/theme/*` | Crear | `Color.kt`, `Type.kt`, `Theme.kt` para Material 3 AMOLED. | RF-07 |
| `presentation/sync/*` | Modificar | `EdenSyncViewModel`, `EdenSyncScreen` y componentes modulares en `components/`. | RF-01 a RF-07 |

## Datos y contratos

- **Modelos y contratos de entrada y salida:** Modelos puros `LocalSaveGame`, `RemoteSaveGame`, `GameSyncItem`, `SyncRecommendation`, `SyncSummaryResult` en el paquete `domain.model`.
- **Identificadores, relaciones y restricciones:** Identificación estricta por Title ID de Switch (16 caracteres hexadecimales).
- **Origen de los datos mostrados y transformaciones:** Datos locales agregados por `EdenStorageManager` y remotos por `GoogleDriveSyncClient`, transformados a través de `DriveFileMapper` y consolidados en `ScanGamesUseCase`.
- **Persistencia, consultas y actualizaciones:** Preferencias gestionadas por `AppPreferences` y archivos SAF gestionados por `EdenStorageManager`.
- **Convivencia entre datos locales y remotos:** Resolución mediante comparación de hash SHA-256 y fecha efectiva de modificación.
- **Compatibilidad y migraciones de datos existentes:** Compatibilidad hacia atrás total con la estructura de carpetas en Google Drive y el formato ZIP `<TITLE_ID>.zip`.

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:** `StateFlow<EdenSyncUiState>` expuesto por `EdenSyncViewModel`.
- **Conservación y restauración del estado:** Recolección segura mediante `collectAsStateWithLifecycle` respetando transiciones de ciclo de vida en Android.
- **Ejecución, concurrencia y cancelación de operaciones:** Trabajo pesado (ZIP, SHA-256, I/O, red) en `Dispatchers.IO`. Nunca se traga `CancellationException`.
- **Errores, reintentos y prevención de duplicados:** Banners de estado informativos descartables y deshabilitación de botones de acción mientras `isSyncing` o `isScanning` esté activo.
- **Otras consideraciones mobile aplicables y su solución:** Soporte para cambios de orientación y multiventana preservando el estado en el ViewModel.

## Dependencias y configuración

- Se mantiene Gradle Wrapper 9.8.0, AGP 9.5.0 y Kotlin 2.4.21.
- Inclusión de `javax.inject` para anotaciones de inyección de constructores (`@Inject`).
- Inclusión explícita de `androidx.lifecycle:lifecycle-runtime-compose` para `collectAsStateWithLifecycle`.

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Test unitario de integración de almacenamiento y SAF. | JVM / Android Test con URIs simuladas. | Permisos validados y escaneo exitoso. |
| CA-02 | Test unitario de normalización de perfiles. | JVM con directorios simulados. | Normalización a Title ID sin contaminación de UUID. |
| CA-03 | Test unitario de recomendación de sincronización y hash. | JVM con `ScanGamesUseCaseTest`. | Recomendaciones `IN_SYNC`, `UPLOAD` y `DOWNLOAD` verificadas. |
| CA-04 | Test unitario de mapeo de DTO y catálogo. | JVM con `SwitchTitleCatalogTest` y DTOs. | Metadatos y nombres resueltos fielmente. |
| CA-05 | Test unitario de sincronización recomendada. | JVM simulando repositorio de guardado. | `SyncSummaryResult` con recuentos exactos. |
| CA-06 | Test de rotación de copias de seguridad. | JVM con archivos ZIP temporales. | Retención de las 5 versiones más recientes. |
| CA-07 | Compilación y lint. | Ejecución de `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`. | Salida de compilador y tests exitosa con código 0. |

**Comprobaciones de regresión:** Ejecución completa de la suite de tests unitarios existentes y nuevos.

**Comandos verificados para compilar y ejecutar tests:**
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

**Pruebas en dispositivo, emulador o simulador:** Verificación visual en pantallas grandes/plegables y emuladores en modo apaisado.

**Limitaciones del entorno:** Entorno CLI sin dispositivo físico conectado; las pruebas de instrumentación conectadas se verificarán mediante tests unitarios JVM y validación estática de compilación.

## Orden de implementación

1. **Modelos de Dominio, Repositorios y Casos de Uso:** Implementar entidades inmutables y contratos en `domain/`.
2. **Capa de Datos:** Implementar DTOs, mappers, repositorios y adaptar `GoogleDriveSyncClient`, `EdenStorageManager`, `AppPreferences` y `DeviceDetector`.
3. **Inyección de Dependencias y Navegación:** Configurar módulos en `core/di/` y rutas en `core/navigation/`.
4. **Capa de Presentación y Tema:** Modularizar componentes Compose en `presentation/sync/components/`, adaptar `EdenSyncScreen`, `EdenSyncViewModel` y crear `ui/theme/`.
5. **Limpieza, Cableado en MainActivity y Validación:** Conectar `MainActivity`, eliminar clases heredadas en desuso, ejecutar `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest` y `:app:lintDebug`.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:** Evitar roturas en la serialización de códigos de emparejamiento conservando el esquema `EDEN-SYNC:<base64>`.
- **Decisiones pendientes:** Ninguna.
