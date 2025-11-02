package com.kaoage.sdk.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object FaceInsightsJson {
    val formatter: Json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
        prettyPrint = false
    }
}

@Serializable
@Parcelize
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val imageWidth: Int,
    val imageHeight: Int
) : Parcelable {
    init {
        require(left <= right) { "Bounding box left must be <= right" }
        require(top <= bottom) { "Bounding box top must be <= bottom" }
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
    }
}

@Serializable
@Parcelize
data class EulerAngles(
    val yaw: Float,
    val pitch: Float,
    val roll: Float
) : Parcelable

@Serializable
enum class LandmarkType {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE_TIP,
    MOUTH_LEFT,
    MOUTH_RIGHT
}

@Serializable
@Parcelize
data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val probability: Float? = null
) : Parcelable

@Serializable
@Parcelize
data class NamedLandmark(
    val type: LandmarkType,
    val point: LandmarkPoint
) : Parcelable

@Serializable
enum class AgeBracket {
    @SerialName("CHILD")
    CHILD,
    @SerialName("TEEN")
    TEEN,
    @SerialName("ADULT")
    ADULT,
    @SerialName("SENIOR")
    SENIOR
}

@Serializable
enum class Gender {
    @SerialName("FEMALE")
    FEMALE,
    @SerialName("MALE")
    MALE,
    @SerialName("UNDETERMINED")
    UNDETERMINED
}

@Serializable
enum class BestShotReason {
    FACE_STABLE,
    CONFIDENCE_PEAK,
    MULTI_FACE_BLOCK,
    LOW_LIGHT
}

@Serializable
enum class BestShotTrigger {
    FACE_STABLE,
    MAX_CONFIDENCE,
    TIMEOUT_RECOVERY
}

@Serializable
@Parcelize
data class BestShotSignal(
    val detectionId: String,
    val frameTimestampMillis: Long,
    val qualityScore: Float,
    val trigger: BestShotTrigger,
    val cooldownMillis: Long
) : Parcelable {
    fun toJson(): String = FaceInsightsJson.formatter.encodeToString(this)

    companion object {
        @JvmStatic
        fun fromJson(json: String): BestShotSignal =
            FaceInsightsJson.formatter.decodeFromString(json)
    }
}

@Serializable
@Parcelize
data class FaceInsightsResult(
    val detectionId: String,
    val frameTimestampMillis: Long,
    val boundingBox: BoundingBox,
    val eulerAngles: EulerAngles,
    val landmarks: List<NamedLandmark>,
    val faceConfidence: Float,
    val ageBracket: AgeBracket,
    val ageConfidence: Float,
    val gender: Gender,
    val genderConfidence: Float,
    val bestShotEligible: Boolean = false,
    val bestShotReasons: List<BestShotReason> = emptyList()
) : Parcelable {
    fun toJson(): String = FaceInsightsJson.formatter.encodeToString(this)

    companion object {
        @JvmStatic
        fun fromJson(json: String): FaceInsightsResult =
            FaceInsightsJson.formatter.decodeFromString(json)
    }
}

@Serializable
@Parcelize
data class SessionConfig(
    val minFaceConfidence: Float = 0.7f,
    val minAgeConfidence: Float = 0.6f,
    val minGenderConfidence: Float = 0.6f,
    val minFaceSizeRatio: Float = 0.15f,
    val maxFrameLatencyMillis: Long = 500,
    val enableBestShot: Boolean = true,
    val cooldownMillis: Long = 2500
) : Parcelable {
    init {
        require(minFaceConfidence in 0f..1f) { "minFaceConfidence must be in [0,1]" }
        require(minAgeConfidence in 0f..1f) { "minAgeConfidence must be in [0,1]" }
        require(minGenderConfidence in 0f..1f) { "minGenderConfidence must be in [0,1]" }
        require(minFaceSizeRatio in 0f..1f) { "minFaceSizeRatio must be in [0,1]" }
        require(maxFrameLatencyMillis >= 0) { "maxFrameLatencyMillis must be >= 0" }
        require(cooldownMillis >= 0) { "cooldownMillis must be >= 0" }
    }

    fun toJson(): String = FaceInsightsJson.formatter.encodeToString(this)

    companion object {
        @JvmStatic
        fun fromJson(json: String): SessionConfig =
            FaceInsightsJson.formatter.decodeFromString(json)
    }
}
