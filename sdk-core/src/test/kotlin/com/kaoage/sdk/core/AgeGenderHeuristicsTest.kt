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

    @Test
    fun `size ratio fallback maps to child bracket`() {
        val (ageBracket, confidence) = AgeGenderHeuristics.estimateAge(
            sizeRatio = 0.08f,
            inference = null
        )

        assertEquals(AgeBracket.CHILD, ageBracket)
        assertTrue(confidence in 0.2f..0.95f)
    }

    @Test
    fun `gender heuristic falls back to neutral when signals align`() {
        val (gender, confidence) = AgeGenderHeuristics.estimateGender(
            leftEyeOpen = 0.5f,
            rightEyeOpen = 0.52f,
            smilingProbability = 0.51f,
            inference = null
        )

        assertEquals(Gender.UNDETERMINED, gender)
        assertTrue(confidence <= 0.21f, "Neutral confidence should remain low")
    }
}
