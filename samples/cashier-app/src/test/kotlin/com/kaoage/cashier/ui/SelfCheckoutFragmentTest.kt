package com.kaoage.cashier.ui

import android.app.Application
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.kaoage.cashier.R
import com.kaoage.cashier.ui.selfcheckout.SelfCheckoutFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.SupportFragmentController
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SelfCheckoutFragmentTest {

    @Before
    fun setUpTheme() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.setTheme(R.style.Theme_KaoAgeCashier)
    }

    @Test
    fun `demo scan populates overlay summary`() {
        val fragment = SelfCheckoutFragment()
        SupportFragmentController.of(fragment)
            .create()
            .start()
            .resume()
            .visible()

        val root = fragment.requireView()
        val summary = root.findViewById<TextView>(R.id.overlaySummary)
        val privacyAssurance = root.findViewById<TextView>(R.id.offlineMessage)
        val overlayOffline = root.findViewById<TextView>(R.id.overlayOfflineMessage)

        assertTrue(summary.text.contains("ADULT"), "Summary should contain age bracket")
        assertTrue(summary.text.contains("FEMALE"), "Summary should contain gender label")
        assertTrue(privacyAssurance.text.contains("On-device"), "Privacy message should highlight on-device processing")
        assertTrue(overlayOffline.text.contains("Offline"), "Overlay footer should reinforce offline processing")
    }
}
