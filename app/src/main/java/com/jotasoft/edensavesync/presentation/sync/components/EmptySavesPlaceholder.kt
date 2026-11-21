package com.jotasoft.edensavesync.presentation.sync.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.CardBorder
import com.jotasoft.edensavesync.ui.theme.CardDark
import com.jotasoft.edensavesync.ui.theme.TextPrimary
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun EmptySavesPlaceholder(
    hasFolder: Boolean,
    onOpenFolderSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hasFolder) {
                    stringResource(R.string.empty_saves_title_has_folder)
                } else {
                    stringResource(R.string.empty_saves_title_no_folder)
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasFolder) {
                    stringResource(R.string.empty_saves_desc_has_folder)
                } else {
                    stringResource(R.string.empty_saves_desc_no_folder)
                },
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenFolderSetup,
                modifier = Modifier.gamepadFocusable(shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Text(stringResource(R.string.empty_saves_button))
            }
        }
    }
}
