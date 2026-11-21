package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.ui.theme.AccentAmber
import com.jotasoft.edensavesync.ui.theme.AccentGreen
import com.jotasoft.edensavesync.ui.theme.CardBorder
import com.jotasoft.edensavesync.ui.theme.CardDark
import com.jotasoft.edensavesync.ui.theme.TextPrimary
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun SetupStatusCard(
    edenConnected: Boolean,
    activeProfileId: String,
    driveConnected: Boolean,
    onConfigureEdenClick: () -> Unit,
    onConfigureDriveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botón Carpeta de Eden
                Surface(
                    onClick = onConfigureEdenClick,
                    color = if (edenConnected) Color(0xFF0E2A25) else Color(0xFF271F16),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (edenConnected) AccentGreen.copy(alpha = 0.6f) else AccentAmber.copy(
                            alpha = 0.6f
                        )
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .gamepadFocusable(shape = RoundedCornerShape(12.dp), onClick = onConfigureEdenClick)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (edenConnected) AccentGreen else AccentAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.setup_eden_folder_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (edenConnected) stringResource(R.string.setup_eden_folder_linked) else stringResource(
                                    R.string.setup_eden_folder_unlinked
                                ),
                                fontSize = 11.sp,
                                color = if (edenConnected) AccentGreen else AccentAmber
                            )
                        }
                    }
                }

                // Botón Google Drive
                Surface(
                    onClick = onConfigureDriveClick,
                    color = if (driveConnected) Color(0xFF0E2A25) else Color(0xFF271F16),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (driveConnected) AccentGreen.copy(alpha = 0.6f) else AccentAmber.copy(
                            alpha = 0.6f
                        )
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .gamepadFocusable(shape = RoundedCornerShape(12.dp), onClick = onConfigureDriveClick)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (driveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (driveConnected) AccentGreen else AccentAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.setup_drive_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (driveConnected) stringResource(R.string.setup_drive_connected) else stringResource(
                                    R.string.setup_drive_disconnected
                                ),
                                fontSize = 11.sp,
                                color = if (driveConnected) AccentGreen else AccentAmber
                            )
                        }
                    }
                }
            }

            if (activeProfileId.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.setup_profile_detected,
                        activeProfileId.take(8),
                        activeProfileId.takeLast(6)
                    ),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
