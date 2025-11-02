package com.kaoage.sdk.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Provides access to sanctioned on-device models that are packaged with the SDK.
 * Exposes helpers usable from both Kotlin and Java callers.
 */
object ModelAssetManager {
    private const val AGE_MODEL_ASSET_NAME = "model_age_nonq.tflite"
    private const val GENDER_MODEL_ASSET_NAME = "model_gender_nonq.tflite"

    /**
     * Maps the age regression model into memory for high-performance inference access.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun loadAgeModelByteBuffer(context: Context): ByteBuffer =
        mapAsset(context, AGE_MODEL_ASSET_NAME)

    /**
     * Maps the gender classification model into memory for high-performance inference access.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun loadGenderModelByteBuffer(context: Context): ByteBuffer =
        mapAsset(context, GENDER_MODEL_ASSET_NAME)

    /**
     * Returns true when the age regression model asset is bundled with the SDK.
     */
    @JvmStatic
    fun hasAgeModel(context: Context): Boolean = runCatching {
        context.assets.openFd(AGE_MODEL_ASSET_NAME).close()
    }.isSuccess

    /**
     * Returns true when the gender classification model asset is bundled with the SDK.
     */
    @JvmStatic
    fun hasGenderModel(context: Context): Boolean = runCatching {
        context.assets.openFd(GENDER_MODEL_ASSET_NAME).close()
    }.isSuccess

    @JvmStatic
    @Throws(IOException::class)
    suspend fun copyAgeModelToCache(context: Context): File =
        copyAssetToCache(context, AGE_MODEL_ASSET_NAME)

    @JvmStatic
    @Throws(IOException::class)
    suspend fun copyGenderModelToCache(context: Context): File =
        copyAssetToCache(context, GENDER_MODEL_ASSET_NAME)

    private suspend fun copyAssetToCache(context: Context, assetName: String): File =
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "kaoage-models").apply { mkdirs() }
            val targetFile = File(cacheDir, assetName)
            if (targetFile.exists()) {
                return@withContext targetFile
            }
            context.assets.open(assetName).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        }

    @Throws(IOException::class)
    private fun mapAsset(context: Context, assetName: String): ByteBuffer =
        context.assets.openFd(assetName).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.length
                ).apply { order(ByteOrder.nativeOrder()) }
            }
        }
}
