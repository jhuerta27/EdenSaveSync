package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
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
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.AccentPurple
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun EdenFolderSetupDialog(
    onDismiss: () -> Unit,
    onPickDirectAndroidData: () -> Unit,
    onPickEdenDocumentsProvider: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.dialog_folder_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.dialog_folder_intro),
                        fontSize = 13.sp
                    )
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.dialog_folder_option_a_title),
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.dialog_folder_option_a_steps),
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onPickEdenDocumentsProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusable(
                                shape = CircleShape,
                                onClick = onPickEdenDocumentsProvider
                            )
                    ) {
                        Text(stringResource(R.string.dialog_folder_option_a_button))
                    }

                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.dialog_folder_option_b_title),
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.dialog_folder_option_b_desc),
                        fontSize = 12.sp
                    )
                    OutlinedButton(
                        onClick = onPickDirectAndroidData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusable(
                                shape = CircleShape,
                                onClick = onPickDirectAndroidData
                            )
                    ) {
                        Text(stringResource(R.string.dialog_folder_option_b_button))
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
