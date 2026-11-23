package com.jotasoft.edensavesync.presentation.sync

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.presentation.sync.components.DeviceNameDialog
import com.jotasoft.edensavesync.presentation.sync.components.DriveConfigDialog
import com.jotasoft.edensavesync.presentation.sync.components.EdenFolderSetupDialog
import com.jotasoft.edensavesync.presentation.sync.components.EmptySavesPlaceholder
import com.jotasoft.edensavesync.presentation.sync.components.GameItemCard
import com.jotasoft.edensavesync.presentation.sync.components.OneTouchSyncHeroCard
import com.jotasoft.edensavesync.presentation.sync.components.RenameGameDialog
import com.jotasoft.edensavesync.presentation.sync.components.SetupStatusCard
import com.jotasoft.edensavesync.presentation.sync.components.SyncSummaryBanner
import com.jotasoft.edensavesync.ui.theme.AccentCyan
import com.jotasoft.edensavesync.ui.theme.BgDark
import com.jotasoft.edensavesync.ui.theme.BindGamepadScroll
import com.jotasoft.edensavesync.ui.theme.TextPrimary
import com.jotasoft.edensavesync.ui.theme.TextSecondary
import com.jotasoft.edensavesync.ui.theme.gamepadFocusable

@Composable
fun EdenSyncScreen(
    viewModel: EdenSyncViewModel,
    onOpenEdenFolderIntent: (useEdenProvider: Boolean) -> Intent,
    onOpenPeanutsFolderIntent: () -> Intent,
    onConnectPeanutsWithShizuku: () -> Unit,
    onOpenBrowserAuthUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val edenFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val treeUri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModel.onFolderSelected(LocalSaveSource.EDEN, treeUri)
    }

    val peanutsFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val treeUri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModel.onFolderSelected(LocalSaveSource.PEANUTS_NATIVE, treeUri)
    }

    EdenSyncContent(
        uiState = uiState,
        onPickFolderStandard = { source ->
            when (source) {
                LocalSaveSource.EDEN -> edenFolderPickerLauncher.launch(onOpenEdenFolderIntent(false))
                LocalSaveSource.PEANUTS_NATIVE -> peanutsFolderPickerLauncher.launch(onOpenPeanutsFolderIntent())
            }
        },
        onPickFolderProvider = { source ->
            when (source) {
                LocalSaveSource.EDEN -> edenFolderPickerLauncher.launch(onOpenEdenFolderIntent(true))
                LocalSaveSource.PEANUTS_NATIVE -> onConnectPeanutsWithShizuku()
            }
        },
        onUnlinkFolder = viewModel::onUnlinkFolder,
        onRefresh = viewModel::refreshAll,
        onOpenSettings = onOpenSettings,
        onSyncAll = viewModel::syncAllRecommended,
        onUploadSingle = viewModel::uploadSingleGame,
        onDownloadSingle = viewModel::downloadSingleGame,
        onRestoreBackup = viewModel::restoreLatestBackup,
        onRenameGame = viewModel::renameGame,
        onUpdateDeviceName = viewModel::updateDeviceName,
        onStartBrowserOAuth = { viewModel.startBrowserOAuth(onOpenBrowserAuthUrl) },
        onDisconnectDrive = viewModel::disconnectGoogleDrive,
        onDismissBanner = viewModel::dismissBanner,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdenSyncContent(
    uiState: EdenSyncUiState,
    onPickFolderStandard: (LocalSaveSource) -> Unit,
    onPickFolderProvider: (LocalSaveSource) -> Unit,
    onUnlinkFolder: (LocalSaveSource) -> Unit,
    onRefresh: () -> Unit,
    onSyncAll: () -> Unit,
    onUploadSingle: (GameSyncItem) -> Unit,
    onDownloadSingle: (GameSyncItem) -> Unit,
    onRestoreBackup: (GameSyncItem) -> Unit,
    onRenameGame: (String, String) -> Unit,
    onUpdateDeviceName: (String) -> Unit,
    onStartBrowserOAuth: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    var activeFolderDialogSource by remember { mutableStateOf<LocalSaveSource?>(null) }
    var showDriveConfigDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var gameToRename by remember { mutableStateOf<GameSyncItem?>(null) }
    val lazyListState = rememberLazyListState()

    BindGamepadScroll(lazyListState)

    val density = LocalDensity.current
    val refreshThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }

    val nestedScrollConnection = remember(onRefresh, uiState.isScanning, uiState.isSyncing, refreshThresholdPx) {
        object : NestedScrollConnection {
            private var pullDistance = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullDistance > 0f) {
                    pullDistance = (pullDistance + available.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput &&
                    available.y > 0f &&
                    lazyListState.firstVisibleItemIndex == 0 &&
                    lazyListState.firstVisibleItemScrollOffset == 0
                ) {
                    pullDistance += available.y
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= refreshThresholdPx && !uiState.isScanning && !uiState.isSyncing) {
                    onRefresh()
                }
                pullDistance = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    val uploadPendingCount = uiState.games.count { it.recommendation == SyncRecommendation.UPLOAD_TO_CLOUD }
    val downloadPendingCount = uiState.games.count { it.recommendation == SyncRecommendation.DOWNLOAD_FROM_CLOUD }

    val statusBannerText = uiState.statusBannerMessage?.asString()
    val syncProgressText = uiState.syncProgressMessage?.asString()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showDeviceNameDialog = true }
                                    .gamepadFocusable(
                                        shape = RoundedCornerShape(6.dp),
                                        onClick = { showDeviceNameDialog = true }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = uiState.deviceName,
                                    fontSize = 12.sp,
                                    color = AccentCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.device_name_change_description),
                                    tint = AccentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.gamepadFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
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
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SyncSummaryBanner(
                    message = statusBannerText,
                    isError = uiState.isStatusError,
                    onDismiss = onDismissBanner
                )
            }

            item {
                SetupStatusCard(
                    edenConnected = uiState.edenFolderUri != null,
                    peanutsConnected = uiState.peanutsFolderUri != null,
                    peanutsViaShizuku = uiState.isPeanutsViaShizuku,
                    activeProfileId = uiState.activeProfileId,
                    driveConnected = uiState.isGoogleDriveConnected,
                    onConfigureEdenClick = { activeFolderDialogSource = LocalSaveSource.EDEN },
                    onConfigurePeanutsClick = { activeFolderDialogSource = LocalSaveSource.PEANUTS_NATIVE },
                    onConfigureDriveClick = { showDriveConfigDialog = true }
                )
            }

            item {
                OneTouchSyncHeroCard(
                    isReady = uiState.hasAnyLocalFolder && uiState.isGoogleDriveConnected,
                    isSyncing = uiState.isSyncing || uiState.isScanning,
                    progressMessage = syncProgressText,
                    uploadCount = uploadPendingCount,
                    downloadCount = downloadPendingCount,
                    totalGames = uiState.games.size,
                    onSyncAllClick = onSyncAll
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_detected_saves, uiState.games.size),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (uiState.isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.status_scanning),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (uiState.games.isEmpty() && !uiState.isScanning) {
                item {
                    EmptySavesPlaceholder(
                        hasFolder = uiState.hasAnyLocalFolder,
                        onOpenEdenSetup = { activeFolderDialogSource = LocalSaveSource.EDEN },
                        onOpenPeanutsSetup = { activeFolderDialogSource = LocalSaveSource.PEANUTS_NATIVE }
                    )
                }
            } else {
                items(uiState.games, key = { it.titleId }) { item ->
                    GameItemCard(
                        item = item,
                        currentDeviceName = uiState.deviceName,
                        isBusy = uiState.isSyncing,
                        driveConnected = uiState.isGoogleDriveConnected,
                        onUploadClick = { onUploadSingle(item) },
                        onDownloadClick = { onDownloadSingle(item) },
                        onRestoreBackupClick = { onRestoreBackup(item) },
                        onRenameClick = { gameToRename = item }
                    )
                }
            }
        }
    }

    activeFolderDialogSource?.let { source ->
        val isLinked = when (source) {
            LocalSaveSource.EDEN -> uiState.edenFolderUri != null
            LocalSaveSource.PEANUTS_NATIVE -> uiState.peanutsFolderUri != null
        }
        EdenFolderSetupDialog(
            source = source,
            isLinked = isLinked,
            onDismiss = { activeFolderDialogSource = null },
            onPickDirectAndroidData = {
                activeFolderDialogSource = null
                onPickFolderStandard(source)
            },
            onPickDocumentsProvider = {
                activeFolderDialogSource = null
                onPickFolderProvider(source)
            },
            onUnlink = {
                activeFolderDialogSource = null
                onUnlinkFolder(source)
            }
        )
    }

    if (showDriveConfigDialog) {
        DriveConfigDialog(
            isConnected = uiState.isGoogleDriveConnected,
            onDismiss = { showDriveConfigDialog = false },
            onStartBrowserOAuth = {
                showDriveConfigDialog = false
                onStartBrowserOAuth()
            },
            onDisconnect = {
                showDriveConfigDialog = false
                onDisconnectDrive()
            }
        )
    }

    if (showDeviceNameDialog) {
        DeviceNameDialog(
            currentName = uiState.deviceName,
            onDismiss = { showDeviceNameDialog = false },
            onSave = { newName ->
                onUpdateDeviceName(newName)
                showDeviceNameDialog = false
            }
        )
    }

    gameToRename?.let { target ->
        RenameGameDialog(
            item = target,
            onDismiss = { gameToRename = null },
            onSave = { newName ->
                onRenameGame(target.titleId, newName)
                gameToRename = null
            }
        )
    }
}
