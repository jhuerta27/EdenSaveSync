# SPEC: Soporte para Gamepad e Indicador Visual de Foco

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

Proporcionamos soporte completo de navegación e interacción mediante gamepad / controlador externo (mandos Bluetooth, mandos USB y controles físicos integrados de consolas Android como AYN Odin 2 / Odin 2 Portal) para los usuarios de EdenSaveSync. La aplicación mostrará un indicador visual de foco claramente distinguible sobre cada elemento pulsable (botones, tarjetas interactivas, iconos, switches y diálogos) únicamente cuando se navegue mediante el mando o teclado físico, preservando la apariencia visual limpia original sin foco cuando el usuario interactúe mediante la pantalla táctil.

## Situación actual

Actualmente, EdenSaveSync está optimizada prioritariamente para interacción táctil. Aunque Jetpack Compose soporta navegación de foco nativa básica, muchos elementos interactivos (iconos, tarjetas personalizadas, diálogos y botones de acción rápida) carecen de un indicador de foco visual destacado adaptado al tema oscuro de la aplicación o no tienen una jerarquía de navegación direccional (D-pad) optimizada. Además, al tocar la pantalla táctil, no debe quedar ningún indicador de foco residual.

## Dentro del alcance

- **RF-01:** Indicador visual de foco condicional: mostrar un borde/resaltado distintivo (acorde a la paleta de diseño de la app, p. ej. Cyan/Purple) sobre el elemento interactivo con foco únicamente en modo de entrada de mando/teclado (`InputMode.Keyboard`), ocultándose por completo en modo táctil (`InputMode.Touch`).
- **RF-02:** Cobertura de foco en todos los elementos interactivos de la pantalla principal (`EdenSyncScreen`): botón de ajustes, botón/fila de edición del nombre de dispositivo, tarjetas de estado SAF y Google Drive, botón de sincronización rápida ("Sincronizar todo") y acciones por juego en las tarjetas de la lista (subir, descargar, renombrar, historial de backups).
- **RF-03:** Cobertura de foco en la pantalla de Ajustes (`SettingsScreen`): botón de regreso, interruptor (Switch) de sincronización programada, selector de hora y conmutador de notificaciones.
- **RF-04:** Cobertura de foco en diálogos modales (configuración de Drive, selección de carpeta de Eden, historial de backups, renombrado de juego, diálogo de nombre de dispositivo y selector de hora), asegurando que el foco quede atrapado de forma natural dentro del diálogo activo y permita seleccionar y confirmar acciones con el gamepad.
- **RF-05:** Navegación bidireccional coherente (D-pad / joystick izquierdo): permitir desplazamiento fluido vertical y horizontal entre elementos interactivos sin perder el foco en elementos no enfocables o quedar bloqueado.
- **RF-06:** Activación mediante gamepad: soporte para pulsar/confirmar la acción enfocada con el botón A / D-pad Center / Enter del controlador, y regresar o cerrar diálogos con el botón B / Back.
- **RF-07:** Apertura de Ajustes con botón START (`KEYCODE_BUTTON_START`/`KEYCODE_MENU`): al presionar el botón START desde la pantalla principal se abre la vista de Ajustes; al presionarlo en Ajustes, se regresa a la pantalla principal.
- **RF-08:** Desplazamiento vertical mediante Analog R (stick derecho): el stick analógico derecho (`AXIS_RZ`/`AXIS_RY`) permite realizar scroll vertical continuo en las listas de la app, con velocidad proporcional a la inclinación del stick.
- **RF-09:** Ejecución directa y unificada de la acción enfocada con Botón A: el botón A ejecuta inmediatamente la acción del control que tiene el foco sin interferencias ni desvíos causados por capas de foco duplicadas.

## Fuera de alcance

- Reasignación personalizada de botones de mando por parte del usuario (mapeo customizado de teclas).
- Sonidos hápticos o efectos de audio específicos de consola al mover el foco.
- Soporte para puntero de ratón virtual / simulación de cursor táctil.

## Flujo de usuario

1. El usuario conecta un mando gamepad (o utiliza los controles integrados de su consola portátil Android).
2. Al pulsar cualquier dirección en el D-pad o stick analógico, el sistema conmuta a modo de navegación por mando y el primer elemento interactivo disponible muestra un indicador visual de foco destacado (borde iluminado/resaltado).
3. El usuario se desplaza entre los botones y tarjetas con el D-pad: el foco se traslada visiblemente al elemento activo contiguo.
4. Al presionar el botón de acción principal (Botón A / Select / D-pad Center), se activa la acción correspondiente (p. ej., sincronizar, abrir ajustes, abrir historial de copias).
5. Si se abre un diálogo, el foco pasa automáticamente al primer elemento interactivo del diálogo. El usuario puede interactuar dentro de él o cerrarlo con el botón de retroceso (Botón B / Back).
6. Si en cualquier momento el usuario toca la pantalla directamente, la interfaz conmuta inmediatamente a modo táctil y los bordes o resaltados de foco desaparecen por completo.

## Datos y reglas de negocio

