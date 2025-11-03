package com.kaoage.sdk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FaceInsightsLegacyModelsTest {

    @Test
    fun `bounding box validates coordinate ordering`() {
        val box = BoundingBox(
            left = 10f,
            top = 12f,
            right = 42f,
            bottom = 64f,
            imageWidth = 200,
            imageHeight = 300
        )
        assertEquals(10f, box.left)

        assertFailsWith<IllegalArgumentException> {
            BoundingBox(
                left = 30f,
                top = 0f,
                right = 20f,
                bottom = 10f,
                imageWidth = 100,
                imageHeight = 100
            )
        }
    }

    @Test
    fun `landmark presence requires both landmarks above confidence threshold`() {
        val landmarks = listOf(
            NamedLandmark(
                type = LandmarkType.LEFT_EYE,
                point = LandmarkPoint(10f, 12f, probability = 0.8f)
            ),
            NamedLandmark(
                type = LandmarkType.RIGHT_EYE,
                point = LandmarkPoint(18f, 12f, probability = 0.3f)
            ),
            NamedLandmark(
                type = LandmarkType.NOSE_TIP,
                point = LandmarkPoint(14f, 20f, probability = 0.7f)
            ),
            NamedLandmark(
                type = LandmarkType.MOUTH_LEFT,
                point = LandmarkPoint(11f, 26f, probability = 0.9f)
            ),
            NamedLandmark(
                type = LandmarkType.MOUTH_RIGHT,
                point = LandmarkPoint(17f, 26f, probability = 0.9f)
            )
        )

        val presence = LandmarkPresence.fromLandmarks(landmarks)

        assertFalse(presence.eyes)
        assertTrue(presence.nose)
        assertTrue(presence.mouth)
    }

    @Test
    fun `face insights result round trips via json`() {
        val result = FaceInsightsResult(
            detectionId = "legacy-1",
            frameTimestampMillis = 1234L,
            boundingBox = BoundingBox(0f, 0f, 20f, 30f, 1920, 1080),
            eulerAngles = EulerAngles(3.4f, -1.2f, 0.5f),
            landmarks = listOf(
                NamedLandmark(LandmarkType.LEFT_EYE, LandmarkPoint(5f, 10f, 0.7f)),
                NamedLandmark(LandmarkType.RIGHT_EYE, LandmarkPoint(15f, 10f, 0.9f)),
                NamedLandmark(LandmarkType.NOSE_TIP, LandmarkPoint(10f, 16f, 0.8f)),
                NamedLandmark(LandmarkType.MOUTH_LEFT, LandmarkPoint(6f, 22f, 0.6f)),
                NamedLandmark(LandmarkType.MOUTH_RIGHT, LandmarkPoint(14f, 22f, 0.6f))
            ),
            faceConfidence = 0.82f,
            ageBracket = AgeBracket.ADULT,
            ageConfidence = 0.78f,
            gender = Gender.FEMALE,
            genderConfidence = 0.74f,
            bestShotEligible = true,
            bestShotReasons = listOf(BestShotReason.FACE_STABLE, BestShotReason.CONFIDENCE_PEAK),
            classifierLabel = "vip",
            classifierConfidence = 0.6f,
            landmarkPresence = LandmarkPresence(eyes = true, nose = true, mouth = true),
            estimatedAgeYears = 29.5f
        )

        val json = result.toJson()
        val restored = FaceInsightsResult.fromJson(json)

        assertEquals(result, restored)
    }

    @Test
    fun `session config enforces value ranges`() {
        val config = SessionConfig(
            minFaceConfidence = 0.75f,
            minAgeConfidence = 0.65f,
            minGenderConfidence = 0.55f,
            minFaceSizeRatio = 0.2f,
            maxFrameLatencyMillis = 400,
            enableBestShot = false,
            cooldownMillis = 1000
        )
        assertEquals(0.75f, config.minFaceConfidence)

        assertFailsWith<IllegalArgumentException> {
            SessionConfig(minFaceConfidence = 1.2f)
        }
    }

    @Test
    fun `best shot signal json preserves trigger data`() {
        val signal = BestShotSignal(
            detectionId = "det-42",
            frameTimestampMillis = 55L,
            qualityScore = 0.91f,
            trigger = BestShotTrigger.MAX_CONFIDENCE,
            cooldownMillis = 500
        )
        val restored = BestShotSignal.fromJson(signal.toJson())
        assertEquals(signal, restored)
    }
}
