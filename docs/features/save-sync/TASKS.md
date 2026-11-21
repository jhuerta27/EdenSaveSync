# TASKS: Sincronización de Partidas de Eden (Google Drive)

Plan de tareas derivadas del plan técnico `PLAN.md` y especificación `SPEC.md`.

- [x] **TSK-01**: Crear entidades de dominio puras (`LocalSaveGame`, `RemoteSaveGame`, `GameSyncItem`, `SyncRecommendation`, `SyncSummaryResult`, `SyncedRecord`) en `com.jotasoft.edensavesync.domain.model`.
  - *Objetivo:* Desacoplar el dominio de cualquier clase de Android (`Uri`, `Build`, `Context`).
  - *Criterios que resuelve:* CA-02, CA-03.
  - *Validación:* Implementado en `domain/model/`. Verificado sin dependencias de Android.

- [x] **TSK-02**: Crear interfaces de contratos de repositorio en `com.jotasoft.edensavesync.domain.repository` (`EdenStorageRepository`, `CloudSyncRepository`, `AuthRepository`, `SaveSyncRepository`).
  - *Objetivo:* Definir abstracciones de acceso a datos y almacenamiento.
  - *Criterios que resuelve:* CA-01, CA-04, CA-05.
  - *Validación:* Contratos declarados e integrados en casos de uso.

- [x] **TSK-03**: Implementar los casos de uso de dominio en `com.jotasoft.edensavesync.domain.usecase` (`ScanGamesUseCase`, `SyncAllRecommendedUseCase`, `UploadSaveGameUseCase`, `DownloadSaveGameUseCase`, `RestoreBackupUseCase`, `PairingCodeUseCase`).
  - *Objetivo:* Aislar la lógica de negocio y las reglas de recomendación/sincronización.
  - *Criterios que resuelve:* CA-03, CA-05.
  - *Validación:* Verificado con `ScanGamesUseCaseTest`.

- [x] **TSK-04**: Adaptar y organizar la capa de datos en `com.jotasoft.edensavesync.data`:
  - DTOs en `data/cloud/model/` (`DriveFileMetadata`, `OAuthTokenResponse`).
  - Mappers en `data/mapper/` (`DriveFileMapper`).
  - `DeviceDetector` en `data/device/DeviceDetector.kt` utilizando `android.os.Build`.
  - Mover `SwitchTitleCatalog.kt` a `data/catalog/SwitchTitleCatalog.kt`.
  - Mover y actualizar `AppPreferences.kt` a `data/storage/AppPreferences.kt`.
  - Mover y actualizar `EdenStorageManager.kt` a `data/storage/EdenStorageManager.kt`.
  - Mover y actualizar `GoogleDriveSyncClient.kt` a `data/cloud/GoogleDriveSyncClient.kt`.
  - Implementar los repositorios en `data/repository/` (`EdenStorageRepositoryImpl`, `CloudSyncRepositoryImpl`, `AuthRepositoryImpl`, `SaveSyncRepositoryImpl`).
  - *Objetivo:* Cumplir con la estructura de Clean Architecture para la capa de datos.
  - *Criterios que resuelve:* CA-01, CA-02, CA-04, CA-06.
  - *Validación:* Verificado con `SwitchTitleCatalogTest` y `DriveFileMapperTest`.

- [x] **TSK-05**: Configurar `core/di` e inicialización de Compose:
  - Módulos Hilt (`DataModule`, `UseCaseModule`).
  - Entrada directa en `MainActivity` hacia `EdenSyncScreen` (eliminado wrapper pasamanos `AppNavigation` y `Routes.kt` innecesarios para single-screen).
  - *Objetivo:* Inyección de dependencias modular y desacoplamiento de navegación.
  - *Criterios que resuelve:* CA-07.
  - *Validación:* Resolución en compilación e inyección en `MainActivity`.

- [x] **TSK-06**: Configurar el tema Material 3 en `com.jotasoft.edensavesync.ui.theme`:
  - `Color.kt` con la paleta AMOLED `EdenDarkColorScheme`.
  - `Type.kt` con la tipografía.
  - `Theme.kt` con `EdenSaveSyncTheme`.
  - *Objetivo:* Extraer el tema a su paquete propio respetando las directrices de `AGENTS.md`.
  - *Criterios que resuelve:* CA-07.
  - *Validación:* Compilación de tema y aplicación en `MainActivity`.

