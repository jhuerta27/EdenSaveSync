package com.jotasoft.edensavesync.data.storage.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY
}

@Singleton
class ShizukuPeanutsClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SHIZUKU_PEANUTS_URI = "shizuku://dev.nxnative.android.peanuts/files/saves"
        const val PEANUTS_SAVES_PATH = "/storage/emulated/0/Android/data/dev.nxnative.android.peanuts/files/saves"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 202612
        private const val SERVICE_BIND_TIMEOUT_MS = 10_000L
        private const val MIN_VALID_ZIP_BYTES = 22L

        fun isShizukuUri(uri: Uri?): Boolean =
            uri?.scheme.equals("shizuku", ignoreCase = true)
    }

    private val bindMutex = Mutex()
    @Volatile
    private var cachedService: IPeanutsFileService? = null
    @Volatile
    private var cachedConnection: ServiceConnection? = null
    @Volatile
    private var cachedArgs: Shizuku.UserServiceArgs? = null
    @Volatile
    private var activePermissionListener: Shizuku.OnRequestPermissionResultListener? = null

    fun checkStatus(): ShizukuStatus {
        val installed = runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        }.isSuccess
        if (!installed) return ShizukuStatus.NOT_INSTALLED
        if (!Shizuku.pingBinder()) return ShizukuStatus.NOT_RUNNING
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuStatus.READY
        } else {
            ShizukuStatus.PERMISSION_REQUIRED
        }
    }

    /**
     * Solicita permiso de Shizuku y devuelve una función para cancelar/desregistrar el listener
     * (p. ej. en `Activity.onDestroy`) evitando retener referencias fuertes a la Activity.
     */
    fun requestPermission(onResult: (Boolean) -> Unit): () -> Unit {
        clearPermissionListener()
        if (!Shizuku.pingBinder()) {
            onResult(false)
            return {}
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return {}
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                    clearPermissionListener()
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        activePermissionListener = listener
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        return ::clearPermissionListener
    }

    fun clearPermissionListener() {
        activePermissionListener?.let { listener ->
            runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
            activePermissionListener = null
        }
    }

    suspend fun scanLocalSaves(): List<LocalSaveGame> = withContext(Dispatchers.IO) {
        val raw = withService { service -> service.scanSavesJson(PEANUTS_SAVES_PATH) }
        PeanutsShizukuFileHelper.fromJsonArray(raw)
    }

    suspend fun scanSingleSave(titleId: String): LocalSaveGame? = withContext(Dispatchers.IO) {
        val raw = withService { service -> service.scanSingleSaveJson(PEANUTS_SAVES_PATH, titleId) }
            ?: return@withContext null
        PeanutsShizukuFileHelper.fromJsonObject(JSONObject(raw))
    }

    suspend fun exportSaveToZip(titleId: String, destZipFile: File): Boolean = withContext(Dispatchers.IO) {
        val transferZip = createTransferZipFile("export_${titleId}_${System.currentTimeMillis()}.zip")
        try {
            val exported = withService { service ->
                service.exportTitleSaveToZip(PEANUTS_SAVES_PATH, titleId, transferZip.absolutePath)
            }
            if (!exported || !transferZip.exists() || transferZip.length() <= MIN_VALID_ZIP_BYTES) {
                return@withContext false
            }
            destZipFile.parentFile?.mkdirs()
            transferZip.copyTo(destZipFile, overwrite = true)
            true
        } finally {
            if (transferZip.exists()) transferZip.delete()
        }
    }

    suspend fun importSaveFromZip(
        titleId: String,
        srcZipFile: File,
        onBackupExported: (File) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!srcZipFile.exists() || srcZipFile.length() <= MIN_VALID_ZIP_BYTES) return@withContext false

        // Crear respaldo previo si existía una partida en Peanuts Native
        val backupTransferZip = createTransferZipFile("backup_${titleId}_${System.currentTimeMillis()}.zip")
        try {
            val hadExisting = runCatching {
                withService { service ->
                    service.exportTitleSaveToZip(PEANUTS_SAVES_PATH, titleId, backupTransferZip.absolutePath)
                }
            }.getOrDefault(false)
            if (hadExisting && backupTransferZip.exists() && backupTransferZip.length() > MIN_VALID_ZIP_BYTES) {
                onBackupExported(backupTransferZip)
            }
        } finally {
            if (backupTransferZip.exists()) backupTransferZip.delete()
        }

        val importTransferZip = createTransferZipFile("import_${titleId}_${System.currentTimeMillis()}.zip")
        try {
            srcZipFile.copyTo(importTransferZip, overwrite = true)
            withService { service ->
                service.importTitleSaveFromZip(PEANUTS_SAVES_PATH, titleId, importTransferZip.absolutePath)
            }
        } finally {
            if (importTransferZip.exists()) importTransferZip.delete()
        }
    }

    // Zona de intercambio accesible tanto por nuestra app como por el proceso shell de Shizuku
    private fun createTransferZipFile(fileName: String): File {
        val baseDir = context.externalCacheDir
            ?: throw IllegalStateException(context.getString(R.string.msg_shizuku_external_cache_unavailable))
        val transferDir = File(baseDir, "shizuku_transfer").apply { mkdirs() }
        return File(transferDir, fileName)
    }

    private suspend fun <T> withService(block: (IPeanutsFileService) -> T): T =
        try {
            block(getOrBindService())
        } catch (e: RemoteException) {
            if (e is DeadObjectException || cachedService != null) {
                invalidateCachedService()
                block(getOrBindService())
            } else {
                throw e
            }
        }

    private suspend fun invalidateCachedService() = bindMutex.withLock {
        val oldConn = cachedConnection
        val oldArgs = cachedArgs
        cachedService = null
        cachedConnection = null
        cachedArgs = null
        if (oldConn != null && oldArgs != null) {
            runCatching { Shizuku.unbindUserService(oldArgs, oldConn, true) }
        }
    }

    private suspend fun getOrBindService(): IPeanutsFileService = bindMutex.withLock {
        val existing = cachedService
        if (existing != null && existing.asBinder().isBinderAlive) {
            return@withLock existing
        }

        when (checkStatus()) {
            ShizukuStatus.NOT_INSTALLED ->
                throw IllegalStateException(context.getString(R.string.msg_shizuku_not_installed))
            ShizukuStatus.NOT_RUNNING ->
                throw IllegalStateException(context.getString(R.string.msg_shizuku_not_running))
            ShizukuStatus.PERMISSION_REQUIRED ->
                throw IllegalStateException(context.getString(R.string.msg_shizuku_permission_required))
            ShizukuStatus.READY -> Unit
        }

        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, PeanutsShizukuService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("peanuts_shizuku")
            .debuggable(false)
            .version(1)

        withTimeout(SERVICE_BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        if (!cont.isActive) {
                            runCatching { Shizuku.unbindUserService(args, this, true) }
                            return
                        }
                        if (binder != null && binder.pingBinder()) {
                            val iface = IPeanutsFileService.Stub.asInterface(binder)
                            cachedService = iface
                            cachedConnection = this
                            cachedArgs = args
                            cont.resume(iface)
                        } else {
                            runCatching { Shizuku.unbindUserService(args, this, true) }
                            cont.resumeWithException(
                                IllegalStateException(context.getString(R.string.msg_shizuku_bind_failed))
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        if (cachedConnection === this) {
                            cachedService = null
                            cachedConnection = null
                            cachedArgs = null
                        }
                    }
                }
                cont.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                }
                Shizuku.bindUserService(args, connection)
            }
        }
    }
}
