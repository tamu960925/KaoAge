package com.kaoage.sdk.core.detection

import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight heuristic to approximate a confidence value when ML Kit does not surface one.
 * We scale by the proportion of the frame covered by the face compared to the configured
 * minimum face ratio, and optionally add a small boost when tracking is stable.
 */
internal object DetectionConfidenceHeuristic {

    fun score(
        areaRatio: Float,
        hasTrackingId: Boolean,
        minFaceSizeRatio: Float
    ): Float {
        val safeMinRatio = max(minFaceSizeRatio, 0.01f)
        val normalizedArea = min(areaRatio / safeMinRatio, 1f).coerceAtLeast(0f)
        val baseConfidence = 0.25f + 0.75f * normalizedArea
        val trackingBonus = if (hasTrackingId) 0.1f else 0f
        return (baseConfidence + trackingBonus).coerceIn(0.05f, 1f)
    }
}

