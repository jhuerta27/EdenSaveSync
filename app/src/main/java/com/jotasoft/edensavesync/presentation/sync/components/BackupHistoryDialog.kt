package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun BackupHistoryDialog(
    item: GameSyncItem,
    onDismiss: () -> Unit,
    onRestoreLatestBackup: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.dialog_backup_title, item.displayName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_backup_count, item.localBackupsCount),
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.dialog_backup_info),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val handleRestore = {
                            onRestoreLatestBackup()
                            onDismiss()
                        }
                        Button(
                            onClick = handleRestore,
                            modifier = Modifier.gamepadFocusable(
                                shape = CircleShape,
                                onClick = handleRestore
                            )
                        ) {
                            Text(stringResource(R.string.dialog_backup_restore_button))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.gamepadFocusable(
                        shape = CircleShape,
                        onClick = onDismiss
                    )
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
