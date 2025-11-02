package com.kaoage.sdk.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.tensorflow.lite.Interpreter

internal class AgeRegressionEstimator(private val context: Context) : Closeable {

    data class Inference(
        val ageYears: Float,
        val genderScores: FloatArray? = null
    )

    private val ageModelAvailable: Boolean = ModelAssetManager.hasAgeModel(context)
    private val genderModelAvailable: Boolean = ModelAssetManager.hasGenderModel(context)

    private val ageInterpreter: Interpreter? by lazy {
        if (!ageModelAvailable) return@lazy null
        Interpreter(
            ModelAssetManager.loadAgeModelByteBuffer(context),
            Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            }
        )
    }

    private val genderInterpreter: Interpreter? by lazy {
        if (!genderModelAvailable) return@lazy null
        Interpreter(
            ModelAssetManager.loadGenderModelByteBuffer(context),
            Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            }
        )
    }

    private val ageInputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(AGE_INPUT_BYTE_SIZE).order(ByteOrder.nativeOrder())
    private val genderInputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(GENDER_INPUT_BYTE_SIZE).order(ByteOrder.nativeOrder())

    fun infer(
        imageProxy: ImageProxy,
        boundingBox: BoundingBox,
        rotationDegrees: Int
    ): Inference? {
        val hasAnyModel = ageModelAvailable || genderModelAvailable
        if (!hasAnyModel) {
            return null
        }

        val bitmap = ImageProxyUtils.toBitmap(imageProxy)
        val rotated = rotateBitmap(bitmap, rotationDegrees)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }

        val cropRect = boundingBox.toRect().clamp(rotated.width, rotated.height) ?: run {
            rotated.recycle()
            return null
        }

        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            rotated.recycle()
            return null
        }

        val faceBitmap = Bitmap.createBitmap(
            rotated,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
        rotated.recycle()

        val ageBitmap = Bitmap.createScaledBitmap(faceBitmap, AGE_INPUT_SIZE, AGE_INPUT_SIZE, true)
        val genderBitmap = Bitmap.createScaledBitmap(faceBitmap, GENDER_INPUT_SIZE, GENDER_INPUT_SIZE, true)

        try {
            val ageYears = runAgeModel(ageInterpreter, ageBitmap)
            val genderScores = runGenderModel(genderInterpreter, genderBitmap)
            if (ageYears == null && genderScores == null) {
                return null
            }
            return Inference(
                ageYears = ageYears ?: Float.NaN,
                genderScores = genderScores
            )
        } catch (t: Throwable) {
            return null
        } finally {
            if (ageBitmap !== faceBitmap) {
                ageBitmap.recycle()
            }
            if (genderBitmap !== faceBitmap && genderBitmap !== ageBitmap) {
                genderBitmap.recycle()
            }
            faceBitmap.recycle()
            ageInputBuffer.rewind()
            genderInputBuffer.rewind()
        }
    }

    private fun prepareInputBuffer(bitmap: Bitmap, size: Int, buffer: ByteBuffer) {
        buffer.rewind()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            }
        }
    }

    private fun runAgeModel(interpreter: Interpreter?, bitmap: Bitmap): Float? {
        if (interpreter == null) return null
        prepareInputBuffer(bitmap, AGE_INPUT_SIZE, ageInputBuffer)
        val output = Array(1) { FloatArray(1) }
        interpreter.run(ageInputBuffer, output)
        val normalized = output[0].getOrNull(0)?.coerceAtLeast(0f) ?: return null
        return normalized * AGE_SCALE
    }

    private fun runGenderModel(interpreter: Interpreter?, bitmap: Bitmap): FloatArray? {
        if (interpreter == null) return null
        prepareInputBuffer(bitmap, GENDER_INPUT_SIZE, genderInputBuffer)
        val output = Array(1) { FloatArray(2) }
        interpreter.run(genderInputBuffer, output)
        return output[0].copyOf()
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun BoundingBox.toRect(): Rect =
        Rect(
            left.roundToInt(),
            top.roundToInt(),
            right.roundToInt(),
            bottom.roundToInt()
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

    override fun close() {
        ageInterpreter?.close()
        genderInterpreter?.close()
    }

    companion object {
        private const val AGE_INPUT_SIZE = 200
        private const val GENDER_INPUT_SIZE = 128
        private const val CHANNELS = 3
        private const val FLOAT_BYTES = 4
        private const val AGE_INPUT_BYTE_SIZE = AGE_INPUT_SIZE * AGE_INPUT_SIZE * CHANNELS * FLOAT_BYTES
        private const val GENDER_INPUT_BYTE_SIZE = GENDER_INPUT_SIZE * GENDER_INPUT_SIZE * CHANNELS * FLOAT_BYTES
        private const val AGE_SCALE = 116f
    }
}
