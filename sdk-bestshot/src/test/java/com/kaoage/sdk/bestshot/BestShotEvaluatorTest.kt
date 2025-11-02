package com.kaoage.sdk.bestshot

import com.kaoage.sdk.core.AgeBracket
import com.kaoage.sdk.core.BestShotReason
import com.kaoage.sdk.core.BestShotTrigger
import com.kaoage.sdk.core.BoundingBox
import com.kaoage.sdk.core.EulerAngles
import com.kaoage.sdk.core.FaceInsightsResult
import com.kaoage.sdk.core.Gender
import com.kaoage.sdk.core.NamedLandmark
import com.kaoage.sdk.core.SessionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BestShotEvaluatorTest {

    private val evaluator = BestShotEvaluator()

    @Test
    fun evaluate_emitsSignalWhenEligible() {
        val result = stubResult(
            timestamp = 1_000L,
            bestShotEligible = true,
            reasons = listOf(BestShotReason.FACE_STABLE)
        )

        val signal = evaluator.evaluate(result, SessionConfig(cooldownMillis = 500))

        assertNotNull(signal)
        assertEquals(result.detectionId, signal.detectionId)
        assertEquals(result.frameTimestampMillis, signal.frameTimestampMillis)
        assertEquals(result.faceConfidence, signal.qualityScore)
        assertEquals(BestShotTrigger.FACE_STABLE, signal.trigger)
    }

    @Test
    fun evaluate_respectsCooldownWindow() {
        val config = SessionConfig(cooldownMillis = 1_000)
        val first = evaluator.evaluate(
            stubResult(
                timestamp = 2_000L,
                bestShotEligible = true,
                reasons = listOf(BestShotReason.CONFIDENCE_PEAK)
            ),
            config
        )
        assertNotNull(first)

        val suppressed = evaluator.evaluate(
            stubResult(
                timestamp = 2_500L,
                bestShotEligible = true,
                reasons = listOf(BestShotReason.FACE_STABLE)
            ),
            config
        )
        assertNull(suppressed)
    }

    @Test
    fun evaluate_usesMaxConfidenceTriggerWhenPresent() {
        val signal = evaluator.evaluate(
            stubResult(
                timestamp = 3_500L,
                bestShotEligible = true,
                reasons = listOf(BestShotReason.CONFIDENCE_PEAK),
                faceConfidence = 0.9f
            ),
            SessionConfig(cooldownMillis = 0)
        )

        assertNotNull(signal)
        assertEquals(BestShotTrigger.MAX_CONFIDENCE, signal.trigger)
        assertEquals(0.9f, signal.qualityScore)
    }

    private fun stubResult(
        timestamp: Long,
        bestShotEligible: Boolean,
        reasons: List<BestShotReason>,
        faceConfidence: Float = 0.8f
    ): FaceInsightsResult = FaceInsightsResult(
        detectionId = "detection-$timestamp",
        frameTimestampMillis = timestamp,
        boundingBox = BoundingBox(0f, 0f, 10f, 10f, 100, 100),
        eulerAngles = EulerAngles(0f, 0f, 0f),
        landmarks = emptyList<NamedLandmark>(),
        faceConfidence = faceConfidence,
        ageBracket = AgeBracket.ADULT,
        ageConfidence = 0.7f,
        gender = Gender.UNDETERMINED,
        genderConfidence = 0.5f,
        bestShotEligible = bestShotEligible,
        bestShotReasons = reasons
    )
}
