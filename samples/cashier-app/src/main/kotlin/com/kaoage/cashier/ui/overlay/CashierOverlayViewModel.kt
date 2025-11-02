package com.kaoage.cashier.ui.overlay

import com.kaoage.sdk.core.model.FaceInsightsResult

class CashierOverlayViewModel {
    fun present(result: FaceInsightsResult): CashierOverlayState {
        val summary = "${result.ageBracket} • ${result.gender} (conf ${"%.0f".format(result.faceConfidence * 100)}%)"
        val offlineMessage = "Offline • On-device processing"
        return CashierOverlayState(summary = summary, offlineMessage = offlineMessage)
    }
}

data class CashierOverlayState(
    val summary: String,
    val offlineMessage: String
)
