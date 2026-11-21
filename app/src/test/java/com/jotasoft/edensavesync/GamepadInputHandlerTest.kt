package com.jotasoft.edensavesync

import android.view.KeyEvent
import androidx.compose.ui.input.InputMode
import com.jotasoft.edensavesync.data.device.GamepadInputHandler
import com.jotasoft.edensavesync.data.device.GamepadScrollController
import com.jotasoft.edensavesync.ui.theme.shouldShowGamepadFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadInputHandlerTest {

    private val handler = GamepadInputHandler()

    @Test
    fun translateKeyCode_buttonA_mapsToDpadCenter() {
        val result = handler.translateKeyCode(KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, result)
    }

    @Test
    fun translateKeyCode_buttonB_mapsToBack() {
        val result = handler.translateKeyCode(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(KeyEvent.KEYCODE_BACK, result)
    }

    @Test
    fun translateKeyCode_otherKeys_remainUnchanged() {
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, handler.translateKeyCode(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, handler.translateKeyCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, handler.translateKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, handler.translateKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(KeyEvent.KEYCODE_ENTER, handler.translateKeyCode(KeyEvent.KEYCODE_ENTER))
    }

    @Test
    fun isStartButton_identifiesStartAndMenuKeys() {
        assertTrue(handler.isStartButton(KeyEvent.KEYCODE_BUTTON_START))
        assertTrue(handler.isStartButton(KeyEvent.KEYCODE_MENU))
        assertFalse(handler.isStartButton(KeyEvent.KEYCODE_BUTTON_A))
        assertFalse(handler.isStartButton(KeyEvent.KEYCODE_BUTTON_B))
        assertFalse(handler.isStartButton(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun calculateScrollDelta_naturalDirection() {
        // Downward tilt produces positive delta (natural scroll down)
        val downDelta = GamepadScrollController.calculateScrollDelta(tiltY = 1.0f, baseSpeed = 28f)
        assertEquals(28f, downDelta, 0.001f)

        // Upward tilt produces negative delta (natural scroll up)
        val upDelta = GamepadScrollController.calculateScrollDelta(tiltY = -1.0f, baseSpeed = 28f)
        assertEquals(-28f, upDelta, 0.001f)
    }

    @Test
    fun shouldShowGamepadFocus_onlyWhenFocusedAndKeyboardMode() {
        assertTrue(shouldShowGamepadFocus(isFocused = true, inputMode = InputMode.Keyboard))
        assertFalse(shouldShowGamepadFocus(isFocused = true, inputMode = InputMode.Touch))
        assertFalse(shouldShowGamepadFocus(isFocused = false, inputMode = InputMode.Keyboard))
        assertFalse(shouldShowGamepadFocus(isFocused = false, inputMode = InputMode.Touch))
    }
}
