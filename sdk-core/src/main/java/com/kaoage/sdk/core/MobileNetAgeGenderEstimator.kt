package com.kaoage.sdk.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class MobileNetAgeGenderEstimator(private val context: Context) : Closeable {

    data class Inference(
        val ageBracket: AgeBracket?,
        val ageConfidence: Float?,
        val gender: Gender?,
        val genderConfidence: Float?,
        val topLabel: String?,
        val topProbability: Float?
    )

    fun infer(
        imageProxy: ImageProxy,
        boundingBox: BoundingBox,
        rotationDegrees: Int
    ): Inference? {
        val bitmap = ImageProxyUtils.toBitmap(imageProxy)
        val rotated = rotateBitmap(bitmap, rotationDegrees)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }

        val cropRect = boundingBox.toRect().intersecting(rotated.width, rotated.height) ?: return null
        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            rotated.recycle()
            return null
        }

        val faceCrop = Bitmap.createBitmap(rotated, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        rotated.recycle()

        val classifier = MobileNetClassifier.getInstance(context)
        val result = classifier.classify(faceCrop)
        faceCrop.recycle()

        val match = result.topK.firstOrNull { hasRelevantKeyword(it.first) }
        val target = match ?: result.topK.firstOrNull() ?: return null
        val age = inferAge(target)
        val gender = inferGender(target)
        return Inference(
            ageBracket = age?.first,
            ageConfidence = age?.second ?: target.second,
            gender = gender?.first,
            genderConfidence = gender?.second ?: target.second,
            topLabel = target.first,
            topProbability = target.second
        )
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun BoundingBox.toRect(): Rect {
        return Rect(
            left.roundToInt(),
            top.roundToInt(),
            right.roundToInt(),
            bottom.roundToInt()
        )
    }

    private fun Rect.intersecting(maxWidth: Int, maxHeight: Int): Rect? {
        val clampedLeft = max(0, min(left, maxWidth))
        val clampedTop = max(0, min(top, maxHeight))
        val clampedRight = max(0, min(right, maxWidth))
        val clampedBottom = max(0, min(bottom, maxHeight))
        if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) {
            return null
        }
        return Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    private fun inferAge(top: Pair<String, Float>): Pair<AgeBracket, Float>? {
        val label = top.first.lowercase()
        val confidence = top.second
        val bracket = when {
            AGE_CHILD_KEYWORDS.any { label.contains(it) } -> AgeBracket.CHILD
            AGE_TEEN_KEYWORDS.any { label.contains(it) } -> AgeBracket.TEEN
            AGE_SENIOR_KEYWORDS.any { label.contains(it) } -> AgeBracket.SENIOR
            AGE_ADULT_KEYWORDS.any { label.contains(it) } -> AgeBracket.ADULT
            else -> null
        }
        return bracket?.let { it to confidence }
    }

    private fun inferGender(top: Pair<String, Float>): Pair<Gender, Float>? {
        val label = top.first.lowercase()
        val confidence = top.second
        val gender = when {
            FEMALE_KEYWORDS.any { label.contains(it) } -> Gender.FEMALE
            MALE_KEYWORDS.any { label.contains(it) } -> Gender.MALE
            else -> null
        }
        return gender?.let { it to confidence }
    }

    private fun hasRelevantKeyword(label: String): Boolean {
        val lower = label.lowercase()
        return AGE_CHILD_KEYWORDS.any { lower.contains(it) } ||
            AGE_TEEN_KEYWORDS.any { lower.contains(it) } ||
            AGE_ADULT_KEYWORDS.any { lower.contains(it) } ||
            AGE_SENIOR_KEYWORDS.any { lower.contains(it) } ||
            FEMALE_KEYWORDS.any { lower.contains(it) } ||
            MALE_KEYWORDS.any { lower.contains(it) }
    }

    override fun close() {
        MobileNetClassifier.close()
    }

    companion object {
        private val AGE_CHILD_KEYWORDS = listOf("baby", "infant", "newborn", "toddler")
        private val AGE_TEEN_KEYWORDS = listOf("boy", "girl", "teen", "adolescent")
        private val AGE_ADULT_KEYWORDS = listOf("man", "woman", "person", "bride", "groom", "adult")
        private val AGE_SENIOR_KEYWORDS = listOf("grandma", "grandpa", "elder", "old woman", "old man", "senior")
        private val FEMALE_KEYWORDS = listOf("woman", "lady", "female", "bride", "girl")
        private val MALE_KEYWORDS = listOf("man", "male", "groom", "boy")
    }
}
