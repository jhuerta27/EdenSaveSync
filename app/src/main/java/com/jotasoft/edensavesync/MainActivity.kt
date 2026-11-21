package com.jotasoft.edensavesync

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jotasoft.edensavesync.core.navigation.AppNavigation
import com.jotasoft.edensavesync.data.device.GamepadInputHandler
import com.jotasoft.edensavesync.data.device.GamepadScrollController
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.presentation.sync.EdenSyncViewModel
import com.jotasoft.edensavesync.ui.theme.EdenSaveSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var storageManager: EdenStorageManager

    private val viewModel: EdenSyncViewModel by viewModels()
    private val gamepadInputHandler = GamepadInputHandler()
    var onStartButtonHandler: (() -> Boolean)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { viewModel.onOAuthRedirect(it) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && gamepadInputHandler.isStartButton(event.keyCode)) {
            if (onStartButtonHandler?.invoke() == true) {
                return true
            }
        }
        return super.dispatchKeyEvent(gamepadInputHandler.translateKeyEvent(event))
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val rightStickY = gamepadInputHandler.getRightStickY(event)
        GamepadScrollController.onRightStickMotion(rightStickY)

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
        intent?.data?.let { viewModel.onOAuthRedirect(it) }

        setContent {
            EdenSaveSyncTheme {
                AppNavigation(
                    viewModel = viewModel,
                    onOpenEdenFolderIntent = { useEdenProvider ->
                        storageManager.createOpenEdenFolderIntent(useEdenProvider = useEdenProvider)
                    },
                    onOpenBrowserAuthUrl = { authUrl ->
                        runOnUiThread {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                            startActivity(browserIntent)
                        }
                    },
                    onRegisterStartButtonHandler = { handler ->
                        onStartButtonHandler = handler
                    }
                )
            }
        }
    }
}
