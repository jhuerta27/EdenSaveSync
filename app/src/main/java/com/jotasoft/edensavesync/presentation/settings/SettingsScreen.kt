package com.jotasoft.edensavesync.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.BackgroundSyncMode
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.BgDark
import com.jotasoft.edensavesync.ui.theme.BindGamepadScroll
import com.jotasoft.edensavesync.ui.theme.CardBorder
import com.jotasoft.edensavesync.ui.theme.CardDark
import com.jotasoft.edensavesync.ui.theme.EdenPurple
import com.jotasoft.edensavesync.ui.theme.SurfaceDark
import com.jotasoft.edensavesync.ui.theme.TextPrimary
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.TrackInactive
import com.jotasoft.edensavesync.ui.theme.dpadVerticalNavigation
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onToggleScheduledSync = viewModel::onToggleScheduledSync,
        onSelectSyncMode = viewModel::onSelectSyncMode,
        onSetScheduledTime = viewModel::onSetScheduledTime,
        onToggleNotification = viewModel::onToggleNotification,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onToggleScheduledSync: (Boolean) -> Unit,
    onSetScheduledTime: (hour: Int, minute: Int) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectSyncMode: (BackgroundSyncMode) -> Unit = {}
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()

    BindGamepadScroll(lazyListState)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onToggleNotification
    )

    val handleNotificationToggle: (Boolean) -> Unit = { enable ->
        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onToggleNotification(true)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onToggleNotification(enable)
        }
    }

    val backButtonFocusRequester = remember { FocusRequester() }
    val switchFocusRequester = remember { FocusRequester() }
    val autoModeFocusRequester = remember { FocusRequester() }
    val scheduledModeFocusRequester = remember { FocusRequester() }
    val timePickerFocusRequester = remember { FocusRequester() }
    val notifyFocusRequester = remember { FocusRequester() }

    val activeModeFocusRequester = if (uiState.syncMode == BackgroundSyncMode.AUTOMATIC) {
        autoModeFocusRequester
    } else {
        scheduledModeFocusRequester
    }
    val downFromModeSelector = if (uiState.syncMode == BackgroundSyncMode.SCHEDULED) {
        timePickerFocusRequester
    } else {
        notifyFocusRequester
    }
    val upFromNotifyRow = if (uiState.syncMode == BackgroundSyncMode.SCHEDULED) {
        timePickerFocusRequester
    } else {
        activeModeFocusRequester
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .dpadVerticalNavigation(
                                requester = backButtonFocusRequester,
                                down = switchFocusRequester
                            )
                            .gamepadFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = TextPrimary
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_automation_title),
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_scheduled_sync_title),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_scheduled_sync_desc),
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                Switch(
                                    checked = uiState.isScheduledSyncEnabled,
                                    onCheckedChange = onToggleScheduledSync,
                                    modifier = Modifier
                                        .dpadVerticalNavigation(
                                            requester = switchFocusRequester,
                                            up = backButtonFocusRequester,
                                            down = activeModeFocusRequester.takeIf { uiState.isScheduledSyncEnabled }
                                        )
                                        .gamepadFocusable(
                                            shape = CircleShape,
                                            borderColor = Color.White,
                                            onClick = { onToggleScheduledSync(!uiState.isScheduledSyncEnabled) }
                                        ),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = TrackInactive
                                    )
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.isScheduledSyncEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_sync_mode_label),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SyncModeOptionPill(
                                            selected = uiState.syncMode == BackgroundSyncMode.AUTOMATIC,
                                            icon = Icons.Default.CloudSync,
                                            label = stringResource(R.string.settings_sync_mode_auto),
                                            accentColor = AccentCyan,
                                            onClick = { onSelectSyncMode(BackgroundSyncMode.AUTOMATIC) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .dpadVerticalNavigation(
                                                    requester = autoModeFocusRequester,
                                                    up = switchFocusRequester,
                                                    down = downFromModeSelector
                                                )
                                                .focusProperties { right = scheduledModeFocusRequester }
                                                .onKeyEvent { keyEvent ->
                                                    val native = keyEvent.nativeKeyEvent
                                                    if (native.action == KeyEvent.ACTION_DOWN &&
                                                        native.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                                                    ) {
                                                        scheduledModeFocusRequester.requestFocus()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                        )

                                        SyncModeOptionPill(
                                            selected = uiState.syncMode == BackgroundSyncMode.SCHEDULED,
                                            icon = Icons.Default.Schedule,
                                            label = stringResource(R.string.settings_sync_mode_scheduled),
                                            accentColor = EdenPurple,
                                            onClick = { onSelectSyncMode(BackgroundSyncMode.SCHEDULED) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .dpadVerticalNavigation(
                                                    requester = scheduledModeFocusRequester,
                                                    up = switchFocusRequester,
                                                    down = downFromModeSelector
                                                )
                                                .focusProperties { left = autoModeFocusRequester }
                                                .onKeyEvent { keyEvent ->
                                                    val native = keyEvent.nativeKeyEvent
                                                    if (native.action == KeyEvent.ACTION_DOWN &&
                                                        native.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                                                    ) {
                                                        autoModeFocusRequester.requestFocus()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                        )
                                    }

                                    Text(
                                        text = stringResource(
                                            if (uiState.syncMode == BackgroundSyncMode.AUTOMATIC) {
                                                R.string.settings_sync_mode_auto_desc
                                            } else {
                                                R.string.settings_sync_mode_scheduled_desc
                                            }
                                        ),
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }

                                AnimatedVisibility(
                                    visible = uiState.syncMode == BackgroundSyncMode.SCHEDULED,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceDark)
                                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                            .dpadVerticalNavigation(
                                                requester = timePickerFocusRequester,
                                                up = scheduledModeFocusRequester,
                                                down = notifyFocusRequester
                                            )
                                            .clickable { showTimePicker = true }
                                            .gamepadFocusable(
                                                shape = RoundedCornerShape(12.dp),
                                                borderColor = EdenPurple,
                                                onClick = { showTimePicker = true }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = EdenPurple,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.settings_scheduled_time_title),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EdenPurple.copy(alpha = 0.15f))
                                                .border(
                                                    1.dp,
                                                    EdenPurple.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = uiState.formattedTime,
                                                color = EdenPurple,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .dpadVerticalNavigation(
                                            requester = notifyFocusRequester,
                                            up = upFromNotifyRow
                                        )
                                        .clickable { handleNotificationToggle(!uiState.isNotificationEnabled) }
                                        .gamepadFocusable(
                                            shape = RoundedCornerShape(12.dp),
                                            borderColor = EdenPurple,
                                            onClick = { handleNotificationToggle(!uiState.isNotificationEnabled) }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = EdenPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.settings_scheduled_sync_notify),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_scheduled_sync_notify_desc),
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = uiState.isNotificationEnabled,
                                        onCheckedChange = handleNotificationToggle,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EdenPurple,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = TrackInactive
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_about_title),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_version, uiState.appVersion),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.scheduledHour,
            initialMinute = uiState.scheduledMinute,
            is24Hour = true
        )

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    val handleSave = {
                        onSetScheduledTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                    TextButton(
                        onClick = handleSave,
                        modifier = Modifier.gamepadFocusable(shape = CircleShape, onClick = handleSave)
                    ) {
                        Text(text = stringResource(R.string.action_save), color = AccentCyan)
                    }
                },
                dismissButton = {
                    val handleCancel = { showTimePicker = false }
                    TextButton(
                        onClick = handleCancel,
                        modifier = Modifier.gamepadFocusable(shape = CircleShape, onClick = handleCancel)
                    ) {
                        Text(text = stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.settings_scheduled_time_dialog_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = SurfaceDark,
                                selectorColor = AccentCyan,
                                containerColor = CardDark,
                                periodSelectorBorderColor = CardBorder,
                                clockDialSelectedContentColor = Color.White,
                                clockDialContentColor = TextSecondary,
                                timeSelectorSelectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                timeSelectorContainerColor = SurfaceDark,
                                timeSelectorSelectedContentColor = AccentCyan,
                                timeSelectorContentColor = TextPrimary
                            )
                        )
                    }
                },
                containerColor = CardDark,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun SyncModeOptionPill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) accentColor.copy(alpha = 0.18f) else CardDark,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else CardBorder
        ),
        modifier = modifier
            .clip(shape)
            .gamepadFocusable(
                shape = shape,
                borderColor = accentColor,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
    }
}
