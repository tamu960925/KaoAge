package com.kaoage.cashier.ui.selfcheckout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kaoage.cashier.R
import com.kaoage.cashier.ui.overlay.CashierOverlayViewModel
import com.kaoage.sdk.core.model.AgeBracket
import com.kaoage.sdk.core.model.BestShotReason
import com.kaoage.sdk.core.model.BoundingBox
import com.kaoage.sdk.core.model.EulerAngles
import com.kaoage.sdk.core.model.FaceInsightsResult
import com.kaoage.sdk.core.model.GenderLabel
import com.kaoage.sdk.core.model.LandmarkSet
import com.kaoage.sdk.core.model.LandmarkType
import com.kaoage.sdk.core.model.NormalizedPoint

class SelfCheckoutFragment : Fragment() {
    private val privacyViewModel = SelfCheckoutPrivacyViewModel()
    private val overlayViewModel = CashierOverlayViewModel()

    private val demoResult = FaceInsightsResult(
        detectionId = "demo-001",
        frameTimestampMillis = 0L,
        boundingBox = BoundingBox(
            left = 240f,
            top = 160f,
            right = 840f,
            bottom = 960f,
            imageWidth = 1080,
            imageHeight = 1920
        ),
        eulerAngles = EulerAngles(
            yaw = 4f,
            pitch = -2f,
            roll = 1.5f
        ),
        landmarks = LandmarkSet(
            mapOf(
                LandmarkType.LEFT_EYE to NormalizedPoint(0.32f, 0.34f, 0.92f),
                LandmarkType.RIGHT_EYE to NormalizedPoint(0.64f, 0.33f, 0.91f),
                LandmarkType.NOSE_TIP to NormalizedPoint(0.49f, 0.48f, 0.93f),
                LandmarkType.MOUTH_LEFT to NormalizedPoint(0.38f, 0.66f, 0.88f),
                LandmarkType.MOUTH_RIGHT to NormalizedPoint(0.62f, 0.66f, 0.87f)
            )
        ),
        faceConfidence = 0.92f,
        ageBracket = AgeBracket.ADULT,
        ageConfidence = 0.86f,
        gender = GenderLabel.FEMALE,
        genderConfidence = 0.81f,
        bestShotEligible = true,
        bestShotReasons = setOf(BestShotReason.CONFIDENCE_PEAK)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_self_checkout, container, false)
        val offlineMessage = view.findViewById<TextView>(R.id.offlineMessage)
        offlineMessage.text = privacyViewModel.state.offlineAssurance

        val overlaySummary = view.findViewById<TextView>(R.id.overlaySummary)
        val overlayOffline = view.findViewById<TextView>(R.id.overlayOfflineMessage)
        val overlayState = overlayViewModel.present(demoResult)
        overlaySummary.text = overlayState.summary
        overlayOffline.text = overlayState.offlineMessage

        return view
    }
}
