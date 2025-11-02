package com.kaoage.sdk.core

import com.kaoage.sdk.core.AgeGenderHeuristics
import com.kaoage.sdk.core.Gender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgeGenderHeuristicsTest {

    @Test
    fun `inference gender scores override heuristics`() {
        val inference = AgeRegressionEstimator.Inference(
            ageYears = 28f,
            genderScores = floatArrayOf(0.25f, 0.72f)
        )

        val (gender, confidence) = AgeGenderHeuristics.estimateGender(
            leftEyeOpen = null,
            rightEyeOpen = null,
            smilingProbability = null,
            inference = inference
        )

        assertEquals(Gender.FEMALE, gender)
        assertTrue(confidence >= 0.72f - 1e-3f, "Model probability should drive confidence, actual=$confidence")
    }
}
