package com.kaoage.sdk.core

import com.kaoage.sdk.core.config.DetectionSessionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DetectionSessionConfigTest {

    @Test
    fun `default config matches expected thresholds`() {
        val config = DetectionSessionConfig()
        assertEquals(0.7f, config.minFaceConfidence)
        assertEquals(0.6f, config.minAgeConfidence)
        assertEquals(0.6f, config.minGenderConfidence)
        assertEquals(0.15f, config.minFaceSizeRatio)
        assertEquals(500L, config.maxFrameLatencyMillis)
        assertEquals(2500L, config.cooldownMillis)
    }

    @Test
    fun `invalid confidence throws`() {
        assertFailsWith<IllegalArgumentException> {
            DetectionSessionConfig(minFaceConfidence = 1.1f)
        }
    }
}
