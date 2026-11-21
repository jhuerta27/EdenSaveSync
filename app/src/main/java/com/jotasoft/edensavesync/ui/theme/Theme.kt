package com.jotasoft.edensavesync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun EdenSaveSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EdenDarkColorScheme,
        typography = Typography,
        content = content
    )
}
