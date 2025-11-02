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
        val ageYears: Float
    )

    private val ageModelAvailable: Boolean = ModelAssetManager.hasAgeModel(context)

    private val interpreter: Interpreter? by lazy {
        if (!ageModelAvailable) return@lazy null
        Interpreter(
            ModelAssetManager.loadAgeModelByteBuffer(context),
            Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            }
        )
    }

    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(INPUT_BYTE_SIZE).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(OUTPUT_BYTE_SIZE).order(ByteOrder.nativeOrder())

    fun infer(
        imageProxy: ImageProxy,
        boundingBox: BoundingBox,
        rotationDegrees: Int
    ): Inference? {
        val interpreter = interpreter ?: return null

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

        val scaled = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        if (scaled !== faceBitmap) {
            faceBitmap.recycle()
        }

        try {
            prepareInputBuffer(scaled)
            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val age = outputBuffer.float
            return Inference(ageYears = age)
        } catch (t: Throwable) {
            return null
        } finally {
            scaled.recycle()
            inputBuffer.rewind()
            outputBuffer.rewind()
        }
    }

    private fun prepareInputBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val pixel = bitmap.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                inputBuffer.putFloat(normalizeChannel(r))
                inputBuffer.putFloat(normalizeChannel(g))
                inputBuffer.putFloat(normalizeChannel(b))
            }
        }
    }

    private fun normalizeChannel(value: Int): Float =
        (value / 127.5f) - 1f

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
        interpreter?.close()
    }

    companion object {
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
        private const val FLOAT_BYTES = 4
        private const val INPUT_BYTE_SIZE = INPUT_SIZE * INPUT_SIZE * CHANNELS * FLOAT_BYTES
        private const val OUTPUT_BYTE_SIZE = FLOAT_BYTES
    }
}
