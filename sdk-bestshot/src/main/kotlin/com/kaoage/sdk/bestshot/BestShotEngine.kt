package com.kaoage.sdk.bestshot

import com.kaoage.sdk.core.bestshot.BestShotEvaluator
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.model.BestShotReason
import com.kaoage.sdk.core.model.BestShotSignal

class BestShotEngine : BestShotEvaluator {
    private var lastSignalTimestamp: Long? = null

    override fun evaluate(detection: DetectionResult, config: DetectionSessionConfig): BestShotSignal? {
        val now = detection.timestampMillis
        val cooldownActive = lastSignalTimestamp?.let { now - it < config.cooldownMillis } ?: false
        val reasons = mutableSetOf<BestShotReason>()
        if (detection.confidence >= 0.85f) {
            reasons += BestShotReason.CONFIDENCE_PEAK
        }
        if (!cooldownActive && reasons.isNotEmpty()) {
            lastSignalTimestamp = now
            return BestShotSignal(
                detectionId = detection.detectionId,
                frameTimestampMillis = now,
                qualityScore = detection.confidence,
                reasons = reasons,
                cooldownMillis = config.cooldownMillis,
                eligible = true
            )
        }
        return BestShotSignal(
            detectionId = detection.detectionId,
            frameTimestampMillis = now,
            qualityScore = detection.confidence,
            reasons = reasons,
            cooldownMillis = config.cooldownMillis,
            eligible = false
        )
    }
}
