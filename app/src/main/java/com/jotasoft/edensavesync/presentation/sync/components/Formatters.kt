package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.jotasoft.edensavesync.R
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun formatDateTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return stringResource(R.string.time_unknown)
    val locale = LocalLocale.current.platformLocale
    val formatter = remember(locale) { SimpleDateFormat("dd MMM yyyy, HH:mm", locale) }
    return formatter.format(Date(timestampMs))
}

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return stringResource(R.string.bytes_b, bytes)
    val kb = bytes / 1024.0
    if (kb < 1024.0) return stringResource(R.string.bytes_kb, kb)
    return stringResource(R.string.bytes_mb, kb / 1024.0)
}
