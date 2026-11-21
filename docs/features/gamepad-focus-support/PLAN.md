# PLAN: Soporte para Gamepad e Indicador Visual de Foco

**SPEC de referencia:** docs/features/gamepad-focus-support/SPEC.md
**Versión de la spec revisada:** 2026-11-12 (Estado: Aprobada)
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
| `MainActivity.kt` | `app/src/main/java/com/jotasoft/edensavesync/MainActivity.kt` | Punto de entrada de la actividad. Interceptará eventos de hardware del gamepad (`dispatchKeyEvent` para `BUTTON_A`/`BUTTON_B` y `onGenericMotionEvent` para sticks analógicos). |
| `EdenSyncScreen.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/EdenSyncScreen.kt` | Pantalla principal con TopAppBar, edición de nombre de dispositivo, botón de Ajustes y lista de guardados. |
| `OneTouchSyncHeroCard.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/OneTouchSyncHeroCard.kt` | Tarjeta destacada con el botón principal "Sincronizar todo". |
| `GameItemCard.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/GameItemCard.kt` | Tarjeta individual de juego con acciones de subir, descargar, historial de copias y renombrado. |
| `SetupStatusCard.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/SetupStatusCard.kt` | Tarjetas interactivas de estado de almacenamiento Eden y conexión a Google Drive. |
| Diálogos modales | `app/src/main/java/com/jotasoft/edensavesync/presentation/sync/components/*Dialog.kt` | Diálogos de configuración de Drive, selección de carpeta Eden, renombrado, nombre de dispositivo e historial. |
| `SettingsScreen.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/settings/SettingsScreen.kt` | Pantalla de configuración con switches, fila selectora de hora y botón de navegación atrás. |
| Paleta de color y tema | `app/src/main/java/com/jotasoft/edensavesync/ui/theme/Color.kt` | Colores base (`AccentCyan`, `EdenPurple`, `CardBorder`, `BgDark`). |

**Convenciones y patrón de referencia:** Arquitectura Clean con UDF. Compose Material 3 con tema oscuro AMOLED. Los modificadores visuales y de interacción deben ser reutilizables, respetando `LocalInputModeManager` para distinguir automáticamente entre modo táctil (`InputMode.Touch`) y modo mando/teclado (`InputMode.Keyboard`).

## Solución propuesta

1. **Modificador de foco condicional para Gamepad (`Modifier.gamepadFocusable`):**
   - Se creará una función de extensión reutilizable en Compose (`ui/theme/GamepadFocus.kt`).
   - Obtendrá el modo de entrada actual mediante `LocalInputModeManager.current.inputMode`.
   - Registrará el estado de foco mediante `Modifier.onFocusChanged { isFocused = it.isFocused }`.
   - Cuando el elemento tenga foco Y el modo sea `InputMode.Keyboard` (controlador externo o gamepad), aplicará un borde resaltado distintivo (por defecto 2.dp con color `AccentCyan` o el color configurado para el componente) respetando el `Shape` del elemento.
   - Cuando el modo sea `InputMode.Touch`, el borde de foco no se dibujará en absoluto, manteniendo la apariencia táctil intacta y limpia.

2. **Mapeo nativo de teclas de Gamepad en `MainActivity`:**
   - **Botón A (`KEYCODE_BUTTON_A`):** En controladores Android físicos, el botón A envía el código de tecla 96. Se interceptará en `dispatchKeyEvent` y se transformará en `KEYCODE_DPAD_CENTER` para que active automáticamente cualquier elemento enfocado en Compose (`Button`, `IconButton`, `Modifier.clickable`).
   - **Botón B (`KEYCODE_BUTTON_B`):** Se interceptará en `dispatchKeyEvent` y se transformará en `KEYCODE_BACK` para permitir retroceso y cierre de diálogos de forma natural.
   - **Sticks analógicos (`onGenericMotionEvent`):** Se leerán los ejes `AXIS_X`, `AXIS_Y`, `AXIS_HAT_X` y `AXIS_HAT_Y`. Al sobrepasar el umbral (0.5f) con debouncing temporal, se despacharán eventos `DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT` o `DPAD_RIGHT`, permitiendo navegar fluidamente tanto con la cruceta (D-pad) como con el stick analógico izquierdo.

