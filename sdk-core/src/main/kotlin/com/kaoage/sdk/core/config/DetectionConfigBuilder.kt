package com.kaoage.sdk.core.config

class DetectionConfigBuilder @JvmOverloads constructor(
    private var base: DetectionSessionConfig = DetectionSessionConfig()
) {
    private var minFaceConfidence: Float = base.minFaceConfidence
    private var minAgeConfidence: Float = base.minAgeConfidence
    private var minGenderConfidence: Float = base.minGenderConfidence
    private var minFaceSizeRatio: Float = base.minFaceSizeRatio
    private var maxFrameLatencyMillis: Long = base.maxFrameLatencyMillis
    private var enableBestShot: Boolean = base.enableBestShot
    private var cooldownMillis: Long = base.cooldownMillis

    fun minFaceConfidence(value: Float) = apply { minFaceConfidence = value }
    fun minAgeConfidence(value: Float) = apply { minAgeConfidence = value }
    fun minGenderConfidence(value: Float) = apply { minGenderConfidence = value }
    fun minFaceSizeRatio(value: Float) = apply { minFaceSizeRatio = value }
    fun maxFrameLatencyMillis(value: Long) = apply { maxFrameLatencyMillis = value }
    fun enableBestShot(value: Boolean) = apply { enableBestShot = value }
    fun cooldownMillis(value: Long) = apply { cooldownMillis = value }

    fun build(): DetectionSessionConfig = DetectionSessionConfig(
        minFaceConfidence = minFaceConfidence,
        minAgeConfidence = minAgeConfidence,
        minGenderConfidence = minGenderConfidence,
        minFaceSizeRatio = minFaceSizeRatio,
        maxFrameLatencyMillis = maxFrameLatencyMillis,
        enableBestShot = enableBestShot,
        cooldownMillis = cooldownMillis
    )
}
