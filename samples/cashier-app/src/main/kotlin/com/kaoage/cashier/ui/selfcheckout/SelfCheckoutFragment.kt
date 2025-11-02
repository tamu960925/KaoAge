package com.kaoage.cashier.ui.selfcheckout

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kaoage.cashier.R
import com.kaoage.cashier.ui.overlay.CashierOverlayViewModel
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint

class SelfCheckoutFragment : Fragment(R.layout.fragment_self_checkout) {

    private val overlayViewModel = CashierOverlayViewModel()
    private val privacyViewModel = SelfCheckoutPrivacyViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = demoResult()
        val overlayState = overlayViewModel.present(result)
        val privacyState = privacyViewModel.state

        view.findViewById<TextView>(R.id.overlaySummary).text = overlayState.summary
        view.findViewById<TextView>(R.id.overlayConfidence).text = overlayState.confidence
        view.findViewById<TextView>(R.id.offlineMessage).text = privacyState.offlineAssurance
        view.findViewById<TextView>(R.id.overlayOfflineMessage).text = overlayState.overlayFooter
    }

    private fun demoResult(): FaceInsightsResult {
        val landmarks = LandmarkSet(
            mapOf(
                LandmarkType.LEFT_EYE to NormalizedPoint(0.3f, 0.3f, 0.8f),
                LandmarkType.RIGHT_EYE to NormalizedPoint(0.6f, 0.3f, 0.8f),
                LandmarkType.NOSE_TIP to NormalizedPoint(0.45f, 0.45f, 0.9f),
                LandmarkType.MOUTH_LEFT to NormalizedPoint(0.35f, 0.65f, 0.85f),
                LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.65f, 0.65f, 0.85f)
            )
        )
        return FaceInsightsResult(
            detectionId = "demo-detection",
            frameTimestampMillis = System.currentTimeMillis(),
            boundingBox = BoundingBox(80f, 120f, 380f, 520f, 1080, 1920),
            eulerAngles = EulerAngles(0f, 0f, 0f),
            landmarks = landmarks,
            faceConfidence = 0.92f,
            ageBracket = AgeBracket.ADULT,
            ageConfidence = 0.88f,
            gender = GenderLabel.FEMALE,
            genderConfidence = 0.84f,
            bestShotEligible = true
        )
    }
}
