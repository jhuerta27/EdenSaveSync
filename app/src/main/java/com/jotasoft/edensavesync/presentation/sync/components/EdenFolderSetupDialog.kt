package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.AccentPurple
import com.jotasoft.edensavesync.ui.theme.DangerRed
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun EdenFolderSetupDialog(
    source: LocalSaveSource,
    isLinked: Boolean,
    onDismiss: () -> Unit,
    onPickDirectAndroidData: () -> Unit,
    onPickDocumentsProvider: () -> Unit,
    onUnlink: () -> Unit
) {
    val isPeanuts = source == LocalSaveSource.PEANUTS_NATIVE
    val titleRes = if (isPeanuts) R.string.dialog_peanuts_folder_title else R.string.dialog_folder_title
    val introRes = if (isPeanuts) R.string.dialog_peanuts_folder_intro else R.string.dialog_folder_intro
    val optionATitleRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_a_title
    } else {
        R.string.dialog_folder_option_a_title
    }
    val optionAStepsRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_a_steps
    } else {
        R.string.dialog_folder_option_a_steps
    }
    val optionAButtonRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_a_button
    } else {
        R.string.dialog_folder_option_a_button
    }
    val optionBTitleRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_b_title
    } else {
        R.string.dialog_folder_option_b_title
    }
    val optionBDescRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_b_desc
    } else {
        R.string.dialog_folder_option_b_desc
    }
    val optionBButtonRes = if (isPeanuts) {
        R.string.dialog_peanuts_folder_option_b_button
    } else {
        R.string.dialog_folder_option_b_button
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(titleRes),
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
                        text = stringResource(introRes),
                        fontSize = 13.sp
                    )
                    HorizontalDivider()
                    Text(
                        text = stringResource(optionATitleRes),
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(optionAStepsRes),
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onPickDocumentsProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusable(
                                shape = CircleShape,
                                onClick = onPickDocumentsProvider
                            )
                    ) {
                        Text(stringResource(optionAButtonRes))
                    }

                    HorizontalDivider()
                    Text(
                        text = stringResource(optionBTitleRes),
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(optionBDescRes),
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
                        Text(stringResource(optionBButtonRes))
                    }

                    if (isLinked) {
                        HorizontalDivider()
                        OutlinedButton(
                            onClick = onUnlink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusable(
                                    shape = CircleShape,
                                    onClick = onUnlink
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_folder_unlink_button),
                                color = DangerRed
                            )
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
