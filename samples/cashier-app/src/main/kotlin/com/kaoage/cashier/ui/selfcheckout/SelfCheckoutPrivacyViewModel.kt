package com.kaoage.cashier.ui.selfcheckout

class SelfCheckoutPrivacyViewModel {

    data class PrivacyState(
        val offlineAssurance: String,
        val persistentWrites: Int,
        val networkTransmissions: Int
    )

    val state: PrivacyState = PrivacyState(
        offlineAssurance = "On-device processing only. Offline fallback is always active.",
        persistentWrites = 0,
        networkTransmissions = 0
    )
}
