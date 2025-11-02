package com.kaoage.sdk.core

import android.content.Context
import android.graphics.Bitmap
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter
import kotlin.math.min

internal class MobileNetClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val outputScale: Float,
    private val outputZeroPoint: Int
) {

    data class Result(
        val topK: List<Pair<String, Float>>
    )

    fun classify(bitmap: Bitmap): Result {
        val input = preprocess(bitmap)
        val output = Array(1) { ByteArray(labels.size) }
        interpreter.run(input, output)
        val probabilities = dequantize(output[0])
        val top = probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(5)
            .map { index -> labels.getOrElse(index) { "label-$index" } to probabilities[index] }
        return Result(top)
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        }
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        buffer.order(ByteOrder.nativeOrder())
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val pixel = scaled.getPixel(x, y)
                buffer.put(((pixel shr 16) and 0xFF).toByte())
                buffer.put(((pixel shr 8) and 0xFF).toByte())
                buffer.put((pixel and 0xFF).toByte())
            }
        }
        buffer.rewind()
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return buffer
    }

    private fun dequantize(output: ByteArray): FloatArray {
        val probs = FloatArray(output.size)
        for (i in output.indices) {
            val value = output[i].toInt() and 0xFF
            probs[i] = (value - outputZeroPoint) * outputScale
        }
        // Ensure probabilities are positive and normalized for downstream logic
        val clipped = probs.map { maxOf(it, 0f) }
        val sum = clipped.sum().takeIf { it > 0f } ?: 1f
        return FloatArray(clipped.size) { clipped[it] / sum }
    }

    companion object {
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3
        private const val LABELS_FILE = "mobilenet_labels.txt"
        @Volatile
        private var instance: MobileNetClassifier? = null

        fun getInstance(context: Context): MobileNetClassifier {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }
        }

        fun close() {
            synchronized(this) {
                instance?.interpreter?.close()
                instance = null
            }
        }

        private fun create(context: Context): MobileNetClassifier {
            val modelBuffer = ModelAssetManager.loadModelByteBuffer(context)
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            }
            val interpreter = Interpreter(modelBuffer, options)
            val outputTensor = interpreter.getOutputTensor(0)
            val quantParams = outputTensor.quantizationParams()
            val labels = loadLabels(context)
            return MobileNetClassifier(interpreter, labels, quantParams.scale, quantParams.zeroPoint)
        }

        private fun loadLabels(context: Context): List<String> {
            context.assets.open(LABELS_FILE).use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    return reader.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                }
            }
        }
    }
}
