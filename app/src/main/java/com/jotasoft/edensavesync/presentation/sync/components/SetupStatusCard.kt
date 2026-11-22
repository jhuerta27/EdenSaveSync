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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.jotasoft.edensavesync.ui.theme.StatusConnectedBg
import com.jotasoft.edensavesync.ui.theme.StatusDisconnectedBg
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
                SetupStatusTile(
                    isConnected = edenConnected,
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.setup_eden_folder_title),
                    subtitle = stringResource(
                        if (edenConnected) R.string.setup_eden_folder_linked else R.string.setup_eden_folder_unlinked
                    ),
                    onClick = onConfigureEdenClick,
                    modifier = Modifier.weight(1f)
                )

                SetupStatusTile(
                    isConnected = driveConnected,
                    icon = if (driveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    title = stringResource(R.string.setup_drive_title),
                    subtitle = stringResource(
                        if (driveConnected) R.string.setup_drive_connected else R.string.setup_drive_disconnected
                    ),
                    onClick = onConfigureDriveClick,
                    modifier = Modifier.weight(1f)
                )
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

@Composable
private fun SetupStatusTile(
    isConnected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isConnected) AccentGreen else AccentAmber
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        color = if (isConnected) StatusConnectedBg else StatusDisconnectedBg,
        shape = shape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier
            .clip(shape)
            .gamepadFocusable(shape = shape, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accent
                )
            }
        }
    }
}
