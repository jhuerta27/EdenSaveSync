package com.jotasoft.edensavesync.data.device

import android.os.Build

object DeviceDetector {
    fun detectDefaultDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()

        if (model.isBlank() && manufacturer.isBlank()) return "Android Device"
        if (manufacturer.isBlank()) return model
        if (model.isBlank()) return manufacturer.replaceFirstChar { it.uppercase() }

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { it.uppercase() }
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }
}
