# TASKS: Soporte para Gamepad e Indicador Visual de Foco

**SPEC:** [SPEC.md](SPEC.md) (Aprobada)  
**PLAN:** [PLAN.md](PLAN.md) (Aprobado)

---

## Tareas de Implementación

- [x] **TSK-01**: Implementar el modificador reutilizable de foco condicional para gamepad (`ui/theme/GamepadFocus.kt`).
  - *Objetivo:* Crear `Modifier.gamepadFocusable` que utilice `LocalInputModeManager` para mostrar un borde resaltado (`AccentCyan`/`EdenPurple`, 2.dp) cuando el elemento esté enfocado en modo gamepad/teclado (`InputMode.Keyboard`), y permanezca oculto en modo táctil (`InputMode.Touch`).
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/ui/theme/GamepadFocus.kt`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-01.
  - *Validación:* Implementado con función pura `shouldShowGamepadFocus` y modificador `@Composable Modifier.gamepadFocusable`. Test unitario en JVM aprobado.

- [x] **TSK-02**: Implementar la interceptación y traducción de eventos de gamepad en `MainActivity.kt`.
  - *Objetivo:* Sobrescribir `dispatchKeyEvent` para mapear `KEYCODE_BUTTON_A` a `KEYCODE_DPAD_CENTER` y `KEYCODE_BUTTON_B` a `KEYCODE_BACK`. Implementar `onGenericMotionEvent` para transformar el movimiento de sticks analógicos (`AXIS_X`, `AXIS_Y`, `AXIS_HAT_X`, `AXIS_HAT_Y`) en eventos de D-pad con debouncing.
  - *Alcance:* 
    - `app/src/main/java/com/jotasoft/edensavesync/data/device/GamepadInputHandler.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/MainActivity.kt`
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-05.
  - *Validación:* Test unitario `GamepadInputHandlerTest` ejecutado con éxito comprobando mapeo de teclas A/B y sticks.

- [x] **TSK-03**: Aplicar soporte e indicador de foco en los componentes de la pantalla principal (`EdenSyncScreen`).
  - *Objetivo:* Integrar `gamepadFocusable` en el botón de ajustes y la fila de edición de nombre de dispositivo en `TopAppBar`, en las tarjetas interactivas de `SetupStatusCard`, en el botón hero de `OneTouchSyncHeroCard`, en el banner de estado `SyncSummaryBanner` y en los botones de acción rápida de `GameItemCard` (subir, descargar, renombrar e historial).
  - *Alcance:* 
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/EdenSyncScreen.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/SetupStatusCard.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/OneTouchSyncHeroCard.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/GameItemCard.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/SyncSummaryBanner.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/EmptySavesPlaceholder.kt`
  - *Dependencias:* TSK-01.
  - *Criterios que resuelve:* CA-01, CA-02, CA-05.
  - *Validación:* Compilación exitosa y verificación de asignación de modificadores de foco en todos los elementos interactivos.

- [x] **TSK-04**: Aplicar soporte e indicador de foco en `SettingsScreen`.
  - *Objetivo:* Integrar `gamepadFocusable` en el botón de navegación atrás de la barra superior, en el conmutador de sincronización programada, en la fila selectora de hora, en el conmutador de notificaciones y en los botones del diálogo selector de hora.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/presentation/settings/SettingsScreen.kt`.
  - *Dependencias:* TSK-01.
  - *Criterios que resuelve:* CA-01, CA-03, CA-05.
  - *Validación:* Compilación exitosa y verificación de foco en controles de ajustes.

- [x] **TSK-05**: Aplicar soporte e indicador de foco en todos los diálogos modales.
  - *Objetivo:* Integrar `gamepadFocusable` en los botones de acción y campos interactivos de `DriveConfigDialog`, `EdenFolderSetupDialog`, `DeviceNameDialog`, `RenameGameDialog` y `BackupHistoryDialog`.
  - *Alcance:* 
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/DriveConfigDialog.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/EdenFolderSetupDialog.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/DeviceNameDialog.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/RenameGameDialog.kt`
    - `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/BackupHistoryDialog.kt`
  - *Dependencias:* TSK-01.
  - *Criterios que resuelve:* CA-01, CA-04, CA-05.
  - *Validación:* Compilación exitosa y verificación de controles enfocables en todos los diálogos.

- [x] **TSK-06**: Pruebas automatizadas y validación completa del sistema.
  - *Objetivo:* Crear tests unitarios en JVM para verificar la lógica de eventos de gamepad y la integración de foco condicional. Ejecutar la compilación completa de depuración, tests unitarios y Android Lint.
  - *Alcance:* Directorio `app/src/test` y suites completas de Gradle.
  - *Dependencias:* TSK-01 a TSK-05.
  - *Criterios que resuelve:* CA-01 a CA-05.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado sin errores).
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (33 tests aprobados, incluyendo `GamepadInputHandlerTest`).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-07**: Integrar soporte para el botón START (`KEYCODE_BUTTON_START`/`KEYCODE_MENU`) para abrir Ajustes desde la pantalla principal y volver desde Ajustes.
  - *Objetivo:* Manejar el evento del botón START en `MainActivity.kt` y conectarlo con la navegación de `AppNavigation.kt`.
  - *Alcance:* `MainActivity.kt`, `AppNavigation.kt`, `GamepadInputHandler.kt`.
  - *Dependencias:* TSK-02.
  - *Criterios que resuelve:* CA-06.
  - *Validación:* Conexión implementada con `DisposableEffect` y verificación mediante test unitario `isStartButton_identifiesStartAndMenuKeys`.

