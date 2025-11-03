package com.kaoage.sdk.core.inference

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.kaoage.sdk.core.ImageProxyUtils
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.GenderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val FACE_CROP_TOP_OFFSET_PX = 5

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
    private val genderModel: ByteBuffer? = null,
    private val interpreterFactory: InterpreterFactory = DefaultInterpreterFactory(),
    private val cropper: FaceCropper = ImageProxyFaceCropper
) : AgeGenderEstimator {

    @Volatile
    private var ageInterpreter: TfLiteInterpreter? = null
    private val ageLock = Any()

    @Volatile
    private var genderInterpreter: TfLiteInterpreter? = null
    private val genderLock = Any()

    override suspend fun estimate(
        frame: FrameInput,
        detection: DetectionResult,
        config: DetectionSessionConfig
    ): AgeGenderEstimate = withContext(Dispatchers.Default) {
        val ageModel = ageModel
        val genderModel = genderModel
        if (ageModel == null || genderModel == null) {
            return@withContext fallback(detection, config)
        }

        val faceBitmap = cropper.crop(frame, detection) ?: return@withContext fallback(detection, config)

        val ageInterpreter = obtainAgeInterpreter(ageModel)
            ?: return@withContext fallback(detection, config).also { faceBitmap.recycle() }

        val genderInterpreter = obtainGenderInterpreter(genderModel)
            ?: return@withContext fallback(detection, config).also { faceBitmap.recycle() }

        val ageYears = runCatching { runAgeEstimator(ageInterpreter, faceBitmap) }.getOrNull()
        val genderScores = runCatching { runGenderEstimator(genderInterpreter, faceBitmap) }.getOrNull()
        faceBitmap.recycle()

        return@withContext AgeGenderEstimate(
            ageBracket = mapAgeBracket(ageYears),
            ageConfidence = computeAgeConfidence(ageYears, detection.confidence, config),
            gender = resolveGender(genderScores, config).first,
            genderConfidence = resolveGender(genderScores, config).second
        )
    }

    private fun obtainAgeInterpreter(model: ByteBuffer): TfLiteInterpreter? {
        val cached = ageInterpreter
        if (cached != null) return cached
        return synchronized(ageLock) {
            ageInterpreter ?: interpreterFactory.create(model.duplicateBuffer()).also {
                ageInterpreter = it
            }
        }
    }

    private fun obtainGenderInterpreter(model: ByteBuffer): TfLiteInterpreter? {
        val cached = genderInterpreter
        if (cached != null) return cached
        return synchronized(genderLock) {
            genderInterpreter ?: interpreterFactory.create(model.duplicateBuffer()).also {
                genderInterpreter = it
            }
        }
    }

    private fun runAgeEstimator(interpreter: TfLiteInterpreter, bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, AGE_INPUT_SIZE, AGE_INPUT_SIZE, true)
        val input = allocateInputBuffer(scaled, AGE_INPUT_SIZE)
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        val normalizedAge = output[0].getOrElse(0) { 0f }.coerceAtLeast(0f)
        return normalizedAge * AGE_SCALE
    }

    private fun runGenderEstimator(interpreter: TfLiteInterpreter, bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, GENDER_INPUT_SIZE, GENDER_INPUT_SIZE, true)
        val input = allocateInputBuffer(scaled, GENDER_INPUT_SIZE)
        val output = Array(1) { FloatArray(2) }
        interpreter.run(input, output)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return output[0]
    }

    private fun fallback(
        detection: DetectionResult,
        config: DetectionSessionConfig
    ): AgeGenderEstimate = AgeGenderEstimate(
        ageBracket = AgeBracket.ADULT,
        ageConfidence = maxOf(config.minAgeConfidence, detection.confidence.coerceIn(0f, 1f)),
        gender = GenderLabel.UNDETERMINED,
        genderConfidence = config.minGenderConfidence
    )

    private fun computeAgeConfidence(
        ageYears: Float?,
        detectionConfidence: Float,
        config: DetectionSessionConfig
    ): Float {
        val base = 0.55f + detectionConfidence.coerceIn(0f, 1f) * 0.4f
        val adjusted = if (ageYears != null && ageYears.isFinite()) {
            (base + 0.05f).coerceAtMost(0.98f)
        } else {
            max(0.5f, base * 0.9f)
        }
        return max(config.minAgeConfidence, adjusted.coerceIn(0f, 1f))
    }

    private fun resolveGender(
        scores: FloatArray?,
        config: DetectionSessionConfig
    ): Pair<GenderLabel, Float> {
        if (scores == null || scores.size < 2) {
            return GenderLabel.UNDETERMINED to config.minGenderConfidence
        }

        val male = scores[0].coerceIn(0f, 1f)
        val female = scores[1].coerceIn(0f, 1f)
        val delta = abs(male - female)
        val label: GenderLabel
        val confidence: Float
        if (delta < 0.08f) {
            label = GenderLabel.UNDETERMINED
            confidence = max(config.minGenderConfidence, (male + female) * 0.25f)
        } else if (male > female) {
            label = GenderLabel.MALE
            confidence = max(config.minGenderConfidence, male)
        } else {
            label = GenderLabel.FEMALE
            confidence = max(config.minGenderConfidence, female)
        }
        return label to confidence.coerceIn(0f, 1f)
    }

    private fun mapAgeBracket(ageYears: Float?): AgeBracket {
        val age = ageYears ?: return AgeBracket.ADULT
        return when {
            age < 13f -> AgeBracket.CHILD
            age < 20f -> AgeBracket.TEEN
            age < 55f -> AgeBracket.ADULT
            else -> AgeBracket.SENIOR
        }
    }

    private fun allocateInputBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val buffer = ByteBuffer
            .allocateDirect(size * size * CHANNELS * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                buffer.putFloat((pixel and 0xFF) / 255f)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun ByteBuffer.duplicateBuffer(): ByteBuffer =
        this.duplicate().apply {
            order(this@duplicateBuffer.order())
            position(0)
        }

    companion object {
        private const val AGE_INPUT_SIZE = 200
        private const val GENDER_INPUT_SIZE = 128
        private const val CHANNELS = 3
        private const val FLOAT_BYTES = 4
        private const val AGE_SCALE = 116f
    }
}

