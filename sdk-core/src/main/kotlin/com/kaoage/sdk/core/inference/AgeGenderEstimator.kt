package com.kaoage.sdk.core.inference

import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.GenderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

interface AgeGenderEstimator {
    suspend fun estimate(frame: FrameInput, detection: DetectionResult, config: DetectionSessionConfig): AgeGenderEstimate
}

data class AgeGenderEstimate(
    val ageBracket: AgeBracket,
    val ageConfidence: Float,
    val gender: GenderLabel,
    val genderConfidence: Float
)

/**
 * Placeholder on-device estimator; expects models to be provisioned through `scripts/download_models.sh`.
 */
class AgeGenderEngine(
    private val ageModel: ByteBuffer? = null,
    private val genderModel: ByteBuffer? = null
) : AgeGenderEstimator {

    override suspend fun estimate(
        frame: FrameInput,
        detection: DetectionResult,
        config: DetectionSessionConfig
    ): AgeGenderEstimate = withContext(Dispatchers.Default) {
        AgeGenderEstimate(
            ageBracket = AgeBracket.ADULT,
            ageConfidence = maxOf(config.minAgeConfidence, detection.confidence.coerceIn(0f, 1f)),
            gender = GenderLabel.UNDETERMINED,
            genderConfidence = config.minGenderConfidence
        )
    }
}
