package com.jotasoft.edensavesync.ui.theme

import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Determines whether the gamepad focus indicator should be visible.
 * Returns true only when the element has focus and the input mode is [InputMode.Keyboard]
 * (which Android/Compose assigns to controllers, D-pads, and physical keyboards).
 */
fun shouldShowGamepadFocus(isFocused: Boolean, inputMode: InputMode): Boolean =
    isFocused && inputMode == InputMode.Keyboard

/**
 * Applies a visual focus border indicator when the component is focused and
 * the user is navigating via gamepad or external controller ([InputMode.Keyboard]).
 * When the user interacts via touchscreen ([InputMode.Touch]), no focus border is drawn.
 *
 * Adapts to the exact shape of each button (pill, circle, or rounded corners),
 * ensuring the focus border hugs the component's geometry seamlessly.
 *
 * Does NOT add a redundant focusable, ensuring the underlying Button or clickable
 * node receives the focus directly and triggers its action on Button A / D-pad Center.
 * Optionally accepts [onClick] to guarantee immediate execution on Button A / D-pad Center.
 */
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
