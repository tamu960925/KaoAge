package com.kaoage.cashier.ui

import com.kaoage.cashier.ui.overlay.CashierOverlayViewModel
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint
import org.junit.Test
import kotlin.test.assertTrue

class CashierFlowTest {

    @Test
    fun `overlay view model surfaces on-device demographics`() {
        val result = FaceInsightsResult(
            detectionId = "det-123",
            frameTimestampMillis = 42L,
            boundingBox = BoundingBox(0f, 0f, 100f, 120f, 1080, 1920),
            eulerAngles = EulerAngles(0f, 0f, 0f),
            landmarks = LandmarkSet(
                mapOf(
                    LandmarkType.LEFT_EYE to NormalizedPoint(0.3f, 0.3f, 0.8f),
                    LandmarkType.RIGHT_EYE to NormalizedPoint(0.6f, 0.3f, 0.8f),
                    LandmarkType.NOSE_TIP to NormalizedPoint(0.45f, 0.45f, 0.8f),
                    LandmarkType.MOUTH_LEFT to NormalizedPoint(0.35f, 0.65f, 0.8f),
                    LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.65f, 0.65f, 0.8f)
                )
            ),
            faceConfidence = 0.9f,
            ageBracket = AgeBracket.ADULT,
            ageConfidence = 0.88f,
            gender = GenderLabel.FEMALE,
            genderConfidence = 0.82f,
            bestShotEligible = true
        )

        val viewModel = CashierOverlayViewModel()
        val state = viewModel.present(result)

        assertTrue(state.summary.contains("ADULT"))
        assertTrue(state.summary.contains("FEMALE"))
        assertTrue(state.offlineMessage.contains("Offline"))
    }
}
