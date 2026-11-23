package com.jotasoft.edensavesync.data.storage.shizuku

import android.content.Context
import androidx.annotation.Keep
import java.io.File
import kotlin.system.exitProcess

/**
 * Servicio ejecutado dentro del proceso privilegiado de Shizuku (`shell`) para leer y escribir
 * `/storage/emulated/0/Android/data/dev.nxnative.android.peanuts/files/saves/`.
 */
@Keep
class PeanutsShizukuService : IPeanutsFileService.Stub {

    @Keep
    constructor() : super()

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : super()

    override fun destroy() {
        exitProcess(0)
    }

    override fun isDirectoryAccessible(savesDirPath: String): Boolean =
        PeanutsShizukuFileHelper.isDirectoryAccessible(File(savesDirPath))

    override fun scanSavesJson(savesDirPath: String): String =
        PeanutsShizukuFileHelper.toJsonArray(PeanutsShizukuFileHelper.scanSaves(File(savesDirPath)))

    override fun scanSingleSaveJson(savesDirPath: String, titleId: String): String? =
        PeanutsShizukuFileHelper.scanSingleSave(File(savesDirPath), titleId)
            ?.let(PeanutsShizukuFileHelper::toJsonObject)
            ?.toString()

    override fun exportTitleSaveToZip(savesDirPath: String, titleId: String, targetZipPath: String): Boolean =
        PeanutsShizukuFileHelper.exportTitleSaveToZip(File(savesDirPath), titleId, File(targetZipPath))

    override fun importTitleSaveFromZip(savesDirPath: String, titleId: String, sourceZipPath: String): Boolean =
        PeanutsShizukuFileHelper.importTitleSaveFromZip(File(savesDirPath), titleId, File(sourceZipPath))
}
