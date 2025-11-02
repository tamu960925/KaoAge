package com.kaoage.sdk.core.serialization

import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import com.kaoage.sdk.core.telemetry.TelemetryReporter
import kotlin.test.Test
import kotlin.test.assertTrue

class TelemetryExportTest {

    @Test
    fun `reporter emits json payload`() {
        val result = FaceInsightsResult(
            detectionId = "det-json",
            frameTimestampMillis = 1L,
            boundingBox = BoundingBox(0f, 0f, 100f, 140f, 1080, 1920),
            eulerAngles = EulerAngles(0f, 0f, 0f),
            landmarks = LandmarkSet(
                mapOf(
                    LandmarkType.LEFT_EYE to NormalizedPoint(0.3f, 0.3f),
                    LandmarkType.RIGHT_EYE to NormalizedPoint(0.6f, 0.3f),
                    LandmarkType.NOSE_TIP to NormalizedPoint(0.45f, 0.45f),
                    LandmarkType.MOUTH_LEFT to NormalizedPoint(0.35f, 0.65f),
                    LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.65f, 0.65f)
                )
            ),
            faceConfidence = 0.9f,
            ageBracket = AgeBracket.ADULT,
            ageConfidence = 0.8f,
            gender = GenderLabel.MALE,
            genderConfidence = 0.75f,
            bestShotEligible = false
        )
        val reporter = TelemetryReporter()
        val record = reporter.serialize(result)
        assertTrue(record.contains("\"detectionId\""))
        assertTrue(record.contains("\"ADULT\""))
    }
}
