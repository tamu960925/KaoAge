package com.kaoage.cashier.privacy

import com.kaoage.cashier.ui.selfcheckout.SelfCheckoutPrivacyViewModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelfCheckoutPrivacyTest {

    @Test
    fun `privacy view model exposes offline assurance`() {
        val viewModel = SelfCheckoutPrivacyViewModel()
        val state = viewModel.state
        assertTrue(state.offlineAssurance.contains("On-device"))
        assertEquals(0, state.persistentWrites)
        assertEquals(0, state.networkTransmissions)
    }
}
