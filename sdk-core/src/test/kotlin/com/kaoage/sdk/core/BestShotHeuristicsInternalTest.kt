package com.kaoage.sdk.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BestShotHeuristicsInternalTest {

    private val heuristicsClass =
        Class.forName("com.kaoage.sdk.core.BestShotHeuristics")
    private val heuristicsInstance = heuristicsClass.getDeclaredField("INSTANCE").apply {
        isAccessible = true
    }.get(null)
    private val evaluateMethod = heuristicsClass.getDeclaredMethod(
        "evaluate",
        java.lang.Float.TYPE,
        EulerAngles::class.java,
        SessionConfig::class.java,
        AgeRegressionEstimator.Inference::class.java
    ).apply { isAccessible = true }

    @Test
    fun `stable pose and strong confidence mark frame as eligible`() {
        val result = evaluate(
            faceConfidence = 0.9f,
            yaw = 4f,
            pitch = 3f,
            session = SessionConfig(minFaceConfidence = 0.6f),
            inference = null
        )

        assertTrue(result.eligible)
        assertTrue(result.reasons.contains(BestShotReason.FACE_STABLE))
        assertTrue(result.reasons.contains(BestShotReason.CONFIDENCE_PEAK))
    }

    @Test
    fun `best shot disabled keeps reasons but blocks eligibility`() {
        val result = evaluate(
            faceConfidence = 0.95f,
            yaw = 0f,
            pitch = 0f,
            session = SessionConfig(enableBestShot = false),
            inference = AgeRegressionEstimator.Inference(ageYears = 25f, genderScores = null)
        )

        assertFalse(result.eligible)
        assertTrue(result.reasons.isNotEmpty())
    }

    private fun evaluate(
        faceConfidence: Float,
        yaw: Float,
        pitch: Float,
        session: SessionConfig,
        inference: AgeRegressionEstimator.Inference?
    ): ResultView {
        val raw = evaluateMethod.invoke(
            heuristicsInstance,
            faceConfidence,
            EulerAngles(yaw, pitch, 0f),
            session,
            inference
        )
        val resultClass = raw.javaClass
        val eligible = resultClass.getDeclaredMethod("getEligible").apply {
            isAccessible = true
        }.invoke(raw) as Boolean
        @Suppress("UNCHECKED_CAST")
        val reasons = resultClass.getDeclaredMethod("getReasons").apply {
            isAccessible = true
        }.invoke(raw) as List<BestShotReason>
        return ResultView(eligible, reasons)
    }

    private data class ResultView(
        val eligible: Boolean,
        val reasons: List<BestShotReason>
    )
}