3. **Aplicación sistemática del foco en componentes interactivos:**
   - **`EdenSyncScreen`:** Aplicación en el botón de Ajustes (`IconButton`), el botón/fila de edición del nombre de dispositivo y en `EmptySavesPlaceholder`.
   - **`SetupStatusCard`:** En las dos tarjetas interactivas (Carpeta de Eden y Google Drive).
   - **`OneTouchSyncHeroCard`:** En el botón "Sincronizar todo".
   - **`GameItemCard`:** En los botones de Subir, Descargar, Historial de copias y en el icono/botón de renombrar juego.
   - **`SettingsScreen`:** En el botón de retroceso (`IconButton`), el conmutador de sincronización programada (`Switch`), la fila selectora de hora y el conmutador de notificaciones.
   - **Diálogos:** En todos los botones de acción (`Button`, `TextButton`, `OutlinedButton`) y campos de texto (`OutlinedTextField`), asegurando orden de foco lógico.

4. **Subagentes:** No se requieren subagentes para esta funcionalidad; todos los cambios se encuentran acotados a la capa de presentación de la app y la actividad principal.

## Módulos y componentes afectados

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| `:app` | Existente | Módulo único de la aplicación. | Ninguna nueva dependencia de biblioteca externa. |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `ui/theme/GamepadFocus.kt` | Crear | Modificador `Modifier.gamepadFocusable(...)` y utilidades de foco condicional según `InputMode`. | RF-01, CA-01 |
| `MainActivity.kt` | Modificar | `dispatchKeyEvent` para `BUTTON_A`/`BUTTON_B` y `onGenericMotionEvent` para sticks analógicos. | RF-05, RF-06, CA-05 |
| `presentation/sync/components/OneTouchSyncHeroCard.kt` | Modificar | Aplicar `gamepadFocusable` al botón de sincronización. | RF-02, CA-02 |
| `presentation/sync/components/SetupStatusCard.kt` | Modificar | Aplicar `gamepadFocusable` a las tarjetas interactivas de configuración. | RF-02, CA-02 |
| `presentation/sync/components/GameItemCard.kt` | Modificar | Aplicar `gamepadFocusable` a botones de subir, descargar, historial y renombrar. | RF-02, CA-02 |
| `presentation/sync/EdenSyncScreen.kt` | Modificar | Aplicar `gamepadFocusable` a barra superior (ajustes, dispositivo) y placeholder vacío. | RF-02, CA-02 |
| `presentation/settings/SettingsScreen.kt` | Modificar | Aplicar `gamepadFocusable` al botón de regreso, switches y fila de hora. | RF-03, CA-03 |
| Diálogos (`*Dialog.kt`) | Modificar | Aplicar `gamepadFocusable` a los botones y campos interactivos de cada diálogo. | RF-04, CA-04 |

## Datos y contratos

- **Modelos y contratos de entrada y salida:** No aplica; es un cambio estrictamente de interfaz de usuario y eventos de entrada física.
- **Identificadores, relaciones y restricciones:** No aplica.
- **Origen de los datos mostrados y transformaciones:** No aplica.
- **Persistencia, consultas y actualizaciones:** No aplica.
- **Convivencia entre datos locales y remotos:** No aplica.
- **Compatibilidad y migraciones de datos existentes:** No aplica cambios en bases de datos o preferencias locales.

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:** El estado del foco es gestionado internamente por el subsistema de foco de Compose (`FocusRequester`, `onFocusChanged`, `LocalInputModeManager`).
- **Conservación y restauración del estado:** Al alternar entre modo táctil y modo mando, la UI no se recrea; simplemente se conmuta la visibilidad del marco de foco reactivamente.
- **Ejecución, concurrencia y cancelación de operaciones:** La activación por botón A dispara el mismo callback `onClick` ya existente, garantizando el mismo manejo de concurrencia y bloqueo durante operaciones en progreso.
- **Errores, reintentos y prevención de duplicados:** Se mantiene el flag `enabled` en los botones cuando la app está ocupada (`isBusy`), evitando pulsaciones duplicadas desde el mando.
- **Otras consideraciones mobile aplicables y su solución:** Soporte para consolas Android dedicadas (como AYN Odin 2 / Odin 2 Portal) y mandos Bluetooth estándar (Xbox, PlayStation, 8BitDo).

