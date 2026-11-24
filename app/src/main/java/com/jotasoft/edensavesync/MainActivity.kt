package com.jotasoft.edensavesync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.jotasoft.edensavesync.core.navigation.AppNavigation
import com.jotasoft.edensavesync.data.device.GamepadInputHandler
import com.jotasoft.edensavesync.data.device.GamepadScrollController
import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.data.storage.shizuku.ShizukuPeanutsClient
import com.jotasoft.edensavesync.data.storage.shizuku.ShizukuStatus
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.presentation.sync.EdenSyncViewModel
import com.jotasoft.edensavesync.ui.theme.EdenSaveSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var storageManager: EdenStorageManager

    @Inject
    lateinit var shizukuPeanutsClient: ShizukuPeanutsClient

    @Inject
    lateinit var scheduledSyncScheduler: ScheduledSyncScheduler

    private val viewModel: EdenSyncViewModel by viewModels()
    private val gamepadInputHandler = GamepadInputHandler()
    private var cancelShizukuPermissionListener: (() -> Unit)? = null
    var onStartButtonHandler: (() -> Boolean)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::onOAuthRedirect)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
        scheduledSyncScheduler.enqueueAutoSyncFromEvent()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP &&
            gamepadInputHandler.isStartButton(event.keyCode) &&
            onStartButtonHandler?.invoke() == true
        ) {
            return true
        }
        return super.dispatchKeyEvent(gamepadInputHandler.translateKeyEvent(event))
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        GamepadScrollController.onRightStickMotion(gamepadInputHandler.getRightStickY(event))

        val dpadKeyCode = gamepadInputHandler.processJoystickMotion(event)
        if (dpadKeyCode != null) {
            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, dpadKeyCode))
            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, dpadKeyCode))
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.data?.let(viewModel::onOAuthRedirect)

        setContent {
            EdenSaveSyncTheme {
                AppNavigation(
                    viewModel = viewModel,
                    onOpenEdenFolderIntent = storageManager::createOpenEdenFolderIntent,
                    onOpenPeanutsFolderIntent = storageManager::createOpenPeanutsFolderIntent,
                    onConnectPeanutsWithShizuku = ::connectPeanutsWithShizuku,
                    onOpenBrowserAuthUrl = { authUrl ->
                        runOnUiThread {
                            startActivity(Intent(Intent.ACTION_VIEW, authUrl.toUri()))
                        }
                    },
                    onRegisterStartButtonHandler = { handler ->
                        onStartButtonHandler = handler
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        cancelShizukuPermissionListener?.invoke()
        cancelShizukuPermissionListener = null
        super.onDestroy()
    }

    private fun connectPeanutsWithShizuku() {
        cancelShizukuPermissionListener?.invoke()
        cancelShizukuPermissionListener = null
        when (shizukuPeanutsClient.checkStatus()) {
            ShizukuStatus.NOT_INSTALLED -> viewModel.showStatusError(R.string.msg_shizuku_not_installed)
            ShizukuStatus.NOT_RUNNING -> viewModel.showStatusError(R.string.msg_shizuku_not_running)
            ShizukuStatus.PERMISSION_REQUIRED -> {
                val vm = viewModel
                cancelShizukuPermissionListener = shizukuPeanutsClient.requestPermission { granted ->
                    if (!isDestroyed && !isFinishing) {
                        runOnUiThread {
                            if (granted) {
                                vm.onFolderSelected(
                                    LocalSaveSource.PEANUTS_NATIVE,
                                    ShizukuPeanutsClient.SHIZUKU_PEANUTS_URI.toUri()
                                )
                            } else {
                                vm.showStatusError(R.string.msg_shizuku_permission_denied)
                            }
                        }
                    }
                }
            }
            ShizukuStatus.READY -> {
                viewModel.onFolderSelected(
                    LocalSaveSource.PEANUTS_NATIVE,
                    ShizukuPeanutsClient.SHIZUKU_PEANUTS_URI.toUri()
                )
            }
        }
    }
}
