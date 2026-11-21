# Privacy Policy / Política de Privacidad — Eden Save Sync

*Last updated / Última actualización: 2026*

## English

**Eden Save Sync** is an open-source Android application that synchronizes Nintendo Switch emulator (Eden) save files between your own devices using your own **Google Drive** account.

### 1. Data Accessed via Google APIs
- Eden Save Sync requests only the **`https://www.googleapis.com/auth/drive.file`** scope.
- This restricted, per-file scope only grants the app access to files and folders **created by Eden Save Sync itself** (specifically the `EdenSaveSync` folder containing your savegame `.zip` backups).
- The app **cannot** view, read, modify, or delete any other files in your Google Drive.

### 2. Data Collection & Sharing
- **No developer servers**: All communication occurs directly between your Android device and Google Drive.
- **No personal data collection**: Eden Save Sync does not collect, sell, transmit, or share your personal information, email address, save files, or analytics with the developer or any third party.

### 3. Local Storage & Revocation
- OAuth tokens (`access_token` and `refresh_token`) are stored locally in Android private `SharedPreferences` on your device so scheduled background sync can work.
- You can disconnect your account at any time inside the app (**Google Drive → Disconnect**), uninstall the app, or revoke access from your [Google Account Permissions](https://myaccount.google.com/permissions).

---

## Español

**Eden Save Sync** es una aplicación Android de código abierto que sincroniza las partidas guardadas del emulador **Eden** entre tus propios dispositivos usando tu propia cuenta de **Google Drive**.

### 1. Datos a los que accede mediante las APIs de Google
- Eden Save Sync solicita únicamente el permiso **`https://www.googleapis.com/auth/drive.file`**.
- Este permiso limita el acceso exclusivamente a los archivos y carpetas **creados por la propia aplicación** (la carpeta `EdenSaveSync` con los `.zip` de tus partidas).
- La aplicación **no puede** ver, leer, modificar ni borrar ningún otro archivo de tu Google Drive.

### 2. Recopilación y compartición de datos
- **Sin servidores propios**: Toda la comunicación se realiza directamente entre tu dispositivo Android y Google Drive.
- **Sin recopilación de datos personales**: Eden Save Sync no recopila, vende ni envía información personal, correos electrónicos, partidas ni analíticas al desarrollador ni a terceros.

### 3. Almacenamiento local y revocación
- Los tokens OAuth se guardan únicamente en el almacenamiento privado local (`SharedPreferences`) de tu dispositivo para permitir la sincronización automática.
- Puedes desconectar tu cuenta en cualquier momento desde la app (**Google Drive → Desconectar**), desinstalando la app o revocando el permiso desde los [Permisos de tu cuenta de Google](https://myaccount.google.com/permissions).
