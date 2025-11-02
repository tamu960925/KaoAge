package com.kaoage.sdk.core

import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FaceDetector
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.inference.AgeGenderEstimate
import com.kaoage.sdk.core.inference.AgeGenderEstimator
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.session.FaceInsightsSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FaceInsightsPipelineTest {

    private val detection = DetectionResult(
        detectionId = "det-1",
        timestampMillis = 100L,
        boundingBox = com.kaoage.sdk.core.model.BoundingBox(0f, 0f, 100f, 200f, 1080, 1920),
        eulerAngles = com.kaoage.sdk.core.model.EulerAngles(0f, 0f, 0f),
        landmarks = com.kaoage.sdk.core.model.LandmarkSet(
            mapOf(
                com.kaoage.sdk.core.model.LandmarkType.LEFT_EYE to com.kaoage.sdk.core.model.NormalizedPoint(0.3f, 0.3f, 0.8f),
                com.kaoage.sdk.core.model.LandmarkType.RIGHT_EYE to com.kaoage.sdk.core.model.NormalizedPoint(0.6f, 0.3f, 0.8f),
                com.kaoage.sdk.core.model.LandmarkType.NOSE_TIP to com.kaoage.sdk.core.model.NormalizedPoint(0.45f, 0.45f, 0.8f),
                com.kaoage.sdk.core.model.LandmarkType.MOUTH_LEFT to com.kaoage.sdk.core.model.NormalizedPoint(0.35f, 0.65f, 0.8f),
                com.kaoage.sdk.core.model.LandmarkType.MOUTH_RIGHT to com.kaoage.sdk.core.model.NormalizedPoint(0.65f, 0.65f, 0.8f)
            )
        ),
        confidence = 0.9f
    )

    private val estimatorResult = AgeGenderEstimate(
        ageBracket = AgeBracket.ADULT,
        ageConfidence = 0.85f,
        gender = GenderLabel.MALE,
        genderConfidence = 0.8f
    )

    @Test
    fun `session merges detection and inference into result`() {
        val session = FaceInsightsSession(
            detector = FakeDetector(detection),
            estimator = FakeEstimator(estimatorResult)
        )

        val result = runBlocking { session.analyze(FrameInput(timestampMillis = 100L)) }
        assertNotNull(result)
        assertEquals(estimatorResult.ageBracket, result.ageBracket)
        assertEquals(detection.boundingBox, result.boundingBox)
        assertEquals(estimatorResult.gender, result.gender)
    }

    private class FakeDetector(private val detection: DetectionResult) : FaceDetector {
        override suspend fun detect(frame: FrameInput, config: DetectionSessionConfig): List<DetectionResult> =
            listOf(detection)

        override suspend fun warmUp(config: DetectionSessionConfig) {
            // no-op
        }
    }

    private class FakeEstimator(private val estimate: AgeGenderEstimate) : AgeGenderEstimator {
        override suspend fun estimate(frame: FrameInput, detection: DetectionResult, config: DetectionSessionConfig): AgeGenderEstimate =
            estimate
    }
}
