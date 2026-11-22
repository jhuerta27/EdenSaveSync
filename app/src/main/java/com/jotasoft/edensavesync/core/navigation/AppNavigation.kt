package com.jotasoft.edensavesync.core.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotasoft.edensavesync.presentation.settings.SettingsScreen
import com.jotasoft.edensavesync.presentation.settings.SettingsViewModel
import com.jotasoft.edensavesync.presentation.sync.EdenSyncScreen
import com.jotasoft.edensavesync.presentation.sync.EdenSyncViewModel

@Composable
fun AppNavigation(
    viewModel: EdenSyncViewModel,
    onOpenEdenFolderIntent: (useEdenProvider: Boolean) -> Intent,
    onOpenBrowserAuthUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRegisterStartButtonHandler: ((() -> Boolean)?) -> Unit = {}
) {
    var currentScreen by rememberSaveable { mutableStateOf<Screen>(Screen.Sync) }

    DisposableEffect(currentScreen) {
        onRegisterStartButtonHandler {
            currentScreen = if (currentScreen == Screen.Sync) Screen.Settings else Screen.Sync
            true
        }
        onDispose {
            onRegisterStartButtonHandler(null)
        }
    }

    BackHandler(enabled = currentScreen != Screen.Sync) {
        currentScreen = Screen.Sync
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppNavigationTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            Screen.Sync -> {
                EdenSyncScreen(
                    viewModel = viewModel,
                    onOpenEdenFolderIntent = onOpenEdenFolderIntent,
                    onOpenBrowserAuthUrl = onOpenBrowserAuthUrl,
                    onOpenSettings = { currentScreen = Screen.Settings }
                )
            }
            Screen.Settings -> {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { currentScreen = Screen.Sync }
                )
            }
        }
    }
}
