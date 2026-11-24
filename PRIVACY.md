# Privacy Policy / Política de Privacidad — Eden Save Sync

*Effective date & Last updated / Fecha de vigencia y última actualización: November 24, 2026*  
*Web version: [https://jhuerta27.github.io/EdenSaveSync/privacy.html](https://jhuerta27.github.io/EdenSaveSync/privacy.html)*

---

## English

### 1. Overview and Scope
**Eden Save Sync** (Android package name `com.jotasoft.edensavesync`, developed by JotaSoft) is a free, open-source Android application that helps users back up and synchronize their own emulator savegame files between their personal Android devices using their own **Google Drive** account.

- **Zero Developer Servers:** Eden Save Sync communicates directly between your Android device and Google's official OAuth 2.0 and Google Drive APIs. The developer does not operate any backend servers, cloud databases, or proxy services, and never receives, stores, or views your Google account credentials, OAuth tokens, or savegame files.

### 2. Google User Data Accessed
Signing in with Google is optional and initiated only when you connect your Google Drive account. Eden Save Sync requests strictly a single, minimal, per-file Google Drive API scope:

- **`https://www.googleapis.com/auth/drive.file`** — Per-file access limited strictly to files and folders created or opened by Eden Save Sync itself.

**What Eden Save Sync accesses in your Google Drive:**
- A dedicated folder named `EdenSaveSync` in your personal Google Drive.
- Compressed `.zip` backups of your savegame files that you choose to synchronize.
- Minimal synchronization metadata (game title ID, game display name, SHA-256 content hash, save modification timestamp, and user-defined device label).

**What Eden Save Sync does NOT access or collect:**
- It does **not** request `email`, `profile`, `openid`, or broad Google Drive scopes (`drive`, `drive.readonly`).
- It **cannot** view, list, read, modify, or delete any other files, photos, documents, or folders in your Google Drive.
- It does **not** collect personal identifiers, contacts, location data, advertising IDs, crash reports, or analytics.

### 3. Local Device Permissions and On-Device Data
- **Storage Access Framework (`OPEN_DOCUMENT_TREE`) / optional Shizuku:** Used strictly on-device to read and write savegame files in the user-selected emulator directory (Eden / Peanuts Native) and keep up to 5 local `.zip` safety backups.
- **Network & Background Sync Permissions:** Used solely to check connectivity, run optional scheduled/automatic background sync, and display optional sync notifications.

### 4. How We Use Google User Data
Google user data is used exclusively to provide user-requested savegame synchronization:
- Comparing SHA-256 hashes and save timestamps between your local device and Google Drive.
- Uploading newer save backups to your personal `EdenSaveSync` folder in Google Drive.
- Downloading newer save backups from your personal `EdenSaveSync` folder to your other Android devices.
- Refreshing your OAuth 2.0 access token locally on your device so optional background sync can operate without repeated manual logins.

Google user data is **never** used for advertising, marketing, profiling, data brokering, or any purpose unrelated to savegame synchronization.

### 5. Google API Services User Data Policy & Limited Use Compliance
Eden Save Sync's use and transfer to any other app of information received from Google APIs adheres strictly to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the **Limited Use** requirements.

- **No AI/ML Model Training:** Google Workspace and Google Drive API data is **never** used to develop, improve, or train generalized Artificial Intelligence (AI) or Machine Learning (ML) models.
- **No Human Access:** Neither the developer nor any third party has human access to your Google user data or save files.

### 6. Data Sharing, Transfer, and Disclosure
- **No third-party sharing:** Eden Save Sync does not share, transfer, sell, rent, or disclose Google user data or local data to the developer or any third party.
- **No ads or analytics:** The app contains no ads, no third-party tracking SDKs, and no telemetry.
- **Open-source transparency:** Full source code is publicly available at [https://github.com/jhuerta27/EdenSaveSync](https://github.com/jhuerta27/EdenSaveSync).

### 7. Data Storage and Security Mechanisms
- **Encryption in transit:** All network requests to Google OAuth 2.0 and Google Drive APIs use encrypted HTTPS (TLS).
- **Private on-device storage:** OAuth 2.0 tokens (`access_token` and `refresh_token`) and app settings are stored locally on your Android device in private application storage (`SharedPreferences` with `Context.MODE_PRIVATE`).
- **User-owned cloud storage:** Cloud backups reside exclusively in your own Google Drive account.

### 8. Data Retention, Deletion, and Revoking Access
- **Retention:** The developer retains zero user data. Local tokens remain on your device until you disconnect your account or uninstall the app. Cloud backups remain in your personal Google Drive until you delete them.
- **How to delete & revoke access:**
  1. Inside the app: tap **Google Drive → Disconnect Google Drive account** to delete all stored OAuth tokens from your device.
  2. Uninstall the app to remove all local app data and local safety backups.
  3. Delete the `EdenSaveSync` folder in [Google Drive](https://drive.google.com) to erase all cloud backups.
  4. Revoke the app's access from your [Google Account Third-Party Connections](https://myaccount.google.com/permissions) page.

### 9. Children's Privacy
Eden Save Sync does not knowingly collect personal information from children under 13 (or the applicable age of digital consent) and does not collect personal information from users of any age.

### 10. Changes to This Privacy Policy
Updates to this Privacy Policy will be published on this page and on the web version with an updated "Last updated" date.

### 11. Contact
- **Email:** [jotasoft.support@gmail.com](mailto:jotasoft.support@gmail.com)
- **GitHub Issues:** [https://github.com/jhuerta27/EdenSaveSync/issues](https://github.com/jhuerta27/EdenSaveSync/issues)

---

## Español (Resumen)

**Eden Save Sync** (`com.jotasoft.edensavesync`) es una aplicación Android gratuita y de código abierto desarrollada por JotaSoft que sincroniza las partidas guardadas del emulador entre tus propios dispositivos usando tu propia cuenta de **Google Drive**.

- **Permiso mínimo:** Solo solicita el alcance **`https://www.googleapis.com/auth/drive.file`** para leer y escribir únicamente los archivos creados por la propia aplicación dentro de la carpeta `EdenSaveSync`. No puede ver, leer, modificar ni borrar ningún otro archivo de tu Google Drive ni accede a tu correo o perfil.
- **Sin servidores propios ni terceros:** Toda la comunicación se realiza directamente entre tu dispositivo Android y Google Drive mediante HTTPS. No hay servidores propios, publicidad, analíticas ni compartición de datos con terceros.
- **Cumplimiento de Uso Limitado e IA/ML:** El uso de los datos recibidos de las APIs de Google cumple estrictamente la [Política de datos de usuario de los servicios API de Google](https://developers.google.com/terms/api-services-user-data-policy) (incluidos los requisitos de Uso Limitado) y **nunca** se utilizan para entrenar o mejorar modelos de IA o aprendizaje automático.
- **Almacenamiento local, retención y revocación:** Los tokens OAuth se guardan únicamente en el almacenamiento privado local (`SharedPreferences` con `MODE_PRIVATE`) de tu dispositivo. Puedes desconectar tu cuenta en cualquier momento desde la app (**Google Drive → Desconectar**), desinstalar la app, borrar la carpeta `EdenSaveSync` en Google Drive o revocar el permiso desde los [Permisos de tu cuenta de Google](https://myaccount.google.com/permissions).
- **Contacto:** [jotasoft.support@gmail.com](mailto:jotasoft.support@gmail.com) · [GitHub Issues](https://github.com/jhuerta27/EdenSaveSync/issues)
