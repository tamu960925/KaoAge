package com.kaoage.sdk.core.config

import kotlin.test.Test
import kotlin.test.assertEquals

class DetectionConfigUpdateTest {

    @Test
    fun `builder applies runtime overrides`() {
        val config = DetectionConfigBuilder()
            .minFaceConfidence(0.8f)
            .minAgeConfidence(0.7f)
            .cooldownMillis(1000L)
            .build()

        assertEquals(0.8f, config.minFaceConfidence)
        assertEquals(0.7f, config.minAgeConfidence)
        assertEquals(1000L, config.cooldownMillis)
    }
}
