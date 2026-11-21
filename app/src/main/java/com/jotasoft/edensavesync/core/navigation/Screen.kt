package com.jotasoft.edensavesync.core.navigation

import java.io.Serializable

sealed interface Screen : Serializable {
    data object Sync : Screen
    data object Settings : Screen
}
