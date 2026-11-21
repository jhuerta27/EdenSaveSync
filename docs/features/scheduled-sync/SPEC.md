# SPEC: Sincronización Programada en Segundo Plano

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

Una funcionalidad de automatización para los usuarios de EdenSaveSync que permite programar la sincronización de partidas guardadas con Google Drive en segundo plano a una hora y minuto específicos del día, sin requerir abrir la app manualmente. Además, provee una vista centralizada de Ajustes que incluye la versión actual de la aplicación.

## Situación actual

Actualmente la sincronización se realiza de manera manual cuando el usuario abre la aplicación y pulsa "Sincronizar todo" o utiliza el gesto *Swipe & Refresh*. La pantalla de Ajustes (`SettingsScreen`) existe en la navegación pero se encuentra vacía, sin opciones configurables ni visualización de la versión instalada.

## Dentro del alcance

- **RF-01:** Conmutador principal (Switch) en la pantalla de Ajustes para activar o desactivar la sincronización automática programada.
- **RF-02:** Selector de hora y minuto del día (diálogo selector de tiempo) visible únicamente cuando la automatización se encuentre activada.
- **RF-03:** Persistencia de la configuración de automatización (estado activo/inactivo, hora y minuto seleccionados) en las preferencias del usuario.
- **RF-04:** Ejecución automática garantizada a la hora programada incluso con la app cerrada o tras reiniciar el dispositivo (mediante `AlarmManager`, `BroadcastReceiver` y `WorkManager`), ejecutando la sincronización recomendada (`syncAllRecommended`) y preservando copias de seguridad locales rotativas.
- **RF-05:** Visualización del número y código de versión de la aplicación en la parte inferior de la pantalla de Ajustes.
- **RF-06:** Interruptor (Switch) bajo la hora programada (visible únicamente si la sincronización programada está activada) para activar o desactivar el envío de una notificación con el resultado de la sincronización.
- **RF-07:** Publicación de notificación local al finalizar la sincronización en segundo plano con el desglose de partidas sincronizadas o errores, requiriendo el permiso de notificaciones en Android 13+.
- **RF-08:** Cancelación previa garantizada: al modificar la hora de sincronización o conmutar el switch se cancela de forma inmediata y explícita la alarma y el trabajo previo en segundo plano, dejando únicamente la última hora configurada.
- **RF-09:** Unicidad de ejecución diaria: se garantiza que solo exista y se ejecute una única sincronización activa en segundo plano por día calendario, evitando alarmas obsoletas residuales o notificaciones duplicadas.

## Fuera de alcance

- Detección de eventos de apertura o cierre de la app Eden mediante servicios de accesibilidad en esta fase.
- Múltiples alarmas o programaciones horarias concurrentes en un mismo día (se programa 1 hora fija diaria recurrente).
- Sincronización sin conexión a Internet (requiere conexión activa para comunicar con Google Drive).

## Flujo de usuario

1. El usuario pulsa el icono de Ajustes en la barra superior de la pantalla principal y accede a `SettingsScreen`.
2. Observa la sección de Automatización con el switch "Sincronización programada diaria".
3. Al activar el switch:
   - Se muestra la opción con la hora programada (por defecto 04:00 AM).
   - Se muestra bajo la hora un interruptor (Switch) con temática morada para "Notificar resultado de la sincronización" (por defecto desactivado).
   - El usuario pulsa sobre la hora y se abre un diálogo para seleccionar hora y minuto.
   - Tras confirmar la hora, queda programada la sincronización diaria.
   - Si el usuario activa el switch de notificación en Android 13+, el sistema solicita el permiso `POST_NOTIFICATIONS`.
4. Si el usuario desactiva el switch principal, las opciones de hora y notificación se ocultan y se cancela la tarea en segundo plano.
5. Al pie de la pantalla, el usuario visualiza la sección de información con la versión instalada (ej. `v1.0.0`).
6. El usuario regresa a la pantalla principal mediante el botón de retroceso superior o el gesto "Atrás" de Android.

