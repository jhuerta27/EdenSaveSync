package com.jotasoft.edensavesync.data.device

import android.os.Handler
import android.os.Looper

/**
 * Coordinates vertical scrolling triggered by gamepad's Analog R (right stick).
 * Supports continuous, smooth frame-based scrolling while the stick is held tilted.
 */
object GamepadScrollController {

    private var activeConsumer: ((Float) -> Unit)? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var currentTiltY = 0f
    private var isTickerRunning = false

    private const val FRAME_INTERVAL_MS = 16L
    const val BASE_SCROLL_SPEED = 28f // pixels per frame at full tilt

    /**
     * Calculates the scroll delta for a given vertical tilt.
     * Natural scrolling: pushing the stick down causes the list to scroll downwards.
     */
    fun calculateScrollDelta(tiltY: Float, baseSpeed: Float = BASE_SCROLL_SPEED): Float =
        tiltY * baseSpeed

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (Math.abs(currentTiltY) > 0.15f) {
                val delta = calculateScrollDelta(currentTiltY)
                activeConsumer?.invoke(delta)
                mainHandler.postDelayed(this, FRAME_INTERVAL_MS)
            } else {
                isTickerRunning = false
            }
        }
    }

    fun registerConsumer(consumer: (Float) -> Unit): AutoCloseable {
        activeConsumer = consumer
        return AutoCloseable {
            if (activeConsumer === consumer) {
                activeConsumer = null
            }
        }
    }

    fun onRightStickMotion(tiltY: Float) {
        currentTiltY = tiltY
        if (Math.abs(tiltY) > 0.15f) {
            if (!isTickerRunning) {
                isTickerRunning = true
                mainHandler.post(tickerRunnable)
            }
        } else {
            isTickerRunning = false
            mainHandler.removeCallbacks(tickerRunnable)
        }
    }
}
