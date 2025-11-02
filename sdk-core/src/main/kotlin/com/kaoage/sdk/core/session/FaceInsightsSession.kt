package com.kaoage.sdk.core.session

import com.kaoage.sdk.core.bestshot.BestShotBridge
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FaceDetector
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.inference.AgeGenderEstimator
import com.kaoage.sdk.core.model.BestShotReason
import com.kaoage.sdk.core.model.FaceInsightsResult

class FaceInsightsSession(
    private val detector: FaceDetector,
    private val estimator: AgeGenderEstimator,
    private val config: DetectionSessionConfig = DetectionSessionConfig(),
    private val bestShotBridge: BestShotBridge? = null
) {
    suspend fun warmUp() {
        detector.warmUp(config)
    }

    suspend fun analyze(frame: FrameInput): FaceInsightsResult? {
        val detections = detector.detect(frame, config)
        val primary = detections.maxByOrNull { it.confidence } ?: return null
        if (primary.confidence < config.minFaceConfidence) {
            bestShotBridge?.handle(primary, config)
            return null
        }

        val estimate = estimator.estimate(frame, primary, config)
        if (estimate.ageConfidence < config.minAgeConfidence || estimate.genderConfidence < config.minGenderConfidence) {
            bestShotBridge?.handle(primary, config)
            return null
        }
        val result = buildResult(primary, estimate)
        bestShotBridge?.handle(primary, config)
        return result
    }

    private fun buildResult(detection: DetectionResult, estimate: com.kaoage.sdk.core.inference.AgeGenderEstimate): FaceInsightsResult {
        val bestShotReasons = mutableSetOf<BestShotReason>()
        if (detection.confidence >= 0.85f) {
            bestShotReasons += BestShotReason.CONFIDENCE_PEAK
        }
        return FaceInsightsResult(
            detectionId = detection.detectionId,
            frameTimestampMillis = detection.timestampMillis,
            boundingBox = detection.boundingBox,
            eulerAngles = detection.eulerAngles,
            landmarks = detection.landmarks,
            faceConfidence = detection.confidence,
            ageBracket = estimate.ageBracket,
            ageConfidence = estimate.ageConfidence,
            gender = estimate.gender,
            genderConfidence = estimate.genderConfidence,
            bestShotEligible = bestShotReasons.isNotEmpty(),
            bestShotReasons = bestShotReasons
        )
    }
}
