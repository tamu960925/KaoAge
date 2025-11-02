package com.kaoage.cashier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kaoage.cashier.ui.overlay.CashierOverlayViewModel
import com.kaoage.cashier.ui.selfcheckout.SelfCheckoutFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SelfCheckoutFragment())
            .commit()
    }
}
