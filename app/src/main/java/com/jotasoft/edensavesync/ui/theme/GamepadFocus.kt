package com.jotasoft.edensavesync.ui.theme

import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jotasoft.edensavesync.data.device.GamepadScrollController

fun shouldShowGamepadFocus(isFocused: Boolean, inputMode: InputMode): Boolean =
    isFocused && inputMode == InputMode.Keyboard

@Composable
fun BindGamepadScroll(scrollableState: ScrollableState) {
    DisposableEffect(scrollableState) {
        val closeable = GamepadScrollController.registerConsumer { delta ->
            scrollableState.dispatchRawDelta(delta)
        }
        onDispose { closeable.close() }
    }
}

fun Modifier.dpadVerticalNavigation(
    requester: FocusRequester,
    up: FocusRequester? = null,
    down: FocusRequester? = null
): Modifier = this
    .focusRequester(requester)
    .focusProperties {
        if (up != null) {
            this.up = up
            this.previous = up
        }
        if (down != null) {
            this.down = down
            this.next = down
        }
    }
    .onKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        if (native.action == KeyEvent.ACTION_DOWN) {
            when (native.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> up?.let {
                    it.requestFocus()
                    true
                } ?: false
                KeyEvent.KEYCODE_DPAD_DOWN -> down?.let {
                    it.requestFocus()
                    true
                } ?: false
                else -> false
            }
        } else {
            false
        }
    }

@Composable
fun Modifier.gamepadFocusable(
    shape: Shape = CircleShape,
    borderColor: Color = AccentCyan,
    borderWidth: Dp = 2.dp,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    if (!enabled) return this
    val inputModeManager = LocalInputModeManager.current
    var isFocused by remember { mutableStateOf(false) }
    val showFocus = shouldShowGamepadFocus(isFocused, inputModeManager.inputMode)

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (onClick != null) {
                Modifier.onKeyEvent { keyEvent ->
                    val native = keyEvent.nativeKeyEvent
                    if ((native.keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                            native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                            native.keyCode == KeyEvent.KEYCODE_ENTER) &&
                        native.action == KeyEvent.ACTION_UP
                    ) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
            } else {
                Modifier
            }
        )
        .then(
            if (showFocus) {
                Modifier.border(borderWidth, borderColor, shape)
            } else {
                Modifier
            }
        )
}
