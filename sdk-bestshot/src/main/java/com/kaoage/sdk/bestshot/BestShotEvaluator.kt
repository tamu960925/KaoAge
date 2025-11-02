package com.kaoage.sdk.bestshot

import com.kaoage.sdk.core.BestShotReason
import com.kaoage.sdk.core.BestShotSignal
import com.kaoage.sdk.core.BestShotTrigger
import com.kaoage.sdk.core.FaceInsightsResult
import com.kaoage.sdk.core.SessionConfig
import kotlin.math.max

class BestShotEvaluator {
    private var lastSignalTimestamp: Long = Long.MIN_VALUE

    @JvmOverloads
    fun evaluate(
        result: FaceInsightsResult,
        sessionConfig: SessionConfig = SessionConfig()
    ): BestShotSignal? {
        if (!sessionConfig.enableBestShot || !result.bestShotEligible) {
            return null
        }

        val now = result.frameTimestampMillis
        val elapsed = now - lastSignalTimestamp
        val cooldown = max(sessionConfig.cooldownMillis, 0L)
        if (elapsed in 0..cooldown) {
            return null
        }

        lastSignalTimestamp = now
        val trigger = if (result.bestShotReasons.contains(BestShotReason.CONFIDENCE_PEAK)) {
            BestShotTrigger.MAX_CONFIDENCE
        } else {
            BestShotTrigger.FACE_STABLE
        }

        val qualityScore = result.faceConfidence.coerceIn(0f, 1f)
        return BestShotSignal(
            detectionId = result.detectionId,
            frameTimestampMillis = now,
            qualityScore = qualityScore,
            trigger = trigger,
            cooldownMillis = cooldown
        )
    }

    fun reset() {
        lastSignalTimestamp = Long.MIN_VALUE
    }
}
