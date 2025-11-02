package com.kaoage.sdk.bestshot

import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.model.BestShotReason
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BestShotEngineTest {

    private val detection = DetectionResult(
        detectionId = "det-001",
        timestampMillis = 100L,
        boundingBox = BoundingBox(0f, 0f, 100f, 150f, 1080, 1920),
        eulerAngles = EulerAngles(0f, 0f, 0f),
        landmarks = LandmarkSet(
            mapOf(
                LandmarkType.LEFT_EYE to NormalizedPoint(0.3f, 0.3f, 0.9f),
                LandmarkType.RIGHT_EYE to NormalizedPoint(0.6f, 0.3f, 0.9f),
                LandmarkType.NOSE_TIP to NormalizedPoint(0.45f, 0.45f, 0.9f),
                LandmarkType.MOUTH_LEFT to NormalizedPoint(0.35f, 0.65f, 0.9f),
                LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.65f, 0.65f, 0.9f)
            )
        ),
        confidence = 0.92f
    )

    @Test
    fun `best shot eligible when confidence exceeds threshold`() {
        val engine = BestShotEngine()
        val signal = engine.evaluate(detection, DetectionSessionConfig())
        assertTrue(signal?.qualityScore ?: 0f >= 0.8f)
        assertTrue(signal?.reasons?.contains(BestShotReason.CONFIDENCE_PEAK) == true)
    }

    @Test
    fun `best shot suppressed during cooldown`() {
        val engine = BestShotEngine()
        val config = DetectionSessionConfig()
        val first = engine.evaluate(detection, config)
        val second = engine.evaluate(detection.copy(timestampMillis = detection.timestampMillis + 100), config)
        assertFalse(second?.eligible ?: true)
        assertEquals(config.cooldownMillis, first?.cooldownMillis)
    }
}
