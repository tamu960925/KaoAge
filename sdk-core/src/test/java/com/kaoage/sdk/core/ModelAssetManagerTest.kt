package com.kaoage.sdk.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModelAssetManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun loadModelByteBuffer_returnsNonEmptyBuffer() {
        val buffer = ModelAssetManager.loadModelByteBuffer(context)
        assertTrue(buffer.capacity() > 0, "Model buffer must not be empty")
    }

    @Test
    fun copyModelToCache_persistsModel() = runBlocking {
        val cacheFile = ModelAssetManager.copyModelToCache(context)
        assertTrue(cacheFile.exists(), "Cached model file should exist")
        val buffer = ModelAssetManager.loadModelByteBuffer(context)
        assertEquals(buffer.capacity().toLong(), cacheFile.length(), "Cache file length should match buffer size")
    }
}