## Dependencias y configuración

- No se añaden nuevas dependencias en `build.gradle.kts`. Se utilizan las APIs nativas de Android (`android.view.KeyEvent`, `android.view.MotionEvent`, `android.view.InputDevice`) y Compose Foundation (`LocalInputModeManager`, `InputMode`, `Modifier.onFocusChanged`, `Modifier.border`).

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Test unitario / JVM Compose test con simulación de `InputMode.Keyboard` e `InputMode.Touch`. | JVM / Compose UI Test. | Verificación de aplicación del borde de foco únicamente en `InputMode.Keyboard`. |
| CA-02 | Comprobación de modificador `gamepadFocusable` en todos los componentes de `EdenSyncScreen`. | JVM compilation / Unit test. | Confirmación de foco en barra superior, tarjetas de setup, botón hero y botones de juego. |
| CA-03 | Comprobación de modificador `gamepadFocusable` en `SettingsScreen`. | JVM compilation / Unit test. | Confirmación de foco en botón volver, switches y selector de hora. |
| CA-04 | Comprobación en diálogos modales. | JVM compilation / Unit test. | Confirmación de foco en botones Aceptar/Cancelar y campos de texto de diálogos. |
| CA-05 | Test de despacho de eventos de teclas en `MainActivity` (`BUTTON_A` -> `DPAD_CENTER`, `BUTTON_B` -> `BACK`). | JVM Unit Test de despacho de teclas. | Comprobación de traducción de códigos de tecla para controlador externo. |

**Comprobaciones de regresión:** Ejecutar la suite completa de pruebas unitarias existentes (`./gradlew :app:testDebugUnitTest`), el ensamblado (`./gradlew :app:assembleDebug`) y el análisis de lint (`./gradlew :app:lintDebug`).

**Comandos verificados para compilar y ejecutar tests:**
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

**Pruebas en dispositivo, emulador o simulador:** Verificación manual conectando un gamepad (o mediante comandos `adb shell input keyevent` simulando D-pad y `BUTTON_A`) en emulador/dispositivo Android.

**Limitaciones del entorno:** La verificación física directa con hardware específico de mando se apoya en la simulación de eventos `KeyEvent` y validaciones unitarias en JVM si no hay un mando físico USB conectado directamente al runner de ejecución.

## Orden de implementación

1. **Creación del modificador de foco para gamepad (`ui/theme/GamepadFocus.kt`):** Implementar `gamepadFocusable` con lógica condicional según `LocalInputModeManager` y estados de foco.
2. **Mapeo de teclas y sticks en `MainActivity.kt`:** Implementar `dispatchKeyEvent` y `onGenericMotionEvent` para soportar `BUTTON_A`, `BUTTON_B` y joysticks analógicos.
3. **Integración en componentes de `EdenSyncScreen`:** Aplicar el modificador a la barra superior, `SetupStatusCard`, `OneTouchSyncHeroCard`, `GameItemCard` y `EmptySavesPlaceholder`.
4. **Integración en `SettingsScreen`:** Aplicar el modificador a la navegación atrás, switches y selector de hora.
5. **Integración en Diálogos modales:** Aplicar el modificador a los controles de `DriveConfigDialog`, `EdenFolderSetupDialog`, `DeviceNameDialog`, `RenameGameDialog` y `BackupHistoryDialog`.
6. **Pruebas y validación:** Crear tests unitarios para verificar el comportamiento de despacho de teclas y el modificador de foco, y ejecutar `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest` y `./gradlew :app:lintDebug`.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:**
  - *Riesgo:* Desplazamientos involuntarios del layout al ganar foco.
    *Medida:* Usar `.border()` que se dibuja dentro de los límites del componente sin alterar su tamaño medido.
  - *Riesgo:* Sensibilidad excesiva del stick analógico generando múltiples saltos de foco.
    *Medida:* Implementar umbral (> 0.5f) con debouncing temporal (mínimo 200 ms entre pasos) en `onGenericMotionEvent`.
- **Decisiones pendientes:** Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->