## Datos y reglas de negocio

- **Estado de automatización:** Booleano persistido en preferencias (`isScheduledSyncEnabled`, por defecto `false`).
- **Hora y minuto programados:** Enteros persistidos en preferencias (`scheduledSyncHour`, por defecto `4`; `scheduledSyncMinute`, por defecto `0`).
- **Estado de notificación de resultado:** Booleano persistido en preferencias (`isScheduledSyncNotificationEnabled`, por defecto `false`).
- **Cálculo de ejecución:** Al activar o modificar la hora, se calcula el retardo inicial hasta la próxima ocurrencia de esa hora exacta (si la hora de hoy ya pasó, se programa para el día siguiente). La tarea se repite periódicamente cada 24 horas.
- **Condición de red:** La tarea en segundo plano requiere conectividad de red (`NetworkType.CONNECTED`). Si no hay red en el minuto exacto, `WorkManager` la ejecuta en cuanto se restablezca la conexión.
- **Red de seguridad:** Toda sincronización en segundo plano debe respetar las copias de seguridad locales rotativas antes de sobrescribir cualquier partida local.
- **Emisión de notificación:** Al completarse la tarea en segundo plano, si `isScheduledSyncNotificationEnabled` está activo y se cuenta con permiso, se publica una notificación en la barra de estado con el resultado (ej. "Sincronización completada: X subidas, Y descargadas" o resumen de incidencias).
- **Información de versión:** Se obtiene directamente de la configuración del paquete (`versionName` y `versionCode`).

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | El selector de hora responde de inmediato; al confirmar se recalcula la alarma en segundo plano. |
| Sin datos / Primera vez | Por defecto la automatización está desactivada y la hora propuesta inicial es las 04:00 AM. |
| Entrada inválida | El selector de hora restringe la entrada a horas (0-23) y minutos (0-59) válidos según el formato del sistema. |
| Error o espera excesiva | Si la sincronización en segundo plano falla por error de red o autenticación, la tarea no corrompe partidas y reintenta según las políticas de WorkManager. |
| Sin conexión o conexión interrumpida | La tarea en segundo plano espera a que haya conexión a Internet para iniciar el proceso con Google Drive. |
| Cancelar o volver atrás | Si el usuario cancela el diálogo de selección de hora, se mantiene la hora previamente guardada sin alteraciones. |
| Permiso de notificaciones denegado | Si el usuario deniega el permiso en Android 13+, el checkbox no se activa o se desactiva informando al usuario sin bloquear la app. |
| Pasar a segundo plano y regresar | El estado del switch, hora y checkbox permanece intacto al volver a la pantalla de Ajustes. |
| Recrear la pantalla (rotación/foldable) | Se preserva la visualización del estado y del diálogo selector sin reiniciarse. |
| Reinicio del dispositivo (Reboot) | Las tareas de `WorkManager` persisten tras reiniciar el dispositivo móvil o la consola. |
| Formato horario del sistema | El selector de hora se adapta al formato regional del dispositivo (12 horas AM/PM o 24 horas). |

**Puntos de la guía no aplicables y motivo:** Entrada de texto y teclado virtual no aplican dado que la interacción se realiza mediante interruptor, checkbox y selector horario.

## Restricciones del pedido

- Interruptor (Switch) para activar/desactivar la automatización.
- Selector de hora y minuto visible solo si la automatización está activada.
- Casilla (Checkbox) bajo la opción de hora para enviar notificación con el resultado, visible solo si la automatización está activada.
- Número de versión de la aplicación situado bajo las opciones de automatización.

## Criterios de aceptación

