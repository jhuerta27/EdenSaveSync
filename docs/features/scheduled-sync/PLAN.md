# PLAN: Sincronización Programada en Segundo Plano

**SPEC de referencia:** [docs/features/scheduled-sync/SPEC.md](docs/features/scheduled-sync/SPEC.md)
**Versión de la spec revisada:** 2026-11-04 (Aprobada)
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
| `AppPreferences.kt` | `app/src/main/java/com/jotasoft/edensavesync/data/storage/AppPreferences.kt` | Persistencia de configuraciones (URIs, credenciales, tokens). Se ampliará para almacenar el flag y la hora de sincronización programada. |
| `SyncAllRecommendedUseCase.kt` | `app/src/main/java/com/jotasoft/edensavesync/domain/usecase/SyncAllRecommendedUseCase.kt` | Orquesta la sincronización segura 1-tap con backups preventivos. Será invocado por el Worker en segundo plano. |
| `SettingsScreen.kt` | `app/src/main/java/com/jotasoft/edensavesync/presentation/settings/SettingsScreen.kt` | Pantalla vacía actual de Ajustes; se construirá la UI con el switch, selector horario y pie de versión. |
| `AppNavigation.kt` | `app/src/main/java/com/jotasoft/edensavesync/core/navigation/AppNavigation.kt` | Orquestador de navegación entre `Screen.Sync` y `Screen.Settings`. |
| `EdenApplication.kt` | `app/src/main/java/com/jotasoft/edensavesync/EdenApplication.kt` | Aplicación Hilt; servirá como contenedor de dependencias para el `CoroutineWorker`. |

**Convenciones y patrón de referencia:**
Clean Architecture y UDF. La capa de presentación observa `StateFlow<SettingsUiState>` desde `SettingsViewModel`. Las operaciones en segundo plano se aíslan en `ScheduledSyncWorker` vía `WorkManager`, inyectando `SyncAllRecommendedUseCase` mediante un `@EntryPoint` de Hilt sobre el contexto de la aplicación para evitar dependencias innecesarias de fábricas personalizadas.

## Solución propuesta

1. **Gestor de Planificación (`ScheduledSyncScheduler`)**:
   - Cancela de forma previa, explícita y completa cualquier alarma existente (`alarmManager.cancel`, `pendingIntent.cancel()`) y trabajo en cola (`WorkManager.cancelUniqueWork`) antes de programar una nueva hora.
   - Calcula el timestamp exacto hasta la próxima ocurrencia de la hora/minuto configurada.
   - Registra una alarma exacta con `AlarmManager` (`setAlarmClock` / `setExactAndAllowWhileIdle`), capaz de despertar al dispositivo y ejecutar la tarea a la hora exacta aunque la aplicación esté cerrada o en modo Doze.
   - Encola además un `PeriodicWorkRequest` con política `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE` para que al cambiar la hora se reinicie verdaderamente el retardo inicial.
   - Valida coherencia horaria mediante `isTimeMatchingSchedule` con cálculo circular para descartar alarmas rezagadas u obsoletas.
   - Cancela tanto la alarma de `AlarmManager` como el trabajo de `WorkManager` de inmediato si el switch se apaga.

2. **Receptores de Sistema y Trabajador (`ScheduledSyncReceiver` y `BootReceiver`)**:
   - `ScheduledSyncReceiver`: `BroadcastReceiver` que se activa al sonar la alarma de `AlarmManager`.
     - Reprograma automáticamente la alarma para el día siguiente (+24h).
     - Comprueba que la hora actual coincida con la configurada (`isTimeMatchingSchedule`); si es obsoleta, sale sin sincronizar ni notificar.
     - Verifica si ya se ejecutó la sincronización de hoy (`lastScheduledSyncDate == today`); si ya se realizó, sale inmediatamente para evitar duplicados.
     - Marca `lastScheduledSyncDate = today` y ejecuta la sincronización en segundo plano con `goAsync()` en `Dispatchers.IO`.
     - Si las notificaciones están activadas, emite la alerta con el resultado.
   - `BootCompletedReceiver`: Escucha `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` para restaurar la alarma si el usuario reinicia el teléfono/consola.
   - `ScheduledSyncWorker`: `CoroutineWorker` que también verifica `lastScheduledSyncDate == today` para garantizar unicidad estricta y emite la notificación si está habilitada.

3. **Capa de Ajustes (`SettingsViewModel` y `SettingsScreen`)**:
   - `SettingsViewModel`: expone `isScheduledSyncEnabled`, `scheduledHour`, `scheduledMinute`, `isNotificationEnabled` y `versionName`. Al cambiar el switch, la hora o el checkbox, persiste en `AppPreferences` y actualiza la programación en `ScheduledSyncScheduler`.
   - `SettingsScreen`: UI estilizada en Material 3 oscuro AMOLED:
     - Card de Automatización con `Switch`.
     - Card animada desplegable con la hora configurada (ej. `04:00 AM`).
     - Switch morado bajo la hora para activar/desactivar la notificación del resultado de sincronización.
     - Diálogo Material 3 `TimePickerDialog` con `rememberTimePickerState`.
     - Sección informativa inferior con la versión de la app.