- [x] **TSK-07**: Modularizar la capa de presentación en `com.jotasoft.edensavesync.presentation`:
  - Dividir `EdenSyncScreen.kt` en componentes modulares bajo `presentation/sync/components/`:
    - `GameItemCard.kt`
    - `SyncSummaryBanner.kt`
    - `DriveConfigDialog.kt`
    - `BackupHistoryDialog.kt`
    - `SetupStatusCard.kt`
    - `OneTouchSyncHeroCard.kt`
    - `EdenFolderSetupDialog.kt`
    - `DeviceNameDialog.kt`
    - `RenameGameDialog.kt`
    - `EmptySavesPlaceholder.kt`
    - `Formatters.kt`
  - Implementar pantalla stateful `EdenSyncScreen` y stateless `EdenSyncContent`.
  - Adaptar `EdenSyncViewModel` para utilizar los casos de uso inyectados y exponer `StateFlow<EdenSyncUiState>`.
  - *Objetivo:* Componentización y adhesión a UDF y ciclo de vida (`collectAsStateWithLifecycle`).
  - *Criterios que resuelve:* CA-07.
  - *Validación:* Compilado exitosamente y estado recolectado con ciclo de vida.

- [x] **TSK-08**: Conectar la aplicación en `EdenApplication` y `MainActivity`:
  - Configurar `EdenApplication` para inicializar el contenedor de DI.
  - Adaptar `MainActivity` para utilizar `EdenSaveSyncTheme`, `AppNavigation` y `collectAsStateWithLifecycle`.
  - Eliminar clases heredadas antiguas redundantes.
  - *Objetivo:* Integración final limpia.
  - *Criterios que resuelve:* CA-01 a CA-07.
  - *Validación:* `./gradlew :app:assembleDebug` ejecutado con éxito.

- [x] **TSK-09**: Validación global y pruebas:
  - Crear y actualizar tests unitarios JVM (`SwitchTitleCatalogTest`, `ScanGamesUseCaseTest`, `DriveFileMapperTest`).
  - Ejecutar `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`.
  - Registrar evidencias de validación en este documento.
  - *Objetivo:* Asegurar ausencia de regresiones y cumplimiento de directrices.
  - *Criterios que resuelve:* CA-01 a CA-07.
  - *Validación:*
    - `:app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado correctamente).
    - `:app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (Todos los tests unitarios pasados).
    - `:app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-10**: Internacionalización (i18n) completa y centralización de strings:
  - Extraer todos los literales de texto de la aplicación hacia `res/values/strings.xml` (Inglés como idioma principal por defecto).
  - Crear la traducción completa al español en `res/values-es/strings.xml`.
  - Estructurar `StringResource` directamente en `EdenSyncUiState` para emitir referencias de recursos desde `EdenSyncViewModel` sin acoplar Android `Context` ni clases wrapper externas como `UiText`.
  - Resolver todos los textos en `EdenSyncScreen` con Compose `stringResource(...)`.
  - Actualizar todos los composables (`EdenSyncScreen`, `GameItemCard`, `SetupStatusCard`, `OneTouchSyncHeroCard`, `EmptySavesPlaceholder`, `DriveConfigDialog`, `EdenFolderSetupDialog`, `DeviceNameDialog`, `RenameGameDialog`, `BackupHistoryDialog`, `Formatters`, `SyncSummaryBanner`) para utilizar `stringResource(...)`.
  - *Objetivo:* Cumplir con la regla de cero strings hardcodeados y soporte multilingüe EN/ES.
  - *Validación:*
    - `:app:assembleDebug` -> `BUILD SUCCESSFUL`.
    - `:app:testDebugUnitTest` -> `BUILD SUCCESSFUL`.
    - `:app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-11**: Configuración de Hilt con KSP y eliminación de factorías manuales:
  - Añadir plugin `com.google.devtools.ksp` y `com.google.dagger.hilt.android` (Hilt 2.60.1 + KSP 2.3.13) en `libs.versions.toml` y Gradle scripts.
  - Anotar `EdenApplication` con `@HiltAndroidApp` y `MainActivity` con `@AndroidEntryPoint`.
  - Anotar `EdenSyncViewModel` con `@HiltViewModel @Inject constructor(...)` e inyectar directamente con `by viewModels()`.
  - Inyectar `EdenStorageManager` mediante `@Inject lateinit var storageManager` en `MainActivity`.
  - Eliminar por completo el contenedor manual `AppContainer.kt` y la función `provideFactory`.
  - *Objetivo:* Inyección de dependencias totalmente nativa y automática en tiempo de compilación.
  - *Validación:*
    - `:app:assembleDebug` -> `BUILD SUCCESSFUL` (KSP y bytecode transform ejecutados).
    - `:app:testDebugUnitTest` -> `BUILD SUCCESSFUL`.
    - `:app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).
