package com.kaoage.sdk.core.inference

import android.graphics.Bitmap
import android.graphics.Color
import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.detection.FrameInput
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import java.nio.ByteBuffer
import kotlin.collections.ArrayDeque
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AgeGenderEngineTest {

    @AfterTest
    fun tearDown() {
        QueuedInterpreterFactory.reset()
    }

    @Test
    fun estimate_usesInterpreterOutputs() = runBlocking {
        QueuedInterpreterFactory.enqueueResult(arrayOf(floatArrayOf(0.1f))) // 0.1 * 116 ≈ 11.6 years
        QueuedInterpreterFactory.enqueueResult(arrayOf(floatArrayOf(0.3f, 0.7f))) // male, female probabilities

        val engine = AgeGenderEngine(
            ageModel = ByteBuffer.allocateDirect(16),
            genderModel = ByteBuffer.allocateDirect(16),
            interpreterFactory = QueuedInterpreterFactory(),
            cropper = FakeFaceCropper
        )

        val detection = DetectionResult(
            detectionId = "d1",
            timestampMillis = 0L,
            boundingBox = BoundingBox(0f, 0f, 10f, 10f, 10, 10),
            eulerAngles = EulerAngles(0f, 0f, 0f),
            landmarks = LandmarkSet(
                LandmarkType.values().associateWith { NormalizedPoint(0.5f, 0.5f) }
            ),
            confidence = 0.8f
        )

        val estimate = engine.estimate(
            frame = FrameInput(timestampMillis = 0L),
            detection = detection,
            config = DetectionSessionConfig()
        )

        assertEquals(AgeBracket.CHILD, estimate.ageBracket, "Model output should map to child bracket")
        assertTrue(estimate.ageConfidence >= 0.6f, "Age confidence should respect minimum threshold")
        assertEquals(GenderLabel.FEMALE, estimate.gender, "Highest probability index should map to female")
        assertEquals(0.7f, estimate.genderConfidence, 1e-3f, "Gender confidence should match model probability")
    }

    private object FakeFaceCropper : FaceCropper {
        override fun crop(frame: FrameInput, detection: DetectionResult): Bitmap? =
            Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
    }

    private class QueuedInterpreterFactory : InterpreterFactory {
        companion object {
            private val queue: ArrayDeque<Array<FloatArray>> = ArrayDeque()

            fun enqueueResult(result: Array<FloatArray>) {
                queue.addLast(result.map { it.copyOf() }.toTypedArray())
            }

            fun reset() {
                queue.clear()
            }
        }

        override fun create(model: ByteBuffer): TfLiteInterpreter {
            return object : TfLiteInterpreter {
                override fun run(input: Any, output: Any) {
                    val tensors = if (queue.isEmpty()) {
                        error("No queued outputs for interpreter invocation")
                    } else {
                        queue.removeFirst()
                    }
                    val target = output as? Array<*> ?: error("Unsupported output type: ${output::class}")
                    target.forEachIndexed { index, buffer ->
                        val destination = buffer as? FloatArray ?: error("Unsupported tensor container")
                        val source = tensors.getOrNull(index) ?: FloatArray(destination.size)
                        for (i in destination.indices) {
                            destination[i] = source.getOrElse(i) { 0f }
                        }
                    }
                }

                override fun close() {
                    // no-op
                }
            }
        }
    }
}
