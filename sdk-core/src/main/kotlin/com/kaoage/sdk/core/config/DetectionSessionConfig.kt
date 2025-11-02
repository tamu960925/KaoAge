package com.kaoage.sdk.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class DetectionSessionConfig(
    val minFaceConfidence: Float = 0.7f,
    val minAgeConfidence: Float = 0.6f,
    val minGenderConfidence: Float = 0.6f,
    val minFaceSizeRatio: Float = 0.15f,
    val maxFrameLatencyMillis: Long = 500L,
    val enableBestShot: Boolean = true,
    val cooldownMillis: Long = 2500L
) {
    init {
        require(minFaceConfidence in 0f..1f) { "minFaceConfidence must be within 0..1" }
        require(minAgeConfidence in 0f..1f) { "minAgeConfidence must be within 0..1" }
        require(minGenderConfidence in 0f..1f) { "minGenderConfidence must be within 0..1" }
        require(minFaceSizeRatio in 0f..1f) { "minFaceSizeRatio must be within 0..1" }
        require(maxFrameLatencyMillis > 0) { "maxFrameLatencyMillis must be positive" }
        require(cooldownMillis >= 0) { "cooldownMillis must be non-negative" }
    }

    fun mergedWith(overrides: DetectionSessionOverrides?): DetectionSessionConfig {
        overrides ?: return this
        return DetectionSessionConfig(
            minFaceConfidence = overrides.minFaceConfidence ?: minFaceConfidence,
            minAgeConfidence = overrides.minAgeConfidence ?: minAgeConfidence,
            minGenderConfidence = overrides.minGenderConfidence ?: minGenderConfidence,
            minFaceSizeRatio = overrides.minFaceSizeRatio ?: minFaceSizeRatio,
            maxFrameLatencyMillis = overrides.maxFrameLatencyMillis ?: maxFrameLatencyMillis,
            enableBestShot = overrides.enableBestShot ?: enableBestShot,
            cooldownMillis = overrides.cooldownMillis ?: cooldownMillis
        )
    }
}

@Serializable
data class DetectionSessionOverrides(
    val minFaceConfidence: Float? = null,
    val minAgeConfidence: Float? = null,
    val minGenderConfidence: Float? = null,
    val minFaceSizeRatio: Float? = null,
    val maxFrameLatencyMillis: Long? = null,
    val enableBestShot: Boolean? = null,
    val cooldownMillis: Long? = null
)