## Módulos y componentes afectados

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| `:app` | Existente | Módulo único de la aplicación. | Incorpora `androidx.work:work-runtime-ktx`. |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `AndroidManifest.xml` | Modificar | Añadir permiso `android.permission.POST_NOTIFICATIONS`. | RF-07 |
| `data/notification/SyncNotificationManager.kt` | Crear | Creación de canal de notificaciones y emisión de alertas con resultado. | RF-07 |
| `data/storage/AppPreferences.kt` | Modificar | Agregar getters/setters para `isScheduledSyncNotificationEnabled`. | RF-06 |
| `data/scheduler/ScheduledSyncWorker.kt` | Modificar | Invocar notificación de resultado si está habilitada tras completar el sync. | RF-07 |
| `presentation/settings/SettingsUiState.kt` | Modificar | Añadir campo `isNotificationEnabled: Boolean`. | RF-06 |
| `presentation/settings/SettingsViewModel.kt` | Modificar | Añadir intent `onToggleNotification(enabled)`. | RF-06 |
| `presentation/settings/SettingsScreen.kt` | Modificar | Añadir `Checkbox` bajo la hora de sincronización y launcher de permiso. | RF-06, RF-07 |
| `res/values/strings.xml` y `res/values-es/strings.xml` | Modificar | Cadenas i18n para checkbox y títulos de notificación. | RF-06, RF-07 |

## Datos y contratos

- **Modelos y contratos de entrada y salida:**
  - `SettingsUiState`:
    - `isScheduledSyncEnabled: Boolean`
    - `scheduledHour: Int`
    - `scheduledMinute: Int`
    - `appVersion: String`
- **Identificadores, relaciones y restricciones:**
  - Nombre único de trabajo de WorkManager: `"EdenScheduledSyncWork"`.
  - Hora válida: `0..23`, Minuto válido: `0..59`.
- **Persistencia, consultas y actualizaciones:**
  - `AppPreferences`: claves `"is_scheduled_sync_enabled"`, `"scheduled_sync_hour"`, `"scheduled_sync_minute"`.
- **Convivencia entre datos locales y remotos:**
  - La sincronización en segundo plano respeta exactamente las reglas de negocio de `SyncAllRecommendedUseCase` (rotación de los 5 últimos backups en ZIP antes de sobrescribir).

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:**
  - `SettingsScreen` conectado a `SettingsViewModel` mediante `collectAsStateWithLifecycle()`.
- **Conservación y restauración del estado:**
  - Los valores provienen de `AppPreferences` y sobreviven a la recreación de actividades y muerte de proceso.
- **Ejecución, concurrencia y cancelación:**
  - `ScheduledSyncScheduler.cancelDailySync()` cancela el trabajo único en `WorkManager`.
  - `ScheduledSyncWorker` ejecuta en `Dispatchers.IO` y no bloquea el hilo principal.
- **Errores y reintentos:**
  - Si no hay conexión o falla la llamada de red a Drive, `WorkManager` retiene la tarea y reintenta cuando se cumplan las restricciones.

## Dependencias y configuración

- **Librerías agregadas:**
  - `androidx.work:work-runtime-ktx:2.10.0` (incluida en `gradle/libs.versions.toml`).
- **Permisos:**
  - `android.permission.INTERNET` (ya presente).
  - `WorkManager` gestiona internamente la persistencia y recepción de boot sin requerir permisos especiales adicionales.

## Subagentes

- **Uso de subagentes:** No se requiere delegación en subagentes; el alcance está acotado a la pantalla de Ajustes y un Worker de WorkManager en el módulo `:app`.

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Test unitario de `AppPreferences` / `SettingsViewModel` | Local JVM | El cambio de switch actualiza el estado y persiste en preferencias. |
| CA-02 | Test unitario de cancelación en scheduler | Local JVM | Desactivar el switch invoca la cancelación del trabajo único. |
| CA-03 | Inspección visual en Compose | Dispositivo / Preview | Si el switch está apagado, el selector horario permanece oculto. |
| CA-04 | Test unitario de cálculo de retardo horario | Local JVM | Al cambiar hora/minuto se calcula el delta correcto para las próximas 24h. |
| CA-05 | Test de worker / ejecución de use case | Local JVM | `ScheduledSyncWorker` invoca `SyncAllRecommendedUseCase` correctamente. |
| CA-06 | Verificación de UI en `SettingsScreen` | Local JVM / Android build | Se comprueba que `appVersion` muestra el formato esperado. |

**Comprobaciones de regresión:**
- Ejecutar `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:lintDebug`.

**Comandos verificados:**
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## Orden de implementación

1. **Configuración y Dependencias:** Agregar `work-runtime-ktx` a `libs.versions.toml` y `app/build.gradle.kts`.
2. **Capa de Datos y Scheduler:** Extender `AppPreferences`, crear `ScheduledSyncScheduler` y `ScheduledSyncWorker`.
3. **Capa de Presentación:** Crear `SettingsUiState`, `SettingsViewModel` y actualizar `SettingsScreen` con Material 3 `TimePickerDialog`, Switch y sección de versión.
4. **Strings e Internacionalización:** Añadir cadenas en `values/strings.xml` y `values-es/strings.xml`.
5. **Validación:** Ejecutar compilación, tests unitarios y lint para asegurar 0 regresiones.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:**
  - *Riesgo:* En Android 12+ Doze Mode puede retrasar ligeramente la ejecución del Worker periódico.
  - *Mitigación:* Se planifica como `PeriodicWorkRequest` con retardo inicial adecuado y restricciones de red; es aceptable para una sincronización en reposo/nocturna diaria.
- **Decisiones pendientes:**
  - Ninguna.
