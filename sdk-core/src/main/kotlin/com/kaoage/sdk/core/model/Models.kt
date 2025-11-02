package com.kaoage.sdk.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { encodeDefaults = true }

@Parcelize
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val imageWidth: Int,
    val imageHeight: Int
) : Parcelable {
    init {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        require(left >= 0f && top >= 0f && right <= imageWidth && bottom <= imageHeight) {
            "Bounding box must lie within image bounds"
        }
        require(right > left && bottom > top) { "Bounding box must have positive size" }
    }
}

@Parcelize
@Serializable
data class EulerAngles(
    val yaw: Float,
    val pitch: Float,
    val roll: Float
) : Parcelable {
    init {
        require(yaw in -180f..180f && pitch in -180f..180f && roll in -180f..180f) {
            "Euler angles must be within -180..180"
        }
    }
}

@Parcelize
@Serializable
data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val probability: Float? = null
) : Parcelable {
    init {
        require(x in 0f..1f && y in 0f..1f) { "Normalized coordinates must be in 0..1" }
        if (probability != null) {
            require(probability in 0f..1f) { "Probability must be in 0..1" }
        }
    }
}

@Serializable
enum class LandmarkType {
    @SerialName("LEFT_EYE") LEFT_EYE,
    @SerialName("RIGHT_EYE") RIGHT_EYE,
    @SerialName("NOSE_TIP") NOSE_TIP,
    @SerialName("MOUTH_LEFT") MOUTH_LEFT,
    @SerialName("MOUTH_RIGHT") MOUTH_RIGHT
}

@Parcelize
@Serializable
data class LandmarkSet(
    val points: Map<LandmarkType, NormalizedPoint>
) : Parcelable {
    init {
        val required = LandmarkType.values().toSet()
        require(points.keys.containsAll(required)) { "All required landmarks must be present" }
    }
}

@Serializable
enum class AgeBracket {
    @SerialName("CHILD") CHILD,
    @SerialName("TEEN") TEEN,
    @SerialName("ADULT") ADULT,
    @SerialName("SENIOR") SENIOR
}

@Serializable
enum class GenderLabel {
    @SerialName("FEMALE") FEMALE,
    @SerialName("MALE") MALE,
    @SerialName("UNDETERMINED") UNDETERMINED
}

@Serializable
enum class BestShotReason {
    @SerialName("FACE_STABLE") FACE_STABLE,
    @SerialName("CONFIDENCE_PEAK") CONFIDENCE_PEAK,
    @SerialName("MULTI_FACE_BLOCK") MULTI_FACE_BLOCK,
    @SerialName("LOW_LIGHT") LOW_LIGHT
}

@Parcelize
@Serializable
data class BestShotSignal(
    val detectionId: String,
    val frameTimestampMillis: Long,
    val qualityScore: Float,
    val reasons: Set<BestShotReason>,
    val cooldownMillis: Long = 0L,
    val eligible: Boolean = qualityScore >= 0.8f && reasons.isNotEmpty()
) : Parcelable {
    init {
        require(qualityScore in 0f..1f) { "qualityScore must be within 0..1" }
        require(cooldownMillis >= 0) { "cooldownMillis must be non-negative" }
    }
}

@Parcelize
@Serializable
data class FaceInsightsResult(
    val detectionId: String,
    val frameTimestampMillis: Long,
    val boundingBox: BoundingBox,
    val eulerAngles: EulerAngles,
    val landmarks: LandmarkSet,
    val faceConfidence: Float,
    val ageBracket: AgeBracket,
    val ageConfidence: Float,
    val gender: GenderLabel,
    val genderConfidence: Float,
    val bestShotEligible: Boolean,
    val bestShotReasons: Set<BestShotReason> = emptySet()
) : Parcelable {
    init {
        require(faceConfidence in 0f..1f) { "Face confidence must be 0..1" }
        require(ageConfidence in 0f..1f) { "Age confidence must be 0..1" }
        require(genderConfidence in 0f..1f) { "Gender confidence must be 0..1" }
    }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        fun fromJson(payload: String): FaceInsightsResult = json.decodeFromString(serializer(), payload)
    }
}
