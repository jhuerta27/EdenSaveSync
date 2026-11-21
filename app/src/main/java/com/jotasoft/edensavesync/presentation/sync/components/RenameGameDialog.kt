package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun RenameGameDialog(
    item: GameSyncItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var customName by remember { mutableStateOf(item.displayName) }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_rename_title, item.titleId)) },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(stringResource(R.string.dialog_rename_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadFocusable(shape = RoundedCornerShape(8.dp))
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSave(customName) },
                    modifier = Modifier.gamepadFocusable(
                        shape = CircleShape,
                        onClick = { onSave(customName) }
                    )
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.gamepadFocusable(
                        shape = CircleShape,
                        onClick = onDismiss
                    )
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
