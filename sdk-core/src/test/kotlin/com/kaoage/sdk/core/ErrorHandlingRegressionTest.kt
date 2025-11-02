package com.kaoage.sdk.core

import com.kaoage.sdk.core.bestshot.BestShotBridge
import com.kaoage.sdk.core.bestshot.BestShotEvaluator
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FaceDetector
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.inference.AgeGenderEstimate
import com.kaoage.sdk.core.inference.AgeGenderEstimator
import com.kaoage.sdk.core.model.BestShotSignal
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import com.kaoage.sdk.core.session.FaceInsightsSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull

class ErrorHandlingRegressionTest {

    @Test
    fun `session returns null when confidence below threshold`() {
        val detector = object : FaceDetector {
            override suspend fun detect(frame: FrameInput, config: DetectionSessionConfig) = listOf(
                DetectionResult(
                    detectionId = "low",
                    timestampMillis = frame.timestampMillis,
                    boundingBox = BoundingBox(0f, 0f, 1f, 1f, 10, 10),
                    eulerAngles = EulerAngles(0f, 0f, 0f),
                    landmarks = LandmarkSet(
                        mapOf(
                            LandmarkType.LEFT_EYE to NormalizedPoint(0.2f, 0.2f),
                            LandmarkType.RIGHT_EYE to NormalizedPoint(0.8f, 0.2f),
                            LandmarkType.NOSE_TIP to NormalizedPoint(0.5f, 0.5f),
                            LandmarkType.MOUTH_LEFT to NormalizedPoint(0.3f, 0.8f),
                            LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.7f, 0.8f)
                        )
                    ),
                    confidence = 0.4f
                )
            )

            override suspend fun warmUp(config: DetectionSessionConfig) {}
        }
        val estimator = object : AgeGenderEstimator {
            override suspend fun estimate(
                frame: FrameInput,
                detection: DetectionResult,
                config: DetectionSessionConfig
            ): AgeGenderEstimate {
                return AgeGenderEstimate(
                    ageBracket = com.kaoage.sdk.core.model.AgeBracket.ADULT,
                    ageConfidence = 0.9f,
                    gender = com.kaoage.sdk.core.model.GenderLabel.UNDETERMINED,
                    genderConfidence = 0.9f
                )
            }
        }
        var lowConfidenceNotified = false
        val session = FaceInsightsSession(
            detector = detector,
            estimator = estimator,
            config = DetectionSessionConfig(minFaceConfidence = 0.7f),
            bestShotBridge = BestShotBridge(
                evaluator = BestShotEvaluator { _, _ ->
                    BestShotSignal(
                        detectionId = "low",
                        frameTimestampMillis = 0L,
                        qualityScore = 0.4f,
                        reasons = emptySet(),
                        eligible = false
                    )
                },
                onBestShot = {},
                onLowConfidence = { lowConfidenceNotified = true }
            )
        )

        val result = runBlocking { session.analyze(FrameInput(0L)) }
        assertNull(result)
        assert(lowConfidenceNotified)
    }
}
