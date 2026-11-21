# AGENTS.md

This file provides guidance to AI coding agents working in this repository.
All paths in this document are relative to the repository root.

## Required reading

Before planning, reviewing, or modifying this project, read
[docs/GENERIC_RULES.md](docs/GENERIC_RULES.md) in full and apply it alongside
this file. The link is an explicit reading requirement; do not assume your
tool automatically imports linked Markdown files.

GENERIC_RULES.md contains reusable working rules. This file adds the project's
Android conventions and spec-driven workflow. For repository conventions,
project-specific rules refine the generic defaults. Neither file overrides
the user's explicit instructions or the agent's higher-priority instructions.
If documents conflict in a way that affects behavior or scope, clarify the
conflict before implementing the affected part.

## Project

A native Android application built with Kotlin and Jetpack Compose, designed 
to synchronize save files from the Eden emulator across multiple Android 
devices using Google Drive.

- Single module: `app`.
- Package: `com.jotasoft.edensavesync`.
- Baseline functionality: Sync Eden emulator save files across multiple Android 
  devices using Google Drive..

## Commands

Run commands from the repository root using the Gradle wrapper.

```bash
./gradlew :app:assembleDebug          # Build the debug APK
./gradlew :app:test                  # Run local JVM tests across variants
./gradlew :app:testDebugUnitTest      # Run local JVM tests for debug
./gradlew :app:connectedDebugAndroidTest  # Requires a device/emulator
./gradlew :app:lintDebug              # Run Android lint for debug

# Run one local test class; replace SomeTest with an existing class
./gradlew :app:testDebugUnitTest --tests "com.jotasoft.edensavesync.SomeTest"
```

Local tests live in `app/src/test`; instrumented tests live in
`app/src/androidTest`. If build variants change, inspect the available Gradle
tasks and use the appropriate variant-specific task.

There is no separate ktlint/detekt configuration in the baseline repository.
Use the existing configuration; do not introduce a new quality tool as an
unrelated change.

For code changes, run `:app:assembleDebug`, `:app:testDebugUnitTest`, and
`:app:lintDebug` before reporting completion. Run relevant instrumented tests
and manual checks when acceptance criteria require device behavior. For
documentation-only changes, verify content and references without requiring
an Android build. Report any checks that could not run and the reason.

## Spec-driven workflow

### Reference documents

Each template carries its own instructions for the agent, its section
structure, and its identifier scheme. Read the template in full and follow it;
this file does not restate its content and must not contradict it.

- `docs/SPEC_TEMPLATE.md`: copy to the feature's `SPEC.md` when creating or
  updating a specification. It owns the spec's sections, its `RF-` requirement
  and `CA-` acceptance-criteria identifiers, and its approval states.
- `docs/PLAN_TEMPLATE.md`: copy to the feature's `PLAN.md` once the
  specification is approved. It owns the plan's sections and approval states.
  Reference requirements and criteria by their spec identifiers.
- `docs/MOBILE_GUIDELINES.md`: consult when writing or reviewing requirements,
  acceptance criteria, the technical plan, and mobile validation. Consider
  lifecycle, state retention, connectivity, persistence, UI states, navigation
  and interruptions, forms, screen adaptation, accessibility, permissions,
  performance, background work, privacy, and internationalization. Apply only
  relevant items; clarify undefined product behavior instead of inventing it.

Preserve each template's structure and its embedded comments in the copy. Do not
rewrite the shared templates for an individual feature.

### Feature documents

Keep each feature's documents together:

- `docs/features/<feature-name>/SPEC.md`
- `docs/features/<feature-name>/PLAN.md`
- `docs/features/<feature-name>/TASKS.md`

Use an existing feature directory when continuing its work.

### Sequence

Each stage is gated by the previous document's state. A document is only
`Aprobada`/`Aprobado` when the user says so: a complete document is not an
approved one, and the agent never changes that state on its own.

1. **Specification:** complete `SPEC.md` from `docs/SPEC_TEMPLATE.md`,
   collaboratively and section by section, following the template's own
   instructions. Do not start the plan until the user approves the spec.
2. **Plan:** write `PLAN.md` from `docs/PLAN_TEMPLATE.md` for the approved
   specification, following the template's own instructions. Additionally,
   record whether subagents are needed and their bounded responsibilities; do
   not assume delegation is required or available. Do not start tasks until
   the user approves the plan.
