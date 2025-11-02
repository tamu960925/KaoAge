package com.kaoage.sdk.core

import android.content.Context
import android.content.res.AssetFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Provides access to sanctioned on-device models that are packaged with the SDK.
 * Exposes helpers usable from both Kotlin and Java callers.
 */
object ModelAssetManager {
    private const val MODEL_ASSET_NAME = "mobilenet_v1_1.0_224_quant.tflite"

    /**
     * Returns an [AssetFileDescriptor] for the bundled MobileNet model.
     * Callers must close the descriptor when finished.
     */
    @JvmStatic
    fun openModelDescriptor(context: Context): AssetFileDescriptor =
        context.assets.openFd(MODEL_ASSET_NAME)

    /**
     * Creates (or reuses) a cache file that hosts the MobileNet model.
     * The file lives under the app's cache directory so it never leaves the device.
     */
    @JvmStatic
    @Throws(IOException::class)
    suspend fun copyModelToCache(context: Context): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "kaoage-models").apply { mkdirs() }
        val targetFile = File(cacheDir, MODEL_ASSET_NAME)
        if (targetFile.exists()) {
            return@withContext targetFile
        }
        context.assets.open(MODEL_ASSET_NAME).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    }

    /**
     * Maps the MobileNet model into memory for high-performance inference access.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun loadModelByteBuffer(context: Context): ByteBuffer =
        openModelDescriptor(context).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
        }
}
