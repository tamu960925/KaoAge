package com.kaoage.sdk.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MobileNetClassifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun classify_returnsNormalizedProbabilities() {
        assumeTrue(canLoadTensorFlow())
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val result = MobileNetClassifier.getInstance(context).classify(bitmap)
        val top = result.topK.first()
        assertTrue(top.second in 0f..1f)
        val sum = result.topK.sumOf { it.second.toDouble() }
        assertTrue(sum <= 1.0)
    }

    @Test
    fun close_releasesInterpreter() {
        assumeTrue(canLoadTensorFlow())
        MobileNetClassifier.close()
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        val result = MobileNetClassifier.getInstance(context).classify(bitmap)
        assertEquals(5, result.topK.size)
    }

    private fun canLoadTensorFlow(): Boolean {
        return try {
            MobileNetClassifier.getInstance(context)
            true
        } catch (error: UnsatisfiedLinkError) {
            false
        }
    }
}