3. **Tasks:** derive `TASKS.md` from the approved plan. There is no shared
   template for it, so this file defines it: small, ordered, verifiable
   checkboxes, each with an identifier, objective, scope, dependencies, the
   spec criteria it resolves, and its validation method. Keep tasks concise
   enough to execute and detailed enough to determine when they are done.
4. **Implementation:** execute the tasks within the agreed scope, preserving
   the architecture below. Authorization to implement must be explicit; it is
   not implied by the documents' state. Update task status as work progresses.
5. **Validation:** verify acceptance criteria with appropriate evidence and
   record the result in `TASKS.md`, including any outstanding checks.

Do not treat a filled template as resolution of unanswered questions. Ask
about missing product decisions that affect behavior; resolve routine technical
details from the code and established conventions. Do not ask for renewed
permission for steps the user has already authorized.

If implementation reveals a requirement gap or contradiction, clarify the
affected behavior and update the relevant documents before continuing that
part. Keep the specification, plan, tasks, and resulting behavior consistent.

## Architecture

Clean Architecture with unidirectional UI data flow (UDF). The application
synchronizes Nintendo Switch save data from the Eden emulator across devices
via Google Drive. The architecture is structured under `com.jotasoft.edensavesync`:

| Path | Responsibility |
| --- | --- |
| `data/cloud/GoogleDriveSyncClient.kt` | Google Drive v3 REST API client, OAuth 2.0 PKCE, loopback server, upload/download with metadata |
| `data/cloud/model/*` | Cloud and OAuth DTOs, Drive file metadata models |
| `data/storage/EdenStorageManager.kt` | Storage Access Framework (SAF) integration, profile normalization, SHA-256 calculation, ZIP compression/decompression, backup rotation |
| `data/storage/AppPreferences.kt` | Local persistent preferences (tree URI, device name, tokens, client credentials, pairing codes) |
| `data/catalog/SwitchTitleCatalog.kt` | Title ID resolution, game name lookup, and user-defined custom titles |
| `data/repository/*RepositoryImpl.kt` | Repository implementations coordinating storage, cloud, auth, and catalog |
| `data/mapper/*Mapper.kt` | Data source DTO/Entity-to-domain extension functions |
| `domain/model/*.kt` | Pure domain models (`LocalSaveGame`, `RemoteSaveGame`, `GameSyncItem`, `SyncRecommendation`, `SyncSummaryResult`) |
| `domain/repository/*.kt` | Repository contracts (`SaveSyncRepository`, `EdenStorageRepository`, `CloudSyncRepository`, `AuthRepository`) |
| `domain/usecase/*.kt` | Focused use cases (`ScanGamesUseCase`, `SyncAllRecommendedUseCase`, `UploadSaveGameUseCase`, `DownloadSaveGameUseCase`, `RestoreBackupUseCase`, `PairingCodeUseCase`) |
| `presentation/sync/EdenSyncViewModel.kt` | ViewModel exposing `StateFlow<EdenSyncUiState>` and handling UI intents |
| `presentation/sync/EdenSyncScreen.kt` | Stateful screen entry point and stateless `EdenSyncContent` |
| `presentation/sync/components/*.kt` | Modular Compose components (GameItemCard, SyncSummaryBanner, DriveConfigDialog, BackupHistoryDialog) |
| `core/navigation/Routes.kt` | Type-safe navigation routes and dialog/screen destinations |
| `core/navigation/AppNavigation.kt` | Navigation host and screen/dialog route orchestration |
| `core/di/*Module.kt` | Dependency injection modules (Hilt) binding data sources, repositories, and use cases |
| `ui/theme/*` (`Color.kt`, `Theme.kt`, `Type.kt`) | Material 3 theme, typography, and dark palette optimized for gaming devices/OLED screens |

### Layer dependencies

- `presentation -> domain` and `data -> domain`.
- **Domain layer**: pure Kotlin without dependencies on presentation, data implementations,
  external DTOs, or Android framework classes (`Context`, `Uri`, `Intent`). It owns
  plain models, repository interfaces, and use cases.
- **Presentation layer**: observes domain use cases and models via `StateFlow<UiState>`.
  It never interacts directly with storage managers, SAF, HTTP clients, or DTOs.
- **Data layer**: implements domain repository contracts. It coordinates Android Storage
  Access Framework (SAF), Google Drive REST APIs, local backups, and persistent preferences,
  mapping all external entities to domain models.
- Dependency injection wires implementations to domain contracts across layers.

### Dependency injection

