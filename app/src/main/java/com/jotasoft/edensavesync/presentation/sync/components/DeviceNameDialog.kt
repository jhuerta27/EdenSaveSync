package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val phonePreset = stringResource(R.string.chip_device_phone)
    val handheldPreset = stringResource(R.string.chip_device_handheld)
    val tabletPreset = stringResource(R.string.chip_device_tablet)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_device_name_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_device_name_description),
                        fontSize = 12.sp
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(phonePreset, handheldPreset, tabletPreset).forEach { preset ->
                            AssistChip(
                                onClick = { name = preset },
                                modifier = Modifier.gamepadFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { name = preset }
                                ),
                                label = {
                                    Text(
                                        text = preset,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.field_device_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusable(shape = RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSave(name) },
                    modifier = Modifier.gamepadFocusable(
                        shape = CircleShape,
                        onClick = { onSave(name) }
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