- Los elementos interactivos deshabilitados (p. ej. botones en estado de carga o no disponibles) no deben poder recibir foco de navegación o deben reflejar claramente su estado inactivo si lo reciben.
- El estilo visual del foco debe mantener alto contraste sobre el fondo oscuro AMOLED (`BgDark`) sin alterar el tamaño o provocar saltos de layout (desplazamientos de píxeles) al ganar o perder el foco.
- En listas perezosas (`LazyColumn`), al navegar hacia abajo o arriba con el gamepad, la lista debe hacer scroll automáticamente para mantener el elemento enfocado dentro del área visible.

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Si un botón se deshabilita durante la sincronización, el foco no realiza acciones duplicadas y se indica visualmente o pasa al siguiente elemento si procede. |
| Sin datos | En la pantalla vacía (`EmptySavesPlaceholder`), los botones disponibles (p. ej. configurar carpeta o Drive) reciben el foco de forma accesible. |
| Entrada inválida | En diálogos con campos de texto (ej. renombrar juego o código de emparejamiento), el campo de texto y los botones Aceptar/Cancelar navegan en orden lógico. |
| Error o espera excesiva | Los diálogos o banners de error muestran opciones enfocables para reintentar o descartar. |
| Sin conexión o conexión interrumpida | Los elementos de UI se mantienen navegables normalmente. |
| Cancelar o volver atrás | El botón B o botón Back del gamepad cierra el diálogo actual o regresa a la pantalla anterior si está en Ajustes. |
| Pasar a segundo plano y regresar | Se conserva el estado de navegación y, si se regresa mediante control externo, el foco sigue respondiendo. |
| Recrear la pantalla | Al rotar la pantalla o cambiar tamaño (plegables), el foco se mantiene en un elemento visible coherente. |
| Reabrir después de terminarse el proceso | La app arranca en su estado inicial; el foco aparece únicamente cuando el usuario presiona una tecla/botón del gamepad. |
| Otros puntos aplicables de la guía | Detección limpia entre modo táctil y modo gamepad gracias a `LocalInputModeManager` para que nunca se dibuje el marco de foco si se interactúa con el dedo. |

**Puntos de la guía no aplicables y motivo:** No aplica gestión de permisos especiales ni hardware de vibración, ya que los gamepads estándar son reconocidos nativamente por el sistema de entrada de Android.

## Restricciones del pedido

- Se debe trabajar sobre una nueva rama llamada `main-support-focus`.
- Debe aparecer un foco visible sobre cada botón o elemento pulsable dentro de la app cuando se use un controlador externo.
- Si el dispositivo usa el control táctil, la aplicación debe aparecer exactamente tal y como está (sin foco visible).

## Criterios de aceptación

- **CA-01 · RF-01:** Dado un dispositivo con gamepad conectado, cuando el usuario presiona una dirección del D-pad, entonces el elemento interactivo activo muestra un borde/resaltado de foco distintivo, y cuando el usuario toca la pantalla táctil, dicho foco visual desaparece por completo.
- **CA-02 · RF-02:** Dado el usuario navegando en `EdenSyncScreen`, cuando se desplaza con el D-pad, entonces puede recorrer y enfocar ordenadamente todos los elementos pulsables: barra superior (ajustes, nombre dispositivo), tarjetas de estado (carpeta Eden, Google Drive), botón "Sincronizar todo" y tarjetas de juegos individuales junto con sus botones de acción (subir, descargar, renombrar, historial).
- **CA-03 · RF-03:** Dado el usuario en `SettingsScreen`, cuando navega con el D-pad, entonces puede enfocar y conmutar el botón de retroceso, el switch de sincronización programada, la fila selectora de hora y el switch de notificaciones.
- **CA-04 · RF-04:** Dado cualquier diálogo modal abierto (configuración Drive, selector de carpeta Eden, renombrar, historial de backups, confirmaciones), cuando el usuario navega con el D-pad, entonces el foco se desplaza entre los elementos interactivos del diálogo sin escaparse al fondo de la pantalla.
- **CA-05 · RF-05, RF-06, RF-09:** Dado cualquier elemento pulsable con el foco activo, cuando el usuario presiona el botón A (o D-pad Center / Enter), entonces se dispara la misma acción que al realizar un tap táctil sobre dicho elemento de forma inmediata y confiable.
- **CA-06 · RF-07:** Dado el usuario en `EdenSyncScreen`, cuando presiona el botón START del gamepad, entonces se abre la pantalla de Ajustes (`SettingsScreen`), y al presionarlo en `SettingsScreen` regresa a `EdenSyncScreen`.
- **CA-07 · RF-08:** Dado el usuario visualizando una lista en `EdenSyncScreen` o `SettingsScreen`, cuando mueve el stick derecho (Analog R) hacia arriba o hacia abajo, entonces la lista se desplaza verticalmente en la dirección indicada de manera fluida y continua.

## Cómo se comprueba el comportamiento

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Simular o pulsar eventos de teclas direccionales (D-pad/teclado) y posteriormente eventos táctiles. | El borde/indicador de foco se muestra solo en modo teclado/gamepad y se oculta inmediatamente al interactuar de forma táctil. |
| CA-02 | Recorrer la pantalla principal con eventos D-pad hacia abajo y arriba. | Todos los botones, tarjetas interactivas y botones secundarios de cada juego reciben el foco visible y quedan en el viewport. |
| CA-03 | Navegar por la pantalla de Ajustes con D-pad y accionar el botón A. | Cada control recibe foco y reacciona a la pulsación de botón A/Enter. |
| CA-04 | Abrir diálogos modales (Drive, carpeta, backup) y pulsar direcciones del D-pad. | El foco se mueve exclusivamente entre los campos y botones del diálogo. |
| CA-05 | Pulsar botón A/Enter sobre botones de acción enfocados (ej. sincronizar todo, subir, descargar). | La acción correspondiente se ejecuta idénticamente a un toque en pantalla. |
| CA-06 | Presionar botón START desde la pantalla principal y desde Ajustes. | Conmuta entre la pantalla principal y la pantalla de Ajustes. |
| CA-07 | Inclinar el stick derecho hacia arriba o abajo en pantallas con scroll. | La lista se desplaza verticalmente en la dirección correspondiente de forma continua. |

## Decisiones pendientes

- Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->