interface InterpreterFactory {
    fun create(model: ByteBuffer): TfLiteInterpreter
}

interface TfLiteInterpreter : AutoCloseable {
    fun run(input: Any, output: Any)
}

internal object ImageProxyFaceCropper : FaceCropper {
    override fun crop(frame: FrameInput, detection: DetectionResult): Bitmap? {
        val proxy = frame.imageProxy ?: return null
        val bitmap = ImageProxyUtils.toBitmap(proxy)
        val rotationDegrees = proxy.imageInfo.rotationDegrees
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                bitmap.recycle()
            }
        } else {
            bitmap
        }
        val rect = detection.boundingBox.toCropRectWithTopOffset(
            offsetPx = FACE_CROP_TOP_OFFSET_PX,
            maxWidth = rotated.width,
            maxHeight = rotated.height
        ) ?: run {
            rotated.recycle()
            return null
        }
        val crop = Bitmap.createBitmap(rotated, rect.left, rect.top, rect.width(), rect.height())
        if (rotated !== crop) {
            rotated.recycle()
        }
        return crop
    }
}

interface FaceCropper {
    fun crop(frame: FrameInput, detection: DetectionResult): Bitmap?
}

class DefaultInterpreterFactory : InterpreterFactory {
    override fun create(model: ByteBuffer): TfLiteInterpreter {
        return object : TfLiteInterpreter {
            private val delegate = org.tensorflow.lite.Interpreter(model)
            override fun run(input: Any, output: Any) {
                delegate.run(input, output)
            }

            override fun close() {
                delegate.close()
            }
        }
    }
}

internal fun BoundingBox.toCropRectWithTopOffset(
    offsetPx: Int,
    maxWidth: Int,
    maxHeight: Int
): Rect? {
    val rect = toRect()
    if (offsetPx == 0) {
        return rect.clamp(maxWidth, maxHeight)
    }
    val offsetRect = Rect(rect.left, rect.top + offsetPx, rect.right, rect.bottom + offsetPx)
    return offsetRect.clamp(maxWidth, maxHeight)
}

private fun BoundingBox.toRect(): Rect =
    Rect(
        left.toInt(),
        top.toInt(),
        right.toInt(),
        bottom.toInt()
    )

private fun Rect.clamp(maxWidth: Int, maxHeight: Int): Rect? {
    val clampedLeft = max(0, min(left, maxWidth))
    val clampedTop = max(0, min(top, maxHeight))
    val clampedRight = max(0, min(right, maxWidth))
    val clampedBottom = max(0, min(bottom, maxHeight))
    if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) {
        return null
    }
    return Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
}
