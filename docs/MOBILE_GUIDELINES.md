# Mobile Guidelines

When drafting a specification, review the following points and apply those relevant to the feature. 
If an expected behavior is undefined, clarify it rather than making assumptions.

* **Lifecycle:** Account for backgrounding, foregrounding, screen recreation/configuration changes, and process death. Define what happens to in-flight operations.
* **State Preservation:** Define what state must persist when navigating or relaunching the app (e.g., form inputs, selections, scroll positions). Distinguish between transient UI state and persistent data.
* **Connectivity:** Handle offline scenarios, slow networks, dropped connections during active requests, and network recovery. Differentiate cold starts/first-time use from cached data states.
* **Persistence & Consistency:** Define what data is stored locally and how it synchronizes with backend data. Address cache invalidation, deduplication, conflict resolution, and schema migrations/backwards compatibility with earlier versions.
* **UI States:** Explicitly cover loading, populated, empty, error, and success states. Provide clear visual feedback for user actions and include retry/recovery paths for recoverable errors.
* **Navigation & Interruptions:** Define back navigation, cancellation flows, unsaved changes handling, and context switching from external apps. Account for deep links and push notifications whenever they touch the flow.
* **Interactions & Forms:** Account for virtual keyboard behavior, input focus, inline validation, and action button visibility. Debounce or disable interactive elements to prevent duplicate submissions from rapid taps.
* **Screen & Visual Adaptation:** Support diverse screen sizes, supported orientations, system safe areas/insets, and dynamic font scaling. Ensure content is never clipped and interactive elements remain accessible.
* **Accessibility:** Ensure screen reader compatibility, proper accessibility labels, logical focus order, minimum color contrast, and adequate touch target sizes. Do not convey information solely through color or complex gestures.
* **Permissions & Hardware Capabilities:** Handle denied, revoked, or restricted permissions, as well as missing hardware capabilities. Request only essential permissions in context and define graceful fallbacks when access is withheld.
* **Performance & Resources:** Keep the UI thread responsive during network requests, disk I/O, and heavy computation. Optimize memory, battery consumption, data usage, image assets, and large list rendering.
* **Background Processing:** Where applicable, account for task deferral, throttling, or OS termination. Define idempotency and recovery logic to prevent duplicate side effects when tasks resume.
* **Privacy & Security:** Clarify what data is stored, transmitted, or displayed, ensuring data is encrypted and expirable when appropriate. Never leak PII or sensitive tokens into logs or user-facing error messages.
* **Localization & Formatting:** Where applicable, account for translated text expansion, RTL layouts, date/time formatting, time zones, currencies, and locale-specific units.
* **Mobile QA & Validation:** Include test cases for edge scenarios such as app interruptions, process death/relaunch, network transitions, permission changes, and multiple device form factors. Complement automated tests with manual verification on physical devices, emulators, or simulators depending on the scope.

These guidelines do not automatically expand project scope. Incorporate into `SPEC.md` only the requirements, technical decisions, and acceptance criteria derived from the applicable points and confirmed design decisions.