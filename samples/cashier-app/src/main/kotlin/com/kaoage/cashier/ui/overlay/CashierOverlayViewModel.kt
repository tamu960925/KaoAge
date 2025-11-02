package com.kaoage.cashier.ui.overlay

import com.kaoage.sdk.core.model.FaceInsightsResult
import kotlin.math.roundToInt

class CashierOverlayViewModel {

    data class UiState(
        val summary: String,
        val confidence: String,
        val offlineMessage: String,
        val overlayFooter: String
    )

    fun present(result: FaceInsightsResult): UiState {
        val ageLabel = result.ageBracket.name
        val genderLabel = result.gender.name
        val summary = buildString {
            append(ageLabel)
            append(" • ")
            append(genderLabel)
            append(" • confidence ")
            append((result.faceConfidence * 100).roundToInt())
            append("%")
        }
        val confidence = "Age ${result.ageConfidence.toPercent()} • Gender ${result.genderConfidence.toPercent()}"
        val offlineMessage = "Offline capture ready — Offline analysis only"
        val footer = "Offline assurance: JSON export stays local"
        return UiState(
            summary = summary,
            confidence = confidence,
            offlineMessage = offlineMessage,
            overlayFooter = footer
        )
    }

    private fun Float.toPercent(): String = "${(this * 100).roundToInt()}%"
}
