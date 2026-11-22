package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.ui.theme.DangerRed
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun DriveConfigDialog(
    isConnected: Boolean,
    onDismiss: () -> Unit,
    onStartBrowserOAuth: () -> Unit,
    onDisconnect: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.dialog_drive_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_drive_oauth_intro),
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onStartBrowserOAuth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusable(
                                shape = CircleShape,
                                onClick = onStartBrowserOAuth
                            )
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.dialog_drive_oauth_button),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isConnected) {
                        HorizontalDivider()
                        TextButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusable(shape = CircleShape, onClick = onDisconnect)
                        ) {
                            Text(
                                stringResource(R.string.dialog_drive_disconnect_button),
                                color = DangerRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.gamepadFocusable(shape = CircleShape, onClick = onDismiss)
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
