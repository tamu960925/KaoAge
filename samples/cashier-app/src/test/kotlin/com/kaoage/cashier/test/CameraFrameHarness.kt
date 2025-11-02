package com.kaoage.cashier.test

import com.kaoage.sdk.core.detection.FrameInput

/**
 * Lightweight harness used by instrumentation stubs to provide frame metadata without binding camera hardware.
 */
class CameraFrameHarness {
    fun createFrame(timestamp: Long = System.currentTimeMillis()): FrameInput =
        FrameInput(timestampMillis = timestamp)
}