- Use Hilt and prefer `@Inject constructor` for project-owned classes.
- Follow modular DI conventions in `core/di/`:
  - `DataModule`: provides network clients, storage managers, and binds repository interfaces.
  - `UseCaseModule` or constructor injection for domain use cases.
- Use `@Binds` for interface-to-implementation bindings and `@Provides` for instances
  requiring builder/factory construction or Android context.
- Match dependency scopes to their intended lifetime (`@Singleton` for storage/network managers,
  `@ViewModelScoped` where appropriate).

### Storage Access Framework (SAF) & Local Storage

- Access the Eden emulator directory via its native `DocumentsProvider` (`dev.eden.eden_emulator`)
  or fallback SAF tree picker.
- Always acquire and persist URI permissions (`takePersistableUriPermission`) with both read
  and write flags.
- **Profile Normalization**: Eden generates arbitrary UUID folders
  (`nand/user/save/0000000000000000/<UUID_PERFIL>/<TITLE_ID>/`). The data layer must
  dynamically detect the active local UUID and map saves strictly by their 16-character
  hexadecimal `Title ID`. Never copy the profile UUID from one device to another.
- **Content Verification**: calculate SHA-256 hashes of actual game files and track the true
  save modification timestamp. Never rely solely on SAF folder timestamps, which are prone
  to falsification upon extraction.
- **Safety Backups**: before overwriting any local save during cloud download or restore,
  automatically package the existing local save into a timestamped `.zip` backup in app-private
  storage, retaining the last 5 backups per game.

### Cloud Sync & Networking

- Use the Google Drive REST API v3 to manage the `EdenSaveSync` application folder.
- Save files are uploaded as `<TITLE_ID>.zip` accompanied by atomic file properties
  (`titleId`, `gameName`, `contentHash`, `lastModifiedMs`, `sourceDeviceName`, `uploadedAtMs`).
- Authentication supports:
  1. OAuth 2.0 with PKCE and local loopback redirect server.
  2. Instant device-to-device pairing via encrypted/encoded pairing string (`EDEN-SYNC:...`).
  3. Direct access/refresh token configuration.
- Handle token expiration, refresh cycles, rate limits, and network errors gracefully without
  terminating active sync operations silently.

### Navigation

- Keep navigation modular and type-safe using Compose Navigation / Navigation3 patterns.
- Define type-safe route keys in `Routes.kt` and centralize screen wiring in `AppNavigation.kt`.
- Keep screen-level modals (Drive settings, backup manager, game rename dialog) managed via
  predictable UI state or navigation routes.

### UI state and Compose

- Each screen exposes a single `StateFlow<UiState>` from its ViewModel (e.g., `EdenSyncUiState`).
- Collect state in Composables using `collectAsStateWithLifecycle()`.
- Separate stateful screen entry points (wiring ViewModel, Activity Result Launchers for SAF,
  and callbacks) from stateless `*Content` composables.
- Responsive Layouts: optimize UI for both standard mobile screens, foldable aspect ratios
  (e.g., Samsung Galaxy Z Fold unfolded/split-screen), and landscape gaming consoles
  (e.g., AYN Odin 2 / Odin 2 Portal).
- Provide clear visual states: scanning, syncing in progress (with current game and action),
  conflict recommendations (`UPLOAD_TO_CLOUD`, `DOWNLOAD_FROM_CLOUD`, `IN_SYNC`), empty states,
  and dismissible status banners for errors or success.
- Keep business logic, file I/O, and direct preferences access out of composables.

### Coroutines and errors

- Use structured concurrency with `viewModelScope` for UI-orchestrated operations.
- Heavy operations (ZIP compression/decompression, SHA-256 hashing, SAF tree traversal,
  and Google Drive network calls) must always run on `Dispatchers.IO`.
- Never swallow `CancellationException`. If catching general exceptions (`Exception` or `Throwable`),
  always check and rethrow `CancellationException`.
- Map failures to clear error states or domain messages rather than failing silently.

### Theming

- Colors and typography reside in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`).
- Use Material 3 with the custom dark palette (`EdenDarkColorScheme`) optimized for AMOLED
  displays and handheld gaming interfaces.
- Avoid hardcoded color literals in Composables; use `MaterialTheme.colorScheme` tokens.

## Completion report

Summarize the implemented behavior, the affected feature documents, and the
validation results. Link acceptance criteria to evidence in `TASKS.md`.
Clearly identify anything incomplete or unverified; do not present a test name,
an unexecuted command, or "should work" as proof of success.