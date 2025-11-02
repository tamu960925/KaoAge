package com.kaoage.cashier.ui.selfcheckout

class SelfCheckoutPrivacyViewModel {
    val state: PrivacyState = PrivacyState(
        offlineAssurance = "On-device analysis only. No frames leave the kiosk.",
        persistentWrites = 0,
        networkTransmissions = 0
    )
}

data class PrivacyState(
    val offlineAssurance: String,
    val persistentWrites: Int,
    val networkTransmissions: Int
)
