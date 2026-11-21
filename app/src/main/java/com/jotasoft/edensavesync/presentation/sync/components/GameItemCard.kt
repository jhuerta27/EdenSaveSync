package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.AccentGreen
import com.jotasoft.edensavesync.ui.theme.AccentPurple
import com.jotasoft.edensavesync.ui.theme.BgDark
import com.jotasoft.edensavesync.ui.theme.CardBorder
import com.jotasoft.edensavesync.ui.theme.CardDark
import com.jotasoft.edensavesync.ui.theme.TextPrimary
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun GameItemCard(
    item: GameSyncItem,
    currentDeviceName: String,
    isBusy: Boolean,
    driveConnected: Boolean,
    onUploadClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (item.recommendation) {
        SyncRecommendation.UPLOAD_TO_CLOUD -> AccentCyan
        SyncRecommendation.DOWNLOAD_FROM_CLOUD -> AccentPurple
        SyncRecommendation.IN_SYNC -> AccentGreen
    }
    val badgeLabel = when (item.recommendation) {
        SyncRecommendation.UPLOAD_TO_CLOUD -> stringResource(R.string.badge_upload_recommended)
        SyncRecommendation.DOWNLOAD_FROM_CLOUD -> stringResource(R.string.badge_download_recommended)
        SyncRecommendation.IN_SYNC -> stringResource(R.string.badge_in_sync)
    }

    val displayTitle =
        if (item.displayName.startsWith("Switch Game (") || item.displayName.startsWith("Juego de Switch (")) {
            stringResource(R.string.switch_game_fallback, item.titleId)
        } else {
            item.displayName
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onRenameClick() }
                                .gamepadFocusable(shape = CircleShape, onClick = onRenameClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.game_card_rename_description),
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.game_card_title_id, item.titleId),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        1.dp,
                        badgeColor.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Comparativa lado a lado: Dispositivo Local vs Google Drive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Columna Local
                Surface(
                    color = BgDark.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(
                                R.string.game_card_local_header,
                                currentDeviceName
                            ),
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (item.localSave != null) {
                            Text(
                                text = formatDateTime(item.localSave.lastModifiedMs),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(
                                    R.string.game_card_stats,
                                    formatBytes(item.localSave.totalSizeBytes),
                                    item.localSave.fileCount
                                ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.game_card_no_local),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Columna Google Drive
                Surface(
                    color = BgDark.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.game_card_cloud_header),
                            color = AccentPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (item.remoteSave != null) {
                            Text(
                                text = formatDateTime(item.remoteSave.lastModifiedMs),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(
                                    R.string.game_card_remote_source,
                                    item.remoteSave.sourceDeviceName
                                ),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.game_card_no_remote),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones manuales individuales (Subir, Descargar, Restaurar Backup)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.localSave != null && driveConnected) {
                        FilledTonalButton(
                            onClick = onUploadClick,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .gamepadFocusable(
                                    shape = CircleShape,
                                    enabled = !isBusy,
                                    onClick = onUploadClick
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.game_card_action_upload), fontSize = 12.sp)
                        }
                    }

                    if (item.remoteSave != null && driveConnected) {
                        FilledTonalButton(
                            onClick = onDownloadClick,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .gamepadFocusable(
                                    shape = CircleShape,
                                    enabled = !isBusy,
                                    onClick = onDownloadClick
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.game_card_action_download), fontSize = 12.sp)
                        }
                    }

                    if (item.localBackupsCount > 0) {
                        OutlinedButton(
                            onClick = onRestoreBackupClick,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .gamepadFocusable(
                                    shape = CircleShape,
                                    enabled = !isBusy,
                                    onClick = onRestoreBackupClick
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.game_card_action_restore_backup,
                                    item.localBackupsCount
                                ),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
