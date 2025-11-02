package com.kaoage.sdk.core

import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BestShotReason
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaceInsightsResultTest {

    @Test
    fun `toJson round trips with all fields`() {
        val landmarks = LandmarkSet(
            mapOf(
                LandmarkType.LEFT_EYE to NormalizedPoint(0.4f, 0.3f, 0.9f),
                LandmarkType.RIGHT_EYE to NormalizedPoint(0.6f, 0.3f, 0.95f),
                LandmarkType.NOSE_TIP to NormalizedPoint(0.5f, 0.45f, 0.85f),
                LandmarkType.MOUTH_LEFT to NormalizedPoint(0.42f, 0.6f, 0.8f),
                LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.58f, 0.6f, 0.82f)
            )
        )
        val result = FaceInsightsResult(
            detectionId = "det-123",
            frameTimestampMillis = 42L,
            boundingBox = BoundingBox(100f, 120f, 300f, 420f, 1080, 1920),
            eulerAngles = EulerAngles(5f, -2.5f, 1.3f),
            landmarks = landmarks,
            faceConfidence = 0.92f,
            ageBracket = AgeBracket.ADULT,
            ageConfidence = 0.88f,
            gender = GenderLabel.FEMALE,
            genderConfidence = 0.81f,
            bestShotEligible = true,
            bestShotReasons = setOf(BestShotReason.FACE_STABLE, BestShotReason.CONFIDENCE_PEAK)
        )

        val json = result.toJson()
        val restored = FaceInsightsResult.fromJson(json)

        assertEquals(result.detectionId, restored.detectionId)
        assertEquals(result.boundingBox, restored.boundingBox)
        assertEquals(result.landmarks, restored.landmarks)
        assertEquals(result.bestShotReasons, restored.bestShotReasons)
    }

    @Test
    fun `validation enforces confidence bounds`() {
        val exception = kotlin.runCatching {
            FaceInsightsResult(
                detectionId = "invalid",
                frameTimestampMillis = 0L,
                boundingBox = BoundingBox(0f, 0f, 1f, 1f, 100, 100),
                eulerAngles = EulerAngles(0f, 0f, 0f),
                landmarks = LandmarkSet(minimumLandmarks()),
                faceConfidence = 1.2f,
                ageBracket = AgeBracket.ADULT,
                ageConfidence = 0.9f,
                gender = GenderLabel.UNDETERMINED,
                genderConfidence = 0.4f,
                bestShotEligible = false,
                bestShotReasons = emptySet()
            )
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    private fun minimumLandmarks(): Map<LandmarkType, NormalizedPoint> = mapOf(
        LandmarkType.LEFT_EYE to NormalizedPoint(0.3f, 0.3f, 0.7f),
        LandmarkType.RIGHT_EYE to NormalizedPoint(0.7f, 0.3f, 0.7f),
        LandmarkType.NOSE_TIP to NormalizedPoint(0.5f, 0.5f, 0.7f),
        LandmarkType.MOUTH_LEFT to NormalizedPoint(0.4f, 0.7f, 0.7f),
        LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.6f, 0.7f, 0.7f)
    )
}
