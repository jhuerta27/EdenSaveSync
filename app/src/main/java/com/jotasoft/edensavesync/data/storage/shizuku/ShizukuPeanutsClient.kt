package com.jotasoft.edensavesync.data.storage.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
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

        fun isShizukuUri(uri: Uri?): Boolean =
            uri?.scheme.equals("shizuku", ignoreCase = true)
    }

    private val bindMutex = Mutex()
    @Volatile
    private var cachedService: IPeanutsFileService? = null

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

    fun requestPermission(onResult: (Boolean) -> Unit) {
        if (!Shizuku.pingBinder()) {
            onResult(false)
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        lateinit var listener: Shizuku.OnRequestPermissionResultListener
        listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                Shizuku.removeRequestPermissionResultListener(listener)
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
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
            if (!exported || !transferZip.exists() || transferZip.length() <= 22L) {
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
        if (!srcZipFile.exists() || srcZipFile.length() <= 22L) return@withContext false

        // Crear respaldo previo si existía una partida en Peanuts Native
        val backupTransferZip = createTransferZipFile("backup_${titleId}_${System.currentTimeMillis()}.zip")
        try {
            val hadExisting = runCatching {
                withService { service ->
                    service.exportTitleSaveToZip(PEANUTS_SAVES_PATH, titleId, backupTransferZip.absolutePath)
                }
            }.getOrDefault(false)
            if (hadExisting && backupTransferZip.exists() && backupTransferZip.length() > 100L) {
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
        val baseDir = context.externalCacheDir ?: context.cacheDir
        val transferDir = File(baseDir, "shizuku_transfer").apply { mkdirs() }
        return File(transferDir, fileName)
    }

    private suspend fun <T> withService(block: (IPeanutsFileService) -> T): T {
        val service = getOrBindService()
        return block(service)
    }

    private suspend fun getOrBindService(): IPeanutsFileService = bindMutex.withLock {
        val existing = cachedService
        if (existing != null && existing.asBinder().isBinderAlive) {
            return@withLock existing
        }

        when (checkStatus()) {
            ShizukuStatus.NOT_INSTALLED ->
                throw IllegalStateException("Shizuku no está instalado. Instálalo e inícialo para sincronizar Peanuts Native.")
            ShizukuStatus.NOT_RUNNING ->
                throw IllegalStateException("Shizuku no está activo. Abre Shizuku e inícialo para sincronizar Peanuts Native.")
            ShizukuStatus.PERMISSION_REQUIRED ->
                throw IllegalStateException("Falta autorizar el permiso de Shizuku para Peanuts Native.")
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
                        if (binder != null && binder.pingBinder()) {
                            val iface = IPeanutsFileService.Stub.asInterface(binder)
                            cachedService = iface
                            if (cont.isActive) cont.resume(iface)
                        } else if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException("No se pudo conectar al servicio de Shizuku.")
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        cachedService = null
                    }
                }
                Shizuku.bindUserService(args, connection)
            }
        }
    }
}