- **CA-01 · RF-01:** Dado que el usuario está en Ajustes con la automatización desactivada, cuando activa el switch, entonces el estado pasa a activado, se persiste en las preferencias y se muestra la configuración horaria y la opción de notificación.
- **CA-02 · RF-01:** Dado que la automatización está activada, cuando el usuario desactiva el switch, entonces se oculta la opción horaria y la casilla de notificación, y se cancela la tarea en segundo plano en `WorkManager`.
- **CA-03 · RF-02:** Dado que la automatización está activa, cuando el usuario pulsa en la hora configurada, entonces se muestra el diálogo selector de hora y minuto con la hora actual preseleccionada.
- **CA-04 · RF-02:** Dado que el diálogo selector de hora está abierto, cuando el usuario selecciona una nueva hora/minuto y confirma, entonces la nueva hora se muestra en pantalla, se persiste en preferencias y se reprograma la tarea de segundo plano.
- **CA-05 · RF-04:** Dado que la automatización está activa y llega la hora configurada con conexión a Internet, cuando `WorkManager` ejecuta la tarea, entonces se sincronizan automáticamente las partidas pendientes aplicando copias de seguridad de seguridad ante descargas.
- **CA-06 · RF-05:** Dado que el usuario visualiza la pantalla de Ajustes, cuando observa el pie de página, entonces se muestra claramente la versión de la aplicación (ej. `v1.0.0 (1)`).
- **CA-07 · RF-06:** Dado que la automatización está activa, cuando el usuario conmuta el Checkbox de notificaciones, entonces el estado `isScheduledSyncNotificationEnabled` se actualiza y persiste en preferencias.
- **CA-08 · RF-07:** Dado que la opción de notificaciones está activa y finaliza la sincronización programada, cuando el Worker termina, entonces se emite una notificación con el resultado de partidas sincronizadas en el dispositivo.
- **CA-09 · RF-08:** Dado que el usuario cambia la hora de sincronización, la alarma anterior en `AlarmManager` y el trabajo previo en `WorkManager` son cancelados inmediatamente y de forma explícita, persistiendo y programando únicamente la última hora fijada.
- **CA-10 · RF-09:** Dado que una sincronización programada ya se ha ejecutado hoy o llega un disparo con una hora que no corresponde a la configurada, el sistema descarta la ejecución y no emite notificaciones repetidas ni concurrentes para ese mismo día.

## Cómo se comprueba el comportamiento

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Entrar a Ajustes y activar el switch de automatización. | El switch cambia a activo y aparece la fila con la hora programada y el checkbox de notificación. |
| CA-02 | Desactivar el switch de automatización. | La opción de hora y el checkbox se ocultan y la tarea en segundo plano queda cancelada. |
| CA-03 | Con el switch activo, pulsar en el selector de hora. | Se abre el TimePicker con la hora guardada. |
| CA-04 | Cambiar la hora en el TimePicker y aceptar. | La nueva hora se refleja en la tarjeta de Ajustes y persiste al salir y volver a entrar. |
| CA-05 | Ejecutar la tarea de WorkManager programada. | La tarea ejecuta `syncAllRecommended` con éxito en segundo plano. |
| CA-06 | Abrir la pantalla de Ajustes y revisar el pie. | Se visualiza el texto de la versión de la aplicación. |
| CA-07 | Conmutar el checkbox de notificación en Ajustes. | El checkbox cambia de estado y se guarda en `AppPreferences`. |
| CA-08 | Finalizar la sincronización en segundo plano con la opción activa. | Se publica la notificación local en el sistema con el resultado. |
| CA-04 | Cambiar la hora en el TimePicker y aceptar. | La nueva hora se refleja en la tarjeta de Ajustes y persiste al salir y volver a entrar. |
| CA-05 | Ejecutar la tarea de WorkManager programada. | La tarea ejecuta `syncAllRecommended` con éxito en segundo plano. |
| CA-06 | Abrir la pantalla de Ajustes y revisar el pie. | Se visualiza el texto de la versión de la aplicación. |

## Decisiones pendientes

- Ninguna.