- [x] **TSK-08**: Implementar scroll vertical continuo con el stick derecho (Analog R).
  - *Objetivo:* Procesar los ejes `AXIS_RZ`/`AXIS_RY`/`AXIS_Z` en `GamepadInputHandler`, implementar `GamepadScrollController` con ticker continuo a 60 FPS mientras el stick esté inclinado, y conectarlo a las `LazyListState` de `EdenSyncScreen` y `SettingsScreen`.
  - *Alcance:* `data/device/GamepadInputHandler.kt`, `data/device/GamepadScrollController.kt`, `MainActivity.kt`, `EdenSyncScreen.kt`, `SettingsScreen.kt`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-07.
  - *Validación:* Implementación de `GamepadScrollController` con deadzone de 0.15f, velocidad proporcional a la inclinación y registro reactivo en `EdenSyncScreen` y `SettingsScreen`.

- [x] **TSK-09**: Reforzar y garantizar la ejecución inmediata de la acción con el Botón A sobre el elemento enfocado.
  - *Objetivo:* Eliminar `.focusable()` redundante en `Modifier.gamepadFocusable` para no ocultar ni duplicar el nodo de foco de los `Button`s y `clickable`s, y añadir soporte de interceptación `onKeyEvent` con `onClick` explícito para `BUTTON_A`/`DPAD_CENTER` en elementos clickables personalizados.
  - *Alcance:* `ui/theme/GamepadFocus.kt` y componentes interactivos (`SetupStatusCard`, `EdenSyncScreen`, `SyncSummaryBanner`, `GameItemCard`, `SettingsScreen`).
  - *Dependencias:* TSK-01, TSK-03, TSK-04.
  - *Criterios que resuelve:* CA-05.
  - *Validación:* `gamepadFocusable` actualizado con delegación directa y disparo en `ACTION_UP`. Compilación y tests en verde.

- [x] **TSK-10**: Validación completa con tests y suite de Gradle.
  - *Objetivo:* Actualizar y ejecutar tests unitarios JVM, compilación completa y análisis de lint sin errores.
  - *Alcance:* Todo el proyecto.
  - *Dependencias:* TSK-07 a TSK-09.
  - *Criterios que resuelve:* CA-01 a CA-07.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL`.
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (34 tests aprobados).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores).

- [x] **TSK-11**: Ajustar el borde de foco a la silueta exacta de los botones de acción en `GameItemCard`, `SetupStatusCard` y el Switch principal de `SettingsScreen`.
  - *Objetivo:* Evitar que `minimumInteractiveComponentSize` (48.dp por defecto en Material 3) expanda el cuadro exterior de dibujo del borde de foco en botones compactos de 34.dp (`FilledTonalButton` y `OutlinedButton`) y en el `Switch` principal de ajustes (32.dp de alto x 52.dp de ancho), y asegurar que los controles respeten su silueta en píldora (`CircleShape` / `RoundedCornerShape(50)`) y tarjetas redondeadas de 12.dp.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/GameItemCard.kt`, `SetupStatusCard.kt` y `presentation/settings/SettingsScreen.kt`.
  - *Dependencias:* TSK-01, TSK-03, TSK-04.
  - *Criterios que resuelve:* CA-01, CA-02, CA-03.
  - *Validación:* Envoltura de `FlowRow` y del `Switch` principal con `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified)` y delegación de `onClick` en `gamepadFocusable`. Verificado con `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (`BUILD SUCCESSFUL`, 0 errores).

- [x] **TSK-12**: Adecuar la altura y forma del foco seleccionado en los botones de todos los diálogos de la aplicación.
  - *Objetivo:* Evitar que el padding mínimo de 48.dp infle el borde de foco alrededor de `Button`, `TextButton`, `OutlinedButton` y `AssistChip` en todos los diálogos modales (`EdenFolderSetupDialog`, `DriveConfigDialog`, `DeviceNameDialog`, `RenameGameDialog`, `BackupHistoryDialog` y diálogo selector de hora en `SettingsScreen`), vinculando además sus callbacks `onClick` a `gamepadFocusable` para ejecución inmediata con el botón A.
  - *Alcance:* `EdenFolderSetupDialog.kt`, `DriveConfigDialog.kt`, `DeviceNameDialog.kt`, `RenameGameDialog.kt`, `BackupHistoryDialog.kt` y `SettingsScreen.kt`.
  - *Dependencias:* TSK-01, TSK-05.
  - *Criterios que resuelve:* CA-01, CA-04, CA-05.
  - *Validación:* Envoltura de `AlertDialog`s con `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified)` y delegación de `onClick`. Verificado con `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (`BUILD SUCCESSFUL`, 35 tests aprobados, 0 errores).
