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
    fun loadAgeModel_whenAvailable() {
        if (!ModelAssetManager.hasAgeModel(context)) {
            return
        }
        val buffer = ModelAssetManager.loadAgeModelByteBuffer(context)
        assertTrue(buffer.capacity() > 0, "Age model buffer must not be empty")
    }

    @Test
    fun loadGenderModel_whenAvailable() {
        if (!ModelAssetManager.hasGenderModel(context)) {
            return
        }
        val buffer = ModelAssetManager.loadGenderModelByteBuffer(context)
        assertTrue(buffer.capacity() > 0, "Gender model buffer must not be empty")
    }

    @Test
    fun copyAgeModelToCache_whenAvailable() = runBlocking {
        if (!ModelAssetManager.hasAgeModel(context)) {
            return@runBlocking
        }
        val cacheFile = ModelAssetManager.copyAgeModelToCache(context)
        assertTrue(cacheFile.exists(), "Cached age model should exist")
        val buffer = ModelAssetManager.loadAgeModelByteBuffer(context)
        assertEquals(buffer.capacity().toLong(), cacheFile.length(), "Cache file length should match buffer size")
    }

    @Test
    fun copyGenderModelToCache_whenAvailable() = runBlocking {
        if (!ModelAssetManager.hasGenderModel(context)) {
            return@runBlocking
        }
        val cacheFile = ModelAssetManager.copyGenderModelToCache(context)
        assertTrue(cacheFile.exists(), "Cached gender model should exist")
        val buffer = ModelAssetManager.loadGenderModelByteBuffer(context)
        assertEquals(buffer.capacity().toLong(), cacheFile.length(), "Cache file length should match buffer size")
    }
}
