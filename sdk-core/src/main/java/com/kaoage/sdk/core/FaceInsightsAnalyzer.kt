package com.kaoage.sdk.core

import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

class FaceInsightsAnalyzer(
    private val detector: FaceDetector,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AutoCloseable {

    constructor(
        config: FaceDetectorOptions = defaultFaceOptions()
    ) : this(FaceDetection.getClient(config))

    suspend fun analyze(
        image: InputImage,
        sessionConfig: SessionConfig = SessionConfig(),
        frameTimestampMillis: Long = System.currentTimeMillis()
    ): FaceInsightsResult? = withContext(dispatcher) {
        val faces = detector.process(image).await()
        val primary = faces.maxByOrNull { area(it.boundingBox) } ?: return@withContext null
        val imageArea = (image.width * image.height).coerceAtLeast(1)
        val sizeRatio = area(primary.boundingBox) / imageArea

        if (sizeRatio < sessionConfig.minFaceSizeRatio) {
            return@withContext null
        }

        val faceConfidence = sizeRatio.coerceIn(0f, 1f)
        val eulerAngles = EulerAngles(
            yaw = primary.headEulerAngleY,
            pitch = primary.headEulerAngleX,
            roll = primary.headEulerAngleZ
        )

        val landmarks = NamedLandmarkFactory.fromFace(primary)

        val (ageBracket, ageConfidence) = AgeGenderHeuristics.estimateAge(sizeRatio)
        val (gender, genderConfidence) = AgeGenderHeuristics.estimateGender(
            primary.leftEyeOpenProbability,
            primary.rightEyeOpenProbability,
            primary.smilingProbability
        )

        val bestShotEvaluation = BestShotHeuristics.evaluate(
            faceConfidence,
            eulerAngles,
            sessionConfig
        )

        FaceInsightsResult(
            detectionId = primary.trackingId?.toString() ?: UUID.randomUUID().toString(),
            frameTimestampMillis = frameTimestampMillis,
            boundingBox = BoundingBox(
                left = primary.boundingBox.left.toFloat(),
                top = primary.boundingBox.top.toFloat(),
                right = primary.boundingBox.right.toFloat(),
                bottom = primary.boundingBox.bottom.toFloat(),
                imageWidth = image.width,
                imageHeight = image.height
            ),
            eulerAngles = eulerAngles,
            landmarks = landmarks,
            faceConfidence = faceConfidence,
            ageBracket = ageBracket,
            ageConfidence = ageConfidence,
            gender = gender,
            genderConfidence = genderConfidence,
            bestShotEligible = bestShotEvaluation.eligible,
            bestShotReasons = bestShotEvaluation.reasons
        )
    }

    override fun close() {
        detector.close()
    }

    private fun area(rect: Rect): Float =
        max(rect.width(), 0) * max(rect.height(), 0).toFloat()

    companion object {
        @JvmStatic
        fun defaultFaceOptions(): FaceDetectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
    }
}

private object NamedLandmarkFactory {
    fun fromFace(face: Face): List<NamedLandmark> = buildList {
        addIfPresent(face.getLandmark(FaceLandmark.RIGHT_EYE), LandmarkType.RIGHT_EYE)
        addIfPresent(face.getLandmark(FaceLandmark.LEFT_EYE), LandmarkType.LEFT_EYE)
        addIfPresent(face.getLandmark(FaceLandmark.NOSE_BASE), LandmarkType.NOSE_TIP)
        addIfPresent(face.getLandmark(FaceLandmark.MOUTH_LEFT), LandmarkType.MOUTH_LEFT)
        addIfPresent(face.getLandmark(FaceLandmark.MOUTH_RIGHT), LandmarkType.MOUTH_RIGHT)
    }

    private fun MutableList<NamedLandmark>.addIfPresent(
        landmark: FaceLandmark?,
        type: LandmarkType
    ) {
        landmark ?: return
        add(
            NamedLandmark(
                type = type,
                point = LandmarkPoint(
                    x = landmark.position.x,
                    y = landmark.position.y,
                    probability = null
                )
            )
        )
    }
}

private object AgeGenderHeuristics {
    fun estimateAge(sizeRatio: Float): Pair<AgeBracket, Float> = when {
        sizeRatio < 0.12f -> AgeBracket.CHILD to confidence(sizeRatio, 0.12f)
        sizeRatio < 0.18f -> AgeBracket.TEEN to confidence(sizeRatio, 0.18f)
        sizeRatio < 0.35f -> AgeBracket.ADULT to confidence(sizeRatio, 0.35f)
        else -> AgeBracket.SENIOR to confidence(sizeRatio, 0.45f)
    }

    fun estimateGender(
        leftEyeOpen: Float?,
        rightEyeOpen: Float?,
        smilingProbability: Float?
    ): Pair<Gender, Float> {
        val baseline = smilingProbability ?: 0.5f
        val eyeScore = listOfNotNull(leftEyeOpen, rightEyeOpen).averageOrNull() ?: 0.5f
        val combined = ((baseline + eyeScore) / 2f).coerceIn(0f, 1f)
        val centered = combined - 0.5f
        val magnitude = abs(centered) * 1.8f
        val confidence = magnitude.coerceIn(0.1f, 0.95f)
        return when {
            centered > 0.05f -> Gender.FEMALE to confidence
            centered < -0.05f -> Gender.MALE to confidence
            else -> Gender.UNDETERMINED to 0.2f
        }
    }

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else sum() / size

    private fun confidence(sizeRatio: Float, peakRatio: Float): Float {
        val normalized = (sizeRatio / peakRatio).coerceIn(0f, 1f)
        return normalized.coerceIn(0.2f, 0.95f)
    }
}

private object BestShotHeuristics {
    data class Result(
        val eligible: Boolean,
        val reasons: List<BestShotReason>
    )

    fun evaluate(
        faceConfidence: Float,
        eulerAngles: EulerAngles,
        sessionConfig: SessionConfig
    ): Result {
        val reasons = mutableListOf<BestShotReason>()
        if (abs(eulerAngles.yaw) < 12f && abs(eulerAngles.pitch) < 10f) {
            reasons += BestShotReason.FACE_STABLE
        }
        if (faceConfidence >= sessionConfig.minFaceConfidence + 0.1f) {
            reasons += BestShotReason.CONFIDENCE_PEAK
        }

        val eligible = sessionConfig.enableBestShot && reasons.isNotEmpty()
        return Result(eligible, reasons)
    }
}
