# TASKS: Sincronización Programada en Segundo Plano

**SPEC:** [docs/features/scheduled-sync/SPEC.md](SPEC.md) (Aprobada)  
**PLAN:** [docs/features/scheduled-sync/PLAN.md](PLAN.md) (Aprobado)

---

## Tareas de Implementación

- [x] **TSK-01**: Incorporar dependencia `androidx.work:work-runtime-ktx:2.10.0` en `gradle/libs.versions.toml` y `app/build.gradle.kts`.
  - *Objetivo:* Habilitar soporte de `WorkManager` para tareas en segundo plano.
  - *Alcance:* `libs.versions.toml` y `app/build.gradle.kts`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-05.
  - *Validación:* `./gradlew :app:assembleDebug` (Completado con éxito).

- [x] **TSK-02**: Extender `AppPreferences.kt` con la persistencia de automatización.
  - *Objetivo:* Guardar y recuperar `isScheduledSyncEnabled`, `scheduledSyncHour` y `scheduledSyncMinute`.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/data/storage/AppPreferences.kt`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-01, CA-04.
  - *Validación:* Test unitario en JVM y getters/setters verificados.

- [x] **TSK-03**: Implementar el programador `ScheduledSyncScheduler` y el trabajador `ScheduledSyncWorker`.
  - *Objetivo:* Orquestar el cálculo del desfase horario para las próximas 24h, registrar `PeriodicWorkRequest` con restricción de red y ejecutar `SyncAllRecommendedUseCase` en segundo plano mediante `@EntryPoint` de Hilt.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/data/scheduler/*`.
  - *Dependencias:* TSK-01, TSK-02.
  - *Criterios que resuelve:* CA-02, CA-04, CA-05.
  - *Validación:* Test unitario `ScheduledSyncCalculationTest` ejecutado con éxito.

- [x] **TSK-04**: Agregar cadenas de texto localizadas (i18n) para la automatización y versión.
  - *Objetivo:* Soporte bilingüe (EN/ES) sin textos hardcodeados para el switch, subtítulos, diálogo de hora y versión.
  - *Alcance:* `res/values/strings.xml` y `res/values-es/strings.xml`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-01 a CA-06.
  - *Validación:* Compilación de recursos XML (verificada).

