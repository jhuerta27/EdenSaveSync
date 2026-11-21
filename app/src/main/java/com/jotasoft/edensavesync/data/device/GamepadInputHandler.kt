package com.jotasoft.edensavesync.data.device

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Handles translation of gamepad input events (buttons and analog sticks)
 * into navigation events compatible with Android and Jetpack Compose.
 */
class GamepadInputHandler {

    private var lastJoystickEventTime = 0L
    private val debounceThresholdMs = 180L
    private val axisThreshold = 0.5f

    /**
     * Checks if the given key code corresponds to the START or MENU button on a gamepad.
     */
    fun isStartButton(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_MENU

    /**
     * Translates gamepad specific key codes (e.g., BUTTON_A, BUTTON_B) into
     * standard navigation key codes (DPAD_CENTER, BACK).
     */
    fun translateKeyCode(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
        KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
        else -> keyCode
    }

    /**
     * Creates a translated [KeyEvent] if [event]'s key code requires translation,
     * or returns the original event otherwise.
     */
    fun translateKeyEvent(event: KeyEvent): KeyEvent =
        translateKeyCode(event.keyCode).let { translatedCode ->
            if (translatedCode != event.keyCode) {
                KeyEvent(
                    event.downTime,
                    event.eventTime,
                    event.action,
                    translatedCode,
                    event.repeatCount,
                    event.metaState,
                    event.deviceId,
                    event.scanCode,
                    event.flags,
                    InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
                )
            } else {
                event
            }
        }

    /**
     * Determines whether an analog stick motion event should produce a directional DPAD key code.
     * Returns the corresponding [KeyEvent.KEYCODE_DPAD_*] if threshold is exceeded and debouncing allows,
     * or null otherwise.
     */
    fun processJoystickMotion(event: MotionEvent, currentTimeMs: Long = System.currentTimeMillis()): Int? {
        val isJoystickOrGamepad = (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) ||
            (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)

        if (!isJoystickOrGamepad) return null

        val x = event.getAxisValue(MotionEvent.AXIS_X).takeIf { it != 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y).takeIf { it != 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val directionCode = when {
            y < -axisThreshold -> KeyEvent.KEYCODE_DPAD_UP
            y > axisThreshold -> KeyEvent.KEYCODE_DPAD_DOWN
            x < -axisThreshold -> KeyEvent.KEYCODE_DPAD_LEFT
            x > axisThreshold -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> return null
        }

        if (currentTimeMs - lastJoystickEventTime < debounceThresholdMs) {
            return null
        }

        lastJoystickEventTime = currentTimeMs
        return directionCode
    }

    /**
     * Extracts vertical tilt from the right analog stick (Analog R).
     * Inspects [MotionEvent.AXIS_RZ] and fallback [MotionEvent.AXIS_RY].
     * Returns 0f if within the deadzone (<= 0.15f) or not a gamepad/joystick event.
     */
    fun getRightStickY(event: MotionEvent): Float {
        val isJoystickOrGamepad = (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) ||
            (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)

        if (!isJoystickOrGamepad) return 0f

        val rz = event.getAxisValue(MotionEvent.AXIS_RZ).takeIf { it != 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_RY)

        return if (Math.abs(rz) > 0.15f) rz else 0f
    }
}
