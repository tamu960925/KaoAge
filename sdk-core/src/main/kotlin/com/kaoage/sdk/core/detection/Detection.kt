@file:OptIn(ExperimentalGetImage::class)

package com.kaoage.sdk.core.detection

import android.annotation.SuppressLint
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import kotlinx.coroutines.tasks.await
import kotlin.math.max

/** Simple frame abstraction to decouple session logic from CameraX specifics. */
data class FrameInput(
    val timestampMillis: Long,
    val imageProxy: ImageProxy? = null
)

/** Primary detector contract to allow stubbing in tests. */
interface FaceDetector {
    suspend fun detect(frame: FrameInput, config: DetectionSessionConfig): List<DetectionResult>
    suspend fun warmUp(config: DetectionSessionConfig)
}

/** Minimal detection value object. */
data class DetectionResult(
    val detectionId: String,
    val timestampMillis: Long,
    val boundingBox: BoundingBox,
    val eulerAngles: EulerAngles,
    val landmarks: LandmarkSet,
    val confidence: Float
)

/** ML Kit backed detector that satisfies constitution guardrails. */
class MlKitFaceAnalyzer : FaceDetector {
    private var detector = buildClient(defaultOptions())

    override suspend fun warmUp(config: DetectionSessionConfig) {
        detector.close()
        detector = buildClient(optionsFrom(config))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun detect(
        frame: FrameInput,
        config: DetectionSessionConfig
    ): List<DetectionResult> {
        val imageProxy = frame.imageProxy ?: return emptyList()
        val mediaImage = imageProxy.image ?: return emptyList()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val faces = detector.process(image).await()
        val imageWidth = mediaImage.width
        val imageHeight = mediaImage.height
        return faces.mapIndexed { index, face ->
            val id = face.trackingId?.toString() ?: "frame-${frame.timestampMillis}-$index"
            val confidence = detectionConfidence(face, imageWidth, imageHeight, config)
            DetectionResult(
                detectionId = id,
                timestampMillis = frame.timestampMillis,
                boundingBox = BoundingBox(
                    left = face.boundingBox.left.toFloat(),
                    top = face.boundingBox.top.toFloat(),
                    right = face.boundingBox.right.toFloat(),
                    bottom = face.boundingBox.bottom.toFloat(),
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                ),
                eulerAngles = EulerAngles(face.headEulerAngleY, face.headEulerAngleX, face.headEulerAngleZ),
                landmarks = LandmarkSet(landmarksFrom(face)),
                confidence = confidence
            )
        }
    }

    private fun buildClient(options: FaceDetectorOptions) = FaceDetection.getClient(options)

    private fun defaultOptions(): FaceDetectorOptions = optionsFrom(DetectionSessionConfig())

    private fun optionsFrom(config: DetectionSessionConfig): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(config.minFaceSizeRatio)
            .enableTracking()
            .build()

    private fun landmarksFrom(face: com.google.mlkit.vision.face.Face): Map<LandmarkType, NormalizedPoint> {
        val required = LandmarkType.values()
        return required.associateWith { type ->
            val landmark = when (type) {
                LandmarkType.LEFT_EYE -> face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                LandmarkType.RIGHT_EYE -> face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                LandmarkType.NOSE_TIP -> face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
                LandmarkType.MOUTH_LEFT -> face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT)
                LandmarkType.MOUTH_RIGHT -> face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT)
            }
            val position = landmark?.position
            NormalizedPoint(
                x = (position?.x ?: 0f) / face.boundingBox.width().coerceAtLeast(1),
                y = (position?.y ?: 0f) / face.boundingBox.height().coerceAtLeast(1),
                probability = null
            )
        }
    }

    private fun detectionConfidence(
        face: com.google.mlkit.vision.face.Face,
        imageWidth: Int,
        imageHeight: Int,
        config: DetectionSessionConfig
    ): Float {
        val faceWidth = max(face.boundingBox.width(), 1)
        val faceHeight = max(face.boundingBox.height(), 1)
        val faceArea = faceWidth * faceHeight
        val imageArea = max(imageWidth * imageHeight, 1)
        val areaRatio = faceArea.toFloat() / imageArea.toFloat()
        val hasTrackingId = face.trackingId != null
        return DetectionConfidenceHeuristic.score(
            areaRatio = areaRatio,
            hasTrackingId = hasTrackingId,
            minFaceSizeRatio = config.minFaceSizeRatio
        )
    }
}