- [x] **TSK-05**: Implementar `SettingsUiState`, `SettingsViewModel` y la UI en `SettingsScreen.kt`.
  - *Objetivo:* Construir la interfaz de Ajustes con Switch de activación, visualización animada de la hora elegida, diálogo nativo `TimePickerDialog` de Material 3 y visualización de la versión al pie.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/presentation/settings/*`.
  - *Dependencias:* TSK-02, TSK-03, TSK-04.
  - *Criterios que resuelve:* CA-01, CA-02, CA-03, CA-04, CA-06.
  - *Validación:* Compilación y rendering Compose verificado.

- [x] **TSK-06**: Integrar `SettingsViewModel` en `AppNavigation.kt`.
  - *Objetivo:* Conectar la navegación para inyectar y proveer `SettingsViewModel` a `SettingsScreen`.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/core/navigation/AppNavigation.kt`.
  - *Dependencias:* TSK-05.
  - *Criterios que resuelve:* CA-01 a CA-06.
  - *Validación:* Inyección Hilt y navegación fluida entre `Screen.Sync` y `Screen.Settings`.

- [x] **TSK-07**: Validación integral y pruebas automatizadas.
  - *Objetivo:* Ejecutar compilación completa, pruebas unitarias y análisis de lint sin errores ni advertencias.
  - *Alcance:* Todo el proyecto.
  - *Dependencias:* TSK-01 a TSK-06.
  - *Criterios que resuelve:* CA-01 a CA-06.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado).
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (Tests unitarios aprobados, incluyendo `ScheduledSyncCalculationTest`).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-08**: Declarar permiso `POST_NOTIFICATIONS` en `AndroidManifest.xml` y crear `SyncNotificationManager` para el canal y construcción de notificaciones del resultado.
  - *Objetivo:* Permitir emisión de notificaciones locales informativas en Android 8+.
  - *Alcance:* `AndroidManifest.xml` y `app/src/main/java/com/jotasoft/edensavesync/data/notification/SyncNotificationManager.kt`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-08.
  - *Validación:* Compilación y verificación de canal (completado).

- [x] **TSK-09**: Ampliar `AppPreferences.kt` con `isScheduledSyncNotificationEnabled` y conectar `ScheduledSyncWorker` para notificar al terminar.
  - *Objetivo:* Emitir la notificación al finalizar la sincronización automática si la opción está activa.
  - *Alcance:* `AppPreferences.kt` y `ScheduledSyncWorker.kt`.
  - *Dependencias:* TSK-08.
  - *Criterios que resuelve:* CA-07, CA-08.
  - *Validación:* Inyección Hilt y ejecución verificada en worker.

- [x] **TSK-10**: Añadir cadenas i18n para el checkbox y los textos de la notificación.
  - *Objetivo:* Soporte bilingüe EN/ES para la opción de notificación y mensajes del sistema.
  - *Alcance:* `res/values/strings.xml` y `res/values-es/strings.xml`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-07, CA-08.
  - *Validación:* Compilación de recursos XML (verificado).

- [x] **TSK-11**: Actualizar `SettingsUiState`, `SettingsViewModel` y `SettingsScreen` con el Switch de notificación condicional.
  - *Objetivo:* Mostrar el interruptor morado (`EdenPurple`) bajo la hora de sincronización y solicitar permiso en Android 13+.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/presentation/settings/*`.
  - *Dependencias:* TSK-08, TSK-09, TSK-10.
  - *Criterios que resuelve:* CA-07, CA-08.
  - *Validación:* Verificación interactiva y compilación Compose aprobada.

- [x] **TSK-12**: Validación integral y pruebas finales.
  - *Objetivo:* Asegurar ausencia de regresiones y 0 errores de compilación o lint.
  - *Alcance:* Todo el proyecto.
  - *Dependencias:* TSK-08 a TSK-11.
  - *Criterios que resuelve:* CA-01 a CA-08.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado).
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (Todos los tests pasados).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-13**: Añadir permisos de alarma exacta y reinicio en `AndroidManifest.xml`.
  - *Objetivo:* Declarar `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` y `WAKE_LOCK`.
  - *Alcance:* `AndroidManifest.xml`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-04, CA-05.
  - *Validación:* Compilación del manifest aprobada.

- [x] **TSK-14**: Implementar `ScheduledSyncReceiver` y `BootCompletedReceiver`.
  - *Objetivo:* Despertar al sistema a la hora exacta aunque la app esté cerrada, ejecutar la sincronización y reprogramar tras reinicio.
  - *Alcance:* `app/src/main/java/com/jotasoft/edensavesync/data/scheduler/*`.
  - *Dependencias:* TSK-13.
  - *Criterios que resuelve:* CA-04, CA-05, CA-08.
  - *Validación:* Compilación y registro en manifest completado.

- [x] **TSK-15**: Actualizar `ScheduledSyncScheduler` con `AlarmManager.setAlarmClock`.
  - *Objetivo:* Configurar `AlarmManager` para disparar la alarma al minuto exacto de forma inmune a Doze Mode y cierres de app.
  - *Alcance:* `ScheduledSyncScheduler.kt`.
  - *Dependencias:* TSK-14.
  - *Criterios que resuelve:* CA-04, CA-05.
  - *Validación:* Cálculo de `calculateTargetTimeMs` y programación verificada.

- [x] **TSK-16**: Validación global con app cerrada y pruebas automatizadas.
  - *Objetivo:* Verificar que la ejecución se desencadena con la app cerrada y sin errores de lint.
  - *Alcance:* Todo el proyecto.
  - *Dependencias:* TSK-13 a TSK-15.
  - *Criterios que resuelve:* CA-01 a CA-08.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado).
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (Tests unitarios pasados).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

- [x] **TSK-17**: Extender `AppPreferences.kt` con `lastScheduledSyncDate` e implementar `isTimeMatchingSchedule` en `ScheduledSyncScheduler.kt`.
  - *Objetivo:* Soportar control de sincronización única por día y verificación circular de concordancia horaria (+/- 15 min).
  - *Alcance:* `AppPreferences.kt` y `ScheduledSyncScheduler.kt`.
  - *Dependencias:* Ninguna.
  - *Criterios que resuelve:* CA-09, CA-10.
  - *Validación:* Tests unitarios en JVM y compilación exitosa.

- [x] **TSK-18**: Cancelación previa explícita y política de reemplazo en `ScheduledSyncScheduler.kt` y `SettingsViewModel.kt`.
  - *Objetivo:* Al cambiar la hora o conmutar el switch, invocar `cancelDailySync()` con cancelación activa de `PendingIntent`, y usar `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE` en `WorkManager`.
  - *Alcance:* `ScheduledSyncScheduler.kt` y `SettingsViewModel.kt`.
  - *Dependencias:* TSK-17.
  - *Criterios que resuelve:* CA-09.
  - *Validación:* Invocación de cancelación en `scheduleDailySync` y reseteo en ViewModel verificado.

- [x] **TSK-19**: Filtrado anti-duplicados y descarte de alarmas obsoletas en `ScheduledSyncReceiver.kt` y `ScheduledSyncWorker.kt`.
  - *Objetivo:* Si una alarma no corresponde a la hora configurada o ya se ejecutó la sincronización del día (`lastScheduledSyncDate == today`), abortar inmediatamente sin sincronizar ni emitir notificación.
  - *Alcance:* `ScheduledSyncReceiver.kt` y `ScheduledSyncWorker.kt`.
  - *Dependencias:* TSK-17, TSK-18.
  - *Criterios que resuelve:* CA-10.
  - *Validación:* Verificación de flujo con retorno anticipado sin disparo de notificación.

- [x] **TSK-20**: Pruebas unitarias para cálculo y descarte en `ScheduledSyncCalculationTest.kt`.
  - *Objetivo:* Probar exhaustivamente concordancia horaria, límites de medianoche (23:55 a 00:05) y rechazo de alarmas rezagadas.
  - *Alcance:* `app/src/test/java/com/jotasoft/edensavesync/ScheduledSyncCalculationTest.kt`.
  - *Dependencias:* TSK-17 a TSK-19.
  - *Criterios que resuelve:* CA-09, CA-10.
  - *Validación:* `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (Tests unitarios pasados al 100%).

- [x] **TSK-21**: Verificación de compilación, tests y lint general.
  - *Objetivo:* Garantizar cero regresiones en el proyecto.
  - *Alcance:* Todo el proyecto.
  - *Dependencias:* TSK-17 a TSK-20.
  - *Criterios que resuelve:* CA-01 a CA-10.
  - *Validación:*
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` (APK generado).
    - `./gradlew :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (0 fallos).
    - `./gradlew :app:lintDebug` -> `BUILD SUCCESSFUL` (0 errores de lint).

---

## Registro de Evidencias

- **CA-01 & CA-02**: El switch en `SettingsScreen` conmuta `isScheduledSyncEnabled`, persistiendo el valor en `AppPreferences` e invocando `scheduleDailySync` o `cancelDailySync`.
- **CA-03 & CA-04**: El selector de hora condicional aparece con animación `AnimatedVisibility` solo cuando la automatización está activa. Al pulsar, abre `TimePickerDialog` de Material 3, actualiza la hora y reprograma la tarea. `ScheduledSyncCalculationTest` verifica los cálculos de tiempo para 24h.
- **CA-05**: La alarma se programa en `AlarmManager` mediante `setAlarmClock(AlarmClockInfo)` y se recibe en `ScheduledSyncReceiver`. Despierta a la CPU y ejecuta `SyncAllRecommendedUseCase` en `Dispatchers.IO` vía `goAsync()`.
- **CA-06**: `SettingsScreen` muestra al pie la versión y nombre de la aplicación (`Eden Save Sync • Versión 1.0.0`) dinámicamente desde `PackageInfo`.
- **CA-07**: Switch morado añadido bajo la fila de hora que conmuta `isScheduledSyncNotificationEnabled` en preferencias y solicita el permiso de sistema `POST_NOTIFICATIONS` en Android 13+ si no está concedido.
- **CA-08**: `SyncNotificationManager` crea el canal de notificaciones y emite el resumen (partidas subidas y descargadas o errores) una vez completada la ejecución en segundo plano.
- **CA-09**: Al cambiar la hora en `SettingsViewModel`, se cancela explícitamente cualquier alarma y `PendingIntent` anterior, y `WorkManager` cancela y reenconla el trabajo único con `CANCEL_AND_REENQUEUE`, impidiendo acumulaciones de alarmas.
- **CA-10**: `ScheduledSyncReceiver` y `ScheduledSyncWorker` verifican `isTimeMatchingSchedule` y `lastScheduledSyncDate == today`. Toda alarma rezagada fuera de tolerancia o intento posterior de sincronización el mismo día es descartado silenciosamente sin ejecutar `syncAllRecommendedUseCase` ni notificar.
- **Boot**: `BootCompletedReceiver` escucha `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` para restaurar la alarma en `AlarmManager` tras el reinicio del dispositivo.
- **Boot**: `BootCompletedReceiver` escucha `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` para restaurar la alarma en `AlarmManager` tras el reinicio del dispositivo.

- **CA-01 & CA-02**: El switch en `SettingsScreen` conmuta `isScheduledSyncEnabled`, persistiendo el valor en `AppPreferences` e invocando `scheduleDailySync` o `cancelDailySync`.
- **CA-03 & CA-04**: El selector de hora condicional aparece con animación `AnimatedVisibility` solo cuando la automatización está activa. Al pulsar, abre `TimePickerDialog` de Material 3, actualiza la hora y reprograma la tarea. `ScheduledSyncCalculationTest` verifica los cálculos de tiempo para 24h.
- **CA-05**: `ScheduledSyncWorker` ejecuta `SyncAllRecommendedUseCase` en segundo plano mediante `@EntryPoint` de Hilt sobre `Dispatchers.IO` y reintenta con seguridad en caso de incidencias de red sin corromper datos.
- **CA-06**: `SettingsScreen` muestra al pie la versión y nombre de la aplicación (`Eden Save Sync • Versión 1.0.0`) dinámicamente desde `PackageInfo`.
- **CA-07**: Checkbox añadido bajo la fila de hora que conmuta `isScheduledSyncNotificationEnabled` en preferencias y solicita el permiso de sistema `POST_NOTIFICATIONS` en Android 13+ si no está concedido.
- **CA-08**: `SyncNotificationManager` crea el canal de notificaciones y emite el resumen (partidas subidas y descargadas o errores) una vez completada la ejecución en `ScheduledSyncWorker`.

