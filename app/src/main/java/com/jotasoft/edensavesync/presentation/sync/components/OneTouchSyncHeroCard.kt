package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.HeroActiveGradient
import com.jotasoft.edensavesync.ui.theme.HeroIdleGradient
import com.jotasoft.edensavesync.ui.theme.OnAccentCyan
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun OneTouchSyncHeroCard(
    isReady: Boolean,
    isSyncing: Boolean,
    progressMessage: String?,
    uploadCount: Int,
    downloadCount: Int,
    totalGames: Int,
    onSyncAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasChanges = uploadCount > 0 || downloadCount > 0
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (hasChanges) HeroActiveGradient else HeroIdleGradient
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .border(1.dp, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isSyncing -> stringResource(R.string.hero_title_syncing)
                            !isReady -> stringResource(R.string.hero_title_not_ready)
                            hasChanges -> stringResource(R.string.hero_title_changes_ready)
                            totalGames > 0 -> stringResource(R.string.hero_title_up_to_date)
                            else -> stringResource(R.string.hero_title_ready_scan)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = progressMessage ?: when {
                            !isReady -> stringResource(R.string.hero_subtitle_not_ready)
                            hasChanges -> stringResource(
                                R.string.hero_subtitle_changes,
                                uploadCount,
                                downloadCount
                            )
                            else -> stringResource(R.string.hero_subtitle_up_to_date)
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSyncAllClick,
                enabled = isReady && !isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = OnAccentCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .gamepadFocusable(
                        shape = RoundedCornerShape(12.dp),
                        borderColor = Color.White,
                        enabled = isReady && !isSyncing
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasChanges) {
                        stringResource(
                            R.string.hero_button_sync_changes,
                            uploadCount,
                            downloadCount
                        )
                    } else {
                        stringResource(R.string.hero_button_sync_all)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
